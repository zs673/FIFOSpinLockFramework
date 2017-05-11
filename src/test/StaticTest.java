package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.IACombinedProtocol;
import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.SystemGeneratorNoAllocation;
import geneticAlgoritmSolver.StaticSolver;

public class StaticTest {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws Exception {
		StaticTest test = new StaticTest();
		final CountDownLatch downLatch = new CountDownLatch(300);

		for (int i = 1; i < 301; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingCriticalSectionLength(cslen);
					downLatch.countDown();
				}

			}).start();
		}

		downLatch.await();

		IOAResultReader.schedreader();
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		SystemGeneratorNoAllocation generator = new SystemGeneratorNoAllocation(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION, true, null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR,
				NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, cs_len);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();
		IACombinedProtocol sCombine = new IACombinedProtocol();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;
		int scombine = 0;

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

			int maxAccess = 0;
			for (int l = 0; l < tasks.size(); l++) {
				for (int j = 0; j < tasks.get(l).size(); j++) {
					SporadicTask task = tasks.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			int[] protocols = new StaticSolver().solve(tasks, resources, tasks.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				scombine++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + (double) scombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingContention(int NoA) {
		SystemGeneratorNoAllocation generator = new SystemGeneratorNoAllocation(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
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

	public void experimentIncreasingParallel(int NoP, int NoA) {
		SystemGeneratorNoAllocation generator = new SystemGeneratorNoAllocation(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, NoP,
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
		SystemGeneratorNoAllocation generator = new SystemGeneratorNoAllocation(MIN_PERIOD, MAX_PERIOD, 0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
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
		SystemGeneratorNoAllocation generator = new SystemGeneratorNoAllocation(MIN_PERIOD, MAX_PERIOD, 0.1 * NoT, TOTAL_PARTITIONS, NoT, true, range,
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
