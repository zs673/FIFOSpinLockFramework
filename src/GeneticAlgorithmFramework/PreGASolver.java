package GeneticAlgorithmFramework;

import java.util.ArrayList;

import GeneticAlgorithmFramework.GASolver.AllocatedSystem;
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

	ArrayList<Integer> allocations = null;
	ArrayList<AllocatedSystem> allocatedSystems = null;

	public PreGASolver(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources, SystemGenerator geneator, int PROTOCOL_NUMBER,
			int ALLOCATION_POLICY_NUMBER, int PRIORITY_SCHEME_NUMBER, ArrayList<Integer> allocations, ArrayList<AllocatedSystem> allocatedSystems,
			boolean print) {
		this.PROTOCOL_NUMBER = PROTOCOL_NUMBER;
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.PRIORITY_SCHEME_NUMBER = PRIORITY_SCHEME_NUMBER;
		this.geneator = geneator;
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;

		this.allocations = allocations;
		this.allocatedSystems = allocatedSystems;
	}

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
	public int initialCheck(boolean lazyMode, boolean withAlloc) {
		if (print) {
			System.out.println("inital check started");
		}
		this.lazyMode = lazyMode;
		int notpossiblecount = 0;

		if (withAlloc) {
			for (int i = 0; i < allocations.size(); i++) {
				System.out.println("check with DMPO, allocation " + allocations.get(i));
				int result = checkwithOneAllocationAndOnePriorityWithAlloc(i, 0);
				if (result == -1)
					notpossiblecount++;
				if (result > 0)
					return result; // has a feasible solution
			}

			if (PRIORITY_SCHEME_NUMBER > 0) {
				for (int i = 0; i < allocations.size(); i++) {
					System.out.println("check with SBPO, allocation " + allocations.get(i));
					int result = checkwithOneAllocationAndOnePriorityWithAlloc(i, 1);
					if (result == -1)
						notpossiblecount++;
					if (result > 0)
						return result; // has a feasible solution
				}
			}
		} else {
			for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
				int result = checkwithOneAllocationAndOnePriority(i, 0);
				if (result == -1)
					notpossiblecount++;
				if (result > 0)
					return result; // has a feasible solution
			}

			if (PRIORITY_SCHEME_NUMBER > 0) {
				for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
					int result = checkwithOneAllocationAndOnePriority(i, 1);
					if (result == -1)
						notpossiblecount++;
					if (result > 0)
						return result; // has a feasible solution
				}
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
			taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 2;
			}
			taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 3;
			}
			taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));
			break;

		default:
			break;
		}

		if (taskschedule_fifonp == null || taskschedule_fifop == null || taskschedule_mrsp == null) {
			System.out.println("hahaha");
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
			// if (print)
			// System.out.println("not schedulable");
			return -1;
		}

		return 0;
	}
	
	/**
	 * 
	 * @param allocPolicy
	 *            The allocation policy assumed ( 0 - 7 )
	 * @return 0: needs GA; -1: not possible; 1: is feasible by MrsP; 2: is
	 *         feasible by MSRP; 1: is feasible by FIFO-P;
	 */
	private int checkwithOneAllocationAndOnePriorityWithAlloc(int allocPolicy, int priorityPolicy) {
		int fifonp_sched = 0, fifop_sched = 0, mrsp_sched = 0;
		boolean isPossible = true;

		ArrayList<ArrayList<SporadicTask>> tasksWithAlloc = allocatedSystems.get(allocPolicy).tasks;

		if (tasksWithAlloc == null)
			return -1;

		if (tasksWithAlloc != null) {
			for (int i = 0; i < tasksWithAlloc.size(); i++) {
				if (tasksWithAlloc.get(i).size() == 0) {
					tasksWithAlloc.remove(i);
					i--;
				}
			}

			for (int i = 0; i < tasksWithAlloc.size(); i++) {
				for (int j = 0; j < tasksWithAlloc.get(i).size(); j++) {
					tasksWithAlloc.get(i).get(j).partition = i;
				}
			}

			if (resources != null && resources.size() > 0) {
				for (int i = 0; i < resources.size(); i++) {
					Resource res = resources.get(i);
					res.isGlobal = false;
					res.partitions.clear();
					res.requested_tasks.clear();
				}

				/* for each resource */
				for (int i = 0; i < resources.size(); i++) {
					Resource resource = resources.get(i);

					/* for each partition */
					for (int j = 0; j < tasksWithAlloc.size(); j++) {

						/* for each task in the given partition */
						for (int k = 0; k < tasksWithAlloc.get(j).size(); k++) {
							SporadicTask task = tasksWithAlloc.get(j).get(k);

							if (task.resource_required_index.contains(resource.id - 1)) {
								resource.requested_tasks.add(task);
								if (!resource.partitions.contains(task.partition)) {
									resource.partitions.add(task.partition);
								}
							}
						}
					}

					if (resource.partitions.size() > 1)
						resource.isGlobal = true;
				}
			}

		}

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
			taskschedule_fifonp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 2;
			}
			taskschedule_fifop = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = 3;
			}
			taskschedule_mrsp = getTaskSchedulability(tasksWithAlloc, analysis.getResponseTimeBySimpleSBPO(tasksWithAlloc, resources, false));
			break;

		default:
			break;
		}

		if (taskschedule_fifonp == null || taskschedule_fifop == null || taskschedule_mrsp == null) {
			System.out.println("hahaha");
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
				System.out.println("mrsp schedulable with allocation: " + allocations.get(allocPolicy) + " and priority: " + priorityPolicy);
			this.allocation = allocations.get(allocPolicy);
			this.protocol = 1;
			this.priority = priorityPolicy;
			return 1;
		}
		if (fifonp_sched == 0) {
			if (print)
				System.out.println("fifonp schedulable with allocation: " + allocations.get(allocPolicy) + " and priority: " + priorityPolicy);
			this.allocation = allocations.get(allocPolicy);
			this.protocol = 2;
			this.priority = priorityPolicy;
			return 1;
		}
		if (fifop_sched == 0) {
			if (print)
				System.out.println("fifop schedulable with allocation: " + allocations.get(allocPolicy) + " and priority: " + priorityPolicy);
			this.allocation = allocations.get(allocPolicy);
			this.protocol = 3;
			this.priority = priorityPolicy;
			return 1;
		}

		/*
		 * If not possible in all cases, we suggest the GA to finish.
		 */
		if (lazyMode && !isPossible) {
			// if (print)
			// System.out.println("not schedulable");
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
