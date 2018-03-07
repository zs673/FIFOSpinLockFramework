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
import GeneticAlgorithmFramework.PreGASolver;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class TestCrossover {

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
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
	public static boolean record = true;

	class Counter {
		int[] results = new int[3];
		int[] results1 = new int[3];
		ArrayList<ArrayList<Double>> recorder1 = new ArrayList<>();
		ArrayList<ArrayList<Double>> recorder2 = new ArrayList<>();
		ArrayList<ArrayList<Double>> recorder3 = new ArrayList<>();

		public synchronized void incResult(int index) {
			results[index] = results[index] + 1;
		}

		public synchronized void incResult1(int index) {
			results1[index] = results1[index] + 1;
		}

		public synchronized void record1(ArrayList<Double> rec) {
			recorder1.add(rec);
		}

		public synchronized void record2(ArrayList<Double> rec) {
			recorder2.add(rec);
		}

		public synchronized void record3(ArrayList<Double> rec) {
			recorder3.add(rec);
		}

		public synchronized void initResults() {
			results = new int[3];
			results1 = new int[3];
			recorder1 = new ArrayList<>();
			recorder2 = new ArrayList<>();
			recorder3 = new ArrayList<>();
		}

	}

	public static void main(String[] args) throws InterruptedException {
		System.out.println("program start");
		long time = System.currentTimeMillis();

		TestCrossover test = new TestCrossover();
		test.parallelExperimentCrossoverRate();
		time = System.currentTimeMillis() - time;

		ResultReader.schedreader();
		System.out.println("The program takes " + time / 1000 / 60 + " minutes to finish.");
		System.out.println("program finish");
	}

	public void parallelExperimentCrossoverRate() {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			final int fatherindex = i;
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					Counter counter = new Counter();
					counter.initResults();

					System.out.println(Thread.currentThread().getName() + " Begin");
					double corssover = 0.2;

					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc1 = generator.generateTasks();
					ArrayList<Resource> resources1 = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc1, resources1);

					int preres = -1;

					PreGASolver pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
					preres = pre.initialCheck(true);
					while (preres != 0) {
						tasksToAlloc1 = generator.generateTasks();
						resources1 = generator.generateResources();
						generator.generateResourceUsage(tasksToAlloc1, resources1);

						pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
						preres = pre.initialCheck(true);
					}

					final ArrayList<SporadicTask> tasksToAlloc = tasksToAlloc1;
					final ArrayList<Resource> resources = resources1;

					final CountDownLatch down = new CountDownLatch(3);
					for (int i = 0; i < 3; i++) {
						if (i % 2 == 0)
							corssover += 0.2;

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

								GASolver solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 1, cross,
										0.01, 2, 5, record, true);
								solver.name = "Solver " + index + ".1";
								if (solver.checkSchedulability(useGA, lazy) == 1)
									counter.incResult(index);

								GASolver solver1 = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, cross,
										0.01, 2, 5, record, true);
								solver1.name = "Solver " + index + ".2";
								if (solver1.checkSchedulability(useGA, lazy) == 1)
									counter.incResult1(index);

								ArrayList<Double> recorder = new ArrayList<>();
								recorder.addAll(solver.resultRecorder);
								recorder.addAll(solver1.resultRecorder);

								if (index == 0) {
									counter.recorder1.add(solver.resultRecorder);
									counter.recorder1.add(solver1.resultRecorder);
								} else if (index == 1) {
									counter.recorder2.add(solver.resultRecorder);
									counter.recorder2.add(solver1.resultRecorder);
								} else {
									counter.recorder3.add(solver.resultRecorder);
									counter.recorder3.add(solver1.resultRecorder);
								}

								down.countDown();
							}
						});
						t.setName("Thead: " + fatherindex + "." + index);
						t.start();
					}

					try {
						down.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					System.out.println(Thread.currentThread().getName() + " Finish");

					String sched_count = "";
					for (int i = 0; i < counter.results.length; i++) {
						sched_count += " " + counter.results[i];
					}
					for (int i = 0; i < counter.results1.length; i++) {
						sched_count += " " + counter.results1[i];
					}
					sched_count += " ";

					sched_count += counter.recorder1.get(0).toString() + " ";
					sched_count += counter.recorder2.get(0).toString() + " ";
					sched_count += counter.recorder3.get(0).toString() + " ";
					sched_count += counter.recorder1.get(1).toString() + " ";
					sched_count += counter.recorder2.get(1).toString() + " ";
					sched_count += counter.recorder3.get(1).toString() + " ";

					sched_count = sched_count.replace("[", "").replace("]", "").replace(",", "");
					sched_count += "\n";

					writeSystem("1 2 " + (fatherindex + 1), sched_count);

					// String rec = "";
					//
					// for (int i = 0; i < counter.recorder1.size(); i++) {
					// rec += counter.recorder1.get(i).toString() + "\n";
					// }
					// rec += "\n";
					// for (int i = 0; i < counter.recorder2.size(); i++) {
					// rec += counter.recorder2.get(i).toString() + "\n";
					// }
					// rec += "\n";
					// for (int i = 0; i < counter.recorder3.size(); i++) {
					// rec += counter.recorder3.get(i).toString() + "\n";
					// }
					// result += rec;
					//
					// writeSystem(filename, result);

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
