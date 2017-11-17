package generatorTools;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class AllocationGeneator {
	public ArrayList<ArrayList<SporadicTask>> allocateTasks(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources, int total_partitions,
			int policy) {

		double totalUtil = 0.1 * (double) tasksToAllocate.size();
		double maxUtilPerCore = 0;
		if (totalUtil / total_partitions <= 0.5)
			maxUtilPerCore = 0.5;
		else if (totalUtil / total_partitions <= 0.6)
			maxUtilPerCore = 0.6;
		else if (totalUtil / total_partitions <= 0.65)
			maxUtilPerCore = 0.65;
		else
			maxUtilPerCore = 1;

		ArrayList<ArrayList<SporadicTask>> tasks;
		switch (policy) {
		case 0:
			tasks = WorstFitAllocation(tasksToAllocate, total_partitions);
			break;
		case 1:
			tasks = BestFitAllocation(tasksToAllocate, total_partitions, maxUtilPerCore);
			break;
		case 2:
			tasks = FirstFitAllocation(tasksToAllocate, total_partitions, maxUtilPerCore);
			break;
		case 3:
			tasks = NextFitAllocation(tasksToAllocate, total_partitions, maxUtilPerCore);
			break;
		case 4:
			tasks = ResourceRequestTasksAllocation(tasksToAllocate, resources, total_partitions, maxUtilPerCore);
			break;
		case 5:
			tasks = ResourceLocalAllocation(tasksToAllocate, resources, total_partitions, maxUtilPerCore);
			break;
		case 6:
			tasks = ResourceLengthDecreaseAllocation(tasksToAllocate, resources, total_partitions, maxUtilPerCore);
			break;
		case 7:
			tasks = ResourceLengthIncreaseAllocation(tasksToAllocate, resources, total_partitions, maxUtilPerCore);
			break;
		default:
			tasks = null;
			break;
		}

		if (tasks != null) {
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).size() == 0) {
					tasks.remove(i);
					i--;
				}
			}

			for (int i = 0; i < tasks.size(); i++) {
				for (int j = 0; j < tasks.get(i).size(); j++) {
					tasks.get(i).get(j).partition = i;
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
					for (int j = 0; j < tasks.size(); j++) {

						/* for each task in the given partition */
						for (int k = 0; k < tasks.get(j).size(); k++) {
							SporadicTask task = tasks.get(j).get(k);

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

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> WorstFitAllocation(ArrayList<SporadicTask> tasksToAllocate, int partitions) {
		// clear tasks' partitions
		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}

		// Init allocated tasks array
		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<SporadicTask> task = new ArrayList<>();
			tasks.add(task);
		}

		// init util array
		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			SporadicTask task = tasksToAllocate.get(i);
			int target = -1;
			double minUtil = 2;
			for (int j = 0; j < partitions; j++) {
				if (minUtil > utilPerPartition.get(j)) {
					minUtil = utilPerPartition.get(j);
					target = j;
				}
			}

			if (target == -1) {
				System.err.println("WF error!");
				return null;
			}

			if ((double) 1 - minUtil >= task.util) {
				task.partition = target;
				utilPerPartition.set(target, utilPerPartition.get(target) + task.util);
			} else
				return null;
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			int partition = tasksToAllocate.get(i).partition;
			tasks.get(partition).add(tasksToAllocate.get(i));
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((p1, p2) -> Double.compare(p1.period, p2.period));
		}

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> BestFitAllocation(ArrayList<SporadicTask> tasksToAllocate, int partitions, double maxUtilPerCore) {

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}
		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<SporadicTask> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			SporadicTask task = tasksToAllocate.get(i);
			int target = -1;
			double maxUtil = -1;
			for (int j = 0; j < partitions; j++) {
				if (maxUtil < utilPerPartition.get(j) && ((maxUtilPerCore - utilPerPartition.get(j) >= task.util)
						|| (task.util > maxUtilPerCore && 1 - utilPerPartition.get(j) >= task.util))) {
					maxUtil = utilPerPartition.get(j);
					target = j;
				}
			}

			if (target < 0) {
				return null;
			} else {
				task.partition = target;
				utilPerPartition.set(target, utilPerPartition.get(target) + task.util);
			}
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			int partition = tasksToAllocate.get(i).partition;
			tasks.get(partition).add(tasksToAllocate.get(i));
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((p1, p2) -> Double.compare(p1.period, p2.period));
		}

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> FirstFitAllocation(ArrayList<SporadicTask> tasksToAllocate, int partitions, double maxUtilPerCore) {

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}
		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<SporadicTask> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			SporadicTask task = tasksToAllocate.get(i);
			for (int j = 0; j < partitions; j++) {
				if ((maxUtilPerCore - utilPerPartition.get(j) >= task.util) || (task.util > maxUtilPerCore && 1 - utilPerPartition.get(j) >= task.util)) {
					task.partition = j;
					utilPerPartition.set(j, utilPerPartition.get(j) + task.util);
					break;
				}
			}
			if (task.partition == -1)
				return null;
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			int partition = tasksToAllocate.get(i).partition;
			tasks.get(partition).add(tasksToAllocate.get(i));
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((p1, p2) -> Double.compare(p1.period, p2.period));
		}

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> NextFitAllocation(ArrayList<SporadicTask> tasksToAllocate, int partitions, double maxUtilPerCore) {

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}
		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<SporadicTask> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		int currentIndex = 0;

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			SporadicTask task = tasksToAllocate.get(i);

			for (int j = 0; j < partitions; j++) {
				if ((maxUtilPerCore - utilPerPartition.get(currentIndex) >= task.util)
						|| (task.util > maxUtilPerCore && 1 - utilPerPartition.get(j) >= task.util)) {
					task.partition = currentIndex;
					utilPerPartition.set(currentIndex, utilPerPartition.get(currentIndex) + task.util);
					break;
				}
				if (currentIndex == partitions - 1)
					currentIndex = 0;
				else
					currentIndex++;
			}
			if (task.partition == -1)
				return null;
		}

		for (int i = 0; i < tasksToAllocate.size(); i++) {
			int partition = tasksToAllocate.get(i).partition;
			tasks.get(partition).add(tasksToAllocate.get(i));
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((p1, p2) -> Double.compare(p1.period, p2.period));
		}

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceRequestTasksAllocation(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources,
			int partitions, double maxUtilPerCore) {
		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}

		int number_of_resources = resources.size();

		ArrayList<ArrayList<Integer>> NoQT = new ArrayList<>();
		for (int i = 0; i < number_of_resources; i++) {
			ArrayList<Integer> noq = new ArrayList<>();
			noq.add(i);
			noq.add(0);
			NoQT.add(noq);
		}

		for (int j = 0; j < tasksToAllocate.size(); j++) {
			SporadicTask task = tasksToAllocate.get(j);
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				NoQT.get(task.resource_required_index.get(k)).set(1, NoQT.get(task.resource_required_index.get(k)).get(1) + 1);
				// TODO whether by task number or request number?
			}
		}

		NoQT.sort((p1, p2) -> -Double.compare(p1.get(1), p2.get(1)));

		ArrayList<SporadicTask> sortedTasks = new ArrayList<>();
		ArrayList<SporadicTask> cleanTasks = new ArrayList<>();
		for (int i = 0; i < NoQT.size(); i++) {
			for (int j = 0; j < tasksToAllocate.size(); j++) {
				SporadicTask task = tasksToAllocate.get(j);
				if (task.resource_required_index.contains(NoQT.get(i).get(0)) && !sortedTasks.contains(task)) {
					sortedTasks.add(task);
				}
				if (!cleanTasks.contains(task) && task.resource_required_index.size() == 0) {
					cleanTasks.add(task);
				}
			}
		}
		sortedTasks.addAll(cleanTasks);

		if (sortedTasks.size() != tasksToAllocate.size()) {
			System.out.println("RESOURCE REQUEST FIT sorted tasks size error!");
			System.exit(-1);
		}

		return NextFitAllocation(sortedTasks, partitions, maxUtilPerCore);
	}

	public ArrayList<ArrayList<SporadicTask>> ResourceRequestTasksAllocationBackUp(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources,
			int partitions, double maxUtilPerCore) {
		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}

		int number_of_resources = resources.size();

		ArrayList<ArrayList<Integer>> NoQT = new ArrayList<>();
		for (int i = 0; i < number_of_resources; i++) {
			ArrayList<Integer> noq = new ArrayList<>();
			noq.add(i);
			noq.add(0);
			NoQT.add(noq);
		}

		for (int j = 0; j < tasksToAllocate.size(); j++) {
			SporadicTask task = tasksToAllocate.get(j);
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				NoQT.get(task.resource_required_index.get(k)).set(1, NoQT.get(task.resource_required_index.get(k)).get(1) + task.number_of_access_in_one_release.get(k));
			}
		}

		NoQT.sort((p1, p2) -> -Double.compare(p1.get(1), p2.get(1)));

		ArrayList<SporadicTask> sortedTasks = new ArrayList<>();
		ArrayList<SporadicTask> cleanTasks = new ArrayList<>();
		for (int i = 0; i < NoQT.size(); i++) {
			for (int j = 0; j < tasksToAllocate.size(); j++) {
				SporadicTask task = tasksToAllocate.get(j);
				if (task.resource_required_index.contains(NoQT.get(i).get(0)) && !sortedTasks.contains(task)) {
					sortedTasks.add(task);
				}
				if (!cleanTasks.contains(task) && task.resource_required_index.size() == 0) {
					cleanTasks.add(task);
				}
			}
		}

		cleanTasks.sort((p1, p2) -> -Double.compare(p1.util, p2.util));
		
		if (sortedTasks.size() + cleanTasks.size() != tasksToAllocate.size()) {
			System.out.println("RESOURCE REQUEST FIT sorted tasks size error!");
			System.exit(-1);
		}

		ArrayList<ArrayList<SporadicTask>> alloc = NextFitAllocation(sortedTasks, partitions, maxUtilPerCore);
		
		if(alloc != null){
			ArrayList<Double> utilPerPartition = new ArrayList<>();
			
			for(int i=0;i<alloc.size();i++){
				double totalUtil = 0;
				for(int j=0; j<alloc.get(i).size();j++){
					
					totalUtil+= alloc.get(i).get(j).util;
				}
				utilPerPartition.add(totalUtil);
			}
			
			
			for (int i = 0; i < cleanTasks.size(); i++) {
				SporadicTask task = cleanTasks.get(i);
				int target = -1;
				double minUtil = 2;
				for (int j = 0; j < partitions; j++) {
					if (minUtil > utilPerPartition.get(j)) {
						minUtil = utilPerPartition.get(j);
						target = j;
					}
				}

				if (target == -1) {
					System.err.println("WF error!");
					return null;
				}

				if ((double) 1 - minUtil >= task.util) {
					task.partition = target;
					alloc.get(target).add(task);
					utilPerPartition.set(target, utilPerPartition.get(target) + task.util);
				} else
					return null;
			}

			for (int i = 0; i < tasksToAllocate.size(); i++) {
				int partition = tasksToAllocate.get(i).partition;
				alloc.get(partition).add(tasksToAllocate.get(i));
			}

			for (int i = 0; i < alloc.size(); i++) {
				alloc.get(i).sort((p1, p2) -> Double.compare(p1.period, p2.period));
			}
		}

		return alloc;
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLocalAllocation(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources, int partitions,
			double maxUtilPerCore) {
		for (int i = 0; i < tasksToAllocate.size(); i++) {
			tasksToAllocate.get(i).partition = -1;
		}

		int number_of_resources = resources.size();

		ArrayList<ArrayList<Double>> NoQT = new ArrayList<>();
		for (int i = 0; i < number_of_resources; i++) {
			ArrayList<Double> noq = new ArrayList<>();
			noq.add((double)i);
			noq.add((double)0);
			NoQT.add(noq);
		}

		for (int j = 0; j < tasksToAllocate.size(); j++) {
			SporadicTask task = tasksToAllocate.get(j);
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				NoQT.get(task.resource_required_index.get(k)).set(1, NoQT.get(task.resource_required_index.get(k)).get(1) + task.util);
			}
		}

		NoQT.sort((p1, p2) -> -Double.compare(p1.get(1), p2.get(1)));

		ArrayList<SporadicTask> sortedTasks = new ArrayList<>();
		ArrayList<SporadicTask> cleanTasks = new ArrayList<>();
		for (int i = 0; i < NoQT.size(); i++) {
			for (int j = 0; j < tasksToAllocate.size(); j++) {
				SporadicTask task = tasksToAllocate.get(j);
				double index = NoQT.get(i).get(0);
				int intIndex = (int) index;
				if (task.resource_required_index.contains(intIndex) && !sortedTasks.contains(task)) {
					sortedTasks.add(task);
				}
				if (!cleanTasks.contains(task) && task.resource_required_index.size() == 0) {
					cleanTasks.add(task);
				}
			}
		}
		sortedTasks.addAll(cleanTasks);

		if (sortedTasks.size() != tasksToAllocate.size()) {
			System.out.println("RESOURCE REQUEST FIT sorted tasks size error!");
			System.exit(-1);
		}

		return NextFitAllocation(sortedTasks, partitions, maxUtilPerCore);
	}
	
	public ArrayList<ArrayList<SporadicTask>> ResourceLocalAllocationBackUp(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources, int partitions,
			double maxUtilPerCore) {
		ArrayList<SporadicTask> UnAllocatedT = new ArrayList<>(tasksToAllocate);

		for (int i = 0; i < UnAllocatedT.size(); i++) {
			UnAllocatedT.get(i).partition = -1;
		}

		int number_of_resources = resources.size();

		ArrayList<ArrayList<Double>> utilOfRT = new ArrayList<>();
		for (int i = 0; i < number_of_resources; i++) {
			ArrayList<Double> noq = new ArrayList<>();
			noq.add((double) i);
			noq.add((double) 0);
			utilOfRT.add(noq);
		}

		for (int j = 0; j < UnAllocatedT.size(); j++) {
			SporadicTask task = UnAllocatedT.get(j);
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				utilOfRT.get(task.resource_required_index.get(k)).set(1, utilOfRT.get(task.resource_required_index.get(k)).get(1) + task.util);
			}
		}

		utilOfRT.sort((p1, p2) -> Double.compare(p1.get(1), p2.get(1)));
		for (int i = 0; i < utilOfRT.size(); i++) {
			if (utilOfRT.get(i).get(1) == 0) {
				utilOfRT.remove(i);
				i--;
			}
		}

		ArrayList<ArrayList<SporadicTask>> resourcesRequestT = new ArrayList<>();
		for (int i = 0; i < utilOfRT.size(); i++) {
			int resource_index = utilOfRT.get(i).get(0).intValue();
			ArrayList<SporadicTask> resReqT = new ArrayList<>();

			for (int j = 0; j < UnAllocatedT.size(); j++) {
				SporadicTask task = UnAllocatedT.get(j);
				if (task.resource_required_index.contains(resource_index)) {
					resReqT.add(task);
					UnAllocatedT.remove(task);
					j--;
				}
			}

			resourcesRequestT.add(resReqT);
		}

		ArrayList<ArrayList<Double>> newUtil = new ArrayList<>();
		for (int i = 0; i < resourcesRequestT.size(); i++) {
			ArrayList<Double> oneU = new ArrayList<>();
			double util = 0;
			for (int j = 0; j < resourcesRequestT.get(i).size(); j++) {
				util += resourcesRequestT.get(i).get(j).util;
			}
			oneU.add(utilOfRT.get(i).get(0));
			oneU.add(util);
			oneU.add((double) 0);
			newUtil.add(oneU);
		}

		for (int i = 0; i < newUtil.size(); i++) {
			if (newUtil.get(i).get(1) == 0) {
				newUtil.remove(i);
				resourcesRequestT.remove(i);
				i--;
			}
		}

		for (int i = 0; i < newUtil.size(); i++) {
			newUtil.get(i).set(2, (double) i);
		}

		newUtil.sort((p1, p2) -> Double.compare(p1.get(1), p2.get(1)));

		// System.out.println("new Util: " +
		// Arrays.deepToString(newUtil.toArray()));
		// System.out.println(Arrays.deepToString(resourcesRequestT.toArray()));

		ArrayList<ArrayList<SporadicTask>> newAllocT = new ArrayList<>();
		for (int i = 0; i < newUtil.size(); i++) {
			int task_row_index = newUtil.get(i).get(2).intValue();
			newAllocT.add(resourcesRequestT.get(task_row_index));
		}
		// System.out.println(Arrays.deepToString(newAllocT.toArray()));

		for (int i = 0; i < newUtil.size(); i++) {
			if (newUtil.get(i).get(1) > maxUtilPerCore) {
				UnAllocatedT.addAll(newAllocT.get(i));
				newUtil.remove(i);
				newAllocT.remove(i);
				i--;
			}
		}

		int allocSize = 0;
		for (int i = 0; i < newAllocT.size(); i++) {
			allocSize += newAllocT.get(i).size();
		}

		if (UnAllocatedT.size() + allocSize != tasksToAllocate.size()) {
			System.out.println("alloc and unalloc tasks size error!");
			System.exit(-1);
		}

		ArrayList<SporadicTask> sortedT = new ArrayList<>();

		for (int i = 0; i < newAllocT.size(); i++) {
			for (int j = 0; j < newAllocT.get(i).size(); j++) {
				sortedT.add(newAllocT.get(i).get(j));
			}
		}

		for (int i = 0; i < UnAllocatedT.size(); i++) {
			sortedT.add(UnAllocatedT.get(i));

		}

		if (sortedT.size() != tasksToAllocate.size()) {
			System.err.println("alloc and unalloc tasks size error!");
			System.exit(-1);
		}

		return NextFitAllocation(sortedT, partitions, maxUtilPerCore);
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLengthDecreaseAllocation(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources,
			int partitions, double maxUtilPerCore) {
		ArrayList<SporadicTask> unallocT = new ArrayList<>(tasksToAllocate);

		for (int i = 0; i < unallocT.size(); i++) {
			unallocT.get(i).partition = -1;
		}

		ArrayList<SporadicTask> sortedT = new ArrayList<>();

		for (int i = 0; i < resources.size(); i++) {
			Resource res = resources.get(i);
			for (int j = 0; j < unallocT.size(); j++) {
				if (unallocT.get(j).resource_required_index.contains(res.id - 1)) {
					sortedT.add(unallocT.get(j));
					unallocT.remove(j);
					j--;
				}
			}
		}

		sortedT.addAll(unallocT);

		if (sortedT.size() != tasksToAllocate.size()) {
			System.err.println("resource len decrease: alloc and unalloc tasks size error!");
			System.exit(-1);
		}

		return NextFitAllocation(sortedT, partitions, maxUtilPerCore);
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLengthIncreaseAllocation(ArrayList<SporadicTask> tasksToAllocate, ArrayList<Resource> resources,
			int partitions, double maxUtilPerCore) {
		ArrayList<Resource> resources_copy = new ArrayList<>(resources);
		resources_copy.sort((p1, p2) -> Double.compare(p1.csl, p2.csl));

		ArrayList<SporadicTask> unallocT = new ArrayList<>(tasksToAllocate);

		for (int i = 0; i < unallocT.size(); i++) {
			unallocT.get(i).partition = -1;
		}

		ArrayList<SporadicTask> sortedT = new ArrayList<>();

		for (int i = 0; i < resources_copy.size(); i++) {
			Resource res = resources_copy.get(i);
			for (int j = 0; j < unallocT.size(); j++) {
				if (unallocT.get(j).resource_required_index.contains(res.id - 1)) {
					sortedT.add(unallocT.get(j));
					unallocT.remove(j);
					j--;
				}
			}
		}

		sortedT.addAll(unallocT);

		if (sortedT.size() != tasksToAllocate.size()) {
			System.err.println("resource length increase: alloc and unalloc tasks size error!");
			System.exit(-1);
		}

		return NextFitAllocation(sortedT, partitions, maxUtilPerCore);
	}

}
