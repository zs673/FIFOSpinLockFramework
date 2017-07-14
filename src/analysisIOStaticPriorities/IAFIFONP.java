package analysisIOStaticPriorities;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class IAFIFONP {

	public long[][] NewRTATest(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			boolean testSchedulability, boolean printDebug, int extendCal) {
		if (tasks == null)
			return null;
		long[][] init_Ri = IOAAnalysisUtils.initResponseTime(tasks);
		long[][] response_time = new long[tasks.size()][];
		boolean isEqual = false, missdeadline = false;

		long count = 0;

		for (int i = 0; i < init_Ri.length; i++) {
			response_time[i] = new long[init_Ri[i].length];
		}

		IOAAnalysisUtils.cloneList(init_Ri, response_time);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			boolean should_finish = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time, testSchedulability, extendCal);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j])
						isEqual = false;
					if (testSchedulability) {
						if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
							missdeadline = true;
					} else {
						if (response_time_plus[i][j] <= tasks.get(i).get(j).deadline * extendCal)
							should_finish = false;
					}
				}
			}

			count++;
			IOAAnalysisUtils.cloneList(response_time_plus, response_time);

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
			IOAAnalysisUtils.printResponseTime(response_time, tasks);
		}

		return response_time;
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] response_time, boolean testSchedulability, int extendCal) {
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

				task.fifonp = new double[resources.size()];
				for (int k = 0; k < task.fifonp.length; k++) {
					task.fifonp[k] = 0;
				}
				task.indirectspin = 0;
				task.implementation_overheads = 0;
				task.implementation_overheads += IOAAnalysisUtils.FULL_CONTEXT_SWTICH1;

				task.spin = directRemoteDelay(task, tasks, resources, response_time, response_time[i][j]);
				task.interference = highPriorityInterference(task, tasks, response_time[i][j], response_time,
						resources);
				task.local = localBlocking(task, tasks, resources, response_time, response_time[i][j]);

				long implementation_overheads = (long) Math.ceil(task.implementation_overheads);
				response_time_plus[i][j] = task.Ri = task.WCET + task.spin + task.interference + task.local
						+ implementation_overheads;

				if (testSchedulability && task.Ri > task.deadline) {
					return response_time_plus;
				}
			}
		}
		return response_time_plus;
	}

	/*
	 * Calculate the spin delay for a given task t.
	 */
	private long directRemoteDelay(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<Resource> resources, long[][] Ris, long Ri) {
		long spin_delay = 0;
		for (int k = 0; k < t.resource_required_index.size(); k++) {
			Resource resource = resources.get(t.resource_required_index.get(k));
			long NoS = getNoSpinDelay(t, resource, tasks, Ris, Ri);
			spin_delay += (NoS
					+ t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(resource.id - 1)))
					* resource.csl;
			t.implementation_overheads += (NoS
					+ t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(resource.id - 1)))
					* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);

			t.fifonp[resource.id - 1] += (NoS + t.number_of_access_in_one_release.get(k))
					* (resource.csl + IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
		}
		return spin_delay;
	}

	/*
	 * gives the number of requests from remote partitions for a resource that
	 * is required by the given task.
	 */
	private int getNoSpinDelay(SporadicTask task, Resource resource, ArrayList<ArrayList<SporadicTask>> tasks,
			long[][] Ris, long Ri) {
		int number_of_spin_dealy = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (i != task.partition) {
				/* For each remote partition */
				int number_of_request_by_Remote_P = 0;
				for (int j = 0; j < tasks.get(i).size(); j++) {
					if (tasks.get(i).get(j).resource_required_index.contains(resource.id - 1)) {
						SporadicTask remote_task = tasks.get(i).get(j);
						int indexR = getIndexRInTask(remote_task, resource);
						int number_of_release = (int) Math
								.ceil((double) (Ri + Ris[i][j]) / (double) remote_task.period);
						number_of_request_by_Remote_P += number_of_release
								* remote_task.number_of_access_in_one_release.get(indexR);
					}
				}
				int getNoRFromHP = getNoRFromHP(resource, task, tasks.get(task.partition), Ris[task.partition], Ri);
				int possible_spin_delay = number_of_request_by_Remote_P - getNoRFromHP < 0 ? 0
						: number_of_request_by_Remote_P - getNoRFromHP;

				int NoRFromT = task.number_of_access_in_one_release.get(getIndexRInTask(task, resource));
				number_of_spin_dealy += Integer.min(possible_spin_delay, NoRFromT);
			}
		}
		return number_of_spin_dealy;
	}

	/*
	 * Calculate the local high priority tasks' interference for a given task t.
	 * CI is a set of computation time of local tasks, including spin delay.
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, long Ri,
			long[][] Ris, ArrayList<Resource> resources) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (Ri) / (double) hpTask.period) * (hpTask.WCET);
				t.implementation_overheads += Math.ceil((double) (Ri) / (double) hpTask.period)
						* (IOAAnalysisUtils.FULL_CONTEXT_SWTICH1 + IOAAnalysisUtils.FULL_CONTEXT_SWTICH2);

				long btb_interference = getIndirectSpinDelay(hpTask, Ri, Ris[partition][i], Ris, allTasks, resources,
						t);
				t.indirectspin += btb_interference;
				interference += btb_interference;
			}
		}
		return interference;
	}

	/*
	 * for a high priority task hpTask, return its back to back hit time when
	 * the given task is pending
	 */
	private long getIndirectSpinDelay(SporadicTask hpTask, long Ri, long Rihp, long[][] Ris,
			ArrayList<ArrayList<SporadicTask>> allTasks, ArrayList<Resource> resources, SporadicTask calTask) {
		long BTBhit = 0;

		for (int i = 0; i < hpTask.resource_required_index.size(); i++) {
			/* for each resource that a high priority task request */
			Resource resource = resources.get(hpTask.resource_required_index.get(i));

			int number_of_higher_request = getNoRFromHP(resource, hpTask, allTasks.get(hpTask.partition),
					Ris[hpTask.partition], Ri);
			int number_of_request_with_btb = (int) Math.ceil((double) (Ri + Rihp) / (double) hpTask.period)
					* hpTask.number_of_access_in_one_release.get(i);

			BTBhit += number_of_request_with_btb * resource.csl;
			calTask.implementation_overheads += number_of_request_with_btb
					* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
			calTask.fifonp[resource.id - 1] += number_of_request_with_btb * resource.csl
					+ number_of_request_with_btb * (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);

			for (int j = 0; j < resource.partitions.size(); j++) {
				if (resource.partitions.get(j) != hpTask.partition) {
					int remote_partition = resource.partitions.get(j);
					int number_of_remote_request = getNoRRemote(resource, allTasks.get(remote_partition),
							Ris[remote_partition], Ri);

					int possible_spin_delay = number_of_remote_request - number_of_higher_request < 0 ? 0
							: number_of_remote_request - number_of_higher_request;

					int spin_delay_with_btb = Integer.min(possible_spin_delay, number_of_request_with_btb);

					BTBhit += spin_delay_with_btb * resource.csl;
					calTask.implementation_overheads += spin_delay_with_btb
							* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);

					calTask.fifonp[resource.id - 1] += spin_delay_with_btb * resource.csl
							+ spin_delay_with_btb * (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
				}
			}

		}
		return BTBhit;
	}

	private long localBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long Ri) {
		ArrayList<Resource> LocalBlockingResources = getLocalBlockingResources(t, resources);
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();
		ArrayList<Double> overheads = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;
			t.fifonp[res.id - 1] += res.csl + IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK;

			if (res.isGlobal) {
				for (int parition_index = 0; parition_index < res.partitions.size(); parition_index++) {
					int partition = res.partitions.get(parition_index);
					int norHP = getNoRFromHP(res, t, tasks.get(t.partition), Ris[t.partition], Ri);
					int norT = t.resource_required_index.contains(res.id - 1)
							? t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(res.id - 1)) : 0;
					int norR = getNoRRemote(res, tasks.get(partition), Ris[partition], Ri);

					if (partition != t.partition && (norHP + norT) < norR) {
						local_blocking += res.csl;
						t.fifonp[res.id - 1] += res.csl + IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK;
					}
				}
			}
			local_blocking_each_resource.add(local_blocking);
			overheads.add((local_blocking / res.csl) * (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK));
		}

		if (local_blocking_each_resource.size() >= 1) {
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));
			overheads.sort((l1, l2) -> -Double.compare(l1, l2));
			t.implementation_overheads += overheads.get(0);
		}

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
	 * gives that number of requests from HP local tasks for a resource that is
	 * required by the given task.
	 */
	private int getNoRFromHP(Resource resource, SporadicTask task, ArrayList<SporadicTask> tasks, long[] Ris, long Ri) {
		int number_of_request_by_HP = 0;
		int priority = task.priority;

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > priority && tasks.get(i).resource_required_index.contains(resource.id - 1)) {
				SporadicTask hpTask = tasks.get(i);
				int indexR = getIndexRInTask(hpTask, resource);
				number_of_request_by_HP += Math.ceil((double) (Ri + Ris[i]) / (double) hpTask.period)
						* hpTask.number_of_access_in_one_release.get(indexR);
			}
		}
		return number_of_request_by_HP;
	}

	private int getNoRRemote(Resource resource, ArrayList<SporadicTask> tasks, long[] Ris, long Ri) {
		int number_of_request_by_Remote_P = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).resource_required_index.contains(resource.id - 1)) {
				SporadicTask remote_task = tasks.get(i);
				int indexR = getIndexRInTask(remote_task, resource);
				number_of_request_by_Remote_P += Math.ceil((double) (Ri + Ris[i]) / (double) remote_task.period)
						* remote_task.number_of_access_in_one_release.get(indexR);
			}
		}
		return number_of_request_by_Remote_P;
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
