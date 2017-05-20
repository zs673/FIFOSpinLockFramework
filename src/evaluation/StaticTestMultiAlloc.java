package evaluation;

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
import analysis.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;
import frameworkWFandDM.StaticSolver;
import generatorTools.GeneatorUtils.ALLOCATION_POLICY;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.IOAResultReader;
import generatorTools.SystemGeneratorWithAllocation;

public class StaticTestMultiAlloc {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.4;
	public static int TOTAL_PARTITIONS = 16;
	public static boolean testSchedulability = true;
	public static int PROTOCOLS = 4;  

	public static void main(String[] args) throws Exception {
		StaticTestMultiAlloc test = new StaticTestMultiAlloc();

		final CountDownLatch cslencountdown = new CountDownLatch(8);
		for (int i = 1; i < 9; i++) {
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
		IOAResultReader.schedreader(null, false);
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		final CS_LENGTH_RANGE cs_range;
		switch (cs_len) {
		case 1:
			cs_range = CS_LENGTH_RANGE.EXTREME_SHORT_CSLEN;
			break;
		case 2:
			cs_range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 3:
			cs_range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 4:
			cs_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 5:
			cs_range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 6:
			cs_range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		case 7:
			cs_range = CS_LENGTH_RANGE.EXTREME_LONG_CSLEN;
			break;
		case 8:
			cs_range = CS_LENGTH_RANGE.RANDOM;
			break;
		default:
			cs_range = null;
			break;
		}

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();
		IACombinedProtocol sCombine = new IACombinedProtocol();

		String result = "";
		int wfsfnp = 0, ffsfnp = 0, bfsfnp = 0, nfsfnp = 0, rrfsfnp = 0, rlfsfnp = 0, rldfsfnp = 0, rlifsfnp = 0;
		int wfsfp = 0, ffsfp = 0, bfsfp = 0, nfsfp = 0, rrfsfp = 0, rlfsfp = 0, rldfsfp = 0, rlifsfp = 0;
		int wfsmrsp = 0, ffsmrsp = 0, bfsmrsp = 0, nfsmrsp = 0, rrfsmrsp = 0, rlfsmrsp = 0, rldfsmrsp = 0, rlifsmrsp = 0;
		int wfscombine = 0, ffscombine = 0, bfscombine = 0, nfscombine = 0, rrfscombine = 0, rlfscombine = 0, rldfscombine = 0,
				rlifscombine = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			int maxAccess = 0;
			int[] protocols = null;
			
			SystemGeneratorWithAllocation generator = new SystemGeneratorWithAllocation(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
					NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS, true, cs_range, RESOURCES_RANGE.PARTITIONS,
					RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			
			/**
			 * WORST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksWF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.WORST_FIT);
			Ris = fnp.NewMrsPRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsfp++;

			Ris = mrsp.getResponseTime(tasksWF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfsmrsp++;

			for (int l = 0; l < tasksWF.size(); l++) {
				for (int j = 0; j < tasksWF.get(l).size(); j++) {
					SporadicTask task = tasksWF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksWF, resources, tasksWF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksWF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksWF, Ris))
				wfscombine++;

			/**
			 * BEST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksBF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.BEST_FIT);
			
			Ris = fnp.NewMrsPRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsfp++;

			Ris = mrsp.getResponseTime(tasksBF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfsmrsp++;

			
			for (int l = 0; l < tasksBF.size(); l++) {
				for (int j = 0; j < tasksBF.get(l).size(); j++) {
					SporadicTask task = tasksBF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksBF, resources, tasksBF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksBF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksBF, Ris))
				bfscombine++;
			
			/**
			 * FIRST FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksFF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.FIRST_FIT);
			Ris = fnp.NewMrsPRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfnp++;

			Ris = fp.NewMrsPRTATest(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsfp++;

			Ris = mrsp.getResponseTime(tasksFF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffsmrsp++;

			
			for (int l = 0; l < tasksFF.size(); l++) {
				for (int j = 0; j < tasksFF.get(l).size(); j++) {
					SporadicTask task = tasksFF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksFF, resources, tasksFF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksFF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksFF, Ris))
				ffscombine++;
			
			/**
			 * NEXT FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksNF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.NEXT_FIT);
			Ris = fnp.NewMrsPRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsfp++;

			Ris = mrsp.getResponseTime(tasksNF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfsmrsp++;

			
			for (int l = 0; l < tasksNF.size(); l++) {
				for (int j = 0; j < tasksNF.get(l).size(); j++) {
					SporadicTask task = tasksNF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksNF, resources, tasksNF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksNF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksNF, Ris))
				nfscombine++;
			
			/**
			 * RESOURCE REQUEST TASKS FIT
			 */
			
			ArrayList<ArrayList<SporadicTask>> tasksRRF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_REQUEST_TASKS_FIT);
			Ris = fnp.NewMrsPRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsfp++;

			Ris = mrsp.getResponseTime(tasksRRF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfsmrsp++;

			
			for (int l = 0; l < tasksRRF.size(); l++) {
				for (int j = 0; j < tasksRRF.get(l).size(); j++) {
					SporadicTask task = tasksRRF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksRRF, resources, tasksRRF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksRRF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRRF, Ris))
				rrfscombine++;
			
			/**
			 * RESOURCE LOCAL FIT
			 */
			
			ArrayList<ArrayList<SporadicTask>> tasksRLF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LOCAL_FIT);
			Ris = fnp.NewMrsPRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsfp++;

			Ris = mrsp.getResponseTime(tasksRLF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfsmrsp++;

			
			for (int l = 0; l < tasksRLF.size(); l++) {
				for (int j = 0; j < tasksRLF.get(l).size(); j++) {
					SporadicTask task = tasksRLF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksRLF, resources, tasksRLF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksRLF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLF, Ris))
				rlfscombine++;
			
			/**
			 * RESOURCE LENGTH DECREASE FIT
			 */
			
			ArrayList<ArrayList<SporadicTask>> tasksRLDF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LENGTH_DECREASE_FIT);
			Ris = fnp.NewMrsPRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfnp++;

			Ris = fp.NewMrsPRTATest(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsfp++;

			Ris = mrsp.getResponseTime(tasksRLDF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfsmrsp++;

			
			for (int l = 0; l < tasksRLDF.size(); l++) {
				for (int j = 0; j < tasksRLDF.get(l).size(); j++) {
					SporadicTask task = tasksRLDF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksRLDF, resources, tasksRLDF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksRLDF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLDF, Ris))
				rldfscombine++;
			
			/**
			 * RESOURCE LENGTH INCREASE FIT
			 */
			ArrayList<ArrayList<SporadicTask>> tasksRLIF = generator.allocateTasks(tasksToAlloc, resources, TOTAL_PARTITIONS,
					ALLOCATION_POLICY.RESOURCE_LENGTH_INCREASE_FIT);
			Ris = fnp.NewMrsPRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfnp++;

			Ris = fp.NewMrsPRTATest(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsfp++;

			Ris = mrsp.getResponseTime(tasksRLIF, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifsmrsp++;

			
			for (int l = 0; l < tasksRLIF.size(); l++) {
				for (int j = 0; j < tasksRLIF.get(l).size(); j++) {
					SporadicTask task = tasksRLIF.get(l).get(j);
					for (int k = 0; k < task.number_of_access_in_one_release.size(); k++) {
						if (maxAccess < task.number_of_access_in_one_release.get(k)) {
							maxAccess = task.number_of_access_in_one_release.get(k);
						}
					}
				}
			}
			protocols = new StaticSolver().solve(tasksRLIF, resources, tasksRLIF.size(), maxAccess, false);
			for (int l = 0; l < resources.size(); l++) {
				resources.get(l).protocol = protocols[l];
			}
			Ris = sCombine.calculateResponseTime(tasksRLIF, resources, testSchedulability, false,
					IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasksRLIF, Ris))
				rlifscombine++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += "WF: " + (double) wfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) wfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) wfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) wfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "BF: " + (double) bfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) bfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) bfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) bfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "FF: " + (double) ffsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) ffsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) ffsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) ffscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "NF: " + (double) nfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) nfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) nfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) nfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "RRF: " + (double) rrfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rrfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rrfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) rrfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "RLF: " + (double) rlfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) rlfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "RLDF: " + (double) rldfsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rldfsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rldfsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) rldfscombine / (double) TOTAL_NUMBER_OF_SYSTEMS + "  ";
		
		result += "RLIF: " + (double) rlifsfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) rlifsfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) rlifsmrsp / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ " " + (double) rlifscombine / (double) TOTAL_NUMBER_OF_SYSTEMS;

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
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
