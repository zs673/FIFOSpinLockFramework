package entity;

import java.util.ArrayList;

public class Resource {

	public int id;
	public long csl;
	public int protocol;
	public boolean isGlobal = false;

	public ArrayList<Integer> partitions;
	public ArrayList<SporadicTask> requested_tasks;

	public Resource(int id, long cs_len) {
		this.id = id;
		this.csl = cs_len;
		requested_tasks = new ArrayList<>();
		partitions = new ArrayList<>();
		this.protocol = 1;
	}

	@Override
	public String toString() {
		return "R" + this.id + " : cs len = " + this.csl + ", partitions: " + partitions.size() + ", tasks: "
				+ requested_tasks.size() + ", isGlobal: " + isGlobal;
	}

	public int getCeilingForProcessor(int partition, ArrayList<ArrayList<SporadicTask>> tasks) {
		int ceiling = -1;

		// if (!this.partitions.contains(partition)) {
		// System.err.println("This resource does not contain the partiton: R" +
		// this.id + " partition: " + partition);
		// System.exit(-1);
		// }

		for (int k = 0; k < tasks.get(partition).size(); k++) {
			SporadicTask task = tasks.get(partition).get(k);

			if (task.resource_required_index.contains(this.id - 1)) {
				ceiling = task.priority > ceiling ? task.priority : ceiling;
			}
		}

		// if (ceiling <= 0) {
		// System.err.println("the ceiling is <= 0. there must be something
		// wrong. Check it!");
		// System.exit(-1);
		// }

		return ceiling;
	}

	public int getCeilingForProcessor(ArrayList<SporadicTask> tasks) {
		int ceiling = -1;

		for (int k = 0; k < tasks.size(); k++) {
			SporadicTask task = tasks.get(k);

			if (task.resource_required_index.contains(this.id - 1)) {
				ceiling = task.priority > ceiling ? task.priority : ceiling;
			}
		}

		// if (ceiling <= 0) {
		// System.err.println("111the ceiling is <= 0. there must be something
		// wrong. Check it!");
		// System.exit(-1);
		// }

		return ceiling;
	}

}
