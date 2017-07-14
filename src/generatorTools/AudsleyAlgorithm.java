package generatorTools;

import java.util.ArrayList;

import analysisWithImplementationOverheads.IACombinedProtocol;
import entity.Resource;
import entity.SporadicTask;

public class AudsleyAlgorithm {

	IACombinedProtocol analysis = new IACombinedProtocol();
	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;
	ArrayList<ArrayList<Integer>> priorities = new ArrayList<>();

	public AudsleyAlgorithm(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		this.tasks = tasks;
		this.resources = resources;

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((p1, p2) -> -Double.compare(p1.deadline, p2.deadline));
		}

		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<Integer> prio = new ArrayList<>();
			for (int j = 0; j < tasks.get(i).size(); j++) {
				prio.add(-1);
			}

			priorities.add(prio);
		}
	}

	public ArrayList<ArrayList<SporadicTask>> solve() {

		return setPriorities(tasks, priorities);
	}

	private ArrayList<ArrayList<SporadicTask>> setPriorities(ArrayList<ArrayList<SporadicTask>> tasks,
			ArrayList<ArrayList<Integer>> priorities) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				tasks.get(i).get(j).priority = priorities.get(i).get(j);
			}
		}

		return tasks;
	}
}
