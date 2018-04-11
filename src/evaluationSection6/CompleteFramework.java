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
import analysis.FIFONP;
import analysis.FIFOP;
import analysis.MrsP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class CompleteFramework {
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	final double RSF = 0.3;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;

	public static int GENERATIONS = 500;
	public static int POPULATION = 500;
	public static int ALLOCATION_POLICY = 8;
	public static int PRIORITY_RULE = 2;

	public static boolean useGA = true;
	public static boolean lazy = true;
	public static boolean record = false;

	class Counter {
		int fnp = 0;
		int fp = 0;
		int mrsp = 0;

		int Dtypical = 0;
		int Dnew = 0;
		int newResourceControl = 0;
		int newallocation = 0;
		int newpriority = 0;

		int count = 0;

		public synchronized void incfnp() {
			fnp++;
		}

		public synchronized void incfp() {
			fp++;
		}

		public synchronized void incmrsp() {
			mrsp++;
		}

		public synchronized void incTypical() {
			Dtypical++;
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
			mrsp = 0;
			fp = 0;
			fnp = 0;

			Dtypical = 0;
			Dnew = 0;
			newResourceControl = 0;
			newallocation = 0;
			newpriority = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {

		CompleteFramework test = new CompleteFramework();

		int bigTest = Integer.parseInt(args[0]);
		if (bigTest == 1) {
			final CountDownLatch tasksdownLatch = new CountDownLatch(5);
			for (int i = 4; i < 10; i++) {
				if (i == 5)
					continue;
				final int count = i;
				new Thread(new Runnable() {

					@Override
					public void run() {
						Counter counter = test.new Counter();
						counter.initResults();
						test.parallelExperimentIncreasingWorkload(count, counter);
						tasksdownLatch.countDown();
					}
				}).start();
			}
			tasksdownLatch.await();
		}

		if (bigTest == 2) {
			final CountDownLatch cslendownLatch = new CountDownLatch(6);
			for (int i = 1; i < 7; i++) {
				final int count = i;
				new Thread(new Runnable() {
					@Override
					public void run() {
						Counter counter = test.new Counter();
						counter.initResults();
						test.parallelExperimentIncreasingCriticalSectionLength(count, counter);
						cslendownLatch.countDown();
					}
				}).start();
			}
			cslendownLatch.await();
		}

		if (bigTest == 3) {
			final CountDownLatch accessdownLatch = new CountDownLatch(9);
			for (int i = 1; i < 42; i = i + 5) {
				final int count = i;
				new Thread(new Runnable() {

					@Override
					public void run() {
						Counter counter = test.new Counter();
						counter.initResults();
						test.parallelExperimentIncreasingAccess(count, counter);
						accessdownLatch.countDown();
					}
				}).start();
			}
			accessdownLatch.await();
		}

		if (bigTest == 4) {
			final CountDownLatch processordownLatch = new CountDownLatch(1);
			for (int i = 22; i < 23; i = i + 2) {
				final int count = i;
				new Thread(new Runnable() {

					@Override
					public void run() {
						Counter counter = test.new Counter();
						counter.initResults();
						test.parallelExperimentIncreasingPartitions(count, counter);
						processordownLatch.countDown();
					}
				}).start();
			}
			processordownLatch.await();
		}

		// else {
		// Counter counter = test.new Counter();
		// counter.initResults();
		// test.parallelExperimentIncreasingWorkload(5, counter);
		// }

		// final CountDownLatch rsfdownLatch = new CountDownLatch(5);
		// for (int i = 1; i < 6; i++) {
		// final int count = i;
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Counter counter = test.new Counter();
		// counter.initResults();
		// test.parallelExperimentIncreasingrsf(count, counter);
		// rsfdownLatch.countDown();
		// }
		// }).start();
		// }
		// rsfdownLatch.await();

		ResultReader.schedreader("result1");
	}

	public void parallelExperimentIncreasingCriticalSectionLength(int cslen, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

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

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
					if (tasks == null)
						return false;

					for (int i = 0; i < tasks.size(); i++) {
						for (int j = 0; j < tasks.get(i).size(); j++) {
							if (tasks.get(i).get(j).deadline < Ris[i][j])
								return false;
						}
					}
					return true;
				}

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, cs_range, RESOURCES_RANGE.PARTITIONS,
							NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							Ris = fnp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfnpS = true;
								counter.incfnp();
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = fp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfpS = true;
								counter.incfp();
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = mrsp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								ismrspS = true;
								counter.incmrsp();
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (isfnpS || isfpS || ismrspS) {
						counter.incTypical();
					} else {
						if (solver.checkSchedulability(useGA, lazy) == 1) {
							counter.incTypical();
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
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
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

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dtypical / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newResourceControl / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newallocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newpriority / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "\n";

		writeSystem("2 2 " + cslen, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingWorkload(int NoT, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
					if (tasks == null)
						return false;

					for (int i = 0; i < tasks.size(); i++) {
						for (int j = 0; j < tasks.get(i).size(); j++) {
							if (tasks.get(i).get(j).deadline < Ris[i][j])
								return false;
						}
					}
					return true;
				}

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NoT, RSF, range,
							RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							Ris = fnp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfnpS = true;
								counter.incfnp();
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = fp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfpS = true;
								counter.incfp();
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = mrsp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								ismrspS = true;
								counter.incmrsp();
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (isfnpS || isfpS || ismrspS) {
						counter.incTypical();
					} else {
						if (solver.checkSchedulability(useGA, lazy) == 1) {
							counter.incTypical();
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
					}


					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
					downLatch.countDown();
				}
			});
			worker.setName("1 " + NoT + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dtypical / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newResourceControl / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newallocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newpriority / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "\n";

		writeSystem("1 2 " + NoT, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingAccess(int NoA, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
					if (tasks == null)
						return false;

					for (int i = 0; i < tasks.size(); i++) {
						for (int j = 0; j < tasks.get(i).size(); j++) {
							if (tasks.get(i).get(j).deadline < Ris[i][j])
								return false;
						}
					}
					return true;
				}

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS,
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range, RESOURCES_RANGE.PARTITIONS, NoA, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							Ris = fnp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfnpS = true;
								counter.incfnp();
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = fp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfpS = true;
								counter.incfp();
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = mrsp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								ismrspS = true;
								counter.incmrsp();
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (isfnpS || isfpS || ismrspS) {
						counter.incTypical();
					} else {
						if (solver.checkSchedulability(useGA, lazy) == 1) {
							counter.incTypical();
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
					}


					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
					downLatch.countDown();
				}
			});
			worker.setName("3 " + NoA + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dtypical / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newResourceControl / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newallocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newpriority / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "\n";

		writeSystem("3 2 " + NoA, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingPartitions(int NoP, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
					if (tasks == null)
						return false;

					for (int i = 0; i < tasks.size(); i++) {
						for (int j = 0; j < tasks.get(i).size(); j++) {
							if (tasks.get(i).get(j).deadline < Ris[i][j])
								return false;
						}
					}
					return true;
				}

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NoP * NUMBER_OF_TASKS_ON_EACH_PARTITION, RSF, range,
							RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();

					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2, 2, 0.8,
							0.01, 2, 2, record, true);
					solver.name = "GA: " + Thread.currentThread().getName();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							Ris = fnp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfnpS = true;
								counter.incfnp();
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = fp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isfpS = true;
								counter.incfp();
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = mrsp.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								ismrspS = true;
								counter.incmrsp();
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (isfnpS || isfpS || ismrspS) {
						counter.incTypical();
					} else {
						if (solver.checkSchedulability(useGA, lazy) == 1) {
							counter.incTypical();
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
					}


					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
					downLatch.countDown();
				}
			});
			worker.setName("4 " + NoP + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dtypical / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newResourceControl / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newallocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newpriority / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "\n";

		writeSystem("4 2 " + NoP, result);
		System.out.println(result);
	}
	
	public void writeSystem(String filename, String result) {
		PrintWriter writer = null;

		try {
			writer = new PrintWriter(new FileWriter(new File("result1/" + filename + ".txt"), false));
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
