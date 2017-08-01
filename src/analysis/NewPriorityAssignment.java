package analysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import utils.AnalysisUtils;

public class NewPriorityAssignment {

	CombinedAnalysis analysis = new CombinedAnalysis();

	public long[][] getResponseTimeNewOPA(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isprint) {
		if (tasks == null)
			return null;

		long[][] deadline = new long[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			deadline[i] = new long[tasks.get(i).size()];
			for (int j = 0; j < tasks.get(i).size(); j++) {
				deadline[i][j] = tasks.get(i).get(j).deadline;
			}
		}

		long npsection = 0;
		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol == 3 && npsection < resource.csl)
				npsection = resources.get(i).csl;
		}

		analysis.getResponseTimeByStaticPriority(tasks, resources, AnalysisUtils.extendCalForStatic, false, true,
				true, true, false);
		return null;

	}

}
