package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.SystemGenerator;

public class TestSchedulability {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;

	public static void main(String[] args) throws InterruptedException {
		boolean runParallel = true;
		if (runParallel) {
			TestSchedulability test = new TestSchedulability();

			final CountDownLatch downLatch = new CountDownLatch(4);

			Thread workload = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 1; i < 11; i++) {
						test.experimentIncreasingWorkLoad(i);
					}
					downLatch.countDown();
				}
			});

			Thread cslen = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 1; i < 301; i++) {
						test.experimentIncreasingCriticalSectionLength(i);
					}
					downLatch.countDown();
				}
			});

			Thread access = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 1; i < 31; i++) {
						test.experimentIncreasingContention(i);
					}
					downLatch.countDown();
				}
			});

			Thread parallel = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 33; i++) {
						test.experimentIncreasingParallel(i);
					}
					downLatch.countDown();
				}
			});

			workload.start();
			cslen.start();
			access.start();
			parallel.start();
			try {
				downLatch.await();
			} catch (InterruptedException e) {
			}

		} else {
			TestSchedulability test = new TestSchedulability();
			for (int i = 1; i < 11; i++) {
				test.experimentIncreasingWorkLoad(i);
			}

			for (int i = 1; i < 301; i++) {
				test.experimentIncreasingCriticalSectionLength(i);
			}
			for (int i = 1; i < 31; i++) {
				test.experimentIncreasingContention(i);
			}
			for (int i = 0; i < 33; i++) {
				test.experimentIncreasingParallel(i);
			}
		}

		IOAResultReader.schedreader();
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		double RESOURCE_SHARING_FACTOR = 0.4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = NoT;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			Ris = fnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;
			System.out.println(1 + "" + 1 + " " + NoT + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
	}

	public void experimentIncreasingCriticalSectionLength(int csLenConfig) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		int cs_len = csLenConfig;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
				cs_len);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			Ris = fnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;
			System.out.println(2 + "" + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingContention(int NoA) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = NoA;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE cs_len = CS_LENGTH_RANGE.MEDIUM_CS_LEN;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, cs_len, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			Ris = fnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;
			System.out.println(3 + "" + 1 + " " + NoA + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 3 + " " + 1 + " " + NoA), result);
	}

	public void experimentIncreasingParallel(int NoP) {
		double RESOURCE_SHARING_FACTOR = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE cs_len = CS_LENGTH_RANGE.MEDIUM_CS_LEN;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION, NoP,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, cs_len, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			Ris = fnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;
			System.out.println(4 + "" + 1 + " " + NoP + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 4 + " " + 1 + " " + NoP), result);
	}

	public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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