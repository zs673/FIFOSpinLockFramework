package evaluationGA;

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

public class TestToumament {

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
	public static boolean lazy = false;
	public static boolean record = true;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("program start");
		long time = System.currentTimeMillis();

		TestToumament test = new TestToumament();
		test.parallelExperimentCrossoverRate();
		time = System.currentTimeMillis() - time;

		ResultReader.read("resultTourmament");
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
					System.out.println(Thread.currentThread().getName() + " Begin");
					int[] toumament = new int[2];

					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc1 = generator.generateTasks();
					ArrayList<Resource> resources1 = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc1, resources1);

					int preres = -1;

					PreGASolver pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
					preres = pre.initialCheck(true,false);
					while (preres != 0) {
						tasksToAlloc1 = generator.generateTasks();
						resources1 = generator.generateResources();
						generator.generateResourceUsage(tasksToAlloc1, resources1);

						pre = new PreGASolver(tasksToAlloc1, resources1, generator, 3, 1, 1, false);
						preres = pre.initialCheck(true,false);
					}

					final ArrayList<SporadicTask> tasksToAlloc = tasksToAlloc1;
					final ArrayList<Resource> resources = resources1;

					final CountDownLatch down = new CountDownLatch(2);
					for (int i = 0; i < 2; i++) {
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

								GASolver solver = null;

								if (index == 0) {
									solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8, 0.01, 2,
											5, record, true);
								} else {
									solver = new GASolver(tasks, res, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8, 0.01, 2,
											2, record, true);
								}

								solver.name = "Thread: " + fatherindex + "." + index;
								if (solver.checkSchedulability(useGA, lazy) == 1) {
									toumament[index] = toumament[index] + 1;
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

					String sched_count = "" + toumament[0] + " " + toumament[1] + "\n";
					writeSystem("1 2 " + (fatherindex), sched_count);
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
			writer = new PrintWriter(new FileWriter(new File("resultTourmament/" + filename + ".txt"), false));
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
