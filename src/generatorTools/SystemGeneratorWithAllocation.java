package generatorTools;

import java.util.ArrayList;
import java.util.Random;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.ALLOCATION_POLICY;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;

public class SystemGeneratorWithAllocation {
	public CS_LENGTH_RANGE cs_len_range;
	long csl = -1;
	public boolean isLogUni;
	public int maxT;
	public int minT;

	public int number_of_max_access;
	public RESOURCES_RANGE range;
	public double rsf;

	public int total_tasks;
	public int total_partitions;
	public double totalUtil;
	boolean print;

	double maxUtilPerCore = 1;

	public SystemGeneratorWithAllocation(int minT, int maxT, int total_partitions, int totalTasks, boolean isLogUni,
			CS_LENGTH_RANGE cs_len_range, RESOURCES_RANGE range, double rsf, int number_of_max_access, boolean print) {
		this.minT = minT;
		this.maxT = maxT;
		this.totalUtil = 0.1 * (double) totalTasks;
		this.total_partitions = total_partitions;
		this.total_tasks = totalTasks;
		this.isLogUni = isLogUni;
		this.cs_len_range = cs_len_range;
		this.range = range;
		this.rsf = rsf;
		this.number_of_max_access = number_of_max_access;
		this.print = print;

		if (totalUtil / total_partitions <= 0.5)
			maxUtilPerCore = 0.5;
		else if (totalUtil / total_partitions <= 0.6)
			maxUtilPerCore = 0.6;
		else if (totalUtil / total_partitions <= 0.65)
			maxUtilPerCore = 0.65;
		else
			maxUtilPerCore = 1;
	}

	public SystemGeneratorWithAllocation(int minT, int maxT, int total_partitions, int totalTasks, boolean isLogUni,
			CS_LENGTH_RANGE cs_len_range, RESOURCES_RANGE range, double rsf, int number_of_max_access, long csl,
			boolean print) {
		this.minT = minT;
		this.maxT = maxT;
		this.totalUtil = 0.1 * (double) totalTasks;
		this.total_partitions = total_partitions;
		this.total_tasks = totalTasks;
		this.isLogUni = isLogUni;
		this.cs_len_range = cs_len_range;
		this.range = range;
		this.rsf = rsf;
		this.number_of_max_access = number_of_max_access;
		this.csl = csl;
		this.print = print;

		if (totalUtil / total_partitions <= 0.5)
			maxUtilPerCore = 0.5;
		else if (totalUtil / total_partitions <= 0.6)
			maxUtilPerCore = 0.6;
		else if (totalUtil / total_partitions <= 0.65)
			maxUtilPerCore = 0.65;
		else
			maxUtilPerCore = 1;
	}

	/*
	 * generate task sets for multiprocessor fully partitioned fixed-priority
	 * system
	 */
	public ArrayList<SporadicTask> generateTasks() {
		ArrayList<SporadicTask> tasks = null;
		while (tasks == null) {
			tasks = generateT();
			if (tasks != null && (WorstFitAllocation(tasks, total_partitions) == null
					&& BestFitAllocation(tasks, total_partitions) == null
					&& FirstFitAllocation(tasks, total_partitions) == null
					&& NextFitAllocation(tasks, total_partitions) == null))
				tasks = null;
		}
		return tasks;
	}

	private ArrayList<SporadicTask> generateT() {
		int task_id = 1;
		ArrayList<SporadicTask> tasks = new ArrayList<>(total_tasks);
		ArrayList<Long> periods = new ArrayList<>(total_tasks);
		Random random = new Random();

		/* generates random periods */
		while (true) {
			if (!isLogUni) {
				long period = (random.nextInt(maxT - minT) + minT) * 1000;
				if (!periods.contains(period))
					periods.add(period);
			} else {
				double a1 = Math.log(minT);
				double a2 = Math.log(maxT + 1);
				double scaled = random.nextDouble() * (a2 - a1);
				double shifted = scaled + a1;
				double exp = Math.exp(shifted);

				int result = (int) exp;
				result = Math.max(minT, result);
				result = Math.min(maxT, result);

				long period = result * 1000;
				if (!periods.contains(period))
					periods.add(period);
			}

			if (periods.size() >= total_tasks)
				break;
		}
		periods.sort((p1, p2) -> Double.compare(p1, p2));

		/* generate utils */
		UUnifastDiscard unifastDiscard = new UUnifastDiscard(totalUtil, total_tasks, 1000);
		ArrayList<Double> utils = null;
		while (true) {
			utils = unifastDiscard.getUtils();

			double tt = 0;
			for (int i = 0; i < utils.size(); i++) {
				tt += utils.get(i);
			}

			if (utils != null)
				if (utils.size() == total_tasks && tt <= totalUtil)
					break;
		}
		if (print) {
			System.out.print("task utils: ");
			double tt = 0;
			for (int i = 0; i < utils.size(); i++) {
				tt += utils.get(i);
				System.out.print(tt + "   ");
			}
			System.out.println("\n total uitls: " + tt);
		}

		/* generate sporadic tasks */
		for (int i = 0; i < utils.size(); i++) {
			long computation_time = (long) (periods.get(i) * utils.get(i));
			if (computation_time == 0) {
				return null;
			}
			SporadicTask t = new SporadicTask(-1, periods.get(i), computation_time, task_id, utils.get(i));
			task_id++;
			tasks.add(t);
		}

		new PriorityGeneator().deadlineMonotonicPriorityAssignment(tasks, total_tasks);
		tasks.sort((p1, p2) -> -Double.compare(p1.util, p2.util));
		return tasks;
	}

