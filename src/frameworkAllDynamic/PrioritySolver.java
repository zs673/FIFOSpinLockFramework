package frameworkAllDynamic;

import java.util.ArrayList;

import analysis.IAFIFONP;
import analysis.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;

public class PrioritySolver {

	public boolean FIFONPNewPriority(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		boolean finish = false;
		IAFIFONP fnp = new IAFIFONP();

		// by default is deadline monotonic
		long[][] Ris = fnp.NewMrsPRTATest(tasks, resources, false, false, IOAAnalysisUtils.extendCalForStatic);
		ArrayList<SporadicTask> unschedulableT = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j]) {
					unschedulableT.add(tasks.get(i).get(j));
				}
			}
		}

		while (!finish) {

		}

		return false;
	}

}
