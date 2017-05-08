package noAllocation.entity;

import java.util.ArrayList;

public class SporadicTask {
	public long deadline;
	public double[] fifonp = null;
	public double[] fifop = null;
	/* Used by LP solver from C code */
	public int hasResource = 0;
	public int id;
	public double implementation_overheads = 0, blocking_overheads = 0, mrsp_arrivalblocking_overheads = 0, fifonp_arrivalblocking_overheads = 0,
			fifop_arrivalblocking_overheads = 0;

	public double migration_overheads_plus = 0;
	public double[] mrsp = null;
	public double np_section = 0;

	public ArrayList<Integer> number_of_access_in_one_release;
	public int[] number_of_access_in_one_release_copy = null;

	public int partition;
	public long period;

	public int priority;
	public long pure_resource_execution_time = 0;
	public ArrayList<Integer> resource_required_index;

	public int[] resource_required_index_cpoy = null;
	public long Ri = 0, spin = 0, interference = 0, local = 0, indirectspin = 0, total_blocking = 0;
	public long spin_delay_by_preemptions = 0;

	public long WCET;

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

	public String RTA() {
		return "T" + this.id + " : R = " + this.Ri + ", S = " + this.spin + ", I = " + this.interference + ", A = " + this.local
				+ ". is schedulable: " + (Ri <= deadline);
	}

	@Override
	public String toString() {
		return "T" + this.id + " : T = " + this.period + ", C = " + this.WCET + ", PRET: " + this.pure_resource_execution_time + ", D = "
				+ this.deadline + ", Priority = " + this.priority + ", Partition = " + this.partition;
	}

}
