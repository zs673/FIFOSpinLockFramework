package test;

import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;

public class GeneralTest {
	public static int MAX_PERIOD = 10;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 4;

	public static void main(String[] args) throws Exception {
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, 5);
		
		ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
		ArrayList<Resource> resources = generator.generateResources();
		generator.generateResourceUsage(tasks, resources);
		
		generator.testifyGeneratedTasksetAndResource(tasks, resources);
	}

}
