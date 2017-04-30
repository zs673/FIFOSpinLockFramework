package basicAnalysis;

import java.util.ArrayList;

import entity.SporadicTask;

public class Utils {

	public long[][] initResponseTime(ArrayList<ArrayList<SporadicTask>> tasks) {
		long[][] response_times = new long[tasks.size()][];

		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<SporadicTask> task_on_a_partition = tasks.get(i);
			task_on_a_partition.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

			long[] Ri = new long[task_on_a_partition.size()];

			for (int j = 0; j < task_on_a_partition.size(); j++) {
				SporadicTask t = task_on_a_partition.get(j);
				Ri[j] = t.Ri = t.WCET + t.pure_resource_execution_time;
				t.interference = t.local = t.spin = 0;
			}
			response_times[i] = Ri;
		}
		return response_times;
	}

	public void printResponseTime(long[][] Ris, ArrayList<ArrayList<SporadicTask>> tasks) {
		int task_id = 1;
		for (int i = 0; i < Ris.length; i++) {
			for (int j = 0; j < Ris[i].length; j++) {
				System.out.println("T" + task_id + " RT: " + Ris[i][j] + ", D: " + tasks.get(i).get(j).deadline
						+ ", S = " + tasks.get(i).get(j).spin + ", L = " + tasks.get(i).get(j).local + ", I = "
						+ tasks.get(i).get(j).interference + ", WCET = " + tasks.get(i).get(j).WCET + ", Resource: "
						+ tasks.get(i).get(j).pure_resource_execution_time );

				task_id++;
			}
			System.out.println();
		}
	}

	public void cloneList(long[][] oldList, long[][] newList) {
		for (int i = 0; i < oldList.length; i++) {
			for (int j = 0; j < oldList[i].length; j++) {
				newList[i][j] = oldList[i][j];
			}
		}
	}

	public static boolean isArrayContain(int[] array, int value) {

		for (int i = 0; i < array.length; i++) {
			if (array[i] == value)
				return true;
		}
		return false;
	}

}
