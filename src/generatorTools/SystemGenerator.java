package generatorTools;

import java.util.ArrayList;
import java.util.Random;

import entity.Resource;
import entity.SporadicTask;

public class SystemGenerator {
	long csl = -1;

	/* define how long the critical section can be */
	public static enum CS_LENGTH_RANGE {
		VERY_LONG_CSLEN, LONG_CSLEN, MEDIUM_CS_LEN, SHORT_CS_LEN, VERY_SHORT_CS_LEN, Random
	};

	/* define how many resources in the system */
	public static enum RESOURCES_RANGE {
		HALF_PARITIONS, /* partitions / 2 us */
		PARTITIONS, /* partitions us */
		DOUBLE_PARTITIONS, /* partitions * 2 us */
	};

	public int task_id = 1;
	public int minT;
	public int maxT;
	public double util;
	public int total_partitions;
	public int number_of_tasks_per_processor;
	public boolean isLogUni;
	public CS_LENGTH_RANGE cs_len_range;
	public RESOURCES_RANGE range;

	public double rsf;
	public int number_of_max_access;

	public SystemGenerator(int minT, int maxT, double util, int total_partitions, int number_of_tasks_per_processor, boolean isLogUni,
			CS_LENGTH_RANGE cs_len_range, RESOURCES_RANGE range, double rsf, int number_of_max_access) {
		this.minT = minT;
		this.maxT = maxT;
		this.util = util;
		this.total_partitions = total_partitions;
		this.number_of_tasks_per_processor = number_of_tasks_per_processor;
		this.isLogUni = isLogUni;
		this.cs_len_range = cs_len_range;
		this.range = range;
		this.rsf = rsf;
		this.number_of_max_access = number_of_max_access;
		this.csl = -1;
	}

	public SystemGenerator(int minT, int maxT, double util, int total_partitions, int number_of_tasks_per_processor, boolean isLogUni,
			CS_LENGTH_RANGE cs_len_range, RESOURCES_RANGE range, double rsf, int number_of_max_access, long csl) {
		this.minT = minT;
		this.maxT = maxT;
		this.util = util;
		this.total_partitions = total_partitions;
		this.number_of_tasks_per_processor = number_of_tasks_per_processor;
		this.isLogUni = isLogUni;
		this.cs_len_range = cs_len_range;
		this.range = range;
		this.rsf = rsf;
		this.number_of_max_access = number_of_max_access;
		this.csl = csl;
	}

	/*
	 * generate task sets for multiprocessor fully partitioned fixed-priority
	 * system
	 */
	public ArrayList<ArrayList<SporadicTask>> generateTasks() {
		task_id = 1;
		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>();
		for (int i = 0; i < total_partitions; i++) {
			ArrayList<SporadicTask> tasks_on_one_partition = null;
			while (tasks_on_one_partition == null) {
				tasks_on_one_partition = generateTaskset(i);
			}
			tasks.add(tasks_on_one_partition);
		}
		return tasks;
	}

