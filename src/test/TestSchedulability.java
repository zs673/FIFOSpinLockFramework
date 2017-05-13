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
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws InterruptedException {
		boolean runParallel = false;
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
					for (int i = 2; i < 17; i++) {
						test.experimentIncreasingParallel(i, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);
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

			// for (int i = 1; i < 11; i++) {
			// test.experimentIncreasingWorkLoad(i);
			// }
			// for (int i = 1; i < 301; i++) {
			// test.experimentIncreasingCriticalSectionLength(i);
			// }
			// for (int i = 1; i < 31; i++) {
			// test.experimentIncreasingContention(i);
			// }
			// for (int i = 1; i < 17; i++) {
			// test.experimentIncreasingParallel(i,
			// NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);
			// }

			/**
			 * cslen & rsf
			 */
			for (int i = 1; i < 6; i++) {
				for (int j = 1; j <= 6; j++) {
					test.experimentIncreasingRSF(j, i);
				}
			}

			/**
			 * parallel partitions & access
			 */
			// final CountDownLatch downLatch = new CountDownLatch(930);
			// for (int j = 1; j < 31; j++) {
			// for (int i = 2; i < 33; i++) {
			// final int access = j;
			// final int partitions = i;
			// new Thread(new Runnable() {
			// public void run() {
			// test.experimentIncreasingParallel(partitions,access);
			// downLatch.countDown();
			// }
			// }).start();
			// }
			// }
			//
			// try {
			// downLatch.await();
			// } catch (InterruptedException e) {
			// }
		}

		IOAResultReader.schedreader(null, false);

	}

	public void experimentIncreasingContention(int NoA) {
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NoA);

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
			System.out.println(3 + " " + 1 + " " + NoA + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 3 + " " + 1 + " " + NoA), result);
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, cs_len);

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
			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingParallel(int NoP, int NoA) {
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, NoP,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NoA);

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
			System.out.println(4 + " " + NoA + " " + NoP + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 4 + " " + NoA + " " + NoP), result);
	}

	public void experimentIncreasingRSF(int RSF, int cslen) {
		double rsf;
		CS_LENGTH_RANGE range;
		switch (RSF) {
		case 1:
			rsf = 0.2;
			break;
		case 2:
			rsf = 0.4;
			break;
		case 3:
			rsf = 0.6;
			break;
		case 4:
			rsf = 0.8;
			break;
		case 5:
			rsf = 1.0;
			break;
		default:
			rsf = 0;
			break;
		}

		switch (cslen) {
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
			range = null;
			break;
		}
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, range, RESOURCES_RANGE.PARTITIONS, rsf, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

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
			System.out.println(5 + " " + cslen + " " + RSF + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 5 + " " + cslen + " " + RSF), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * NoT, TOTAL_PARTITIONS, NoT, true, range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

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
			System.out.println(1 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
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
