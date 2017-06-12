package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysisWithImplementationOverheads.IAFIFONP;
import analysisWithImplementationOverheads.IAFIFOP;
import analysisWithImplementationOverheads.IANewMrsPRTAWithMCNP;
import analysisWithImplementationOverheads.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.IOAResultReader;
import generatorTools.SystemGeneratorNoAllicationDM;

public class StaticTest3cslen {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws Exception {
		StaticTest3cslen test = new StaticTest3cslen();

		final CountDownLatch workloadcd = new CountDownLatch(300);
		for (int i = 1; i < 301; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingCriticalSectionLength(cslen);
					workloadcd.countDown();
				}
			}).start();
		}
		workloadcd.await();
		IOAResultReader.schedreader("minT: " + MIN_PERIOD + "  maxT: " + MAX_PERIOD, true);

	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		SystemGeneratorNoAllicationDM generator = new SystemGeneratorNoAllicationDM(MIN_PERIOD, MAX_PERIOD,
				0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION, true,
				null, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
				cs_len);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = mrsp.newRTATest(tasks, resources, true, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			Ris = fnp.NewRTATest(tasks, resources, true, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.newRTATest(tasks, resources, true, false, IOAAnalysisUtils.extendCalForStatic);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result = (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

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
