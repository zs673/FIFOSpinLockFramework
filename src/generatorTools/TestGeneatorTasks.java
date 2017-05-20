package generatorTools;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.ALLOCATION_POLICY;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;

public class TestGeneatorTasks {

	public static void main(String[] args) {
		int NUMBER_OF_SYSTEMS = 1;

		int MAX_PERIOD = 1000;
		int MIN_PERIOD = 1;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.2;
		int TOTAL_PARTITIONS = 16;

		SystemGeneratorWithAllocation geneator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, range, RESOURCES_RANGE.PARTITIONS,
				RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int j = 0; j < NUMBER_OF_SYSTEMS; j++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			System.out.println(" WORST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksWF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.WORST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksWF, resources);

			System.out.println("\n\n");
			System.out.println(" BEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksBF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.BEST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksBF, resources);

			System.out.println("\n\n");
			System.out.println(" FIRST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksFF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.FIRST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksFF, resources);

			System.out.println("\n\n");
			System.out.println(" NEXT FIT");
			ArrayList<ArrayList<SporadicTask>> tasksNF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.NEXT_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksNF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE REQUEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRRF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksRRF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE LOCAL FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRLF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksRLF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscslendF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			geneator.testifyAllocatedTasksetAndResource(taskscslendF, resources);
			
			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscsleniF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			geneator.testifyAllocatedTasksetAndResource(taskscsleniF, resources);

			System.err.println("\n\n " + j + " \n\n");
		}

	}

}
