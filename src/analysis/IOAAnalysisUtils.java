package analysis;

import java.util.ArrayList;

import entity.SporadicTask;

public class IOAAnalysisUtils {
	public static int extendCal = 5;

//	public static double FIFONP_LOCK = (double) (501 + 259 + 219) / (double) 1000;
//	public static double FIFONP_UNLOCK = (double) 602 / (double) 1000;
//	public static double FIFOP_DEQUEUE_IN_SCHEDULE = (double) 703 / (double) 1000;
//
//	public static double FIFOP_LOCK = (double) (744 + 216 + 295) / (double) 1000;
//	public static double FIFOP_RE_REQUEST = (double) (744 + 216) / (double) 1000;
//	public static double FIFOP_UNLOCK = (double) 602 / (double) 1000;
//
//	public static double FINISH_SWITCH = (double) (1113 + 1165) / (double) 1000;
//
//	public static double LINUX_CONTEXT_SWTICH = (double) (965) / (double) 1000;
//	// private static double CACHE_OVERHEADS = (double) (1905) / (double) 1000;
//	public static double LINUX_SCHED = (double) (845) / (double) 1000;
//
//	public static double LINUX_SCHED_AWAY = (double) (736) / (double) 1000;
//	public static double LITMUS_COMPLETE = (double) (411) / (double) 1000;
//
//	public static double LITMUS_RELEASE = (double) (1383) / (double) 1000;
//	public static double MrsP_HELP_IN_LOCK = (double) 2431 / (double) 1000;
//	public static double MrsP_HELP_IN_SCHEDULE = (double) 745 / (double) 1000;
//	public static double MrsP_INSERT = (double) 2347 / (double) 1000;
//
//	public static double MrsP_LOCK = (double) (794 + 259 + 219) / (double) 1000;
//
//	public static double MrsP_UNLOCK = (double) (744 + 65 + 571 + 262) / (double) 1000;
//	public static double PFP_SCHED_CHECK = (double) (492) / (double) 1000;
//	public static double PFP_SCHED_REQUEUE = (double) (603) / (double) 1000;
//
//	public static double PFP_SCHED_SET_NEXT = (double) (308) / (double) 1000;
//	public static double PFP_SCHED_TAKE_NEXT = (double) (274) / (double) 1000;
//
//	public static double PFP_SCHEDULER = (double) (492 + 603 + 274 + 308) / (double) 1000;
//
//	public static double MrsP_PREEMPTION_AND_MIGRATION = LINUX_SCHED * 2 + PFP_SCHED_CHECK * 2 + MrsP_INSERT + PFP_SCHED_REQUEUE
//			+ MrsP_HELP_IN_SCHEDULE + PFP_SCHED_SET_NEXT + LINUX_SCHED_AWAY + LINUX_CONTEXT_SWTICH;
//
//	public static double FULL_CONTEXT_SWTICH1 = LINUX_SCHED + LINUX_SCHED_AWAY + LINUX_CONTEXT_SWTICH + PFP_SCHEDULER;
//	public static double FULL_CONTEXT_SWTICH2 = FULL_CONTEXT_SWTICH1 + LITMUS_RELEASE + LITMUS_COMPLETE;

	 public static double FIFONP_LOCK = 0;
	 public static double FIFONP_UNLOCK = 0;
	 public static double FIFOP_LOCK = 0;
	 public static double FIFOP_UNLOCK = 0;
	 public static double FIFOP_DEQUEUE_IN_SCHEDULE = 0;
	 public static double FIFOP_RE_REQUEST = 0;
	 public static double MrsP_LOCK = 0;
	 public static double MrsP_UNLOCK = 0;
	 public static double MrsP_HELP_IN_LOCK = 0;
	 public static double MrsP_INSERT = 0;
	 public static double MrsP_HELP_IN_SCHEDULE = 0;
	 public static double FULL_CONTEXT_SWTICH1 = 0;
	 public static double FULL_CONTEXT_SWTICH2 = 0;
	 public static double MrsP_PREEMPTION_AND_MIGRATION = 6;

	public static void cloneList(long[][] oldList, long[][] newList) {
		for (int i = 0; i < oldList.length; i++) {
			for (int j = 0; j < oldList[i].length; j++) {
				newList[i][j] = oldList[i][j];
			}
		}
	}

	public static long[][] initResponseTime(ArrayList<ArrayList<SporadicTask>> tasks) {
		long[][] response_times = new long[tasks.size()][];

		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<SporadicTask> task_on_a_partition = tasks.get(i);
			task_on_a_partition.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

			long[] Ri = new long[task_on_a_partition.size()];

			for (int j = 0; j < task_on_a_partition.size(); j++) {
				SporadicTask t = task_on_a_partition.get(j);
				Ri[j] = t.Ri = t.WCET + t.pure_resource_execution_time;
				t.interference = t.local = t.spin = t.indirectspin = 0;
			}
			response_times[i] = Ri;
		}
		return response_times;
	}

	public static boolean isArrayContain(int[] array, int value) {

		for (int i = 0; i < array.length; i++) {
			if (array[i] == value)
				return true;
		}
		return false;
	}

	public static void main(String args[]) {
		System.out.println(" FIFO-P Lock:   " + FIFOP_LOCK + "   FIFO-P UNLOCK:   " + FIFOP_UNLOCK);
		System.out.println(" FIFO-NP Lock:   " + FIFONP_LOCK + "   FIFO-NP UNLOCK:   " + FIFONP_UNLOCK + "   RE-REQUEST:   "
				+ (IOAAnalysisUtils.FIFOP_DEQUEUE_IN_SCHEDULE + IOAAnalysisUtils.FIFOP_RE_REQUEST));
		System.out.println(" MrsP Lock:   " + MrsP_LOCK + "   MrsP UNLOCK:   " + MrsP_UNLOCK + "   MIG:   " + MrsP_PREEMPTION_AND_MIGRATION);
		System.out.println(" CX1:    " + FULL_CONTEXT_SWTICH1 + "   CX2:   " + FULL_CONTEXT_SWTICH2);
	}

	public static void printResponseTime(long[][] Ris, ArrayList<ArrayList<SporadicTask>> tasks) {
		int task_id = 1;
		for (int i = 0; i < Ris.length; i++) {
			for (int j = 0; j < Ris[i].length; j++) {
				System.out.println("T" + task_id + " RT: " + Ris[i][j] + ", D: " + tasks.get(i).get(j).deadline + ", S = " + tasks.get(i).get(j).spin
						+ ", L = " + tasks.get(i).get(j).local + ", I = " + tasks.get(i).get(j).interference + ", WCET = " + tasks.get(i).get(j).WCET
						+ ", Resource: " + tasks.get(i).get(j).pure_resource_execution_time + ", B = " + tasks.get(i).get(j).indirectspin
						+ ", implementation_overheads: " + tasks.get(i).get(j).implementation_overheads);
				task_id++;
			}
			System.out.println();
		}
	}

}
