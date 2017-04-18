package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import basicAnalysis.FIFONP;
import basicAnalysis.FIFOP;
import basicAnalysis.NewMrsPRTA;
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
	public static CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	public static double RSF = 0.2;

	public static void main(String[] args) throws InterruptedException {
		// int experiment = 0;
		// int bigSet = 0;
		// int smallSet = 0;

		// if (args.length == 3) {
		// experiment = Integer.parseInt(args[0]);
		// bigSet = Integer.parseInt(args[1]);
		// smallSet = Integer.parseInt(args[2]);
		//
		// switch (experiment) {
		// case 1:
		// experimentIncreasingWorkLoad(bigSet, smallSet);
		// break;
		// case 2:
		// experimentIncreasingCriticalSectionLength(bigSet, smallSet);
		// break;
		// case 3:
		// experimentIncreasingContention(bigSet, smallSet);
		// break;
		// default:
		// break;
		// }
		//
		// } else
		// System.err.println("wrong parameter.");

		// for (int i = 1; i < 6; i++) {
		// for (int j = 1; j < 11; j++) {
		// experimentIncreasingWorkLoad(i, j);
		// }
		// }

		// for (int i = 1; i < 6; i++) {
		// for (int j = 1; j < 6; j++) {
		// experimentIncreasingCriticalSectionLength(i, j);
		// }
		// }

		// for (int j = 1; j < 11; j++) {
		// experimentIncreasingWorkLoad(3, j);
		// }
		// //
		// for (int j = 1; j < 11; j++) {
		// experimentIncreasingContention(2, j);
		// }

		for (int j = 113; j < 501; j++) {
			experimentIncreasingCriticalSectionLength(2, j);
		}

		// for (int j = 1; j <= 11; j++) {
		// experimentIncreasingParallel(3, j);
		// }

		IOAResultReader.schedreader();

		// for (int j = 1; j < 6; j++) {
		// MrsPSchedulabilityTestNPLen(2, j);
		// }
	}

	public static void MrsPSchedulabilityTestNPLen(int tasksNumConfig, int csLenConfig) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

		CS_LENGTH_RANGE range = null;
		switch (csLenConfig) {
		case 1:
			range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 2:
			range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 3:
			range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 4:
			range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 5:
			range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		default:
			break;
		}

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION,
				true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFONP IOAfifonp = new IAFIFONP();
		IAFIFOP IOAfifop = new IAFIFOP();

		for (int i = 0; i < 100; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			IOAmrsp.getResponseTime(tasks, resources, false);
			IOAfifonp.NewMrsPRTATest(tasks, resources, false);
			IOAfifop.NewMrsPRTATest(tasks, resources, false);
			System.out.println(i);
		}
	}

	public static void experimentIncreasingWorkLoad(int bigSet, int smallSet) {
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		// double RESOURCE_SHARING_FACTOR = 0.2 + 0.1 * (double) (bigSet - 1);

		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = smallSet;

		long[][] Ris;
		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		NewMrsPRTA mrsp = new NewMrsPRTA();
		FIFOP fp = new FIFOP();
		FIFONP fnp = new FIFONP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		String result = "";
		int smrsp = 0;
		int sfp = 0;
		int sfnp = 0;
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				smrsp++;

				Ris = IOAmrsp.NewMrsPRTATest(tasks, resources, 100, false);
				if (isSystemSchedulable(tasks, Ris))
					siamrsp++;
			}

			Ris = fp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfp++;

				Ris = IOAfp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafp++;
			}

			Ris = fnp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfnp++;

				Ris = IOAfnp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafnp++;
			}

			System.out.println(1 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";
		writeSystem(("ioa 1" + " " + bigSet + " " + smallSet), result);
	}

	public static void experimentIncreasingParallel(int bigSet, int smallSet) {
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 5;
		// double RESOURCE_SHARING_FACTOR = 0.2 + 0.1 * (double) (bigSet - 1);

		int TOTAL_PARTITIONS = 4 + 2 * (smallSet - 1);

		long[][] Ris;
		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		NewMrsPRTA mrsp = new NewMrsPRTA();
		FIFOP fp = new FIFOP();
		FIFONP fnp = new FIFONP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		String result = "";
		int smrsp = 0;
		int sfp = 0;
		int sfnp = 0;
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				smrsp++;

				Ris = IOAmrsp.NewMrsPRTATest(tasks, resources, 100, false);
				if (isSystemSchedulable(tasks, Ris))
					siamrsp++;
			}

			Ris = fp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfp++;

				Ris = IOAfp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafp++;
			}

			Ris = fnp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfnp++;

				Ris = IOAfnp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafnp++;
			}

			System.out.println(4 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";
		writeSystem(("ioa 4" + " " + bigSet + " " + smallSet), result);
	}

	public static void experimentIncreasingCriticalSectionLength(int tasksNumConfig, int cs_len) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 3 + tasksNumConfig - 1;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION,
				true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
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

			Ris = IOAmrsp.getResponseTime(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris))
				siamrsp++;

			Ris = IOAfp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris))
				siafp++;

			Ris = IOAfnp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris))
				siafnp++;

			System.out.println(2 + "" + tasksNumConfig + " " + cs_len + " times: " + i);
		}

		result += (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa 2" + " " + tasksNumConfig + " " + cs_len), result);
	}

	public static void experimentIncreasingContention(int bigSet, int smallSet) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 1 + 5 * (smallSet - 1);
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4 + 1 * (bigSet - 1);

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD,
				0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION,
				true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);
		long[][] Ris;

		IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
		IAFIFOP IOAfp = new IAFIFOP();
		IAFIFONP IOAfnp = new IAFIFONP();

		NewMrsPRTA mrsp = new NewMrsPRTA();
		FIFOP fp = new FIFOP();
		FIFONP fnp = new FIFONP();

		String result = "";

		int smrsp = 0;
		int sfp = 0;
		int sfnp = 0;
		int siamrsp = 0;
		int siafp = 0;
		int siafnp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {

			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				smrsp++;

				Ris = IOAmrsp.NewMrsPRTATest(tasks, resources, 100, false);
				if (isSystemSchedulable(tasks, Ris))
					siamrsp++;
			}

			Ris = fp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfp++;

				Ris = IOAfp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafp++;
			}

			Ris = fnp.NewMrsPRTATest(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris)) {
				sfnp++;

				Ris = IOAfnp.NewMrsPRTATest(tasks, resources, false);
				if (isSystemSchedulable(tasks, Ris))
					siafnp++;
			}

			System.out.println(3 + "" + bigSet + " " + smallSet + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
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
