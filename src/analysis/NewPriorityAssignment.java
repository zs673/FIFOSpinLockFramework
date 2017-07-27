package analysis;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class NewPriorityAssignment {

	CombinedAnalysis analysis = new CombinedAnalysis();

	public long[][] getResponseTime(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isprint) {
		if (tasks == null)
			return null;

		long[][] response_time = new long[tasks.size()][];
		for (int i = 0; i < response_time.length; i++) {
			response_time[i] = new long[tasks.get(i).size()];
		}

		long npsection = 0;
		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.protocol == 3 && npsection < resource.csl)
				npsection = resources.get(i).csl;
		}
		
		analysis.getResponseTimeByStaticPriority(tasks, resources, false, false, utils.AnalysisUtils.extendCalForStatic, true, true);
		return null;

	}

}
