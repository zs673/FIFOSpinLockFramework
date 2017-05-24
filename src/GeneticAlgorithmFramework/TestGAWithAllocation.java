package GeneticAlgorithmFramework;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.SystemGeneratorWithAllocation;

public class TestGAWithAllocation {

	public static void main(String[] args) {

		int MAX_PERIOD = 1000;
		int MIN_PERIOD = 1;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.25;
		int TOTAL_PARTITIONS = 16;
		int schedulable = 0;

		SystemGeneratorWithAllocation geneator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD,
				TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int i = 0; i < 100; i++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			GASolverWithAllocation gene = new GASolverWithAllocation(tasks, resources, geneator, 100, 100, 5, 0.5, 0.1,
					5, 5, 5, true);
			if (gene.findSchedulableProtocols(true) >= 0) {
				schedulable++;
			}
			System.out.println(i);
		}

		System.out.println("schedulable " + schedulable);

	}
}
