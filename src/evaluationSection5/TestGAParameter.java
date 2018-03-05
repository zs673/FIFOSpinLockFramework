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
	public static int GENERATIONS = 100;
	public static int POPULATION = 100;
	public static boolean useGA = true;
	public static boolean lazy = true;

	class Counter {
		int[] results = new int[10];
		int[] results1 = new int[10];

		public synchronized void incResult(int index) {
			results[index] = results[index] + 1;
		}

		public synchronized void incResult1(int index) {
			results1[index] = results1[index] + 1;
		}

		public synchronized void initResults() {
			results = new int[10];
			results1 = new int[10];
		}

	}

	public static void main(String[] args) throws InterruptedException {
		TestGAParameter test = new TestGAParameter();

		final CountDownLatch downLatch = new CountDownLatch(2);
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentCrossoverRate(counter);
				downLatch.countDown();
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentPopulation(counter);
				downLatch.countDown();
			}
		});

		t1.start();
		t2.start();
		downLatch.await();

		ResultReader.schedreader();
	}

	public void parallelExperimentCrossoverRate(Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					double corssover = 0.3;

					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					final CountDownLatch down = new CountDownLatch(10);

					for (int i = 0; i < 10; i++) {
						if (i % 2 == 0)
							corssover += 0.1;

						final int point = i % 2 + 1;
						final double cross = corssover;
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

								GASolver solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, point,
										cross, 0.01, 2, 5, true);
								if (solver.checkSchedulability(useGA, lazy) == 1) {
									counter.incResult(index);
									if (solver.bestProtocol == 0) {
										counter.incResult1(index);
									}
								}

								down.countDown();
							}
						});
						t.start();

					}

					try {
						down.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
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

		String result = Arrays.toString(counter.results) + "\n" + Arrays.toString(counter.results1);

		writeSystem("Crossover Rate and Crossover Point", result);
		System.out.println(result);
	}

	public void parallelExperimentPopulation(Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					int population = 0;

					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					final CountDownLatch down = new CountDownLatch(10);

					for (int i = 0; i < 10; i++) {
						if (i % 2 == 0)
							population += 100;
						final int pop = population;
						final int generation = i % 2 == 0 ? 100 : 200;
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

								GASolver solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, pop, generation, 2, 2, 0.8, 0.01, 2, 5,
										true);
								if (solver.checkSchedulability(useGA, lazy) == 1) {
									counter.incResult(index);
									if (solver.bestProtocol == 0) {
										counter.incResult1(index);
									}
								}

								down.countDown();
							}
						});
						t.start();

					}

					try {
						down.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
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

		String result = Arrays.toString(counter.results) + "\n" + Arrays.toString(counter.results1);

		writeSystem("Population and Generation", result);
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
