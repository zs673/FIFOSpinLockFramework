package generatorTools;

import java.util.ArrayList;

import entity.SporadicTask;

public class PriorityUtil {
	public static final int MAX_PRIORITY = 1000;

	private ArrayList<Integer> generatePriorities(int number) {
		ArrayList<Integer> priorities = new ArrayList<>();
		for (int i = 0; i < number; i++)
			priorities.add(MAX_PRIORITY - (i + 1) * 2);
		return priorities;
	}

	public void deadlineMonotonicPriorityAssignment(ArrayList<SporadicTask> taskset, int number) {
		ArrayList<Integer> priorities = generatePriorities(number);
		/* deadline monotonic assignment */
		taskset.sort((t1, t2) -> Double.compare(t1.deadline, t2.deadline));
		priorities.sort((p1, p2) -> -Integer.compare(p1, p2));
		for (int i = 0; i < taskset.size(); i++) {
			taskset.get(i).priority = priorities.get(i);
		}
	}
}
