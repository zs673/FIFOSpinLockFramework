package GeneticAlgorithmFramework;

import java.util.ArrayList;

import analysisWithImplementationOverheads.IAFIFONP;
import analysisWithImplementationOverheads.IAFIFOP;
import analysisWithImplementationOverheads.IANewMrsPRTAWithMCNP;
import analysisWithImplementationOverheads.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGeneratorWithAllocation;

public class PreGASolverWithAllocation {
	int ALLOCATION_POLICY_NUMBER;

	boolean print;
	ArrayList<SporadicTask> tasks;
	ArrayList<Resource> resources;
	SystemGeneratorWithAllocation geneator;

	IAFIFONP fifonp = new IAFIFONP();
	IAFIFOP fifop = new IAFIFOP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	public PreGASolverWithAllocation(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources,
			SystemGeneratorWithAllocation geneator, int ALLOCATION_POLICY_NUMBER, boolean print) {
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.geneator = geneator;
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;
	}

	public int initialCheck() {
		for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
			int result = checkwithOneAllocationPolicy(i);
			if (result > 0)
				return result;
		}
		return 0;
	}

	private int checkwithOneAllocationPolicy(int allocPolicy) {
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;

		ArrayList<ArrayList<SporadicTask>> tasksWithAlloc = geneator.allocateTasks(tasks, resources, allocPolicy);
		if (tasksWithAlloc == null)
			return 0;

		int[][] taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc,
				fifonp.NewRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));
		int[][] taskschedule_fifop = getTaskSchedulability(tasksWithAlloc,
				fifop.newRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));
		int[][] taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc,
				mrsp.newRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));

		for (int i = 0; i < tasksWithAlloc.size(); i++) {
			for (int j = 0; j < tasksWithAlloc.get(i).size(); j++) {
				if (taskschedule_fifonp[i][j] == 0)
					fifonp_sched++;
				if (taskschedule_fifop[i][j] == 0)
					fifop_sched++;
				if (taskschedule_mrsp[i][j] == 0)
					mrsp_sched++;
			}
		}

		if (fifonp_sched == 0) {
			if (print)
				System.out.println("fifonp schedulable with allocation: " + allocPolicy);
			return 1;
		}
		if (fifop_sched == 0) {
			if (print)
				System.out.println("fifop schedulable with allocation: " + allocPolicy);
			return 2;
		}
		if (mrsp_sched == 0) {
			if (print)
				System.out.println("mrsp schedulable with allocation: " + allocPolicy);
			return 3;
		}

		return 0;
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