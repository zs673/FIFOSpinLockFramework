package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysisWithRiOnly.IAFIFONP;
import analysisWithRiOnly.IAFIFOP;
import analysisWithRiOnly.IANewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.IOAResultReader;
import generatorTools.SystemGenerator;

public class StaticTest {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 5000;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.3;
	public static int TOTAL_PARTITIONS = 16;
	public static boolean testSchedulability = true;
	public static int PROTOCOLS = 3;

	public static void main(String[] args) throws Exception {
		StaticTest test = new StaticTest();

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

		final CountDownLatch accesscountdown = new CountDownLatch(5);
		for (int i = 1; i < 21; i++) {
			final int access = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingContention(access);
					accesscountdown.countDown();
				}
			}).start();
		}

		final CountDownLatch processorscountdown = new CountDownLatch(8);
		for (int i = 2; i < 33; i = i + 2) {
			final int processors = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingParallel(processors);
					processorscountdown.countDown();
				}
			}).start();
		}

		cslencountdown.await();
		workloadcountdown.await();
		accesscountdown.await();
		processorscountdown.await();
		IOAResultReader.schedreader(null, false);
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
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, cs_range, RESOURCES_RANGE.PARTITIONS,
				RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);
			Ris = fnp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 1), resources);

			Ris = fnp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 2), resources);
			Ris = fnp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 3), resources);
			Ris = fnp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 4), resources);
			Ris = fnp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 5), resources);
			Ris = fnp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 6), resources);
			Ris = fnp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 7), resources);
			Ris = fnp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		long[][] Ris;

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS, NoT * TOTAL_PARTITIONS, true,
				range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);
			Ris = fnp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 1), resources);

			Ris = fnp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 2), resources);
			Ris = fnp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 3), resources);
			Ris = fnp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 4), resources);
			Ris = fnp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 5), resources);
			Ris = fnp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 6), resources);
			Ris = fnp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 7), resources);
			Ris = fnp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(1 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
	}

	public void experimentIncreasingParallel(int NoP) {
		long[][] Ris;

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, NoP, NUMBER_OF_TASKS_ON_EACH_PARTITION * NoP,
				true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);
			Ris = fnp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 1), resources);

			Ris = fnp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 2), resources);
			Ris = fnp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 3), resources);
			Ris = fnp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 4), resources);
			Ris = fnp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 5), resources);
			Ris = fnp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 6), resources);
			Ris = fnp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 7), resources);
			Ris = fnp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(4 + " " + 1 + " " + NoP + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 4 + " " + 1 + " " + NoP), result);
	}

	public void experimentIncreasingContention(int NoA) {
		long[][] Ris;

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
				NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, range, RESOURCES_RANGE.PARTITIONS,
				RESOURCE_SHARING_FACTOR, NoA, false);

		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);
			Ris = fnp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTime(tasksWF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 1), resources);

			Ris = fnp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTime(tasksBF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 2), resources);
			Ris = fnp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTime(tasksFF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 3), resources);
			Ris = fnp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTime(tasksNF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 4), resources);
			Ris = fnp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTime(tasksRRF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 5), resources);
			Ris = fnp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTime(tasksRLF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 6), resources);
			Ris = fnp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTime(tasksRLDF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 7), resources);
			Ris = fnp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTime(tasksRLIF, resources, testSchedulability, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(3 + " " + 1 + " " + NoA + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

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
