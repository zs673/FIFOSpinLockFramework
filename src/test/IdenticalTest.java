package test;

import java.util.ArrayList;
import java.util.Random;

import audsleyAlgorithm.AudsleyOptimalPriorityAssignment;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class IdenticalTest {

	public static int MAX_PERIOD = 1000;
	static long maxC = 0;
	public static int MIN_PERIOD = 1;
	public static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 5;
	public static int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 4;
	public static double RESOURCE_SHARING_FACTOR = .3;
	public static boolean testSchedulability = false;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 20000;
	public static int TOTAL_PARTITIONS = 8;

	static int extendCal = 1;

	public static boolean isEqual(long[][] r1, long[][] r2, boolean print) {
		boolean isequal = true;
		for (int i = 0; i < r1.length; i++) {
			for (int j = 0; j < r1[i].length; j++) {
				if (r1[i][j] != r2[i][j]) {
					if (print)
						System.out.println("not equal at:  i=" + i + "  j=" + j + "   r1: " + r1[i][j] + "   r2:" + r2[i][j]);
					isequal = false;
				}
			}
		}
		return isequal;
	}

	public static boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		analysisWithDiorRi.IAFIFOP fp = new analysisWithDiorRi.IAFIFOP();
		analysisWithDiorRi.IAFIFONP fnp = new analysisWithDiorRi.IAFIFONP();
		analysisWithDiorRi.IANewMrsPRTAWithMCNP mrsp = new analysisWithDiorRi.IANewMrsPRTAWithMCNP();
		analysisWithDiorRi.IACombinedProtocol combined_analysis = new analysisWithDiorRi.IACombinedProtocol();

		AudsleyOptimalPriorityAssignment opa = new AudsleyOptimalPriorityAssignment();

		long[][] r1, r2;
		int i = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
				TOTAL_PARTITIONS * NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, CS_LENGTH_RANGE.VERY_SHORT_CS_LEN,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);

			Random random = new Random();

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = random.nextInt(65535) % 3 + 1;
			}

			r1 = opa.getResponseTime(tasks, resources);
			r2 = combined_analysis.getResponseTime(tasks, resources, false, false, extendCal, false);

			boolean isEqual1 = isEqual(r1, r2, false);

			if (!isEqual1) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.testifyAllocatedTasksetAndResource(tasks, resources);
				r1 = opa.getResponseTime(tasks, resources);
				r2 = combined_analysis.getResponseTime(tasks, resources, false, true, extendCal, false);
				System.exit(0);
			}
			i++;
			System.out.println(i);
		}

		System.out.println("!!!!!");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 3;
			}

			r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);
			r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);

			boolean isEqual1 = isEqual(r1, r2, false);

			if (!isEqual1) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.testifyAllocatedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);
				r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);

				System.exit(0);
			}
			i++;
			System.out.println(i);
		}
		System.out.println("MrsP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 1;
			}

			r1 = fnp.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);
			r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);

			boolean isEqual1 = isEqual(r1, r2, false);

			if (!isEqual1) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.testifyAllocatedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);
				r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);
				System.exit(0);
			}
			i++;
			System.out.println(i);
		}
		System.out.println("FIFO-NP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 2;
			}

			r1 = fp.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);
			r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, false, extendCal, true);

			boolean isEqual1 = isEqual(r1, r2, false);

			if (!isEqual1) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.testifyAllocatedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);
				r2 = combined_analysis.getResponseTime(tasks, resources, testSchedulability, true, extendCal, true);
				System.exit(0);
			}
			i++;
			System.out.println(i);
		}

		System.out.println("FIFO-P TEST DONE");
	}

}