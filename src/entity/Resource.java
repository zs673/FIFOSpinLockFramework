package entity;

import java.util.ArrayList;

public class Resource {

	public ArrayList<Integer> ceiling;
	public long csl;

	public int id;
	public boolean isGlobal = false;
	public ArrayList<Integer> partitions;

	public int protocol;

	public ArrayList<SporadicTask> requested_tasks;

	public Resource(int id, long cs_len) {
		this.id = id;
		this.csl = cs_len;
		requested_tasks = new ArrayList<>();
		partitions = new ArrayList<>();
		ceiling = new ArrayList<>();
		this.protocol = 1;
	}

	@Override
	public String toString() {
		return "R" + this.id + " : cs len = " + this.csl + ", partitions: " + partitions.size() + ", tasks: " + requested_tasks.size()
				+ ", isGlobal: " + isGlobal;
	}

}
