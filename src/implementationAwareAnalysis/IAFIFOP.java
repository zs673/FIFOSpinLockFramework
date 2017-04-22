package implementationAwareAnalysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.Utils;

public class IAFIFOP {
	long count = 0;

	public long[][] NewMrsPRTATest(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean testSchedulability, boolean printDebug) {

		long[][] init_Ri = Utils.initResponseTime(tasks);

		long[][] response_time = new long[tasks.size()][];
		boolean isEqual = false, missdeadline = false;
		count = 0;

		for (int i = 0; i < init_Ri.length; i++) {
			response_time[i] = new long[init_Ri[i].length];
		}

		Utils.cloneList(init_Ri, response_time);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			boolean should_finish = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time, testSchedulability);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j]) {
						isEqual = false;
					}
					if (testSchedulability) {
						if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
							missdeadline = true;
					} else {
						if (response_time_plus[i][j] <= tasks.get(i).get(j).deadline)
							should_finish = false;
					}
				}
			}
			if (count > 1000) {
				Utils.printResponseTime(response_time, tasks);
			}
			count++;
			Utils.cloneList(response_time_plus, response_time);

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
			Utils.printResponseTime(response_time, tasks);
		}

		return response_time;
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time, boolean testSchedulability) {
		long[][] response_time_plus = new long[tasks.size()][];

		for (int i = 0; i < response_time.length; i++) {
			response_time_plus[i] = new long[response_time[i].length];
		}

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				tasks.get(i).get(j).fifop = new double[resources.size()];
				for (int k = 0; k < tasks.get(i).get(j).fifop.length; k++) {
					tasks.get(i).get(j).fifop[k] = 0;
				}
			}
		}

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				if (response_time[i][j] > task.deadline){
					response_time_plus[i][j] = response_time[i][j];
					continue;
				}
				
				task.spin_delay_by_preemptions = 0;
				task.implementation_overheads = 0;
				task.implementation_overheads += Utils.FULL_CONTEXT_SWTICH1;

				task.spin = getSpinDelay(task, tasks, resources, response_time[i][j], response_time);
				task.interference = highPriorityInterference(task, tasks, response_time[i][j], response_time, resources);
				task.local = localBlocking(task, tasks, resources, response_time, response_time[i][j]);

				long implementation_overheads = (long) Math.ceil(task.implementation_overheads);
				response_time_plus[i][j] = task.Ri = task.WCET + task.spin + task.interference + task.local + implementation_overheads;

				if (testSchedulability && task.Ri > task.deadline) {
					return response_time_plus;
				} 
			}
		}
		return response_time_plus;
	}

	private long getSpinDelay(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long time, long[][] Ris) {
		long spin = 0;
		ArrayList<ArrayList<Long>> requestsLeftOnRemoteP = new ArrayList<>();
		ArrayList<Resource> fifop_resources = new ArrayList<>();
		for (int i = 0; i < resources.size(); i++) {
			requestsLeftOnRemoteP.add(new ArrayList<Long>());
			fifop_resources.add(resources.get(i));
			Resource res = resources.get(i);
			spin += getSpinDelayForOneResoruce(task, tasks, res, time, Ris, requestsLeftOnRemoteP.get(i));
		}

		// preemptions
		long preemptions = 0;
		long request_by_preemptions = 0;
		for (int i = 0; i < tasks.get(task.partition).size(); i++) {
			if (tasks.get(task.partition).get(i).priority > task.priority) {
				preemptions += (int) Math.ceil((double) (time) / (double) tasks.get(task.partition).get(i).period);
			}
		}

		task.implementation_overheads += preemptions * (Utils.FIFOP_DEQUEUE_IN_SCHEDULE + Utils.FIFOP_RE_REQUEST);

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

	private long getSpinDelayForOneResoruce(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks, Resource resource, long time, long[][] Ris,
			ArrayList<Long> requestsLeftOnRemoteP) {
		long spin = 0;
		long ncs = 0;

		for (int i = 0; i < tasks.get(task.partition).size(); i++) {
			SporadicTask hpTask = tasks.get(task.partition).get(i);
			if (hpTask.priority > task.priority && hpTask.resource_required_index.contains(resource.id - 1)) {
				ncs += (int) Math.ceil((double) (time + Ris[hpTask.partition][i]) / (double) hpTask.period)
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
							int number_of_release = (int) Math.ceil((double) (time + Ris[i][j]) / (double) remote_task.period);
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

		task.implementation_overheads += (spin + ncs) * (Utils.FIFOP_LOCK + Utils.FIFOP_UNLOCK);

		task.fifop[resource.id - 1] += spin * resource.csl + ncs * resource.csl + (spin + ncs) * (Utils.FIFOP_LOCK + Utils.FIFOP_UNLOCK);
		return spin * resource.csl + ncs * resource.csl;
	}

	/*
	 * Calculate the local high priority tasks' interference for a given task t.
	 * CI is a set of computation time of local tasks, including spin delay.
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, long time, long[][] Ris, ArrayList<Resource> resources) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (time) / (double) hpTask.period) * (hpTask.WCET);
				t.implementation_overheads += Math.ceil((double) (time) / (double) hpTask.period) * (Utils.FULL_CONTEXT_SWTICH1 + Utils.FULL_CONTEXT_SWTICH2);
			}
		}
		return interference;
	}

	private long localBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] Ris, long Ri) {
		ArrayList<Resource> LocalBlockingResources = getLocalBlockingResources(t, resources);
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;
			local_blocking_each_resource.add(local_blocking);
			t.fifop[res.id - 1] += res.csl + Utils.FIFOP_LOCK + Utils.FIFOP_UNLOCK;
		}

		if (local_blocking_each_resource.size() > 1)
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));

		if (local_blocking_each_resource.size() > 0)
			t.implementation_overheads += Utils.FIFOP_LOCK + Utils.FIFOP_UNLOCK;

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;
	}

	private ArrayList<Resource> getLocalBlockingResources(SporadicTask task, ArrayList<Resource> resources) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			// local resources that have a higher ceiling
			if (resource.partitions.size() == 1 && resource.partitions.get(0) == partition
					&& resource.ceiling.get(resource.partitions.indexOf(partition)) >= task.priority) {
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
