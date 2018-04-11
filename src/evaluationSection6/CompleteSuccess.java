package evaluationSection6;

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

public class CompleteSuccess {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	final double RSF = 0.3;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int GENERATIONS = 500;
	public static int POPULATION = 500;
	public static int ALLOCATION_POLICY = 8;
	public static int PRIORITY_RULE = 2;

	public static boolean useGA = true;
	public static boolean lazy = true;
	public static boolean record = false;

	class Counter {
		int count = 0;
		int Dcombine = 0;
		int Dnew = 0;
		int newResourceControl = 0;
		int newallocation = 0;
		int newpriority = 0;

		public synchronized void incDcombine() {
			Dcombine++;
		}

		public synchronized void incDnew() {
			Dnew++;
		}

		public synchronized void incNewResourceControl() {
			newResourceControl++;
		}

		public synchronized void incNewAllocation() {
			newallocation++;
		}

		public synchronized void incNewPriority() {
			newpriority++;
		}

		public synchronized void incCount() {
			count++;
		}

		public synchronized void initResults() {
			Dcombine = 0;
			Dnew = 0;

			newResourceControl = 0;
			newallocation = 0;
			newpriority = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		CompleteSuccess test = new CompleteSuccess();
		
		final CountDownLatch downLatch = new CountDownLatch(5);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentIncreasingParallelism(4, counter);
				downLatch.countDown();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentIncreasingParallelism(6, counter);
				downLatch.countDown();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentIncreasingParallelism(8, counter);
				downLatch.countDown();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentIncreasingParallelism(10, counter);
				downLatch.countDown();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Counter counter = test.new Counter();
				counter.initResults();
				test.parallelExperimentIncreasingParallelism(12, counter);
				downLatch.countDown();
			}
		}).start();
		downLatch.await();

//		final CountDownLatch downLatch = new CountDownLatch(6);
//
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(1, counter);
//				downLatch.countDown();
//			}
//		}).start();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(2, counter);
//				downLatch.countDown();
//			}
//		}).start();
//
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(3, counter);
//				downLatch.countDown();
//			}
//		}).start();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(4, counter);
//				downLatch.countDown();
//			}
//		}).start();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(5, counter);
//				downLatch.countDown();
//			}
//		}).start();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Counter counter = test.new Counter();
//				counter.initResults();
//				test.parallelExperimentIncreasingCriticalSectionLength(6, counter);
//				downLatch.countDown();
//			}
//		}).start();
//
//		downLatch.await();

//		final CountDownLatch downLatch = new CountDownLatch(1);
//
//		int bigTest = Integer.parseInt(args[0]);
//		int smallTest = Integer.parseInt(args[1]);
//
//		switch (bigTest) {
//		case 2:
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					Counter counter = test.new Counter();
//					counter.initResults();
//					test.parallelExperimentIncreasingCriticalSectionLength(smallTest, counter);
//					downLatch.countDown();
//				}
//			}).start();
//			break;
//		case 3:
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					Counter counter = test.new Counter();
//					counter.initResults();
//					test.parallelExperimentIncreasingAccess(smallTest, counter);
//					downLatch.countDown();
//				}
//			}).start();
//			break;
//		case 4:
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					Counter counter = test.new Counter();
//					counter.initResults();
//					test.parallelExperimentIncreasingParallelism(smallTest, counter);
//					downLatch.countDown();
//				}
//			}).start();
//			break;
//		default:
//			break;
//		}
//
//		downLatch.await();

		System.out.println("FINISHED!!!");
	}

	public void parallelExperimentIncreasingCriticalSectionLength(int cslen, Counter counter) {
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

		CountDownLatch numbersdownLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			final int childindex = i;
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, cs_range, RESOURCES_RANGE.PARTITIONS,
							NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);
					PreGASolver pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);

					int preres = pre.initialCheck(true, false);
					while (preres == 1) {
						tasksToAlloc = generator.generateTasks();
						resources = generator.generateResources();
						generator.generateResourceUsage(tasksToAlloc, resources);

						pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);
						preres = pre.initialCheck(true, false);
					}

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					if (solver.checkSchedulability(useGA, lazy) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 0) {
							counter.incDnew();
						}
						if (solver.bestProtocol == 0)
							counter.incNewResourceControl();
						if (solver.bestAllocation > 4)
							counter.incNewAllocation();
						if (solver.bestPriority > 0)
							counter.incNewPriority();
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " Finish. count: " + counter.count);
					numbersdownLatch.countDown();
				}
			});
			worker.setName("2 " + cslen + " numbers: " + childindex);
			worker.run();
		}

		try {
			numbersdownLatch.await();
		} catch (InterruptedException e) {
		}

		String result = "cslen-" + cslen + " " + counter.Dcombine + " " + counter.Dnew + " " + counter.newResourceControl + " " + counter.newallocation + " "
				+ counter.newpriority;
		writeSystem("Success 2 2 " + cslen, result);

	}

	public void parallelExperimentIncreasingParallelism(int NoP, Counter counter) {
		CountDownLatch numbersdownLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			final int childindex = i;
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NoP * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range,
							RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);
					PreGASolver pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);

					int preres = pre.initialCheck(true, false);
					while (preres == 1) {
						tasksToAlloc = generator.generateTasks();
						resources = generator.generateResources();
						generator.generateResourceUsage(tasksToAlloc, resources);

						pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);
						preres = pre.initialCheck(true, false);
					}

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					if (solver.checkSchedulability(useGA, lazy) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 0) {
							counter.incDnew();
						}
						if (solver.bestProtocol == 0)
							counter.incNewResourceControl();
						if (solver.bestAllocation > 4)
							counter.incNewAllocation();
						if (solver.bestPriority > 0)
							counter.incNewPriority();
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " Finish. count: " + counter.count);
					numbersdownLatch.countDown();
				}
			});
			worker.setName("4 " + NoP + " numbers: " + childindex);
			worker.run();
		}

		try {
			numbersdownLatch.await();
		} catch (InterruptedException e) {
		}

		String result = "processor-" + NoP + " " + counter.Dcombine + " " + counter.Dnew + " " + counter.newResourceControl + " " + counter.newallocation + " "
				+ counter.newpriority;
		writeSystem("Success 4 2 " + NoP, result);
	}

	public void parallelExperimentIncreasingAccess(int NoA, Counter counter) {
		CountDownLatch numbersdownLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			final int childindex = i;
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NoA, false);

					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);
					PreGASolver pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);

					int preres = pre.initialCheck(true, false);
					while (preres == 1) {
						tasksToAlloc = generator.generateTasks();
						resources = generator.generateResources();
						generator.generateResourceUsage(tasksToAlloc, resources);

						pre = new PreGASolver(tasksToAlloc, resources, generator, 3, 5, 1, false);
						preres = pre.initialCheck(true, false);
					}

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					if (solver.checkSchedulability(useGA, lazy) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 0) {
							counter.incDnew();
						}
						if (solver.bestProtocol == 0)
							counter.incNewResourceControl();
						if (solver.bestAllocation > 4)
							counter.incNewAllocation();
						if (solver.bestPriority > 0)
							counter.incNewPriority();
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " Finish. count: " + counter.count);
					numbersdownLatch.countDown();
				}
			});
			worker.setName("3 " + NoA + " numbers: " + childindex);
			worker.run();
		}

		try {
			numbersdownLatch.await();
		} catch (InterruptedException e) {
		}

		String result = "access-" + NoA + " " + counter.Dcombine + " " + counter.Dnew + " " + counter.newResourceControl + " " + counter.newallocation + " "
				+ counter.newpriority;
		writeSystem("Success 3 2 " + NoA, result);
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
