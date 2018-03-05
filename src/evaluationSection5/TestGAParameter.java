package evaluationSection5;

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
	public static int TOTAL_NUMBER_OF_SYSTEMS = 10000;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	final double RSF = 0.3;

	public static int ALLOCATION_POLICY = 1;
	public static int PRIORITY_RULE = 1;
	public static int GENERATIONS = 50;
	public static int POPULATION = 100;
	public static boolean useGA = true;

	class Counter {
		int msrp = 0;
		int pwlp = 0;
		int mrsp = 0;

		int result1 = 0;
		int result1_1 = 0;
		int result2 = 0;
		int result2_1 = 0;
		int result3 = 0;
		int result3_1 = 0;
		int result4 = 0;
		int result4_1 = 0;
		int result5 = 0;
		int result5_1 = 0;
		int result6 = 0;
		int result6_1 = 0;

		public synchronized void incMSRP() {
			msrp++;
		}

		public synchronized void incPWLP() {
			pwlp++;
		}

		public synchronized void incMrsP() {
			mrsp++;
		}

		public synchronized void incResult1() {
			result1++;
		}

		public synchronized void incResult1_1() {
			result1_1++;
		}

		public synchronized void incResult2() {
			result2++;
		}

		public synchronized void incResult2_1() {
			result2_1++;
		}

		public synchronized void incResult3() {
			result3++;
		}

		public synchronized void incResult3_1() {
			result3_1++;
		}

		public synchronized void incResult4() {
			result4++;
		}

		public synchronized void incResult4_1() {
			result4_1++;
		}

		public synchronized void incResult5() {
			result5++;
		}

		public synchronized void incResult5_1() {
			result5_1++;
		}

		public synchronized void incResult6() {
			result6++;
		}

		public synchronized void incResult6_1() {
			result6_1++;
		}

		public synchronized void initResults() {
			msrp = 0;
			pwlp = 0;
			mrsp = 0;
			result1 = 0;
			result1_1 = 0;
			result2 = 0;
			result2_1 = 0;
			result3 = 0;
			result3_1 = 0;
			result4 = 0;
			result4_1 = 0;
			result5 = 0;
			result5_1 = 0;
			result6 = 0;
			result6_1 = 0;
		}

	}

	public static void main(String[] args) throws InterruptedException {
		TestGAParameter test = new TestGAParameter();

		Counter counter = test.new Counter();
		counter.initResults();
		test.parallelExperimentCrossoverRate(counter);

		ResultReader.schedreader();
	}

	public void parallelExperimentCrossoverRate(Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					GASolver solver1 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 1, 0.4,
							0.01, 3, 5, 5, true);
					GASolver solver2 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 1, 0.5,
							0.01, 3, 5, 5, true);
					GASolver solver3 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 1, 0.6,
							0.01, 3, 5, 5, true);
					GASolver solver4 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 2, 0.4,
							0.01, 3, 5, 5, true);
					GASolver solver5 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 2, 0.5,
							0.01, 3, 5, 5, true);
					GASolver solver6 = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 2, 0.6,
							0.01, 3, 5, 5, true);

					if (solver1.checkSchedulability(useGA) == 1) {
						counter.incResult1();
						if (solver1.bestProtocol == 0) {
							counter.incResult1_1();
						}
					}

					if (solver2.checkSchedulability(useGA) == 1) {
						counter.incResult2();
						if (solver2.bestProtocol == 0) {
							counter.incResult2_1();
						}
					}

					if (solver3.checkSchedulability(useGA) == 1) {
						counter.incResult3();
						if (solver3.bestProtocol == 0) {
							counter.incResult3_1();
						}
					}

					if (solver4.checkSchedulability(useGA) == 1) {
						counter.incResult4();
						if (solver4.bestProtocol == 0) {
							counter.incResult4_1();
						}
					}

					if (solver5.checkSchedulability(useGA) == 1) {
						counter.incResult5();
						if (solver5.bestProtocol == 0) {
							counter.incResult5_1();
						}
					}

					if (solver6.checkSchedulability(useGA) == 1) {
						counter.incResult6();
						if (solver6.bestProtocol == 0) {
							counter.incResult6_1();
						}
					}

					System.out.println(Thread.currentThread().getName() + " F");
					downLatch.countDown();
				}
			});
			worker.setName("Thead: " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.result1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result1_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result2 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result2_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result3 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result3_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result4 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result4_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result5 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result5_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.result6 / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.result6_1 / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("crossover Rate", result);
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
