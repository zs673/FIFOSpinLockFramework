package evaluationForThesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import GeneticAlgorithmFramework.GASolver;
import analysis.FIFONP;
import analysis.FIFOP;
import analysis.MrsP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import generatorTools.TestResultFileReader;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class CombinedProtocolWFDM {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.3;
	public static int TOTAL_PARTITIONS = 16;
	public static boolean testSchedulability = true;
	public static boolean btbHit = true;
	public static boolean useRi = true;
	public static int PROTOCOLS = 3;

	AllocationGeneator allocGeneator = new AllocationGeneator();

	public static void main(String[] args) throws Exception {
		CombinedProtocolWFDM test = new CombinedProtocolWFDM();

		final CountDownLatch workloadcountdown = new CountDownLatch(9);
		for (int i = 1; i < 10; i++) {
			final int workload = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingWorkLoad(workload);
					workloadcountdown.countDown();
				}
			}).start();
		}
		workloadcountdown.await();

		final CountDownLatch cslencountdown = new CountDownLatch(6);
		for (int i = 1; i < 7; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingCriticalSectionLength(cslen);
					cslencountdown.countDown();
				}
			}).start();
		}
		cslencountdown.await();

		final CountDownLatch accesscountdown = new CountDownLatch(9);
		for (int i = 1; i < 42; i = i + 5) {
			final int access = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingContention(access);
					accesscountdown.countDown();
				}
			}).start();
		}
		accesscountdown.await();

		final CountDownLatch processorscountdown = new CountDownLatch(10);
		for (int i = 4; i < 23; i = i + 2) {
			final int processors = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingParallel(processors);
					processorscountdown.countDown();
				}
			}).start();
		}
		processorscountdown.await();

//		test.experimentIncreasingCriticalSectionLength(6);
		
		TestResultFileReader.schedreader(null, false);
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		final CS_LENGTH_RANGE cs_range;
		switch (cs_len) {
		case 1:
			cs_range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 2:
			cs_range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 3:
			cs_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 4:
			cs_range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 5:
			cs_range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		case 6:
			cs_range = CS_LENGTH_RANGE.RANDOM;
			break;
		default:
			cs_range = null;
			break;
		}

		long[][] Ris;
		String result = "";

		int sfnp = 0, sfp = 0, smrsp = 0, scombine = 0, snew = 0;

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, cs_range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			if (tasksToAlloc == null) {
				System.err.println("tasks are null! at csl test");
				System.exit(-1);
			}

			GASolver gene = new GASolver(tasksToAlloc, resources, generator, 1, 1, 100, 100, 5, 0.5, 0.1, 5, 5, 5, true);
			if (gene.checkSchedulability(true)[0] == 1) {
				snew++;
				if (gene.checkSchedulability(true)[1] == 0) {
					scombine++;
				}
			}

			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) snew / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) scombine / (double) TOTAL_NUMBER_OF_SYSTEMS;

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		long[][] Ris;

		String result = "";
		int sfnp = 0, sfp = 0, smrsp = 0, scombine = 0, snew = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NoT * TOTAL_PARTITIONS, RESOURCE_SHARING_FACTOR, range,
				RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			if (tasksToAlloc == null) {
				System.err.println("tasks are null! at csl test");
				System.exit(-1);
			}

			GASolver gene = new GASolver(tasksToAlloc, resources, generator, 1, 1, 100, 100, 5, 0.5, 0.1, 5, 5, 5, true);
			if (gene.checkSchedulability(true)[0] == 1) {
				snew++;
				if (gene.checkSchedulability(true)[1] == 0) {
					scombine++;
				}
			}

			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) snew / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) scombine / (double) TOTAL_NUMBER_OF_SYSTEMS;

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
	}

	public void experimentIncreasingParallel(int NoP) {
		long[][] Ris;

		String result = "";
		int sfnp = 0, sfp = 0, smrsp = 0, scombine = 0, snew = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NUMBER_OF_TASKS_ON_EACH_PARTITION * NoP, RESOURCE_SHARING_FACTOR,
				range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			if (tasksToAlloc == null) {
				System.err.println("tasks are null! at csl test");
				System.exit(-1);
			}

			GASolver gene = new GASolver(tasksToAlloc, resources, generator, 1, 1, 100, 100, 5, 0.5, 0.1, 5, 5, 5, true);
			if (gene.checkSchedulability(true)[0] == 1) {
				snew++;
				if (gene.checkSchedulability(true)[1] == 0) {
					scombine++;
				}
			}

			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + NoP + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) snew / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) scombine / (double) TOTAL_NUMBER_OF_SYSTEMS;
		writeSystem(("ioa " + 4 + " " + 1 + " " + NoP), result);
	}

	public void experimentIncreasingContention(int NoA) {
		long[][] Ris;

		String result = "";
		int sfnp = 0, sfp = 0, smrsp = 0, scombine = 0, snew = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, range, RESOURCES_RANGE.PARTITIONS, NoA, false);

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			if (tasksToAlloc == null) {
				System.err.println("tasks are null! at csl test");
				System.exit(-1);
			}

			GASolver gene = new GASolver(tasksToAlloc, resources, generator, 1, 1, 100, 100, 5, 0.5, 0.1, 5, 5, 5, true);
			if (gene.checkSchedulability(true)[0] == 1) {
				snew++;
				if (gene.checkSchedulability(true)[1] == 0) {
					scombine++;
				}
			}

			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				sfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + NoA + " times: " + i);
		}

		result += (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) snew / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) scombine / (double) TOTAL_NUMBER_OF_SYSTEMS;

		writeSystem(("ioa " + 3 + " " + 1 + " " + NoA), result);
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
