package generatorTools;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class PriorityGeneator {
	public static final int MAX_PRIORITY = 1000;
	
	public ArrayList<ArrayList<SporadicTask>> assignPrioritiesByDM(ArrayList<ArrayList<SporadicTask>> tasksToAssgin,
			ArrayList<Resource> resources) {
		if (tasksToAssgin == null) {
			return null;
		}

		ArrayList<ArrayList<SporadicTask>> tasks = new ArrayList<>(tasksToAssgin);
		// ASSIGN PRIORITIES
		for (int i = 0; i < tasks.size(); i++) {
			new PriorityGeneator().deadlineMonotonicPriorityAssignment(tasks.get(i), tasks.get(i).size());
		}

		if (resources != null && resources.size() > 0) {
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


	private void deadlineMonotonicPriorityAssignment(ArrayList<SporadicTask> taskset, int number) {
		ArrayList<Integer> priorities = generatePriorities(number);
		/* deadline monotonic assignment */
		taskset.sort((t1, t2) -> Double.compare(t1.deadline, t2.deadline));
		priorities.sort((p1, p2) -> -Integer.compare(p1, p2));
		for (int i = 0; i < taskset.size(); i++) {
			taskset.get(i).priority = priorities.get(i);
		}
	}

	private ArrayList<Integer> generatePriorities(int number) {
		ArrayList<Integer> priorities = new ArrayList<>();
		for (int i = 0; i < number; i++)
			priorities.add(MAX_PRIORITY - (i + 1) * 2);
		return priorities;
	}
}
