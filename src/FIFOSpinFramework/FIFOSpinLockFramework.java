package FIFOSpinFramework;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.SystemGenerator.CS_LENGTH_RANGE;
import generatorTools.SystemGenerator.RESOURCES_RANGE;
import implementationAwareAnalysis.IAFIFONP;
import implementationAwareAnalysis.IAFIFOP;
import implementationAwareAnalysis.IANewMrsPRTAWithMCNP;

public class FIFOSpinLockFramework {

	public static int TOTAL_PARTITIONS = 3;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 3;
	public static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	public static double RESOURCE_SHARING_FACTOR = .4;

	public static void main(String args[]) {
		ArrayList<ArrayList<SporadicTask>> tasks = null;
		ArrayList<Resource> resources = null;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, CS_LENGTH_RANGE.MEDIUM_CS_LEN, RESOURCES_RANGE.PARTITIONS,
				RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		tasks = generator.generateTasks();
		resources = generator.generateResources();
		generator.generateResourceUsage(tasks, resources);

		testProtocols(tasks, resources);
		allocateResources(tasks, resources);
	}

	/**
	 * Calculate the response time of all tasks using each protocol. Obtain the
	 * blocking time of each resource with each protocol.
	 * 
	 * @param tasks
	 * @param resources
	 */
	public static void testProtocols(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		IAFIFONP fifonp = new IAFIFONP();
		IAFIFOP fifop = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		fifonp.NewMrsPRTATest(tasks, resources, false, false);
		fifop.NewMrsPRTATest(tasks, resources, false, false);
		mrsp.getResponseTime(tasks, resources, false, false);

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				for (int k = 0; k < resources.size(); k++) {
					System.out.print(tasks.get(i).get(j).fifonp[k] + " | " + tasks.get(i).get(j).fifop[k] + " | "
							+ tasks.get(i).get(j).mrsp[k]);
					System.out.println();
				}
				System.out.println("np section due to mrsp resources: " + tasks.get(i).get(j).np_section);
				System.out.println();
			}
			System.out.println();
		}

	}

	public static void allocateResources(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		for (int k = 0; k < resources.size(); k++) {
			// Resource resource = resources.get(k);
			// int fifomp_perference = 0, fifop_perference = 0, mrsp_perference
			// = 0;
			// for(int i=0;i<tasks.size();i++){
			//
			// }
		}
	}
}
