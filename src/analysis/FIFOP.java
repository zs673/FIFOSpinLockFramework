package analysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.PriorityGeneator;
import utils.AnalysisUtils;

public class FIFOP {

	public long[][] getResponseTimeByDMPO(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, int extendCal, boolean testSchedulability,
			boolean btbHit, boolean useRi, boolean printDebug) {
		if (tasks == null)
			return null;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		long count = 0;
		boolean isEqual = false, missdeadline = false;
		long[][] response_time = AnalysisUtils.initResponseTime(tasks);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			boolean should_finish = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time, extendCal, testSchedulability, btbHit, useRi);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j]) {
						isEqual = false;
					}
					if (testSchedulability) {
						if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
							missdeadline = true;
					} else {
						if (response_time_plus[i][j] <= tasks.get(i).get(j).deadline * extendCal)
							should_finish = false;
					}
				}
			}
			if (count > 1000) {
				AnalysisUtils.printResponseTime(response_time, tasks);
			}
			count++;
			AnalysisUtils.cloneList(response_time_plus, response_time);

			if (testSchedulability) {
				if (missdeadline)
					break;
			} else {
				if (should_finish)
					break;
			}
		}

		if (printDebug) {
			System.out.println("FIFONP JAVA    after " + count + " tims of recursion, we got the response time.");
			AnalysisUtils.printResponseTime(response_time, tasks);
		}

		return response_time;
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time, int extendCal,
			boolean testSchedulability, boolean btbHit, boolean useRi) {
		long[][] response_time_plus = new long[tasks.size()][];

		for (int i = 0; i < response_time.length; i++) {
			response_time_plus[i] = new long[response_time[i].length];
		}

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				if (response_time[i][j] > task.deadline * extendCal) {
					response_time_plus[i][j] = response_time[i][j];
					continue;
				}

				task.fifop = new double[resources.size()];
				for (int k = 0; k < task.fifop.length; k++) {
					task.fifop[k] = 0;
				}
				task.spin_delay_by_preemptions = 0;
				task.implementation_overheads = 0;
				task.implementation_overheads += AnalysisUtils.FULL_CONTEXT_SWTICH1;

				task.spin = getSpinDelay(task, tasks, resources, response_time[i][j], response_time, btbHit, useRi);
				task.interference = highPriorityInterference(task, tasks, resources, response_time, response_time[i][j], btbHit, useRi);
				task.local = localBlocking(task, tasks, resources, response_time, response_time[i][j], btbHit, useRi);

				long implementation_overheads = (long) Math.ceil(task.implementation_overheads);
				response_time_plus[i][j] = task.Ri = task.WCET + task.spin + task.interference + task.local + implementation_overheads;

				if (testSchedulability && task.Ri > task.deadline) {
					return response_time_plus;
				}
			}
		}
		return response_time_plus;
	}

	private long getSpinDelay(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long time, long[][] Ris,
			boolean btbHit, boolean useRi) {
		long spin = 0;
		ArrayList<ArrayList<Long>> requestsLeftOnRemoteP = new ArrayList<>();
		ArrayList<Resource> fifop_resources = new ArrayList<>();
		for (int i = 0; i < resources.size(); i++) {
			requestsLeftOnRemoteP.add(new ArrayList<Long>());
			fifop_resources.add(resources.get(i));
			Resource res = resources.get(i);
			spin += getSpinDelayForOneResoruce(task, res, tasks, requestsLeftOnRemoteP.get(i), time, Ris, btbHit, useRi);
		}

		// Preemption
		long preemptions = 0;
		long request_by_preemptions = 0;
		for (int i = 0; i < tasks.get(task.partition).size(); i++) {
			if (tasks.get(task.partition).get(i).priority > task.priority) {
				preemptions += (int) Math.ceil((time) / (double) tasks.get(task.partition).get(i).period);
			}
		}

		task.implementation_overheads += preemptions * (AnalysisUtils.FIFOP_CANCEL);

		while (preemptions > 0) {

			long max_delay = 0;
			Resource resource = null;
			int max_delay_resource_index = -1;
			for (int i = 0; i < fifop_resources.size(); i++) {
				if (max_delay < fifop_resources.get(i).csl * requestsLeftOnRemoteP.get(i).size()) {
					max_delay = fifop_resources.get(i).csl * requestsLeftOnRemoteP.get(i).size();
					max_delay_resource_index = i;
					resource = fifop_resources.get(i);
				}
			}

			if (max_delay > 0) {
				spin += max_delay;
				task.fifop[resource.id - 1] += max_delay;
				for (int i = 0; i < requestsLeftOnRemoteP.get(max_delay_resource_index).size(); i++) {
					requestsLeftOnRemoteP.get(max_delay_resource_index).set(i, requestsLeftOnRemoteP.get(max_delay_resource_index).get(i) - 1);
					if (requestsLeftOnRemoteP.get(max_delay_resource_index).get(i) < 1) {
						requestsLeftOnRemoteP.get(max_delay_resource_index).remove(i);
						i--;
					}
				}
				preemptions--;
				request_by_preemptions++;
			} else
				break;
		}

		task.spin_delay_by_preemptions = request_by_preemptions;

		return spin;
	}

	private long getSpinDelayForOneResoruce(SporadicTask task, Resource resource, ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<Long> requestsLeftOnRemoteP, long time, long[][] Ris, boolean btbHit, boolean useRi) {
		long spin = 0;
		long ncs = 0;

		for (int i = 0; i < tasks.get(task.partition).size(); i++) {
			SporadicTask hpTask = tasks.get(task.partition).get(i);
			if (hpTask.priority > task.priority && hpTask.resource_required_index.contains(resource.id - 1)) {
				ncs += (int) Math.ceil((double) (time + (btbHit ? (useRi ? Ris[hpTask.partition][i] : hpTask.deadline) : 0)) / (double) hpTask.period)
						* hpTask.number_of_access_in_one_release.get(hpTask.resource_required_index.indexOf(resource.id - 1));
			}
		}

		if (task.resource_required_index.contains(resource.id - 1))
			ncs += task.number_of_access_in_one_release.get(task.resource_required_index.indexOf(resource.id - 1));

		if (ncs > 0) {
			for (int i = 0; i < tasks.size(); i++) {
				if (task.partition != i) {
					/* For each remote partition */
					long number_of_request_by_Remote_P = 0;
					for (int j = 0; j < tasks.get(i).size(); j++) {
						if (tasks.get(i).get(j).resource_required_index.contains(resource.id - 1)) {
							SporadicTask remote_task = tasks.get(i).get(j);
							int indexR = getIndexRInTask(remote_task, resource);
							int number_of_release = (int) Math
									.ceil((double) (time + (btbHit ? (useRi ? Ris[i][j] : remote_task.deadline) : 0)) / (double) remote_task.period);
							number_of_request_by_Remote_P += number_of_release * remote_task.number_of_access_in_one_release.get(indexR);
						}
					}

					long possible_spin_delay = Long.min(number_of_request_by_Remote_P, ncs);
					spin += possible_spin_delay;
					if (number_of_request_by_Remote_P - ncs > 0)
						requestsLeftOnRemoteP.add(number_of_request_by_Remote_P - ncs);
				}
			}
		}

		task.implementation_overheads += (spin + ncs) * (AnalysisUtils.FIFOP_LOCK + AnalysisUtils.FIFOP_UNLOCK);

		task.fifop[resource.id - 1] += spin * resource.csl + ncs * resource.csl + (spin + ncs) * (AnalysisUtils.FIFOP_LOCK + AnalysisUtils.FIFOP_UNLOCK);
		return spin * resource.csl + ncs * resource.csl;
	}

	/*
	 * Calculate the local high priority tasks' interference for a given task t.
	 * CI is a set of computation time of local tasks, including spin delay.
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, ArrayList<Resource> resources, long[][] Ris, long time,
			boolean btbHit, boolean useRi) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (time) / (double) hpTask.period) * (hpTask.WCET);
				t.implementation_overheads += Math.ceil((double) (time) / (double) hpTask.period) * (AnalysisUtils.FULL_CONTEXT_SWTICH2);
			}
		}
		return interference;
	}

	private long localBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] Ris, long Ri, boolean btbHit,
			boolean useRi) {
		ArrayList<Resource> LocalBlockingResources = getLocalBlockingResources(t, resources, tasks.get(t.partition));
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;
			local_blocking_each_resource.add(local_blocking);
			t.fifop[res.id - 1] += res.csl + AnalysisUtils.FIFOP_LOCK + AnalysisUtils.FIFOP_UNLOCK;
		}

		if (local_blocking_each_resource.size() > 1)
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));

		if (local_blocking_each_resource.size() > 0)
			t.implementation_overheads += AnalysisUtils.FIFOP_LOCK + AnalysisUtils.FIFOP_UNLOCK;

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;
	}

	private ArrayList<Resource> getLocalBlockingResources(SporadicTask task, ArrayList<Resource> resources, ArrayList<SporadicTask> localTasks) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			// local resources that have a higher ceiling
			if (resource.partitions.size() == 1 && resource.partitions.get(0) == partition && resource.getCeilingForProcessor(localTasks) >= task.priority) {
				for (int j = 0; j < resource.requested_tasks.size(); j++) {
					SporadicTask LP_task = resource.requested_tasks.get(j);
					if (LP_task.partition == partition && LP_task.priority < task.priority) {
						localBlockingResources.add(resource);
						break;
					}
				}
			}
			// global resources that are accessed from the partition
			if (resource.partitions.contains(partition) && resource.partitions.size() > 1) {
				for (int j = 0; j < resource.requested_tasks.size(); j++) {
					SporadicTask LP_task = resource.requested_tasks.get(j);
					if (LP_task.partition == partition && LP_task.priority < task.priority) {
						localBlockingResources.add(resource);
						break;
					}
				}
			}
		}

		return localBlockingResources;
	}

	/*
	 * Return the index of a given resource in stored in a task.
	 */
	private int getIndexRInTask(SporadicTask task, Resource resource) {
		int indexR = -1;
		if (task.resource_required_index.contains(resource.id - 1)) {
			for (int j = 0; j < task.resource_required_index.size(); j++) {
				if (resource.id - 1 == task.resource_required_index.get(j)) {
					indexR = j;
					break;
				}
			}
		}
		return indexR;
	}
}
