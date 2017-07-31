package GeneticAlgorithmFramework;

import java.util.ArrayList;

import analysis.CombinedAnalysis;
import analysis.FIFONP;
import analysis.FIFOP;
import analysis.MrsP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;

public class PreGASolver {
	int ALLOCATION_POLICY_NUMBER;
	int PROTOCOL_NUMBER;

	boolean print;
	ArrayList<SporadicTask> tasks;
	ArrayList<Resource> resources;
	SystemGenerator geneator;

	FIFONP fifonp = new FIFONP();
	FIFOP fifop = new FIFOP();
	MrsP mrsp = new MrsP();
	CombinedAnalysis analysis = new CombinedAnalysis();

	public int allocation = -1;
	public int protocol = -1;
	public int priority = -1;

	boolean lazyMode; // If on, tell GA to finish if the system is unlike
						// to schedule.

	public static boolean useRi = true;
	public static boolean btbHit = true;
	public static boolean testSchedulability = false;

	public PreGASolver(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources, SystemGenerator geneator,
			int PROTOCOL_NUMBER, int ALLOCATION_POLICY_NUMBER, int PRIORITY_SCHEME_NUMBER, boolean print) {
		this.PROTOCOL_NUMBER = PROTOCOL_NUMBER;
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.geneator = geneator;
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;
	}

	/**
	 * Perform an initial check to see whether there is a feasible static
	 * solution.
	 * 
	 * @return 1: is feasible; 0: needs GA; -1: not possible
	 */
	public int initialCheck(boolean lazyMode) {
		this.lazyMode = lazyMode;
		int notpossiblecount = 0;

		for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
			int result = checkwithOneAllocationPolicyDM(i);
			if (result == -1)
				notpossiblecount++;
			if (result > 0)
				return result; // has a feasible solution
		}

		if (notpossiblecount == ALLOCATION_POLICY_NUMBER) {
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
	 * @return 1: is feasible; 0: needs GA; -1: not possible
	 */
	private int checkwithOneAllocationPolicyDM(int allocPolicy) {
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;

		ArrayList<ArrayList<SporadicTask>> tasksWithAlloc = geneator.allocateTasks(tasks, resources, allocPolicy);

		if (tasksWithAlloc == null)
			return -1;

		int[][] taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, fifonp.getResponseTimeByDM(tasksWithAlloc, resources,
				AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false));
		int[][] taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, fifop.getResponseTimeByDM(tasksWithAlloc, resources,
				AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false));
		int[][] taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, mrsp.getResponseTimeByDM(tasksWithAlloc, resources,
				AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false));

		for (int i = 0; i < tasksWithAlloc.size(); i++) {
			for (int j = 0; j < tasksWithAlloc.get(i).size(); j++) {
				if (taskschedule_fifonp[i][j] == 0)
					fifonp_sched++;
				if (taskschedule_fifop[i][j] == 0)
					fifop_sched++;
				if (taskschedule_mrsp[i][j] == 0)
					mrsp_sched++;

				if (lazyMode && taskschedule_fifonp[i][j] == taskschedule_fifop[i][j]
						&& taskschedule_fifop[i][j] == taskschedule_mrsp[i][j] && taskschedule_mrsp[i][j] == 0) {
					isPossible = false;

				}
			}
		}

		/*
		 * If the system is feasible with DM.
		 */
		if (fifonp_sched == 0) {
			if (print)
				System.out.println("fifonp schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy;
			this.protocol = 1;
			this.priority = 0;
			return 1;
		}
		if (fifop_sched == 0) {
			if (print)
				System.out.println("fifop schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy;
			this.protocol = 2;
			this.priority = 0;
			return 1;
		}
		if (mrsp_sched == 0) {
			if (print)
				System.out.println("mrsp schedulable with allocation: " + allocPolicy);
			this.allocation = allocPolicy;
			this.protocol = 3;
			this.priority = 0;
			return 1;
		}

		/*
		 * If not found with DM, we try it again with OPA
		 **/

		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = 1;
		}
		long[][] rt_fifonp = analysis.getResponseTimeByOPA(tasksWithAlloc, resources, true, false);
		if (isSystemSchedulable(tasksWithAlloc, rt_fifonp)) {
			if (print)
				System.out.println("fifonp schedulable with allocation: " + allocPolicy + " and OPA");
			this.allocation = allocPolicy;
			this.protocol = 1;
			this.priority = 1;
			return 1;
		}

		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = 2;
		}
		long[][] rt_fifop = analysis.getResponseTimeByOPA(tasksWithAlloc, resources, true, false);
		if (isSystemSchedulable(tasksWithAlloc, rt_fifop)) {
			if (print)
				System.out.println("fifop schedulable with allocation: " + allocPolicy + " and OPA");
			this.allocation = allocPolicy;
			this.protocol = 2;
			this.priority = 1;
			return 1;
		}

		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = 3;
		}
		long[][] rt_fifomrsp = analysis.getResponseTimeByOPA(tasksWithAlloc, resources, true, false);
		if (isSystemSchedulable(tasksWithAlloc, rt_fifomrsp)) {
			if (print)
				System.out.println("fifomrsp schedulable with allocation: " + allocPolicy + " and OPA");
			this.allocation = allocPolicy;
			this.protocol = 3;
			this.priority = 1;
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
