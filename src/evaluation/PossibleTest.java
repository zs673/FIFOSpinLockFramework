package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import GeneticAlgorithmFramework.PreGASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class PossibleTest {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final double RSF = 0.3;

	public static void main(String[] args) throws InterruptedException {
		PossibleTest possible = new PossibleTest();
		CountDownLatch down = new CountDownLatch(3);
		// for (int i = 1; i < 7; i++) {
		// final int cslen = i;
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// possible.test(cslen, 3);
		// down.countDown();
		// }
		// }).start();
		// }

		for (int i = 31; i < 42; i += 5) {
			final int access = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					possible.test(6, access);
					down.countDown();
				}
			}).start();
		}

		down.await();
	}

	public void test(int cslen, int NoA) {
		int possible = 0, impossible = 0;
		final CS_LENGTH_RANGE cs_range;
		switch (cslen) {
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

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
					TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, cs_range, RESOURCES_RANGE.PARTITIONS, NoA, false);

			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			PreGASolver pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 1, 1, false);

			int preres = pre.initialCheck(true, false);
			while (preres == 1) {
				tasksToAlloc = generator.generateTasks();
				resources = generator.generateResources();
				generator.generateResourceUsage(tasksToAlloc, resources);

				pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 1, 1, false);
				preres = pre.initialCheck(true, false);
			}

			if (preres == 0)
				possible++;
			if (preres == -1)
				impossible++;
			System.out.println("cslen : " + cslen + " access: " + NoA + " count: " + i);
		}
		System.out.println("cslen : " + cslen + " access: " + NoA + "   possible: " + possible + "  impossible: " + impossible);
		String result = "cslen : " + cslen + " access: " + NoA + "   possible: " + possible + "  impossible: " + impossible;
		writeSystem("cslen" + cslen + "access" + NoA, result);
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
