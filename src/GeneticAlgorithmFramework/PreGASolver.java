package GeneticAlgorithmFramework;

import java.util.ArrayList;
import java.util.Random;

import analysis.CombinedAnalysis;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;

public class PreGASolver {
	int ALLOCATION_POLICY_NUMBER;
	int PRIORITY_SCHEME_NUMBER;

	int PROTOCOL_NUMBER;

	boolean print;
	ArrayList<SporadicTask> tasks;
	ArrayList<Resource> resources;
	SystemGenerator geneator;

	AllocationGeneator allocGeneator = new AllocationGeneator();
	CombinedAnalysis analysis = new CombinedAnalysis();

	public int allocation = -1;
	public int protocol = -1; // 1 MrsP; 2 FIFONP; 3 FIFOP;
	public int priority = -1;

	boolean lazyMode; // If on, tell GA to finish if the system is unlike
						// to schedule.

	public static boolean useRi = true;
	public static boolean btbHit = true;
	public static boolean testSchedulability = false;

	public PreGASolver(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources, SystemGenerator geneator, int PROTOCOL_NUMBER,
			int ALLOCATION_POLICY_NUMBER, int PRIORITY_SCHEME_NUMBER, boolean print) {
		this.PROTOCOL_NUMBER = PROTOCOL_NUMBER;
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.PRIORITY_SCHEME_NUMBER = PRIORITY_SCHEME_NUMBER;
		this.geneator = geneator;
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;
	}

	/**
	 * Perform an initial check to see whether there is a feasible static
	 * solution.
	 * 
	 * @return 0: needs GA; -1: not possible; 1: is feasible;
	 */
	public int initialCheck(boolean lazyMode) {
		this.lazyMode = lazyMode;
		int notpossiblecount = 0;

		for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
			for (int j = 0; j < PRIORITY_SCHEME_NUMBER; j++) {
				int result = checkwithOneAllocationAndOnePriority(i, j);
				if (result == -1)
					notpossiblecount++;
				if (result > 0)
					return result; // has a feasible solution
			}
		}

		if (notpossiblecount == ALLOCATION_POLICY_NUMBER * PRIORITY_SCHEME_NUMBER) {
			if (print) {
				System.out.println("inital check say: NO POSSIBLE");
			}
			return -1; // not possible
		}

		return 0; // need GA
	}

	/**
	 * 
	 * @param allocPolicy
	 *            The allocation policy assumed ( 0 - 7 )
	 * @return 0: needs GA; -1: not possible; 1: is feasible by MrsP; 2: is
	 *         feasible by MSRP; 1: is feasible by FIFO-P;
	 */
	private int checkwithOneAllocationAndOnePriority(int allocPolicy, int priorityPolicy) {
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;

		ArrayList<ArrayList<SporadicTask>> tasksWithAlloc = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, allocPolicy);

		if (tasksWithAlloc == null)
			return -1;

		int[][] taskschedule_fifonp = null;
		int[][] taskschedule_fifop = null;
		int[][] taskschedule_mrsp = null;

		switch (priorityPolicy) {
		case 0:
			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 1;
			}
			taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByDMPO(tasksWithAlloc, resources,
					AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, true, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 2;
			}
			taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByDMPO(tasksWithAlloc, resources,
					AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, true, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 3;
			}
			taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByDMPO(tasksWithAlloc, resources,
					AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, true, false));
			break;

		case 1:
			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 1;
			}
			taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByOPA(tasksWithAlloc, resources, btbHit, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 2;
			}
			taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByOPA(tasksWithAlloc, resources, btbHit, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 3;
			}
			taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeByOPA(tasksWithAlloc, resources, btbHit, false));
			break;

		case 2:
			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = new Random().nextInt(65535) % 3 + 1;
			}
			taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySBPO(tasksWithAlloc, resources, false));
			taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySBPO(tasksWithAlloc, resources, false));
			taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySBPO(tasksWithAlloc, resources, false));

			break;

		default:
			break;
		}

		for (int i = 0; i < tasksWithAlloc.size(); i++) {
			for (int j = 0; j < tasksWithAlloc.get(i).size(); j++) {
				if (taskschedule_fifonp[i][j] == 0)
					fifonp_sched++;
				if (taskschedule_fifop[i][j] == 0)
					fifop_sched++;
				if (taskschedule_mrsp[i][j] == 0)
					mrsp_sched++;

				if (lazyMode && taskschedule_fifonp[i][j] == taskschedule_fifop[i][j] && taskschedule_fifop[i][j] == taskschedule_mrsp[i][j]
						&& taskschedule_mrsp[i][j] == 0) {
					isPossible = false;

				}
			}
		}

		/*
		 * If the system is feasible with DM.
		 */
		if (mrsp_sched == 0) {
			if (print)
				System.out.println("mrsp schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy + 1;
			this.protocol = 1;
			this.priority = priorityPolicy + 1;
			return 1;
		}
		if (fifonp_sched == 0) {
			if (print)
				System.out.println("fifonp schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy + 1;
			this.protocol = 2;
			this.priority = priorityPolicy + 1;
			return 1;
		}
		if (fifop_sched == 0) {
			if (print)
				System.out.println("fifop schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy + 1;
			this.protocol = 3;
			this.priority = priorityPolicy + 1;
			return 1;
		}

		/*
		 * If not possible in all cases, we suggest the GA to finish.
		 */
		if (lazyMode && !isPossible) {
			if (print)
				System.out.println("not schedulable");
			return -1;
		}

		return 0;
	}

	boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}

	int[][] getTaskSchedulability(ArrayList<ArrayList<SporadicTask>> tasks, long[][] rt) {
		int[][] tasksrt = new int[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			tasksrt[i] = new int[tasks.get(i).size()];
		}

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < rt[i][j])
					tasksrt[i][j] = 0;
				else
					tasksrt[i][j] = 1;
			}
		}

		return tasksrt;
	}
}
