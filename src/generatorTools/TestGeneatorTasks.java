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
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 9;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.2;
		int TOTAL_PARTITIONS = 16;

		SystemGeneratorWithAllocation geneator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int j = 0; j < NUMBER_OF_SYSTEMS; j++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			ArrayList<ArrayList<SporadicTask>> tasksWF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS, ALLOCATION_POLICY.WORST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksWF, resources);

			System.out.println("\n\n");

			ArrayList<ArrayList<SporadicTask>> tasksBF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS, ALLOCATION_POLICY.BEST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksBF, null);

			System.out.println("\n\n");
			ArrayList<ArrayList<SporadicTask>> tasksFF = geneator.allocateTasks(tasks, resources, TOTAL_PARTITIONS, ALLOCATION_POLICY.FIRST_FIT);
			geneator.testifyAllocatedTasksetAndResource(tasksFF, null);

			System.out.println(j);
		}

	}

}
