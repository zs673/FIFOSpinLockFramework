package test;

import java.util.ArrayList;

import GeneticAlgorithmFramework.GASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class TestGAWithAllocation {

	public static void main(String[] args) {

		int MAX_PERIOD = 1000;
		int MIN_PERIOD = 1;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.3;
		int TOTAL_PARTITIONS = 16;
		int schedulable = 0;

		SystemGenerator geneator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int i = 0; i < 1000; i++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			GASolver gene = new GASolver(tasks, resources, geneator, 8, 1, 100, 50, 5, 1, 0.5, 0.1, 5, 5, true, true);
			if (gene.checkSchedulability(true, true) == 1) {
				schedulable++;
			}
			System.out.println(i);
		}

		System.out.println("schedulable " + schedulable);

	}
}
