package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import analysis.CombinedAnalysis;
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
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 10000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws Exception {
		TestRiDiandDMvsOPA test = new TestRiDiandDMvsOPA();

		final CountDownLatch work = new CountDownLatch(6);
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

		// for (int i = 6; i >0; i--) {
		// test.experimentIncreasingCriticalSectionLength(i);
		// }

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
		CombinedAnalysis combined = new CombinedAnalysis();

		String result = "";
		int RiDM = 0;
		int DiDM = 0;
		int OPA = 0;
		int wecan = 0;
		int wecannot = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = generator.allocateTasks(tasksToAlloc, resources, 0);

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = new Random().nextInt(65535) % 3 + 1;
			}

			boolean b1 = false, b2 = false, b3 = false;
			Ris = combined.getResponseTimeByDM(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, true);
			if (isSystemSchedulable(tasks, Ris)) {
				RiDM++;
				b1 = true;
			}

			Ris = combined.getResponseTimeByDM(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, false);
			if (isSystemSchedulable(tasks, Ris)) {
				DiDM++;
				b2 = true;
			}

			if (combined.checkSchedulabilityByOPA(tasks, resources, false)) {
				OPA++;
				b3 = true;
			}

			if (!b1 && b3)
				wecan++;

			if (b1 && !b3)
				wecannot++;

			if (b2 && !b3) {
				System.err.println("found!");

				Ris = combined.getResponseTimeByDM(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, false);
				combined.checkSchedulabilityByOPA(tasks, resources, true);

				Ris = combined.getResponseTimeByDM(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, false);
				Ris = combined.getResponseTimeByDM(tasks, resources, true, false, AnalysisUtils.extendCalForStatic, false);
				combined.checkSchedulabilityByOPA(tasks, resources, true);

				System.exit(-1);
			}

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result = "Ri with DM: " + (double) RiDM / (double) TOTAL_NUMBER_OF_SYSTEMS + "    Di with DM: "
				+ (double) DiDM / (double) TOTAL_NUMBER_OF_SYSTEMS + "    Di with OPA: "
				+ (double) OPA / (double) TOTAL_NUMBER_OF_SYSTEMS + "    OPA can but Ri with DM cannot: " + wecan
				+ "    Ri with DM can but OPA cannot: " + wecannot + "\n";

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
