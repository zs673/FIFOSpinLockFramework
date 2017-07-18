package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysisWithRiOnly.IACombinedProtocol;
import audsleyAlgorithm.AudsleyOptimalPriorityAssignment;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.IOAResultReader;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class TestRiDiandDMvsOPA {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws Exception {
		TestRiDiandDMvsOPA test = new TestRiDiandDMvsOPA();

		final CountDownLatch work = new CountDownLatch(300);
		for (int i = 1; i < 7; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingCriticalSectionLength(cslen);
					work.countDown();
				}
			}).start();
		}
		work.await();
		IOAResultReader.schedreader("minT: " + MIN_PERIOD + "  maxT: " + MAX_PERIOD, true);

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
		
		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS,
				TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, true, cs_range, RESOURCES_RANGE.PARTITIONS,
				RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		long[][] Ris;
		AudsleyOptimalPriorityAssignment opa = new AudsleyOptimalPriorityAssignment();
		IACombinedProtocol ri = new IACombinedProtocol();
		analysisWithDiorRi.IACombinedProtocol di = new analysisWithDiorRi.IACombinedProtocol();

		String result = "";
		int RiDM = 0;
		int DiDM = 0;
		int OPA = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator
					.assignPrioritiesByDM(generator.allocateTasks(tasksToAlloc, resources, 0), resources);

			Ris = ri.getResponseTime(tasks, resources, true, false, AnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasks, Ris))
				RiDM++;

			Ris = di.getResponseTime(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, false);
			if (isSystemSchedulable(tasks, Ris))
				DiDM++;

			ArrayList<ArrayList<SporadicTask>> opa_result = opa.AssignedSchedulableTasks(tasks, resources);
			if (opa_result != null)
				OPA++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result = (double) RiDM / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) DiDM / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) OPA / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

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
