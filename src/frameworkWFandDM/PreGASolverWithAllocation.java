package frameworkWFandDM;

import java.util.ArrayList;

import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import analysis.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGeneratorWithAllocation;

public class PreGASolverWithAllocation {

	boolean print;
	ArrayList<Resource> resources;
	ArrayList<SporadicTask> tasks;

	SystemGeneratorWithAllocation geneator;
	IAFIFONP fifonp = new IAFIFONP();
	IAFIFOP fifop = new IAFIFOP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	public PreGASolverWithAllocation(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources,
			SystemGeneratorWithAllocation geneator, boolean print) {
		this.geneator = geneator;
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;
	}

	public int initialCheck() {
		for (int i = 0; i < 8; i++) {
			int result = checkwithOneAllocationPolicy(i);
			if (result > 0)
				return result;
		}
		return 0;
	}

	private int checkwithOneAllocationPolicy(int allocPolicy) {
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;

		ArrayList<ArrayList<SporadicTask>> tasksWithAlloc = geneator.allocateTasks(tasks, resources, allocPolicy);
		int[][] taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc,
				fifonp.NewRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));
		int[][] taskschedule_fifop = getTaskSchedulability(tasksWithAlloc,
				fifop.newRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));
		int[][] taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc,
				mrsp.newRTATest(tasksWithAlloc, resources, false, false, IOAAnalysisUtils.extendCalForStatic));

		for (int i = 0; i < tasksWithAlloc.size(); i++) {
			for (int j = 0; j < tasksWithAlloc.get(0).size(); j++) {
				if (taskschedule_fifonp[i][j] == 0)
					fifonp_sched++;
				if (taskschedule_fifop[i][j] == 0)
					fifop_sched++;
				if (taskschedule_mrsp[i][j] == 0)
					mrsp_sched++;

				if (taskschedule_fifonp[i][j] == taskschedule_fifop[i][j]
						&& taskschedule_fifop[i][j] == taskschedule_mrsp[i][j] && taskschedule_mrsp[i][j] == 0) {
					isPossible = false;

				}
			}
		}

		if (!isPossible) {
			if (print)
				System.out.println("not schedulable");
			return -1;
		}

		if (fifonp_sched == 0) {
			if (print)
				System.out.println("fifonp schedulable");
			return 1;
		}
		if (fifop_sched == 0) {
			if (print)
				System.out.println("fifop schedulable");
			return 2;
		}
		if (mrsp_sched == 0) {
			if (print)
				System.out.println("mrsp schedulable");
			return 3;
		}

		return 0;
	}

	int[][] getTaskSchedulability(ArrayList<ArrayList<SporadicTask>> tasks, long[][] rt) {
		int[][] tasksrt = new int[tasks.size()][tasks.get(0).size()];

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