	private ArrayList<SporadicTask> generateTaskset(int partition_id) {
		ArrayList<SporadicTask> tasks = new ArrayList<>(number_of_tasks_per_processor);
		ArrayList<Long> periods = new ArrayList<>(number_of_tasks_per_processor);
		Random random = new Random();

		tasks.clear();
		periods.clear();
		/* generates random periods */
		while (true) {
			if (!isLogUni) {
				long period = (random.nextInt((int) (maxT - minT)) + (int) minT) * 1000;
				if (!periods.contains(period))
					periods.add(period);
			} else {
				double a1 = Math.log(minT);
				double a2 = Math.log(maxT + 1);
				double scaled = random.nextDouble() * (a2 - a1);
				double shifted = scaled + a1;
				double exp = Math.exp(shifted);

				int result = (int) exp;
				result = Math.max((int) minT, result);
				result = Math.min((int) maxT, result);

				long period = result * 1000;
				if (!periods.contains(period))
					periods.add(period);
			}

			if (periods.size() >= number_of_tasks_per_processor)
				break;
		}
		periods.sort((p1, p2) -> Double.compare(p1, p2));

		/* generate utils */
		UUnifastDiscard unifastDiscard = new UUnifastDiscard(util, number_of_tasks_per_processor, 1000);
		ArrayList<Double> utils = null;
		while (true) {
			utils = unifastDiscard.getUtils();
			if (utils != null)
				if (utils.size() == number_of_tasks_per_processor)
					break;
		}

		// double tt = 0;
		// for(int i=0;i<utils.size();i++){
		// tt += utils.get(i);
		// }
		// System.out.println("total uitls: "+ tt);

		/* generate sporadic tasks */
		for (int i = 0; i < utils.size(); i++) {
			long computation_time = (long) (periods.get(i) * utils.get(i));
			if (computation_time == 0)
				return null;
			SporadicTask t = new SporadicTask(-1, periods.get(i), computation_time, partition_id, task_id);
			task_id++;
			tasks.add(t);
		}

		/* assign priorities */
		new PriorityUtil().deadlineMonotonicPriorityAssignment(tasks, number_of_tasks_per_processor);
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
				case Random:
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

	public int generateResourceUsage(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		Random ran = new Random();
		int fails = 0;

		for (int i = 0; i < tasks.size(); i++) {
			int failed = 0;
			int number_of_resource_requested_tasks = 0;
			try {
				number_of_resource_requested_tasks = (int) (rsf * tasks.get(i).size());
			} catch (NullPointerException e) {
				System.out.println("i" + i);
			}

			/* Generate resource usage */
			for (int l = 0; l < number_of_resource_requested_tasks; l++) {
				if (failed > 1000) {
					// System.out.println("hi" + " i = " + i);
					ArrayList<SporadicTask> taskoni = generateTaskset(i);
					while (taskoni == null)
						taskoni = generateTaskset(i);

					tasks.set(i, taskoni);
					// System.out.println("hi" + " i = " + i + ". task on i has
					// a size: " + taskoni.size());
					i--;
					fails++;

					break;
					// return -1;
				}

				int task_index = ran.nextInt(tasks.get(i).size());
				while (true) {
					if (tasks.get(i).get(task_index).resource_required_index.size() == 0)
						break;
					task_index = ran.nextInt(tasks.get(i).size());
				}
				SporadicTask task = tasks.get(i).get(task_index);

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
				/*
				 * Generate the number_of_requests that we are going to issue
				 * for each requested resource
				 */
				for (int k = 0; k < task.resource_required_index.size(); k++) {
					int number_of_requests = ran.nextInt(number_of_max_access) + 1;
					task.number_of_access_in_one_release.add(number_of_requests);
					total_resource_execution_time += number_of_requests * resources.get(task.resource_required_index.get(k)).csl;
				}

				/*
				 * Check whether the resource accessing time is bigger than the
				 * computation time, if so we discard this computation.
				 */
				if (total_resource_execution_time > task.WCET) {
					l--;
					task.resource_required_index.clear();
					task.number_of_access_in_one_release.clear();
					failed++;
				} else {
					task.WCET = task.WCET - total_resource_execution_time;
					task.pure_resource_execution_time = total_resource_execution_time;

					if (task.resource_required_index.size() > 0) {
						task.hasResource = 1;

						task.resource_required_index_cpoy = new int[task.resource_required_index.size()];
						task.number_of_access_in_one_release_copy = new int[task.number_of_access_in_one_release.size()];
						if (task.number_of_access_in_one_release_copy.length != task.resource_required_index_cpoy.length) {
							System.err.println("error, task copyies not equal size");
							System.exit(-1);
						}
						for (int resource_index = 0; resource_index < task.resource_required_index.size(); resource_index++) {
							task.resource_required_index_cpoy[resource_index] = task.resource_required_index.get(resource_index);
							task.number_of_access_in_one_release_copy[resource_index] = task.number_of_access_in_one_release.get(resource_index);
						}
					}

				}

			}
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

		// System.out.println("failed: " + failed);
		return fails;
	}

	static public void testifyGeneratedTasksetAndResource(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		System.out.println("----------------------------------------------------");
		for (int i = 0; i < tasks.size(); i++) {
			double util = 0;
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				util += ((double) (task.WCET + task.pure_resource_execution_time)) / (double) task.period;
				System.out.println(tasks.get(i).get(j).toString());
			}
			System.out.println("util on partition: " + i + " : " + util);
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
			for (int j = 0; j < tasks.get(i).size(); j++) {

				SporadicTask task = tasks.get(i).get(j);
				String usage = "T" + task.id + ": ";
				for (int k = 0; k < task.resource_required_index.size(); k++) {
					usage = usage + "R" + resources.get(task.resource_required_index.get(k)).id + " - " + task.number_of_access_in_one_release.get(k) + ";  ";
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
