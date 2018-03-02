package basicAnalysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.PriorityGeneator;
import utils.AnalysisUtils;

public class SimpleRTA {

	public long[][] getResponseTimeByDM(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean printBebug) {
		if (tasks == null)
			return null;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		long count = 0;
		boolean isEqual = false, missDeadline = false;
		long[][] response_time = AnalysisUtils.initResponseTime(tasks);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j])
						isEqual = false;
					if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
						missDeadline = true;
				}
			}

			count++;
			AnalysisUtils.cloneList(response_time_plus, response_time);
			if (missDeadline)
				break;
		}

		if (printBebug) {
			if (missDeadline)
				System.out.println("after " + count + " tims of recursion, the tasks miss the deadline.");
			else
				System.out.println("after " + count + " tims of recursion, we got the response time.");
			AnalysisUtils.printResponseTime(response_time, tasks);
		}

		return response_time;
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time) {
		long[][] response_time_plus = new long[tasks.size()][];
		for (int i = 0; i < response_time.length; i++)
			response_time_plus[i] = new long[response_time[i].length];

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				task.interference = highPriorityInterference(task, tasks, response_time[i][j]);
				response_time_plus[i][j] = task.Ri = task.WCET + task.pure_resource_execution_time + task.interference;
				if (task.Ri > task.deadline)
					return response_time_plus;
			}
		}
		return response_time_plus;

	}

	/*
	 * Calculate the local high priority tasks' interference for a given task t.
	 * CI is a set of computation time of local tasks, including spin delay.
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, long Ri) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (Ri) / (double) hpTask.period) * (hpTask.WCET + hpTask.pure_resource_execution_time);
			}
		}
		return interference;
	}
}
