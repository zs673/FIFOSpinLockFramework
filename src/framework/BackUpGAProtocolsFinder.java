package framework;

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

public class BackUpGAProtocolsFinder {
	int PROTOCOL_SIZE = 3;

	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;
	boolean print;

	FIFOSpinLocksFramework framework = new FIFOSpinLocksFramework();
	IAFIFOP fifop = new IAFIFOP();
	IAFIFONP fifonp = new IAFIFONP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	Random ran = new Random(System.currentTimeMillis());
	int randomBound = 65535;

	int[] bestSolution = null;
	int expect_fitness;

	int[][] parent_generation;
	int[][] next_generation;
	int[] fitness;

	float[] Pi;
	float Pc;
	float Pm;

	int bestFitness = 0;
	int bestGeneration = 0;
	int[] bestGene;

	int population;
	int maxGeneration;
	int currentGeneration = 0;

	public BackUpGAProtocolsFinder(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			int population, int maxGene, float Pc, float Pm, boolean print) {
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;

		this.population = population;
		this.maxGeneration = maxGene;
		this.Pc = Pc;
		this.Pm = Pm;

		Pi = new float[population];
		bestGene = new int[resources.size()];

		expect_fitness = 0;
		fitness = new int[population];
		parent_generation = new int[population][resources.size()];
		next_generation = new int[population][resources.size()];

		for (int i = 0; i < tasks.size(); i++) {
			expect_fitness += tasks.get(i).size();
		}

		for (int i = 0; i < population; i++) {
			fitness[i] = 0;
		}
	}

	public boolean findSchedulableProtocols() {
		if (isSystemSchedulable(fifonp.NewMrsPRTATest(tasks, resources, true, false))
				|| isSystemSchedulable(fifop.NewMrsPRTATest(tasks, resources, true, false))
				|| isSystemSchedulable(mrsp.getResponseTime(tasks, resources, true, false))) {
			System.out.println("schedulable with one protocl");
			return true;
		}
		boolean isPossible = true;
		if (!initialCheck()) {
			System.out.println("system is not possible");
			// return false;
			isPossible = false;
		}
		initAndGetFirstGene();
		if (print) {
			// System.out.println("Init Done, expect fitness: " +
			// expect_fitness);
		}
		getFitness(parent_generation);
		countRate();

		if (bestSolution != null) {
			System.err.println("best found:  " + Arrays.toString(bestSolution) + " is possible: " + isPossible);
			if (!isPossible) {
				System.err.println("not possible but found best!!!");
				System.exit(-1);
			}
			return true;
		}

		System.out.print("Gene: ");
		for (currentGeneration = 1; currentGeneration < maxGeneration; currentGeneration++) {
			evlove();
			getFitness(parent_generation);
			countRate();

			if (bestSolution != null) {
				System.err.println("best found:  " + Arrays.toString(bestSolution) + " is possible: " + isPossible);
				if (!isPossible) {
					System.err.println("not possible but found best!!!");
					System.exit(-1);
				}
				return true;
			}
			System.out.print(currentGeneration);
		}

		System.out.println();
		return false;
	}