	/*
	 * Generate a set of resources.
	 */
	public ArrayList<Resource> generateResources() {
		/* generate resources from partitions/2 to partitions*2 */
		Random ran = new Random();
		int number_of_resources = 0;

		switch (range) {
		case PARTITIONS:
			number_of_resources = total_partitions;
			break;
		case HALF_PARITIONS:
			number_of_resources = total_partitions / 2;
			break;
		case DOUBLE_PARTITIONS:
			number_of_resources = total_partitions * 2;
			break;
		default:
			break;
		}

		ArrayList<Resource> resources = new ArrayList<>(number_of_resources);

		for (int i = 0; i < number_of_resources; i++) {
			long cs_len = 0;
			if (csl == -1) {
				switch (cs_len_range) {
				case VERY_LONG_CSLEN:
					cs_len = ran.nextInt(300 - 200) + 201;
					break;
				case LONG_CSLEN:
					cs_len = ran.nextInt(200 - 100) + 101;
					break;
				case MEDIUM_CS_LEN:
					cs_len = ran.nextInt(100 - 50) + 51;
					break;
				case SHORT_CS_LEN:
					cs_len = ran.nextInt(50 - 15) + 16;
					break;
				case VERY_SHORT_CS_LEN:
					cs_len = ran.nextInt(15) + 1;
					break;
				case RANDOM:
					cs_len = ran.nextInt(300) + 1;
				default:
					break;
				}
			} else
				cs_len = csl;

			Resource resource = new Resource(i + 1, cs_len);
			resources.add(resource);
		}

		resources.sort((r2, r1) -> Long.compare(r1.csl, r2.csl));

		for (int i = 0; i < resources.size(); i++) {
			Resource res = resources.get(i);
			res.id = i + 1;
		}

		return resources;
	}

