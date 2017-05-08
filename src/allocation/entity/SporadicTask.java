package allocation.entity;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class SporadicTask {
	public int id;
	public long deadline;
	public long WCET;
	public int partition;
	public long period;
	public int priority;
	public double util;

	public int hasResource = 0;
	public long pure_resource_execution_time = 0;
	public ArrayList<Integer> resource_required_index;
	public long Ri = 0, spin = 0, interference = 0, local = 0, indirectspin = 0, total_blocking = 0;
	public long spin_delay_by_preemptions = 0;

	public ArrayList<Integer> number_of_access_in_one_release;
	public double np_section = 0;

	public double[] mrsp = null;
	public double[] fifonp = null;
	public double[] fifop = null;

	public double implementation_overheads = 0, blocking_overheads = 0, mrsp_arrivalblocking_overheads = 0, fifonp_arrivalblocking_overheads = 0,
			fifop_arrivalblocking_overheads = 0;
	public double migration_overheads_plus = 0;

	public SporadicTask(int priority, long t, long c, int partition, int id, double util) {
		this.priority = priority;
		this.period = t;
		this.WCET = c;
		this.deadline = t;
		this.partition = partition;
		this.id = id;
		this.util = util;

		resource_required_index = new ArrayList<>();
		number_of_access_in_one_release = new ArrayList<>();

		Ri = 0;
		spin = 0;
		interference = 0;
		local = 0;
	}

	public SporadicTask(int priority, long t, long c, int id, double util) {
		this.priority = priority;
		this.period = t;
		this.WCET = c;
		this.deadline = t;
		this.id = id;
		this.partition = -1;
		this.util = util;

		resource_required_index = new ArrayList<>();
		number_of_access_in_one_release = new ArrayList<>();

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
		DecimalFormat df = new DecimalFormat("#.#######");
		return "T" + this.id + " : T = " + this.period + ", C = " + this.WCET + ", PRET: " + this.pure_resource_execution_time + ", D = "
				+ this.deadline + ", Priority = " + this.priority + ", Partition = " + this.partition + ", Util: "
				+ Double.parseDouble(df.format(util));
	}

}
