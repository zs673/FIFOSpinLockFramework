package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysisWithImplementationOverheads.IAFIFONP;
import analysisWithImplementationOverheads.IAFIFOP;
import analysisWithImplementationOverheads.IANewMrsPRTAWithMCNP;
import analysisWithImplementationOverheads.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.ALLOCATION_POLICY;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.IOAResultReader;
import generatorTools.SystemGeneratorWithAllocation;

public class DynamicTestWithAllocation {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.3;
	public static int TOTAL_PARTITIONS = 16;
	public static boolean testSchedulability = true;
	public static int PROTOCOLS = 3;

	public static void main(String[] args) throws Exception {
		DynamicTestWithAllocation test = new DynamicTestWithAllocation();

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

		final CountDownLatch accesscountdown = new CountDownLatch(20);
		for (int i = 1; i < 22; i = i + 5) {
			final int access = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingContention(access);
					accesscountdown.countDown();
				}
			}).start();
		}

		final CountDownLatch processorscountdown = new CountDownLatch(16);
		for (int i = 2; i < 17; i = i + 2) {
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
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0,
				rlifsmrsp = 0;

		SystemGeneratorWithAllocation generator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD,
				TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, cs_range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

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
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.WORST_FIT);
			Ris = fnp.NewRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.BEST_FIT);

			Ris = fnp.NewRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.FIRST_FIT);
			Ris = fnp.NewRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.NEXT_FIT);
			Ris = fnp.NewRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			Ris = fnp.NewRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			Ris = fnp.NewRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

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
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0,
				rlifsmrsp = 0;

		SystemGeneratorWithAllocation generator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD,
				TOTAL_PARTITIONS, NoT * TOTAL_PARTITIONS, true, range, RESOURCES_RANGE.PARTITIONS,
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
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.WORST_FIT);
			Ris = fnp.NewRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.BEST_FIT);

			Ris = fnp.NewRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.FIRST_FIT);
			Ris = fnp.NewRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.NEXT_FIT);
			Ris = fnp.NewRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			Ris = fnp.NewRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			Ris = fnp.NewRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(1 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

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
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0,
				rlifsmrsp = 0;

		SystemGeneratorWithAllocation generator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD, NoP,
				NUMBER_OF_TASKS_ON_EACH_PARTITION * NoP, true, range, RESOURCES_RANGE.PARTITIONS,
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
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.WORST_FIT);
			Ris = fnp.NewRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.BEST_FIT);

			Ris = fnp.NewRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.FIRST_FIT);
			Ris = fnp.NewRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.NEXT_FIT);
			Ris = fnp.NewRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			Ris = fnp.NewRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			Ris = fnp.NewRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(4 + " " + 1 + " " + NoP + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

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
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0,
				rlifsmrsp = 0;

		SystemGeneratorWithAllocation generator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD,
				TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NoA, false);

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
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.WORST_FIT);
			Ris = fnp.NewRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.newRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.BEST_FIT);

			Ris = fnp.NewRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.newRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.FIRST_FIT);
			Ris = fnp.NewRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.newRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.NEXT_FIT);
			Ris = fnp.NewRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.newRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			Ris = fnp.NewRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.newRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			Ris = fnp.NewRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.newRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.newRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator.allocateTasks(tasksToAlloc, resources,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			Ris = fnp.NewRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.newRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			writeSystem(("ioa " + 3 + " " + 1 + " " + NoA), result);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

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