	public void generateResourceUsage(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources) {
		int fails = 0;
		Random ran = new Random();
		long number_of_resource_requested_tasks = Math.round(rsf * tasks.size());

		/* Generate resource usage */
		for (long l = 0; l < number_of_resource_requested_tasks; l++) {
			if (fails > 1000) {
				tasks = generateTasks();
				while (tasks == null)
					tasks = generateTasks();
				l = 0;
				fails++;
			}
			int task_index = ran.nextInt(tasks.size());
			while (true) {
				if (tasks.get(task_index).resource_required_index.size() == 0)
					break;
				task_index = ran.nextInt(tasks.size());
			}
			SporadicTask task = tasks.get(task_index);

			/* Find the resources that we are going to access */
			int number_of_requested_resource = ran.nextInt(resources.size()) + 1;
			for (int j = 0; j < number_of_requested_resource; j++) {
				while (true) {
					int resource_index = ran.nextInt(resources.size());
					if (!task.resource_required_index.contains(resource_index)) {
						task.resource_required_index.add(resource_index);
						break;
					}
				}
			}
			task.resource_required_index.sort((r1, r2) -> Integer.compare(r1, r2));

			long total_resource_execution_time = 0;
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				int number_of_requests = ran.nextInt(number_of_max_access) + 1;
				task.number_of_access_in_one_release.add(number_of_requests);
				total_resource_execution_time += number_of_requests
						* resources.get(task.resource_required_index.get(k)).csl;
			}

			if (total_resource_execution_time > task.WCET) {
				l--;
				task.resource_required_index.clear();
				task.number_of_access_in_one_release.clear();
				fails++;
			} else {
				task.WCET = task.WCET - total_resource_execution_time;
				task.pure_resource_execution_time = total_resource_execution_time;
				if (task.resource_required_index.size() > 0)
					task.hasResource = 1;
			}
		}

	}

	public ArrayList<ArrayList<SporadicTask>> allocateTasks(ArrayList<SporadicTask> tasksToAllocate,
			ArrayList<Resource> resources, ALLOCATION_POLICY policy) {

		ArrayList<ArrayList<SporadicTask>> tasks;
		switch (policy) {
		case WORST_FIT:
			tasks = WorstFitAllocation(tasksToAllocate, total_partitions);
			break;
		case BEST_FIT:
			tasks = BestFitAllocation(tasksToAllocate, total_partitions);
			break;
		case FIRST_FIT:
			tasks = FirstFitAllocation(tasksToAllocate, total_partitions);
			break;
		case NEXT_FIT:
			tasks = NextFitAllocation(tasksToAllocate, total_partitions);
			break;
		case RESOURCE_REQUEST_TASKS_FIT:
			tasks = ResourceRequestTasksAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case RESOURCE_LOCAL_FIT:
			tasks = ResourceLocalAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case RESOURCE_LENGTH_DECREASE_FIT:
			tasks = ResourceLengthDecreaseAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case RESOURCE_LENGTH_INCREASE_FIT:
			tasks = ResourceLengthIncreaseAllocation(tasksToAllocate, total_partitions, resources);
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
		}

		if (resources != null && tasks != null) {
			for (int i = 0; i < resources.size(); i++) {
				Resource res = resources.get(i);
				res.ceiling.clear();
				res.isGlobal = false;
				res.partitions.clear();
				res.requested_tasks.clear();
			}

			/* for each resource */
			for (int i = 0; i < resources.size(); i++) {
				Resource resource = resources.get(i);

				/* for each partition */
				for (int j = 0; j < tasks.size(); j++) {
					int ceiling = 0;

					/* for each task in the given partition */
					for (int k = 0; k < tasks.get(j).size(); k++) {
						SporadicTask task = tasks.get(j).get(k);

						if (task.resource_required_index.contains(resource.id - 1)) {
							resource.requested_tasks.add(task);
							ceiling = task.priority > ceiling ? task.priority : ceiling;
							if (!resource.partitions.contains(task.partition)) {
								resource.partitions.add(task.partition);
							}
						}
					}

					if (ceiling > 0)
						resource.ceiling.add(ceiling);
				}

				if (resource.partitions.size() > 1)
					resource.isGlobal = true;
			}
		}

		return tasks;
	}

	public ArrayList<ArrayList<SporadicTask>> allocateTasks(ArrayList<SporadicTask> tasksToAllocate,
			ArrayList<Resource> resources, int policy) {

		ArrayList<ArrayList<SporadicTask>> tasks;
		switch (policy) {
		case 0:
			tasks = WorstFitAllocation(tasksToAllocate, total_partitions);
			break;
		case 1:
			tasks = BestFitAllocation(tasksToAllocate, total_partitions);
			break;
		case 2:
			tasks = FirstFitAllocation(tasksToAllocate, total_partitions);
			break;
		case 3:
			tasks = NextFitAllocation(tasksToAllocate, total_partitions);
			break;
		case 4:
			tasks = ResourceRequestTasksAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case 5:
			tasks = ResourceLocalAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case 6:
			tasks = ResourceLengthDecreaseAllocation(tasksToAllocate, total_partitions, resources);
			break;
		case 7:
			tasks = ResourceLengthIncreaseAllocation(tasksToAllocate, total_partitions, resources);
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
		}

		if (resources != null && tasks != null) {
			for (int i = 0; i < resources.size(); i++) {
				Resource res = resources.get(i);
				res.ceiling.clear();
				res.isGlobal = false;
				res.partitions.clear();
				res.requested_tasks.clear();
			}

			/* for each resource */
			for (int i = 0; i < resources.size(); i++) {
				Resource resource = resources.get(i);

				/* for each partition */
				for (int j = 0; j < tasks.size(); j++) {
					int ceiling = 0;

					/* for each task in the given partition */
					for (int k = 0; k < tasks.get(j).size(); k++) {
						SporadicTask task = tasks.get(j).get(k);

						if (task.resource_required_index.contains(resource.id - 1)) {
							resource.requested_tasks.add(task);
							ceiling = task.priority > ceiling ? task.priority : ceiling;
							if (!resource.partitions.contains(task.partition)) {
								resource.partitions.add(task.partition);
							}
						}
					}

					if (ceiling > 0)
						resource.ceiling.add(ceiling);
				}

				if (resource.partitions.size() > 1)
					resource.isGlobal = true;
			}
		}

		return tasks;
	}

	private ArrayList<ArrayList<SporadicTask>> WorstFitAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions) {
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
			double minUtil = 2;
			for (int j = 0; j < partitions; j++) {
				if (minUtil > utilPerPartition.get(j)) {
					minUtil = utilPerPartition.get(j);
					target = j;
				}
			}

			if (target == -1) {
				System.err.println("WF error!");
				System.exit(-1);
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

	private ArrayList<ArrayList<SporadicTask>> BestFitAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions) {

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

	private ArrayList<ArrayList<SporadicTask>> FirstFitAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions) {

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
				if ((maxUtilPerCore - utilPerPartition.get(j) >= task.util)
						|| (task.util > maxUtilPerCore && 1 - utilPerPartition.get(j) >= task.util)) {
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

	private ArrayList<ArrayList<SporadicTask>> NextFitAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions) {

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
				if ((maxUtilPerCore - utilPerPartition.get(j) >= task.util)
						|| (task.util > maxUtilPerCore && 1 - utilPerPartition.get(j) >= task.util)) {
					task.partition = currentIndex;
					utilPerPartition.set(currentIndex, utilPerPartition.get(currentIndex) + task.util);
					break;
				}
				if (currentIndex == total_partitions - 1)
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

	private ArrayList<ArrayList<SporadicTask>> ResourceRequestTasksAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions, ArrayList<Resource> resources) {
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
				NoQT.get(task.resource_required_index.get(k)).set(1,
						NoQT.get(task.resource_required_index.get(k)).get(1) + 1);
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

		return FirstFitAllocation(sortedTasks, partitions);
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLocalAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions, ArrayList<Resource> resources) {
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
				utilOfRT.get(task.resource_required_index.get(k)).set(1,
						utilOfRT.get(task.resource_required_index.get(k)).get(1) + task.util);
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

		return FirstFitAllocation(sortedT, partitions);
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLengthDecreaseAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions, ArrayList<Resource> resources) {
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

		return FirstFitAllocation(sortedT, partitions);
	}

	private ArrayList<ArrayList<SporadicTask>> ResourceLengthIncreaseAllocation(ArrayList<SporadicTask> tasksToAllocate,
			int partitions, ArrayList<Resource> resources) {
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

		return FirstFitAllocation(sortedT, partitions);
	}

	public void testifyAllocatedTasksetAndResource(ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<Resource> resources) {
		System.out.println("----------------------------------------------------");
		if (tasks == null) {
			System.out.println("no tasks generated.");
		} else {
			for (int i = 0; i < tasks.size(); i++) {
				double util = 0;
				for (int j = 0; j < tasks.get(i).size(); j++) {
					SporadicTask task = tasks.get(i).get(j);
					util += ((double) (task.WCET + task.pure_resource_execution_time)) / (double) task.period;
					System.out.println(tasks.get(i).get(j).getInfo());
				}
				System.out.println("util on partition: " + i + " : " + util);
			}
			System.out.println("----------------------------------------------------");

			if (resources != null) {
				System.out.println("****************************************************");
				for (int i = 0; i < resources.size(); i++) {
					System.out.println(resources.get(i).toString());
				}
				System.out.println("****************************************************");

				String resource_usage = "";
				/* print resource usage */
				System.out.println("---------------------------------------------------------------------------------");
				for (int i = 0; i < tasks.size(); i++) {
					for (int j = 0; j < tasks.get(i).size(); j++) {

						SporadicTask task = tasks.get(i).get(j);
						String usage = "T" + task.id + ": ";
						for (int k = 0; k < task.resource_required_index.size(); k++) {
							usage = usage + "R" + resources.get(task.resource_required_index.get(k)).id + " - "
									+ task.number_of_access_in_one_release.get(k) + ";  ";
						}
						usage += "\n";
						if (task.resource_required_index.size() > 0)
							resource_usage = resource_usage + usage;
					}
				}

				System.out.println(resource_usage);
				System.out.println("---------------------------------------------------------------------------------");
			}
		}

	}

	public void testifyGeneratedTasksetAndResource(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources) {
		System.out.println("----------------------------------------------------");
		for (int i = 0; i < tasks.size(); i++) {
			System.out.println(tasks.get(i).getInfo());
		}
		System.out.println("----------------------------------------------------");
		System.out.println("****************************************************");
		for (int i = 0; i < resources.size(); i++) {
			System.out.println(resources.get(i).toString());
		}
		System.out.println("****************************************************");

		String resource_usage = "";
		/* print resource usage */
		System.out.println("---------------------------------------------------------------------------------");
		for (int i = 0; i < tasks.size(); i++) {
			SporadicTask task = tasks.get(i);
			String usage = "T" + task.id + ": ";
			for (int k = 0; k < task.resource_required_index.size(); k++) {
				usage = usage + "R" + resources.get(task.resource_required_index.get(k)).id + " - "
						+ task.number_of_access_in_one_release.get(k) + ";  ";
			}
			usage += "\n";
			if (task.resource_required_index.size() > 0)
				resource_usage = resource_usage + usage;

		}
		System.out.println(resource_usage);
		System.out.println("---------------------------------------------------------------------------------");

	}
}
