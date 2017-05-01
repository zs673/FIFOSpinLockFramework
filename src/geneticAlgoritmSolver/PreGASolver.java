package geneticAlgoritmSolver;

import java.util.ArrayList;

import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;

public class PreGASolver {
	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;

	IAFIFOP fifop = new IAFIFOP();
	IAFIFONP fifonp = new IAFIFONP();
	IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

	public PreGASolver(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean print) {
		this.tasks = tasks;
		this.resources = resources;
	}

	public int initialCheck() {
		int[][] taskschedule_fifonp = getTaskSchedulability(fifonp.NewMrsPRTATest(tasks, resources, false, false));
		int[][] taskschedule_fifop = getTaskSchedulability(fifop.NewMrsPRTATest(tasks, resources, false, false));
		int[][] taskschedule_mrsp = getTaskSchedulability(mrsp.getResponseTime(tasks, resources, false, false));

		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;
		
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(0).size(); j++) {
				if (taskschedule_fifonp[i][j] == 0)
					fifonp_sched++;
				if (taskschedule_fifop[i][j] == 0)
					fifop_sched++;
				if (taskschedule_mrsp[i][j] == 0)
					mrsp_sched++;
				
				if (taskschedule_fifonp[i][j] == taskschedule_fifop[i][j] && taskschedule_fifop[i][j] == taskschedule_mrsp[i][j]
						&& taskschedule_mrsp[i][j] == 0) {
					isPossible = false;

				}
			}
		}
		
		if (!isPossible) {
			System.out.println("not schedulable");
			return -1;
		}
		if (fifonp_sched == 0) {
			System.out.println("fifonp schedulable");
			return 1;
		}
		if (fifop_sched == 0) {
			System.out.println("fifop schedulable");
			return 2;
		}
		if (mrsp_sched == 0) {
			System.out.println("mrsp schedulable");
			return 3;
		}

		return 0;
	}

	int[][] getTaskSchedulability(long[][] rt) {
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
