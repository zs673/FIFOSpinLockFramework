package evaluationGA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import GeneticAlgorithmFramework.GASolver;
import GeneticAlgorithmFramework.PreGASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class TestSuccess {

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	final double RSF = 0.3;

	public static int ALLOCATION_POLICY = 1;
	public static int PRIORITY_RULE = 1;
	public static int GENERATIONS = 10;
	public static int POPULATION = 100;

	public static boolean useGA = true;
	public static boolean lazy = false;
	public static boolean record = true;

	public static void main(String[] args) throws InterruptedException {
		TestSuccess test = new TestSuccess();
		for (int i = 0; i < 9999; i++) {
			test.testSuccess();
		}

	}

	public void testSuccess() {

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION,
				RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
		ArrayList<SporadicTask> tasksToAlloc1 = generator.generateTasks();
		ArrayList<Resource> resources1 = generator.generateResources();
		generator.generateResourceUsage(tasksToAlloc1, resources1);

		int preres = -1;

		PreGASolver pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
		preres = pre.initialCheck(true,false);
		while (preres != 0) {
			tasksToAlloc1 = generator.generateTasks();
			resources1 = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc1, resources1);

			pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
			preres = pre.initialCheck(true,false);
		}

		GASolver solver = new GASolver(tasksToAlloc1, resources1, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8, 0.01, 2, 2,
				record, true);

		solver.checkSchedulability(useGA, lazy);

	}

	public void writeSystem(String filename, String result) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("result/" + filename + ".txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

}
