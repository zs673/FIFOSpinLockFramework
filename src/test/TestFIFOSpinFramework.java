package test;

import java.util.ArrayList;

import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import geneticAlgoritmSolver.GASolver;

public class TestFIFOSpinFramework {

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RSF = 0.3;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 4;
		int NUMBER_OF_SYSTEMS = 1000;

		int sfp = 0, sfnp = 0, smrsp = 0, combine = 0;
		long[][] Ris;

		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		for (int i = 0; i < NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.getResponseTime(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;
			Ris = fp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;
			Ris = fnp.NewMrsPRTATest(tasks, resources, true, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			GASolver finder = new GASolver(tasks, resources);
			int result = finder.findSchedulableProtocols(true);
			if (result >= 1)
				combine++;
			System.out.println(i);
		}
		System.out.println(NUMBER_OF_SYSTEMS + "system:   fifonp: " + sfnp + "   fifop: " + sfp + "   mrsp: " + smrsp + "   combine: " + combine);
	}

	public static boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}
}
