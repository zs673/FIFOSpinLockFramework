package test;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class IdenticalTest {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 10000;

	public static int MAX_PERIOD = 1000;
	static long maxC = 0;
	public static int MIN_PERIOD = 1;
	public static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 5;
	public static int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 4;
	public static double RESOURCE_SHARING_FACTOR = .3;
	public static boolean testSchedulability = false;
	public static int TOTAL_PARTITIONS = 8;

	public static boolean isPrint = false;
	public static boolean useDM = true;

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
		analysis.FIFOP fp = new analysis.FIFOP();
		analysis.FIFONP fnp = new analysis.FIFONP();
		analysis.MrsP mrsp = new analysis.MrsP();
		analysis.CombinedAnalysis combined_analysis = new analysis.CombinedAnalysis();

		long[][] r1, r2, r3, r4, r5, r6;
		int i = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
				TOTAL_PARTITIONS * NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, RESOURCE_SHARING_FACTOR, CS_LENGTH_RANGE.VERY_SHORT_CS_LEN,
				RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 3;
			}

			r1 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, isPrint);
			r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, isPrint);

			r3 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, isPrint);
			r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, isPrint);

			r5 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, isPrint);
			r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, isPrint);

			boolean isEqual1 = isEqual(r1, r2, false);
			boolean isEqual2 = isEqual(r3, r4, false);
			boolean isEqual3 = isEqual(r5, r6, false);

			if (!isEqual1 || !isEqual2 || !isEqual3) {
				System.out.println("not equal: " + isEqual1 + " " + isEqual2 + " " + isEqual3);
				isEqual(r1, r2, true);
				isEqual(r3, r4, true);
				isEqual(r5, r6, true);
				generator.PrintAllocatedSystem(tasks, resources);
				r1 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, true);
				r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, true);

				r3 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, true);
				r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, true);

				r5 = mrsp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, true);
				r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, true);
				System.exit(0);
			}
			i++;
			System.out.println(i + "    MrsP");
		}
		System.out.println("MrsP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 1;
			}

			r1 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, isPrint);
			r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, isPrint);

			r3 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, isPrint);
			r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, isPrint);

			r5 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, isPrint);
			r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, isPrint);

			boolean isEqual1 = isEqual(r1, r2, false);
			boolean isEqual2 = isEqual(r3, r4, false);
			boolean isEqual3 = isEqual(r5, r6, false);

			if (!isEqual1 || !isEqual2 || !isEqual3) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.PrintAllocatedSystem(tasks, resources);
				r1 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, true);
				r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, true);

				r3 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, true);
				r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, true);

				r5 = fnp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, true);
				r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, true);
				System.exit(0);
			}
			i++;
			System.out.println(i + "    FIFO-NP");
		}
		System.out.println("FIFO-NP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 2;
			}

			r1 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, isPrint);
			r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, isPrint);

			r3 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, isPrint);
			r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, isPrint);

			r5 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, isPrint);
			r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, isPrint);

			boolean isEqual1 = isEqual(r1, r2, false);
			boolean isEqual2 = isEqual(r3, r4, false);
			boolean isEqual3 = isEqual(r5, r6, false);

			if (!isEqual1 || !isEqual2 || !isEqual3) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				generator.PrintAllocatedSystem(tasks, resources);
				r1 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, true);
				r2 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, true, useDM, true);

				r3 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, true);
				r4 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, true, false, useDM, true);

				r5 = fp.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, true);
				r6 = combined_analysis.getResponseTimeByDMPO(tasks, resources, extendCal, testSchedulability, false, false, useDM, true);

				System.exit(0);
			}
			i++;
			System.out.println(i + "    FIFO-P");
		}

		System.out.println("FIFO-P TEST DONE");
	}

}