package framework;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.SystemGenerator.CS_LENGTH_RANGE;
import generatorTools.SystemGenerator.RESOURCES_RANGE;
import implementationAwareAnalysis.IAFIFONP;
import implementationAwareAnalysis.IAFIFOP;
import implementationAwareAnalysis.IANewMrsPRTAWithMCNP;

public class GAProtocolsSolver {
	public static int PROTOCOL_SIZE = 3;

	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;

	DecimalFormat df = new DecimalFormat("##.00");
	FIFOSpinLocksFramework framework = new FIFOSpinLocksFramework();;
	IAFIFONP fifonp = new IAFIFONP();
	IAFIFOP fifop = new IAFIFOP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	int[] candidate_solution;
	int expect_result;

	ArrayList<SporadicTask> unschedulableTasks = new ArrayList<>();
	ArrayList<SporadicTask> schedulableTasks = new ArrayList<>();
	double[][][][] resourceBlocking;

	/****** GA Properties ******/
	int population = 1000;
	int elitismSize = 2;
	int maxGeneration = 500;
	int currentGeneration = 0;

	Random ran = new Random(System.currentTimeMillis());
	int randomBound = 65535;

	int[][] parent_generation;
	int[][] next_generation;
	int[] fitness;

	int bestFitness = 0;
	int bestGeneration = 0;
	int[] bestGene;
	int[] bestSolution = null;

	public GAProtocolsSolver(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		this.tasks = tasks;
		this.resources = resources;
		candidate_solution = new int[resources.size()];
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				expect_result++;
			}
		}

		resourceBlocking = new double[resources.size()][expect_result][PROTOCOL_SIZE][2];
		parent_generation = new int[population][resources.size()];
		next_generation = new int[population][resources.size()];
		bestGene = new int[resources.size()];
		fitness = new int[population];
		for (int i = 0; i < population; i++) {
			fitness[i] = 0;
		}
	}

	public int findSchedulableProtocols() {
		// response time of each protocol
		long[][] fifonp_rt = fifonp.NewMrsPRTATest(tasks, resources, false, false);
		long[][] fifop_rt = fifop.NewMrsPRTATest(tasks, resources, false, false);
		long[][] mrsp_rt = mrsp.getResponseTime(tasks, resources, false, false);

		// task schedulability of each protocol
		int[][] taskschedule_fifonp = checkTaskSchedulability(fifonp_rt);
		int[][] taskschedule_fifop = checkTaskSchedulability(fifop_rt);
		int[][] taskschedule_mrsp = checkTaskSchedulability(mrsp_rt);

		// number of schedulable tasks by each protocol
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;

		// initial check, return if the system is not possible to schedule.
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(0).size(); j++) {
				if (taskschedule_fifonp[i][j] == 1) {
					fifonp_sched++;
				}
				if (taskschedule_fifop[i][j] == 1) {
					fifop_sched++;
				}
				if (taskschedule_mrsp[i][j] == 1) {
					mrsp_sched++;
				}

				if (taskschedule_fifonp[i][j] == taskschedule_fifop[i][j] && taskschedule_fifop[i][j] == taskschedule_mrsp[i][j]
						&& taskschedule_mrsp[i][j] == 0) {
					isPossible = false;

				}
			}
		}
		System.out.println("fifonp: " + fifonp_sched + "    fifop: " + fifop_sched + "    mrsp: " + mrsp_sched);

		if (!isPossible) {
			System.out.println("not schedulable");
			return -1;
		}
		if (fifonp_sched == expect_result) {
			System.out.println("fifonp schedulable");
			return 1;
		}
		if (fifop_sched == expect_result) {
			System.out.println("fifop schedulable");
			return 2;
		}
		if (mrsp_sched == expect_result) {
			System.out.println("mrsp schedulable");
			return 3;
		}

		for (int i = 0; i < resources.size(); i++) {
			if (mrsp_sched >= fifonp_sched && mrsp_sched >= fifop_sched)
				candidate_solution[i] = 3;
			if (fifop_sched >= fifonp_sched && fifop_sched >= mrsp_sched)
				candidate_solution[i] = 2;
			if (fifonp_sched >= fifop_sched && fifonp_sched >= mrsp_sched)
				candidate_solution[i] = 1;
		}
		System.out.println("candidate: " + Arrays.toString(candidate_solution));

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (taskschedule_mrsp[i][j] == 0)
					unschedulableTasks.add(tasks.get(i).get(j));
				else
					schedulableTasks.add(tasks.get(i).get(j));
			}
		}

		for (int k = 0; k < resources.size(); k++) {
			for (int i = 0; i < tasks.size(); i++) {
				for (int j = 0; j < tasks.get(i).size(); j++) {
					resourceBlocking[k][tasks.get(i).get(j).id - 1][0][0] = Double.parseDouble(df.format(tasks.get(i).get(j).fifonp[k]));
					resourceBlocking[k][tasks.get(i).get(j).id - 1][0][1] = 1;

					resourceBlocking[k][tasks.get(i).get(j).id - 1][1][0] = Double.parseDouble(df.format(tasks.get(i).get(j).fifop[k]));
					resourceBlocking[k][tasks.get(i).get(j).id - 1][1][1] = 2;

					resourceBlocking[k][tasks.get(i).get(j).id - 1][2][0] = Double.parseDouble(df.format(tasks.get(i).get(j).mrsp[k]));
					resourceBlocking[k][tasks.get(i).get(j).id - 1][2][1] = 3;
				}
			}
		}
		return solve();
	}

	int solve() {
		initAndGetFirstGene();
		getFitness(parent_generation);
		return 0;
	}

	private void initAndGetFirstGene() {
		for (int i = 0; i < parent_generation.length; i++) {
			if (i < PROTOCOL_SIZE) {
				for (int j = 0; j < parent_generation[i].length; j++) {
					parent_generation[i][j] = i + 1;
				}
			} else {
				for (int j = 0; j < parent_generation[i].length; j++) {
					parent_generation[i][j] = ran.nextInt(randomBound) % 3 + 1;
				}
			}
		}
	}

	int[][] checkTaskSchedulability(long[][] rt) {
		int[][] tasksrt = new int[tasks.size()][tasks.get(0).size()];

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < rt[i][j])
					tasksrt[i][j] = 0;
				else
					tasksrt[i][j] = 1;
			}
		}

		return tasksrt;
	}

	void getFitness(int[][] gene) {
		int maxFitness = 0, maxIndex = 0;
		for (int i = 0; i < gene.length; i++) {
			fitness[i] = isSystemSchedulable(gene[i]);

			if (maxFitness < fitness[i]) {
				maxFitness = fitness[i];
				maxIndex = i;
			}

			if (fitness[i] == expect_result) {
				bestSolution = gene[i];
			}
		}

		System.out.println("Generation: " + currentGeneration + " max fitness: " + maxFitness + " gene: " + Arrays.toString(gene[maxIndex]));
	}

	private int isSystemSchedulable(int[] gene) {
		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}

		long[][] Ris = framework.calculateResponseTime(tasks, resources, false, false);

		int fitness = 0;
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline >= Ris[i][j])
					fitness++;
			}
		}
		return fitness;
	}

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.Random;
		double RSF = 0.2;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 5;
		int result = -1;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		while (result != 0) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);
			GAProtocolsSolver finder = new GAProtocolsSolver(tasks, resources);
			result = finder.findSchedulableProtocols();
		}
	}
}
