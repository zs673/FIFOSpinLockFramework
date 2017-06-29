package analysisWithImplementationOverheads;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class AOPTest {

	public static boolean testSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		boolean isSchedulable = true;

		ArrayList<Integer> priority_levels = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			priority_levels.add(tasks.get(i).size());
		}

		return isSchedulable;
	}
}
