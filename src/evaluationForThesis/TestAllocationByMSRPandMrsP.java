package evaluationForThesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.FIFONP;
import analysis.FIFOP;
import analysis.MrsP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.TestResultFileReader;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

/**
 * OLD Approach Work Load CS Length 11 WF: 0.958 0.776 BF: 0.969 0.795 FF: 0.963
 * 0.793 NF: 0.972 0.838 RRF: 0.897 0.853 RLF: 0.865 0.786 RLDF: 0.896 0.836
 * RLIF: 0.898 0.845 12 WF: 0.812 0.667 BF: 0.829 0.701 FF: 0.826 0.699 NF: 0.85
 * 0.724 RRF: 0.844 0.784 RLF: 0.792 0.724 RLDF: 0.834 0.769 RLIF: 0.831 0.777
 * 13 WF: 0.543 0.552 BF: 0.613 0.568 FF: 0.613 0.569 NF: 0.676 0.645 RRF: 0.763
 * 0.714 RLF: 0.686 0.629 RLDF: 0.75 0.708 RLIF: 0.758 0.703 14 WF: 0.12 0.394
 * BF: 0.125 0.376 FF: 0.122 0.378 NF: 0.131 0.449 RRF: 0.547 0.525 RLF: 0.397
 * 0.449 RLDF: 0.554 0.523 RLIF: 0.544 0.534 15 WF: 0.046 0.316 BF: 0.024 0.286
 * FF: 0.023 0.289 NF: 0.04 0.373 RRF: 0.433 0.456 RLF: 0.293 0.384 RLDF: 0.408
 * 0.431 RLIF: 0.401 0.426 16 WF: 0.077 0.404 BF: 0.071 0.387 FF: 0.074 0.391
 * NF: 0.081 0.464 RRF: 0.526 0.528 RLF: 0.366 0.456 RLDF: 0.541 0.526 RLIF:
 * 0.482 0.52 Resource Access Parallel RSF
 *
 */

public class TestAllocationByMSRPandMrsP {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.3;
	public static int TOTAL_PARTITIONS = 16;
	public static boolean testSchedulability = true;
	public static boolean useRi = true;
	public static boolean btbHit = true;
	public static int PROTOCOLS = 3;

	AllocationGeneator allocGeneator = new AllocationGeneator();

	public static void main(String[] args) throws Exception {
		TestAllocationByMSRPandMrsP test = new TestAllocationByMSRPandMrsP();

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
		cslencountdown.await();

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
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, cs_range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		FIFONP fnp = new FIFONP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = null;
			ArrayList<Resource> resources = null;
			while (tasksToAlloc == null) {
				tasksToAlloc = generator.generateTasks();
				resources = generator.generateResources();

				generator.generateResourceUsage(tasksToAlloc, resources);

				int allocOK = 0;

				for (int a = 0; a < 8; a++) {
					if (allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, a) != null)
						allocOK++;
				}

				if (allocOK != 8) {
					tasksToAlloc = null;
				}
			}

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
			Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
			Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
			Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRRF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 4);
			Ris = fnp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 5);
			Ris = fnp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLDF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 6);
			Ris = fnp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 7);
			Ris = fnp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		long[][] Ris;

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NoT * TOTAL_PARTITIONS, RESOURCE_SHARING_FACTOR, range,
				RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);

			Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
			Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
			Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 4);
			Ris = fnp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 5);
			Ris = fnp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 6);
			Ris = fnp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 7);
			Ris = fnp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(1 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
	}

	public void experimentIncreasingParallel(int NoP) {
		long[][] Ris;

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NUMBER_OF_TASKS_ON_EACH_PARTITION * NoP, RESOURCE_SHARING_FACTOR,
				range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		FIFONP fnp = new FIFONP();
		FIFOP fp = new FIFOP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);

			Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
			Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
			Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRRF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 4);
			Ris = fnp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 5);
			Ris = fnp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */

			ArrayList<ArrayList<SporadicTask>> tasksRLDF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 6);
			Ris = fnp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 7);
			Ris = fnp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(4 + " " + 1 + " " + NoP + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		writeSystem(("ioa " + 4 + " " + 1 + " " + NoP), result);
	}

	public void experimentIncreasingContention(int NoA) {
		long[][] Ris;
		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, range, RESOURCES_RANGE.PARTITIONS, NoA, false);

		FIFONP fnp = new FIFONP();
		MrsP mrsp = new MrsP();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = null;
			ArrayList<Resource> resources = null;
			while (tasksToAlloc == null) {
				tasksToAlloc = generator.generateTasks();
				resources = generator.generateResources();

				generator.generateResourceUsage(tasksToAlloc, resources);

				int allocOK = 0;

				for (int a = 0; a < 8; a++) {
					if (allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, a) != null)
						allocOK++;
				}

				if (allocOK != 8) {
					tasksToAlloc = null;
				}
			}

			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
			Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
			Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
			Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
			Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			/**
			 * RESOURCE REQUEST TASKS FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRRF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 4);
			Ris = fnp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRRF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			/**
			 * RESOURCE LOCAL FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 5);
			Ris = fnp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLDF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 6);
			Ris = fnp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLDF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = allocGeneator.allocateTasks(tasksToAlloc, resources, generator.total_partitions, 7);
			Ris = fnp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = mrsp.getResponseTimeByDMPO(tasksRLIF, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			System.out.println(3 + " " + 1 + " " + NoA + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";

		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " ";

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
