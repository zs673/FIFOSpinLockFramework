package evaluationForSection5;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import GeneticAlgorithmFramework.GASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class TestGAParameter {

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;

	public static int GENERATIONS = 100;
	public static int POPULATION = 100;

	public static int ALLOCATION_POLICY = 1;
	public static int PRIORITY_RULE = 1;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
	final double RSF = 0.3;

	class Counter {
		int result1 = 0;
		int result2 = 0;
		int result3 = 0;
		int result4 = 0;
		int result5 = 0;

		public synchronized void incResult1() {
			result1++;
		}

		public synchronized void incResult2() {
			result2++;
		}

		public synchronized void incResult3() {
			result3++;
		}

		public synchronized void incResult4() {
			result4++;
		}

		public synchronized void incResult5() {
			result5++;
		}

		public synchronized void initResults() {
			result5 = 0;
			result4 = 0;
			result3 = 0;
			result1 = 0;
			result2 = 0;
		}

	}

	public static void main(String[] args) throws InterruptedException {
		TestGAParameter test = new TestGAParameter();

		final CountDownLatch downLatch = new CountDownLatch(1);
		for (int i = 6; i < 7; i++) {
			final int count = i;
			new Thread(new Runnable() {

				@Override
				public void run() {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingCriticalSectionLength(count, counter);
					downLatch.countDown();
				}
			}).start();
		}
		downLatch.await();

		ResultReader.schedreader();
	}

	public void parallelExperimentIncreasingCriticalSectionLength(int cslen, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
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
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, cs_range, RESOURCES_RANGE.PARTITIONS,
							NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 0.4, 0.01,
							3, 5, 5, true);

					solver.crossoverRate = 0.4;
					if (solver.checkSchedulability(true) == 1) {
						counter.incResult1();
					}

					solver.crossoverRate = 0.5;
					if (solver.checkSchedulability(true) == 1) {
						counter.incResult2();

					}

					solver.crossoverRate = 0.6;
					if (solver.checkSchedulability(true) == 1) {
						counter.incResult3();

					}

					solver.crossoverRate = 0.7;
					if (solver.checkSchedulability(true) == 1) {
						counter.incResult4();

					}

					solver.crossoverRate = 0.8;
					if (solver.checkSchedulability(true) == 1) {
						counter.incResult5();

					}

					System.out.println(Thread.currentThread().getName() + " F");
					downLatch.countDown();
				}
			});
			worker.setName("2 " + cslen + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.result1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result2 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result3 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result4 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result5 / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 2 2 " + cslen, result);
		System.out.println(result);
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
