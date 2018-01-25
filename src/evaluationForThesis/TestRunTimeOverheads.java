package evaluationForThesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.FIFONP;
import analysis.FIFOP;
import analysis.MrsP;
import basicAnalysis.FIFONPbasic;
import basicAnalysis.FIFOPbasic;
import basicAnalysis.MrsPbasic;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import generatorTools.TestResultFileReader;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class TestRunTimeOverheads {
	public static int MAX_PERIOD = 5000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.4;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static boolean testSchedulability = true;
	public static boolean useRi = true;
	public static boolean btbHit = true;
	public static int PROTOCOLS = 3;

	public static void main(String[] args) throws Exception {
		TestRunTimeOverheads test = new TestRunTimeOverheads();

		final CountDownLatch cslencountdown = new CountDownLatch(6);
		for (int i = 1; i < 7; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingCriticalSectionLength(cslen);
					cslencountdown.countDown();
				}
			}).start();
		}

		cslencountdown.await();

		TestResultFileReader.schedreader(null, false);
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		final CS_LENGTH_RANGE cs_range;
		switch (cs_len) {
		case 1:
			cs_range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 2:
			cs_range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 3:
			cs_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 4:
			cs_range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 5:
			cs_range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		case 6:
			cs_range = CS_LENGTH_RANGE.RANDOM;
			break;
		default:
			cs_range = null;
			break;
		}

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, cs_range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		long[][] Ris;
		FIFONPbasic fnp = new FIFONPbasic();
		FIFOPbasic fp = new FIFOPbasic();
		MrsPbasic mrsp = new MrsPbasic();

		FIFONP fnpIO = new FIFONP();
		FIFOP fpIO = new FIFOP();
		MrsP mrspIO = new MrsP();

		String result = "";
		int sfnpIO = 0;
		int sfpIO = 0;
		int smrspIONP = 0;
		int smrspIO = 0;
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

			Ris = fnpIO.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnpIO++;

			Ris = fpIO.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasks, Ris))
				sfpIO++;

			Ris = mrspIO.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, false);
			if (isSystemSchedulable(tasks, Ris))
				smrspIONP++;

			Ris = mrspIO.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, testSchedulability, btbHit, useRi, true, false, false);
			if (isSystemSchedulable(tasks, Ris))
				smrspIO++;

			Ris = fnp.getResponseTimeByDM(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.getResponseTimeByDM(tasks, resources, false);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			Ris = mrsp.getResponseTimeByDM(tasks, resources, 0, false, false);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfnpIO / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) sfpIO / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) smrspIO / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrspIONP / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}

	public void writeSystem(String filename, String result) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("result/" + filename + ".txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}
}
