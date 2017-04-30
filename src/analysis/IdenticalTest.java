package analysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;

public class IdenticalTest {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 8;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 4;
	public static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	public static double RESOURCE_SHARING_FACTOR = .4;
	public static boolean testSchedulability = true;

	static long maxC = 0;

	public static void main(String[] args) {
		IAFIFOP fp = new IAFIFOP();
		IAFIFONP fnp = new IAFIFONP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();
		IACombinedProtocol combined_analysis = new IACombinedProtocol();
		long[][] r1, r2;
		int i = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, CS_LENGTH_RANGE.VERY_SHORT_CS_LEN, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 3;
			}

			r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, false);
			r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, false);
			boolean isEqual = isEqual(r1, r2, false);

			if (!isEqual /*
							 * && isSystemSchedulable(tasks, r1) &&
							 * isSystemSchedulable(tasks, r2)
							 */) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				SystemGenerator.testifyGeneratedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true);
				r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, true);
				System.exit(0);
			}
			// if (isEqual && isSystemSchedulable(tasks, r1) &&
			// isSystemSchedulable(tasks, r2)) {
			// System.out.println(i);
			// i++;
			// }

			// if (!isSystemSchedulable(tasks, r1) ||
			// !isSystemSchedulable(tasks, r2))
			// System.out.println("miss");
			i++;
			System.out.println(i);
		}
		System.out.println("MrsP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 1;
			}

			r1 = fnp.NewMrsPRTATest(tasks, resources, testSchedulability, false);
			r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, false);
			boolean isEqual = isEqual(r1, r2, false);

			if (!isEqual /*
							 * && isSystemSchedulable(tasks, r1) &&
							 * isSystemSchedulable(tasks, r2)
							 */) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				SystemGenerator.testifyGeneratedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true);
				r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, true);
				System.exit(0);
			}
			// if (isEqual && isSystemSchedulable(tasks, r1) &&
			// isSystemSchedulable(tasks, r2)) {
			// System.out.println(i);
			// i++;
			// }

			// if (!isSystemSchedulable(tasks, r1) ||
			// !isSystemSchedulable(tasks, r2))
			// System.out.println("miss");
			i++;
			System.out.println(i);
		}
		System.out.println("FIFO-NP TEST DONE");

		i = 0;
		while (i <= TOTAL_NUMBER_OF_SYSTEMS) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			for (int j = 0; j < resources.size(); j++) {
				resources.get(j).protocol = 2;
			}

			r1 = fp.NewMrsPRTATest(tasks, resources, testSchedulability, false);
			r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, false);
			boolean isEqual = isEqual(r1, r2, false);

			if (!isEqual /*
							 * && isSystemSchedulable(tasks, r1) &&
							 * isSystemSchedulable(tasks, r2)
							 */) {
				System.out.println("not equal");
				isEqual(r1, r2, true);
				SystemGenerator.testifyGeneratedTasksetAndResource(tasks, resources);
				r1 = mrsp.getResponseTime(tasks, resources, testSchedulability, true);
				r2 = combined_analysis.calculateResponseTime(tasks, resources, testSchedulability, true);
				System.exit(0);
			}
			// if (isEqual && isSystemSchedulable(tasks, r1) &&
			// isSystemSchedulable(tasks, r2)) {
			// System.out.println(i);
			// i++;
			// }

			// if (!isSystemSchedulable(tasks, r1) ||
			// !isSystemSchedulable(tasks, r2))
			// System.out.println("miss");
			i++;
			System.out.println(i);
		}
		System.out.println("FIFO-P TEST DONE");
	}

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

}
