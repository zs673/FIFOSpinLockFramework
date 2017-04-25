package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.SystemGenerator.CS_LENGTH_RANGE;
import generatorTools.SystemGenerator.RESOURCES_RANGE;
import implementationAwareAnalysis.IAFIFONP;
import implementationAwareAnalysis.IAFIFOP;
import implementationAwareAnalysis.IANewMrsPRTAWithMCNP;

public class IOASchedulabilityTest {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
	public static double RSF = 0.2;

	public static void main(String[] args) throws InterruptedException {
		// for (int j = 1; j < 10; j++) {
		// experimentIncreasingWorkLoad(3, j);
		// }

		// for (int j = 1; j < 31; j++) {
		// experimentIncreasingContention(2, j);
		// }

		// for (int j = 0; j < 51; j++) {
		// experimentIncreasingCriticalSectionLength(2, j);
		// }

		for (int j = 2; j <= 32; j++) {
			experimentIncreasingParallel(3, j);
		}

		IOAResultReader.schedreader();
	}

	public static void experimentIncreasingWorkLoad(int bigSet, int smallSet) {
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = smallSet;

		long[][] Ris;
		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		String result = "";
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = IOAmrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siamrsp++;

			Ris = IOAfp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafp++;

			Ris = IOAfnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafnp++;

			System.out.println(1 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";
		writeSystem(("ioa 1" + " " + bigSet + " " + smallSet), result);
	}

	public static void experimentIncreasingParallel(int bigSet, int smallSet) {
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 5;
		int TOTAL_PARTITIONS = smallSet;

		long[][] Ris;
		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		String result = "";
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = IOAmrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siamrsp++;

			Ris = IOAfp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafp++;

			Ris = IOAfnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafnp++;

			System.out.println(4 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";
		writeSystem(("ioa 4" + " " + bigSet + " " + smallSet), result);
	}

	public static void experimentIncreasingCriticalSectionLength(int tasksNumConfig, int cs_len) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 5;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 3 + tasksNumConfig - 1;
		cs_len = cs_len * 10;
		if (cs_len == 0)
			cs_len = 1;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
				cs_len);
		long[][] Ris;

		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		String result = "";
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = IOAmrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siamrsp++;

			Ris = IOAfp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafp++;

			Ris = IOAfnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafnp++;

			System.out.println(2 + "" + tasksNumConfig + " " + cs_len + " times: " + i);
		}

		result += (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa 2" + " " + tasksNumConfig + " " + cs_len), result);
	}

	public static void experimentIncreasingContention(int bigSet, int smallSet) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = smallSet;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 3 + 1 * (bigSet - 1);

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		String result = "";
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {

			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = IOAmrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siamrsp++;

			Ris = IOAfp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafp++;

			Ris = IOAfnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				siafnp++;

			System.out.println(3 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa 3" + " " + bigSet + " " + smallSet), result);
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

	public static void writeSystem(String filename, String result) {
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
