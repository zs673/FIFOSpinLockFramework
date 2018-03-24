package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class ResourceOrientedAllocationSuccessTest {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 10000;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_PARTITIONS = 16;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static double RESOURCE_SHARING_FACTOR = 0.3;
	int allocSize = 8;

	AllocationGeneator allocGeneator = new AllocationGeneator();

	public static void main(String[] args) throws Exception {
		ResourceOrientedAllocationSuccessTest test = new ResourceOrientedAllocationSuccessTest();

		for (int i = 1; i < 11; i++)
			test.experimentIncreasingWorkLoad(i);

		for (int i = 2; i < 25; i = i + 2)
			test.experimentIncreasingParallel(i);

		ResultReader.schedreader();
	}

	public void experimentIncreasingParallel(int NoP) {

		String result = "";
		int[] allocOK = new int[allocSize];

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NoP * NUMBER_OF_TASKS_ON_EACH_PARTITION, RESOURCE_SHARING_FACTOR,
				range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			for (int a = 0; a < allocSize; a++) {
				if (allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, a) != null)
					allocOK[a]++;
			}

			System.out.println(4 + " " + 3 + " " + NoP + " times: " + i);
		}

		for (int i = 0; i < allocOK.length; i++) {
			result += (double) allocOK[i] / (double) TOTAL_NUMBER_OF_SYSTEMS + "    ";
		}

		writeSystem((4 + " " + 3 + " " + NoP), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		String result = "";

		int[] allocOK = new int[allocSize];
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NoT, RESOURCE_SHARING_FACTOR, range,
				RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			for (int a = 0; a < allocSize; a++) {
				if (allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, a) != null)
					allocOK[a]++;
			}

			System.out.println(1 + " " + 3 + " " + NoT + " times: " + i);
		}

		for (int i = 0; i < allocOK.length; i++) {
			result += (double) allocOK[i] / (double) TOTAL_NUMBER_OF_SYSTEMS + "    ";
		}

		writeSystem((1 + " " + 3 + " " + NoT), result);
	}

	public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		if (tasks == null)
			return false;
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
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
