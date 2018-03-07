package evaluationSection5;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import GeneticAlgorithmFramework.GASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class TestGeneration {

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 3000;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	final double RSF = 0.3;

	public static int ALLOCATION_POLICY = 1;
	public static int PRIORITY_RULE = 1;
	public static int GENERATIONS = 100;
	public static int POPULATION = 100;
	public static boolean useGA = true;
	public static boolean lazy = true;

	class Counter {
		int[] results = new int[3];
		int[] results1 = new int[3];

		public synchronized void incResult(int index) {
			results[index] = results[index] + 1;
		}

		public synchronized void incResult1(int index) {
			results1[index] = results1[index] + 1;
		}

		public synchronized void initResults() {
			results = new int[3];
			results1 = new int[3];
		}

	}

	public static void main(String[] args) throws InterruptedException {
		TestGeneration test = new TestGeneration();

		Counter counter = test.new Counter();
		counter.initResults();
		test.parallelExperimentPopulation(counter);

		ResultReader.schedreader();
	}

	public void parallelExperimentPopulation(Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					int population = -100;

					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					final CountDownLatch down = new CountDownLatch(3);

					for (int i = 0; i < 3; i++) {
						population += 200;
						final int pop = population;
						final int index = i;

						Thread t = new Thread(new Runnable() {

							@Override
							public void run() {
								ArrayList<SporadicTask> tasks = new ArrayList<>();
								ArrayList<Resource> res = new ArrayList<>();

								for (int i = 0; i < tasksToAlloc.size(); i++) {
									SporadicTask task = new SporadicTask(tasksToAlloc.get(i).priority, tasksToAlloc.get(i).period, tasksToAlloc.get(i).WCET,
											tasksToAlloc.get(i).partition, tasksToAlloc.get(i).id, tasksToAlloc.get(i).util,
											tasksToAlloc.get(i).pure_resource_execution_time, tasksToAlloc.get(i).resource_required_index,
											tasksToAlloc.get(i).number_of_access_in_one_release, tasksToAlloc.get(i).hasResource);
									tasks.add(task);
								}
								for (int i = 0; i < resources.size(); i++) {
									Resource resource = new Resource(resources.get(i).id, resources.get(i).csl, resources.get(i).protocol,
											resources.get(i).isGlobal, resources.get(i).partitions, resources.get(i).requested_tasks, tasks);
									res.add(resource);
								}

								GASolver solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, pop, 100, 2, 2, 0.8, 0.01, 2, 5, true,
										true);
								if (solver.checkSchedulability(useGA, lazy) == 1)
									counter.incResult(index);

								GASolver solver1 = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, pop, 300, 2, 2, 0.8, 0.01, 2, 5, true,
										true);
								if (solver1.checkSchedulability(useGA, lazy) == 1)
									counter.incResult1(index);

								down.countDown();
							}
						});
						t.start();

					}

					try {
						down.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
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

		double[] result_double = new double[counter.results.length];
		double[] result_double1 = new double[counter.results1.length];
		for (int i = 0; i < counter.results.length; i++) {
			result_double[i] = (double) counter.results[i] / (double) TOTAL_NUMBER_OF_SYSTEMS;
			result_double1[i] = (double) counter.results1[i] / (double) TOTAL_NUMBER_OF_SYSTEMS;
		}
		String result = Arrays.toString(result_double) + "\n" + Arrays.toString(result_double1);

		writeSystem("Generation", result);
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
