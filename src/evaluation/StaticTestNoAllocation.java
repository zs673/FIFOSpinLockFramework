package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class StaticTestNoAllocation {
	public static int MAX_PERIOD = 5000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static boolean testSchedulability = false;
	public static int PROTOCOLS = 3;

	public static void main(String[] args) throws Exception {
		StaticTestNoAllocation test = new StaticTestNoAllocation();

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

		final CountDownLatch workloadcountdown = new CountDownLatch(9);
		for (int i = 1; i < 10; i++) {
			final int workload = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingWorkLoad(workload);
					workloadcountdown.countDown();
				}
			}).start();
		}

		final CountDownLatch accesscountdown = new CountDownLatch(20);
		for (int i = 1; i < 21; i++) {
			final int access = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingContention(access);
					accesscountdown.countDown();
				}
			}).start();
		}

		final CountDownLatch processorscountdown = new CountDownLatch(16);
		for (int i = 1; i < 17; i++) {
			final int processors = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.experimentIncreasingParallel(processors, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);
					processorscountdown.countDown();
				}
			}).start();
		}

		cslencountdown.await();
		workloadcountdown.await();
		accesscountdown.await();
		processorscountdown.await();

		IOAResultReader.schedreader(null, false);
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

		SystemGeneratorNoAllicationDM generator = new SystemGeneratorNoAllicationDM(MIN_PERIOD, MAX_PERIOD,
				0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION, true,
				cs_range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		long[][] results = new long[PROTOCOLS][NUMBER_OF_TASKS_ON_EACH_PARTITION];

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = fnp.NewRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[0]);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[1]);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			Ris = mrsp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[2]);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS;
		result += "    fifonp: " + Arrays.toString(results[0]);
		result += "    fifop: " + Arrays.toString(results[1]);
		result += "    mrsp: " + Arrays.toString(results[2]) + "\n\n";

		writeSystem(("ioa " + 2 + " " + 1 + " " + cs_len), result);
	}

	public void experimentIncreasingWorkLoad(int NoT) {
		SystemGeneratorNoAllicationDM generator = new SystemGeneratorNoAllicationDM(MIN_PERIOD, MAX_PERIOD, 0.1 * NoT, TOTAL_PARTITIONS, NoT,
				true, range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		long[][] results = new long[PROTOCOLS][NoT];

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = fnp.NewRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[0]);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[1]);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			Ris = mrsp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[2]);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			System.out.println(1 + " " + 1 + " " + NoT + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS;
		result += "    fifonp: " + Arrays.toString(results[0]);
		result += "    fifop: " + Arrays.toString(results[1]);
		result += "    mrsp: " + Arrays.toString(results[2]) + "\n\n";

		writeSystem(("ioa " + 1 + " " + 1 + " " + NoT), result);
	}

	public void experimentIncreasingParallel(int NoP, int NoA) {
		SystemGeneratorNoAllicationDM generator = new SystemGeneratorNoAllicationDM(MIN_PERIOD, MAX_PERIOD,
				0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, NoP, NUMBER_OF_TASKS_ON_EACH_PARTITION, true, range,
				RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NoA);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		long[][] results = new long[PROTOCOLS][NUMBER_OF_TASKS_ON_EACH_PARTITION];

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = fnp.NewRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[0]);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[1]);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			Ris = mrsp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[2]);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			System.out.println(4 + " " + NoA + " " + NoP + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS;
		result += "    fifonp: " + Arrays.toString(results[0]);
		result += "    fifop: " + Arrays.toString(results[1]);
		result += "    mrsp: " + Arrays.toString(results[2]) + "\n\n";

		writeSystem(("ioa " + 4 + " " + NoA + " " + NoP), result);
	}

	public void experimentIncreasingContention(int NoA) {
		SystemGeneratorNoAllicationDM generator = new SystemGeneratorNoAllicationDM(MIN_PERIOD, MAX_PERIOD,
				0.1 * NUMBER_OF_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION, true,
				range, RESOURCES_RANGE.PARTITIONS, RESOURCE_SHARING_FACTOR, NoA);

		long[][] Ris;
		IAFIFONP fnp = new IAFIFONP();
		IAFIFOP fp = new IAFIFOP();
		IANewMrsPRTAWithMCNP mrsp = new IANewMrsPRTAWithMCNP();

		String result = "";
		int sfnp = 0;
		int sfp = 0;
		int smrsp = 0;

		long[][] results = new long[PROTOCOLS][NUMBER_OF_TASKS_ON_EACH_PARTITION];

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);

			Ris = fnp.NewRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[0]);
			if (isSystemSchedulable(tasks, Ris))
				sfnp++;

			Ris = fp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[1]);
			if (isSystemSchedulable(tasks, Ris))
				sfp++;

			Ris = mrsp.newRTATest(tasks, resources, testSchedulability, false, IOAAnalysisUtils.extendCalForStatic);
			getUnschedulableTasks(tasks, Ris, results[2]);
			if (isSystemSchedulable(tasks, Ris))
				smrsp++;

			System.out.println(3 + " " + 1 + " " + NoA + " times: " + i);
		}

		result += (double) sfnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) sfp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) smrsp / (double) TOTAL_NUMBER_OF_SYSTEMS;
		result += "    fifonp: " + Arrays.toString(results[0]);
		result += "    fifop: " + Arrays.toString(results[1]);
		result += "    mrsp: " + Arrays.toString(results[2]) + "\n\n";

		writeSystem(("ioa " + 3 + " " + 1 + " " + NoA), result);
	}

	public ArrayList<SporadicTask> getUnschedulableTasks(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris,
			long[] results) {
		ArrayList<SporadicTask> unschedulabletasks = new ArrayList<>();

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				if (task.deadline < Ris[i][j]) {
					unschedulabletasks.add(task);
					results[j] = results[j] + 1;
				}
			}
		}
		return unschedulabletasks;
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
