package entity;

import java.util.ArrayList;

public class SporadicTask {
	public int priority;
	public long period;
	public long deadline;
	public long WCET;
	public int partition;
	public int id;

	public long pure_resource_execution_time = 0;
	public long Ri = 0, spin = 0, interference = 0, local = 0, indirectspin = 0, total_blocking = 0;

	public ArrayList<Integer> resource_required_index;
	public ArrayList<Integer> number_of_access_in_one_release;

	public long spin_delay_by_preemptions = 0;

	public double implementation_overheads = 0;
	public double migration_overheads_plus = 0;

	/* Used by LP solver from C code */
	public int hasResource = 0;
	public int[] resource_required_index_cpoy = null;
	public int[] number_of_access_in_one_release_copy = null;
	
	public double[] fifonp = null;
	public double[] fifop = null;
	public double[] mrsp = null;
	
	public double np_section = 0;

	public SporadicTask(int priority, long t, long c, int partition, int id) {
		this.priority = priority;
		this.period = t;
		this.WCET = c;
		this.deadline = t;
		this.partition = partition;
		this.id = id;

		resource_required_index = new ArrayList<>();
		number_of_access_in_one_release = new ArrayList<>();

		resource_required_index_cpoy = null;
		number_of_access_in_one_release_copy = null;

		Ri = 0;
		spin = 0;
		interference = 0;
		local = 0;
	}

	@Override
	public String toString() {
		return "T" + this.id + " : T = " + this.period + ", C = " + this.WCET + ", PRET: " + this.pure_resource_execution_time + ", D = "
				+ this.deadline + ", Priority = " + this.priority + ", Partition = " + this.partition;
	}

	public String RTA() {
		return "T" + this.id + " : R = " + this.Ri + ", S = " + this.spin + ", I = " + this.interference + ", A = " + this.local
				+ ". is schedulable: " + (Ri <= deadline);
	}

}
