package entity;

import java.util.ArrayList;

public class Resource {

	public int id;
	public long csl;

	public ArrayList<SporadicTask> requested_tasks;
	public ArrayList<Integer> partitions;
	public ArrayList<Integer> ceiling;

	public int protocol;

	public boolean isGlobal = false;

	public Resource(int id, long cs_len) {
		this.id = id;
		this.csl = cs_len;
		requested_tasks = new ArrayList<>();
		partitions = new ArrayList<>();
		ceiling = new ArrayList<>();
		this.protocol = 0;
	}

	@Override
	public String toString() {
		return "R" + this.id + " : cs len = " + this.csl + ", partitions: " + partitions.size() + ", tasks: " + requested_tasks.size()
				+ ", isGlobal: " + isGlobal;
	}

}
