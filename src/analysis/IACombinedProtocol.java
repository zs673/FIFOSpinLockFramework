package analysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class IACombinedProtocol {

	public long[][] calculateResponseTime(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			boolean testSchedulability, boolean printDebug, int extendCal) {
		long count = 0; // The number of calculations
		long np = 0; // The NP section length if MrsP is applied

		long npsection = 0;
		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol == 3 && npsection < resource.csl)
				npsection = resources.get(i).csl;
		}
		np = npsection;

		long[][] init_Ri = IOAAnalysisUtils.initResponseTime(tasks);
		long[][] response_time = new long[tasks.size()][];
		boolean isEqual = false, missdeadline = false;
		count = 0;

		for (int i = 0; i < init_Ri.length; i++) {
			response_time[i] = new long[init_Ri[i].length];
		}

		IOAAnalysisUtils.cloneList(init_Ri, response_time);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			boolean should_finish = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time,
					IOAAnalysisUtils.MrsP_PREEMPTION_AND_MIGRATION, np, testSchedulability, extendCal);

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
			System.out.println("FIFO Spin Locks Framework    after " + count + " tims of recursion, we got the response time.");
			IOAAnalysisUtils.printResponseTime(response_time, tasks);
		}

		return response_time;
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time,
			double oneMig, long np, boolean testSchedulability, int extendCal) {
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

				task.Ri = task.spin = task.interference = task.local = task.indirectspin = task.total_blocking = 0;
				task.blocking_overheads = task.np_section = task.implementation_overheads = task.migration_overheads_plus = task.mrsp_arrivalblocking_overheads = task.fifonp_arrivalblocking_overheads = task.fifop_arrivalblocking_overheads = 0;

				task.implementation_overheads += IOAAnalysisUtils.FULL_CONTEXT_SWTICH1;
				task.spin = resourceAccessingTime(task, tasks, resources, response_time, response_time[i][j], 0, oneMig, np,
						task);
				task.interference = highPriorityInterference(task, tasks, response_time[i][j], response_time, resources, oneMig,
						np);
				task.local = localBlocking(task, tasks, resources, response_time, response_time[i][j], oneMig, np);

				long implementation_overheads = (long) Math.ceil(task.implementation_overheads + task.migration_overheads_plus);
				response_time_plus[i][j] = task.Ri = task.WCET + task.spin + task.interference + task.local
						+ implementation_overheads;

				task.total_blocking = task.spin + task.indirectspin + task.local - task.pure_resource_execution_time
						+ (long) Math.ceil(task.blocking_overheads);
				if (task.total_blocking < 0) {
					System.err.println("total blocking error: T" + task.id + "   total blocking: " + task.total_blocking);
					System.exit(-1);
				}

				if (testSchedulability && task.Ri > task.deadline) {
					return response_time_plus;
				}
			}
		}
		return response_time_plus;
	}

	private ArrayList<Resource> FIFONPgetLocalBlockingResources(SporadicTask task, ArrayList<Resource> resources) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol != 2 && resource.protocol != 3) {
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

		}

		return localBlockingResources;
	}

	private long FIFONPlocalBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long Ri) {
		ArrayList<Resource> LocalBlockingResources = FIFONPgetLocalBlockingResources(t, resources);
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();
		ArrayList<Double> overheads = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;

			if (res.isGlobal) {
				for (int parition_index = 0; parition_index < res.partitions.size(); parition_index++) {
					int partition = res.partitions.get(parition_index);
					int norHP = getNoRFromHP(res, t, tasks.get(t.partition), Ris[t.partition], Ri);
					int norT = t.resource_required_index.contains(res.id - 1)
							? t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(res.id - 1)) : 0;
					int norR = getNoRRemote(res, tasks.get(partition), Ris[partition], Ri);

					if (partition != t.partition && (norHP + norT) < norR) {
						local_blocking += res.csl;
					}
				}
			}
			local_blocking_each_resource.add(local_blocking);
			overheads.add((local_blocking / res.csl) * (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK));
		}

		if (local_blocking_each_resource.size() >= 1) {
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));
			overheads.sort((l1, l2) -> -Double.compare(l1, l2));
			t.fifonp_arrivalblocking_overheads = overheads.get(0);
		}

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;
	}

	/**
	 * FIFO-NP resource accessing time.
	 */
	private long FIFONPResourceTime(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long Ri) {
		long spin_delay = 0;
		for (int k = 0; k < t.resource_required_index.size(); k++) {
			Resource resource = resources.get(t.resource_required_index.get(k));
			if (resource.protocol != 2 && resource.protocol != 3) {
				long NoS = getNoSpinDelay(t, resource, tasks, Ris, Ri);
				spin_delay += NoS * resource.csl;
				t.implementation_overheads += (NoS
						+ t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(resource.id - 1)))
						* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
				t.blocking_overheads += NoS * (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);

				spin_delay += resource.csl
						* t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(resource.id - 1));
			}

		}
		return spin_delay;
	}

	private ArrayList<Resource> FIFOPgetLocalBlockingResources(SporadicTask task, ArrayList<Resource> resources) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol == 2) {
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

		}

		return localBlockingResources;
	}

	private long FIFOPlocalBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long Ri) {
		ArrayList<Resource> LocalBlockingResources = FIFOPgetLocalBlockingResources(t, resources);
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;
			local_blocking_each_resource.add(local_blocking);
		}

		if (local_blocking_each_resource.size() > 1)
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));

		if (local_blocking_each_resource.size() > 0)
			t.fifop_arrivalblocking_overheads = IOAAnalysisUtils.FIFOP_LOCK + IOAAnalysisUtils.FIFOP_UNLOCK;

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;
	}

	/**
	 * FIFO-P resource accessing time.
	 */
	private long FIFOPResourceAccessTime(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<Resource> resources, long time, long[][] Ris) {
		long spin = 0;
		ArrayList<ArrayList<Long>> requestsLeftOnRemoteP = new ArrayList<>();
		ArrayList<Resource> fifo_resources = new ArrayList<>();
		for (int i = 0; i < resources.size(); i++) {
			Resource res = resources.get(i);
			if (res.protocol == 2) {
				requestsLeftOnRemoteP.add(new ArrayList<Long>());
				fifo_resources.add(res);
				spin += getSpinDelayForOneResoruce(task, tasks, res, time, Ris,
						requestsLeftOnRemoteP.get(requestsLeftOnRemoteP.size() - 1));
			}
		}

		if (fifo_resources.size() > 0) {
			// preemptions
			long preemptions = 0;
			long request_by_preemptions = 0;
			for (int i = 0; i < tasks.get(task.partition).size(); i++) {
				if (tasks.get(task.partition).get(i).priority > task.priority) {
					preemptions += (int) Math.ceil((double) (time) / (double) tasks.get(task.partition).get(i).period);
				}
			}
			task.implementation_overheads += preemptions
					* (IOAAnalysisUtils.FIFOP_DEQUEUE_IN_SCHEDULE + IOAAnalysisUtils.FIFOP_RE_REQUEST);
			task.blocking_overheads += preemptions
					* (IOAAnalysisUtils.FIFOP_DEQUEUE_IN_SCHEDULE + IOAAnalysisUtils.FIFOP_RE_REQUEST);

			while (preemptions > 0) {

				long max_delay = 0;
				int max_delay_resource_index = -1;
				for (int i = 0; i < fifo_resources.size(); i++) {
					if (max_delay < fifo_resources.get(i).csl * requestsLeftOnRemoteP.get(i).size()) {
						max_delay = fifo_resources.get(i).csl * requestsLeftOnRemoteP.get(i).size();
						max_delay_resource_index = i;
					}
				}

				if (max_delay > 0) {
					spin += max_delay;
					for (int i = 0; i < requestsLeftOnRemoteP.get(max_delay_resource_index).size(); i++) {
						requestsLeftOnRemoteP.get(max_delay_resource_index).set(i,
								requestsLeftOnRemoteP.get(max_delay_resource_index).get(i) - 1);
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
		}

		return spin;
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

	/**
	 * FIFO-NP indirect spin delay.
	 */
	private long getIndirectSpinDelay(SporadicTask hpTask, long Ri, long Rihp, long[][] Ris,
			ArrayList<ArrayList<SporadicTask>> allTasks, ArrayList<Resource> resources, SporadicTask calTask) {
		long BTBhit = 0;

		for (int i = 0; i < hpTask.resource_required_index.size(); i++) {
			/* for each resource that a high priority task request */
			Resource resource = resources.get(hpTask.resource_required_index.get(i));

			if (resource.protocol != 2 && resource.protocol != 3) {
				int number_of_higher_request = getNoRFromHP(resource, hpTask, allTasks.get(hpTask.partition),
						Ris[hpTask.partition], Ri);
				int number_of_request_with_btb = (int) Math.ceil((double) (Ri + Rihp) / (double) hpTask.period)
						* hpTask.number_of_access_in_one_release.get(i);

				BTBhit += number_of_request_with_btb * resource.csl;
				calTask.implementation_overheads += number_of_request_with_btb
						* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
				calTask.blocking_overheads += number_of_request_with_btb
						* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);

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
						calTask.blocking_overheads += spin_delay_with_btb
								* (IOAAnalysisUtils.FIFONP_LOCK + IOAAnalysisUtils.FIFONP_UNLOCK);
					}
				}
			}

		}
		return BTBhit;
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
	 * gives the number of requests from remote partitions for a resource that
	 * is required by the given task.
	 */
	private int getNoSpinDelay(SporadicTask task, Resource resource, ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris,
			long Ri) {
		int number_of_spin_dealy = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (i != task.partition) {
				/* For each remote partition */
				int number_of_request_by_Remote_P = 0;
				for (int j = 0; j < tasks.get(i).size(); j++) {
					if (tasks.get(i).get(j).resource_required_index.contains(resource.id - 1)) {
						SporadicTask remote_task = tasks.get(i).get(j);
						int indexR = getIndexRInTask(remote_task, resource);
						int number_of_release = (int) Math.ceil((double) (Ri + Ris[i][j]) / (double) remote_task.period);
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

	private long getSpinDelayForOneResoruce(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks, Resource resource,
			long time, long[][] Ris, ArrayList<Long> requestsLeftOnRemoteP) {
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
							number_of_request_by_Remote_P += number_of_release
									* remote_task.number_of_access_in_one_release.get(indexR);
						}
					}

					long possible_spin_delay = Long.min(number_of_request_by_Remote_P, ncs);
					spin += possible_spin_delay;
					if (number_of_request_by_Remote_P - ncs > 0)
						requestsLeftOnRemoteP.add(number_of_request_by_Remote_P - ncs);
				}
			}
		}

		task.implementation_overheads += (spin + ncs) * (IOAAnalysisUtils.FIFOP_LOCK + IOAAnalysisUtils.FIFOP_UNLOCK);
		task.blocking_overheads += (spin + ncs
				- (task.resource_required_index.contains(resource.id - 1)
						? task.number_of_access_in_one_release.get(task.resource_required_index.indexOf(resource.id - 1)) : 0))
				* (IOAAnalysisUtils.FIFOP_LOCK + IOAAnalysisUtils.FIFOP_UNLOCK);
		return spin * resource.csl + ncs * resource.csl;
	}

	/*
	 * Indirect Spin delay
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, long time, long[][] Ris,
			ArrayList<Resource> resources, double oneMig, long np) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (time) / (double) hpTask.period) * (hpTask.WCET);
				t.implementation_overheads += Math.ceil((double) (time) / (double) hpTask.period)
						* (IOAAnalysisUtils.FULL_CONTEXT_SWTICH1 + IOAAnalysisUtils.FULL_CONTEXT_SWTICH2);

				long btb_interference = getIndirectSpinDelay(hpTask, time, Ris[partition][i], Ris, allTasks, resources, t);
				interference += MrsPresourceAccessingTime(hpTask, allTasks, resources, Ris, time, Ris[partition][i], oneMig, np,
						t);
				t.indirectspin += btb_interference;
				interference += btb_interference;
			}
		}
		return interference;
	}

	private boolean isTaskIncurNPSection(SporadicTask task, ArrayList<SporadicTask> tasksOnItsParititon,
			ArrayList<Resource> resources) {
		int partition = task.partition;
		int priority = task.priority;
		int minCeiling = 1000;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol == 3 && resource.partitions.contains(partition)
					&& minCeiling > resource.ceiling.get(resource.partitions.indexOf(partition))) {
				minCeiling = resource.ceiling.get(resource.partitions.indexOf(partition));
			}
		}

		if (priority > minCeiling)
			return true;
		else
			return false;
	}

	/*
	 * Arrival Blocking
	 */
	private long localBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long time, double oneMig, long np) {
		long localblocking = 0;
		long fifonp_localblocking = FIFONPlocalBlocking(t, tasks, resources, Ris, time);
		long fifop_localblocking = FIFOPlocalBlocking(t, tasks, resources, Ris, time);
		long MrsP_localblocking = MrsPlocalBlocking(t, tasks, resources, Ris, time, oneMig, np);
		long npsection = (isTaskIncurNPSection(t, tasks.get(t.partition), resources) ? np : 0);

		ArrayList<Double> blocking = new ArrayList<>();

		double fifonp = t.fifonp_arrivalblocking_overheads + fifonp_localblocking;
		double fifop = t.fifop_arrivalblocking_overheads + fifop_localblocking;
		double mrsp = t.mrsp_arrivalblocking_overheads + MrsP_localblocking;

		blocking.add(fifonp);
		blocking.add(fifop);
		blocking.add(mrsp);
		blocking.add((double) npsection);

		blocking.sort((l1, l2) -> -Double.compare(l1, l2));

		if (blocking.get(0) == fifonp) {
			t.np_section = 0;
			localblocking = fifonp_localblocking;
			t.implementation_overheads += t.fifonp_arrivalblocking_overheads;
			t.blocking_overheads += t.fifonp_arrivalblocking_overheads;
		} else if (blocking.get(0) == fifop) {
			t.np_section = 0;
			localblocking = fifop_localblocking;
			t.implementation_overheads += t.fifop_arrivalblocking_overheads;
			t.blocking_overheads += t.fifonp_arrivalblocking_overheads;
		} else if (blocking.get(0) == mrsp) {
			t.np_section = 0;
			localblocking = MrsP_localblocking;
			t.implementation_overheads += t.mrsp_arrivalblocking_overheads;
			t.blocking_overheads += t.fifonp_arrivalblocking_overheads;
		} else if (blocking.get(0) == npsection) {
			t.np_section = npsection;
			localblocking = npsection;
		}

		return localblocking;
	}

	private double migrationCost(double oneMig, long np, ArrayList<Integer> migration_targets, Resource resource,
			ArrayList<ArrayList<SporadicTask>> tasks, SporadicTask calT) {
		double migrationCost = 0;
		ArrayList<Integer> migration_targets_with_P = new ArrayList<>();

		// identify the migration targets with preemptors
		for (int i = 0; i < migration_targets.size(); i++) {
			int partition = migration_targets.get(i);
			if (tasks.get(partition).get(0).priority > resource.ceiling.get(resource.partitions.indexOf(partition)))
				migration_targets_with_P.add(migration_targets.get(i));
		}

		// check
		if (!migration_targets.containsAll(migration_targets_with_P)) {
			System.out.println("migration targets error!");
			System.exit(0);
		}

		// now we compute the migration cost for each request
		for (int i = 0; i < migration_targets.size(); i++) {
			double migration_cost_for_one_access = 0;
			int partition = migration_targets.get(i); // the request issued
														// from.

			// calculating migration cost
			// 1. If there is no preemptors on the task's partition OR there is
			// no
			// other migration targets
			if (!migration_targets_with_P.contains(partition)
					|| (migration_targets.size() == 1 && migration_targets.get(0) == partition))
				migration_cost_for_one_access = 0;

			// 2. If there is preemptors on the task's partition AND there are
			// no
			// preemptors on other migration targets
			else if (migration_targets_with_P.size() == 1 && migration_targets_with_P.get(0) == partition
					&& migration_targets.size() > 1)
				migration_cost_for_one_access = 2 * oneMig;

			// 3. If there exist multiple migration targets with preemptors.
			// With NP
			// section applied.
			else {
				if (np > 0) {
					double migCostWithNP = (long) (1 + Math.ceil((double) resource.csl / (double) np)) * oneMig;
					double migCostWithHP = migrationCostBusyWindow(migration_targets_with_P, oneMig, resource, tasks, calT,
							migCostWithNP);
					migration_cost_for_one_access = Math.min(migCostWithHP, migCostWithNP);
				} else {
					migration_cost_for_one_access = migrationCostBusyWindow(migration_targets_with_P, oneMig, resource, tasks,
							calT, -1);
				}
			}

			migrationCost += migration_cost_for_one_access;
		}

		return migrationCost;
	}

	public double migrationCostBusyWindow(ArrayList<Integer> migration_targets_with_P, double oneMig, Resource resource,
			ArrayList<ArrayList<SporadicTask>> tasks, SporadicTask calT, double migByNP) {
		double migCost = 0;

		double newMigCost = migrationCostOneCal(migration_targets_with_P, oneMig, resource.csl + migCost, resource, tasks);

		while (migCost != newMigCost) {
			migCost = newMigCost;
			newMigCost = migrationCostOneCal(migration_targets_with_P, oneMig, resource.csl + migCost, resource, tasks);

			if (newMigCost > calT.deadline) {
				return newMigCost;
			}
			if (migByNP > 0 && newMigCost > migByNP) {
				return newMigCost;
			}
		}

		return migCost;
	}

	private double migrationCostForArrival(double oneMig, long np, ArrayList<Integer> migration_targets, Resource resource,
			ArrayList<ArrayList<SporadicTask>> tasks, SporadicTask calT) {
		return migrationCost(oneMig, np, migration_targets, resource, tasks, calT);
	}

	private double migrationCostForSpin(double oneMig, long np, SporadicTask task, int request_number, Resource resource,
			ArrayList<ArrayList<SporadicTask>> tasks, long time, long[][] Ris, SporadicTask calT) {

		ArrayList<Integer> migration_targets = new ArrayList<>();

		// identify the migration targets
		migration_targets.add(task.partition);
		for (int i = 0; i < tasks.size(); i++) {
			if (i != task.partition) {
				int number_requests_left = 0;
				number_requests_left = getNoRRemote(resource, tasks.get(i), Ris[i], time)
						- getNoRFromHP(resource, task, tasks.get(task.partition), Ris[task.partition], time) - request_number + 1;

				if (number_requests_left > 0)
					migration_targets.add(i);
			}
		}

		return migrationCost(oneMig, np, migration_targets, resource, tasks, calT);
	}

	private double migrationCostOneCal(ArrayList<Integer> migration_targets_with_P, double oneMig, double duration,
			Resource resource, ArrayList<ArrayList<SporadicTask>> tasks) {
		double migCost = 0;

		for (int i = 0; i < migration_targets_with_P.size(); i++) {
			int partition_with_p = migration_targets_with_P.get(i);
			int ceiling_index = resource.partitions.indexOf(partition_with_p);

			for (int j = 0; j < tasks.get(partition_with_p).size(); j++) {
				SporadicTask hpTask = tasks.get(partition_with_p).get(j);

				if (hpTask.priority > resource.ceiling.get(ceiling_index))
					migCost += Math.ceil((duration) / hpTask.period) * oneMig;
			}
		}

		return migCost + oneMig;
	}

	private ArrayList<Resource> MrsPgetLocalBlockingResources(SporadicTask task, ArrayList<Resource> resources) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);

			if (resource.protocol == 3 && resource.partitions.contains(partition)
					&& resource.ceiling.get(resource.partitions.indexOf(partition)) >= task.priority) {
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

	private long MrsPlocalBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long time, double oneMig, long np) {
		ArrayList<Resource> LocalBlockingResources = MrsPgetLocalBlockingResources(t, resources);
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();
		ArrayList<Double> overheads = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			double arrivalBlockingOverheads = 0;
			ArrayList<Integer> migration_targets = new ArrayList<>();

			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;
			arrivalBlockingOverheads += IOAAnalysisUtils.MrsP_LOCK + IOAAnalysisUtils.MrsP_UNLOCK;

			migration_targets.add(t.partition);
			if (res.isGlobal) {
				int remoteblocking = 0;
				for (int parition_index = 0; parition_index < res.partitions.size(); parition_index++) {
					int partition = res.partitions.get(parition_index);
					int norHP = getNoRFromHP(res, t, tasks.get(t.partition), Ris[t.partition], time);
					int norT = t.resource_required_index.contains(res.id - 1)
							? t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(res.id - 1)) : 0;
					int norR = getNoRRemote(res, tasks.get(partition), Ris[partition], time);

					if (partition != t.partition && (norHP + norT) < norR) {
						local_blocking += res.csl;
						remoteblocking++;
						migration_targets.add(partition);
					}
				}
				arrivalBlockingOverheads += remoteblocking * (IOAAnalysisUtils.MrsP_LOCK + IOAAnalysisUtils.MrsP_UNLOCK);
				double mc_plus = 0;
				if (oneMig != 0) {
					double mc = migrationCostForArrival(oneMig, np, migration_targets, res, tasks, t);

					long mc_long = (long) Math.floor(mc);
					mc_plus += mc - mc_long;
					if (mc - mc_long < 0) {
						System.err.println("MrsP mig error");
						System.exit(-1);
					}
					local_blocking += mc_long;
				}
				arrivalBlockingOverheads += mc_plus;
			}

			local_blocking_each_resource.add(local_blocking);
			overheads.add(arrivalBlockingOverheads);
		}

		if (local_blocking_each_resource.size() >= 1) {
			if (overheads.size() <= 0) {
				System.err.println("overheads error!");
				System.exit(-1);
			}
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));
			overheads.sort((l1, l2) -> -Double.compare(l1, l2));
			t.mrsp_arrivalblocking_overheads = overheads.get(0);
		}

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;

	}

	/**
	 * MrsP resource accessing time.
	 */
	private long MrsPresourceAccessingTime(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<Resource> resources, long[][] Ris, long time, long jitter, double oneMig, long np, SporadicTask calT) {
		long resource_accessing_time = 0;

		for (int i = 0; i < task.resource_required_index.size(); i++) {
			Resource resource = resources.get(task.resource_required_index.get(i));

			if (resource.protocol == 3) {
				int number_of_request_with_btb = (int) Math.ceil((double) (time + jitter) / (double) task.period)
						* task.number_of_access_in_one_release.get(i);

				for (int j = 1; j < number_of_request_with_btb + 1; j++) {
					long oneAccess = 0;
					oneAccess += MrsPresourceAccessingTimeInOne(task, resource, tasks, Ris, time, jitter, j, calT);

					if (oneMig != 0) {
						double mc = migrationCostForSpin(oneMig, np, task, j, resource, tasks, time, Ris, calT);
						long mc_long = (long) Math.floor(mc);
						calT.migration_overheads_plus += mc - mc_long;
						if (mc - mc_long < 0) {
							System.err.println("MrsP mig error");
							System.exit(-1);
						}
						oneAccess += mc_long;
					}

					resource_accessing_time += oneAccess;
				}
			}

		}

		return resource_accessing_time;
	}

	private long MrsPresourceAccessingTimeInOne(SporadicTask task, Resource resource, ArrayList<ArrayList<SporadicTask>> tasks,
			long[][] Ris, long time, long jitter, int n, SporadicTask calTask) {
		int number_of_access = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (i != task.partition) {
				/* For each remote partition */
				int number_of_request_by_Remote_P = 0;
				for (int j = 0; j < tasks.get(i).size(); j++) {
					if (tasks.get(i).get(j).resource_required_index.contains(resource.id - 1)) {
						SporadicTask remote_task = tasks.get(i).get(j);
						int indexR = getIndexRInTask(remote_task, resource);
						int number_of_release = (int) Math.ceil((double) (time + Ris[i][j]) / (double) remote_task.period);
						number_of_request_by_Remote_P += number_of_release
								* remote_task.number_of_access_in_one_release.get(indexR);
					}
				}
				int getNoRFromHP = getNoRFromHP(resource, task, tasks.get(task.partition), Ris[task.partition], time);
				int possible_spin_delay = number_of_request_by_Remote_P - getNoRFromHP - n + 1 < 0 ? 0
						: number_of_request_by_Remote_P - getNoRFromHP - n + 1;
				number_of_access += Integer.min(possible_spin_delay, 1);
			}
		}

		// account for the request of the task itself
		number_of_access++;

		calTask.implementation_overheads += number_of_access * (IOAAnalysisUtils.MrsP_LOCK + IOAAnalysisUtils.MrsP_UNLOCK);
		calTask.blocking_overheads += (number_of_access - 1) * (IOAAnalysisUtils.MrsP_LOCK + IOAAnalysisUtils.MrsP_UNLOCK);

		return number_of_access * resource.csl;
	}

	/*
	 * Direct Spin delay
	 */
	private long resourceAccessingTime(SporadicTask task, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] Ris, long time, long jitter, double oneMig, long np, SporadicTask calT) {
		long resourceTime = 0;
		resourceTime += FIFONPResourceTime(task, tasks, resources, Ris, time);
		resourceTime += FIFOPResourceAccessTime(task, tasks, resources, time, Ris);
		resourceTime += MrsPresourceAccessingTime(task, tasks, resources, Ris, time, jitter, oneMig, np, calT);
		return resourceTime;
	}

}
