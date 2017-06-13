package generatorTools;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;

public class TestGeneatorTasks {

	public static void main(String[] args) {
		int NUMBER_OF_SYSTEMS = 100000;

		int MAX_PERIOD = 1000;
		int MIN_PERIOD = 1;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.2;
		int TOTAL_PARTITIONS = 16;

		SystemGeneratorWithAllocation geneator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD,
				TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int j = 0; j < NUMBER_OF_SYSTEMS; j++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			System.out.println(" WORST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksWF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 0), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksWF, resources);

			System.out.println("\n\n");
			System.out.println(" BEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksBF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 1), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksBF, resources);

			System.out.println("\n\n");
			System.out.println(" FIRST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksFF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 2), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksFF, resources);

			System.out.println("\n\n");
			System.out.println(" NEXT FIT");
			ArrayList<ArrayList<SporadicTask>> tasksNF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 3), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksNF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE REQUEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRRF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 4), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksRRF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE LOCAL FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRLF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 5), resources);
			geneator.testifyAllocatedTasksetAndResource(tasksRLF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscslendF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 6), resources);
			geneator.testifyAllocatedTasksetAndResource(taskscslendF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscsleniF = geneator
					.assignPrioritiesByDM(geneator.allocateTasks(tasks, resources, 7), resources);
			geneator.testifyAllocatedTasksetAndResource(taskscsleniF, resources);

			System.err.println("\n\n " + j + " \n\n");

			System.out.println((int) Math.round(0.4));
		}

	}

}
