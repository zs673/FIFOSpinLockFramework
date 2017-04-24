package framework;

import java.util.ArrayList;
import java.util.Arrays;

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

	FIFOSpinLocksFramework framework = new FIFOSpinLocksFramework();;
	IAFIFONP fifonp = new IAFIFONP();
	IAFIFOP fifop = new IAFIFOP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	ArrayList<Integer> protocols = new ArrayList<>();
	int[] candidate_solution;
	int expect_result;

	public GAProtocolsSolver(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		this.tasks = tasks;
		this.resources = resources;
		candidate_solution = new int[resources.size()];
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				expect_result++;
			}
		}
	}

	public int findSchedulableProtocols() {
		long[][] fifonp_rt = fifonp.NewMrsPRTATest(tasks, resources, false, false);
		long[][] fifop_rt = fifop.NewMrsPRTATest(tasks, resources, false, false);
		long[][] mrsp_rt = mrsp.getResponseTime(tasks, resources, false, false);

		int[][] taskschedule_fifonp = checkTaskSchedulability(fifonp_rt);
		int[][] taskschedule_fifop = checkTaskSchedulability(fifop_rt);
		int[][] taskschedule_mrsp = checkTaskSchedulability(mrsp_rt);

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

				if (taskschedule_fifonp[i][j] == taskschedule_fifop[i][j]
						&& taskschedule_fifop[i][j] == taskschedule_mrsp[i][j] && taskschedule_mrsp[i][j] == 0) {
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
			if (fifonp_sched >= fifop_sched && fifonp_sched >= mrsp_sched)
				candidate_solution[i] = 1;
			if (fifop_sched >= fifonp_sched && fifop_sched >= mrsp_sched)
				candidate_solution[i] = 2;
			if (mrsp_sched >= fifonp_sched && mrsp_sched >= fifop_sched)
				candidate_solution[i] = 3;
		}

		System.out.println("candidate: " + Arrays.toString(candidate_solution));

		ArrayList<SporadicTask> unschedulableTasks = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if(taskschedule_mrsp[i][j] == 0){
					unschedulableTasks.add(tasks.get(i).get(j));
				}
			}
		}
		
		System.out.println("unschedulable tasks:");
		for(int i=0;i<unschedulableTasks.size();i++){
			System.out.print("T" + unschedulableTasks.get(i).id + "    ");
		}
		System.out.println();

		return 0;
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

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.Random;
		double RSF = 0.2;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 6;
		int NUMBER_OF_SYSTEMS = 1;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		for (int i = 0; i < NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);
			GAProtocolsSolver finder = new GAProtocolsSolver(tasks, resources);
			finder.findSchedulableProtocols();

		}
	}
}