	boolean initialCheck() {
		boolean isPossible = true;

		int[][] fifonp_rt = checkTaskSchedulability(fifonp.NewMrsPRTATest(tasks, resources, false, false));
		int[][] fifop_rt = checkTaskSchedulability(fifop.NewMrsPRTATest(tasks, resources, false, false));
		int[][] mrsp_rt = checkTaskSchedulability(mrsp.getResponseTime(tasks, resources, false, false));

		// for (int i = 0; i < tasks.size(); i++) {
		// System.out.println(Arrays.toString(fifonp_rt[i]) + " " +
		// Arrays.toString(fifop_rt[i]) + " " + Arrays.toString(mrsp_rt[i]));
		// }

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(0).size(); j++) {
				if (fifonp_rt[i][j] == fifop_rt[i][j] && fifop_rt[i][j] == mrsp_rt[i][j] && mrsp_rt[i][j] == 0)
					isPossible = false;
			}
		}
		return isPossible;
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

	public void evlove() {
		// selectBestGene();
		// select();
		//
		// float r;
		// for (int k = 0; k < population; k = k + 2) {
		// r = ran.nextFloat();
		// if (r < Pc)
		// OXCross(k, k + 1);// 进行交叉
		// else {
		// r = ran.nextFloat();
		// if (r < Pm)
		// OnCVariation(k);
		// r = ran.nextFloat();
		// if (r < Pm)
		// OnCVariation(k + 1);
		// }
		// }
		//
		// for (int k = 0; k < population; k++) {
		// for (int i = 0; i < resources.size(); i++) {
		// parent_generation[k][i] = next_generation[k][i];
		// }
		// }

		for (int i = 0; i < population; i++) {
			for (int j = 0; j < parent_generation[i].length; j++) {
				parent_generation[i][j] = ran.nextInt(randomBound) % 3 + 1;
			}
		}
	}

	void getFitness(int[][] gene) {
		int maxFitness = 0, maxIndex = 0;
		for (int i = 0; i < gene.length; i++) {
			fitness[i] = isSystemSchedulable(gene[i]);

			if (maxFitness < fitness[i]) {
				maxFitness = fitness[i];
				maxIndex = i;
			}

			if (fitness[i] == expect_fitness) {
				bestSolution = gene[i];
			}
		}

		System.out.println("Generation: " + currentGeneration + " max fitness: " + maxFitness + " gene: "
				+ Arrays.toString(gene[maxIndex]));
	}

	private void countRate() {
		int k;
		double sumFitness = 0;// 适应度总和

		int[] tempf = new int[population];

		for (k = 0; k < population; k++) {
			tempf[k] = fitness[k];
			sumFitness += tempf[k];
		}

		Pi[0] = (float) (tempf[0] / sumFitness);
		for (k = 1; k < population; k++) {
			Pi[k] = (float) (tempf[k] / sumFitness + Pi[k - 1]);
		}
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

	private boolean isSystemSchedulable(long[][] Ri) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ri[i][j])
					return false;
			}
		}
		return true;
	}

	void selectBestGene() {
		int k, i, maxid;
		int maxevaluation;

		maxid = 0;
		maxevaluation = fitness[0];
		for (k = 1; k < population; k++) {
			if (maxevaluation <= fitness[k]) {
				maxevaluation = fitness[k];
				maxid = k;
			}
		}

		if (bestFitness < maxevaluation) {
			bestFitness = maxevaluation;
			bestGeneration = currentGeneration;
			for (i = 0; i < resources.size(); i++) {
				bestGene[i] = parent_generation[maxid][i];
			}
		}
		copyGene(0, maxid);
	}

	void select() {
		int k, i, selectId;
		float ran1;
		for (k = 1; k < population; k++) {
			ran1 = (float) (ran.nextInt(65535) % 1000 / 1000.0);
			for (i = 0; i < population; i++) {
				if (ran1 <= Pi[i]) {
					break;
				}
			}
			selectId = i;
			copyGene(k, selectId);
		}
	}

	void OXCross(int k1, int k2) {
		int i, j, flag;
		int ran1, ran2, temp = 0;

		ran1 = ran.nextInt(65535) % resources.size();
		ran2 = ran.nextInt(65535) % resources.size();

		while (ran1 == ran2) {
			ran2 = ran.nextInt(65535) % resources.size();
		}
		if (ran1 > ran2)// 确保ran1<ran2
		{
			temp = ran1;
			ran1 = ran2;
			ran2 = temp;
		}
		flag = ran2 - ran1 + 1;// 个数
		for (i = 0, j = ran1; i < flag; i++, j++) {
			temp = next_generation[k1][j];
			next_generation[k1][j] = next_generation[k2][j];
			next_generation[k2][j] = temp;
		}

	}

	public void OnCVariation(int k) {
		int ran1, ran2, temp;
		int count;// 对换次数
		count = ran.nextInt(65535) % resources.size();

		for (int i = 0; i < count; i++) {

			ran1 = ran.nextInt(65535) % resources.size();
			ran2 = ran.nextInt(65535) % resources.size();
			while (ran1 == ran2) {
				ran2 = ran.nextInt(65535) % resources.size();
			}
			temp = next_generation[k][ran1];
			next_generation[k][ran1] = next_generation[k][ran2];
			next_generation[k][ran2] = temp;
		}
	}

	public void copyGene(int newIndex, int oldIndex) {
		int i;
		for (i = 0; i < resources.size(); i++) {
			next_generation[newIndex][i] = parent_generation[oldIndex][i];
		}
	}

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RSF = 0.2;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 5;
		int NUMBER_OF_SYSTEMS = 9999999;
		int schedulablesystem = 0;
		boolean print = true;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		for (int i = 0; i < NUMBER_OF_SYSTEMS; i++) {
			System.err.println(i);
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);
			BackUpGAProtocolsFinder finder = new BackUpGAProtocolsFinder(tasks, resources, 20, 50, 0.8f, 0.9f, print);
			if (finder.findSchedulableProtocols())
				schedulablesystem++;

		}

		System.out.println(schedulablesystem + " systems are schedulable among " + NUMBER_OF_SYSTEMS + " systems.");

	}
}
