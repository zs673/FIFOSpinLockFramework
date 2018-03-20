package evaluation;

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

/**
 * 100 100 GA Work Load 21 1.0 1.0 1.0 1.0 0.0 22 0.989 0.989 0.989 0.989 0.0 23
 * 0.921 0.949 0.947 0.952 0.0 24 0.799 0.84 0.855 0.87 0.0 25 0.681 0.713 0.755
 * 0.795 0.005 26 0.492 0.497 0.564 0.631 0.013 27 0.294 0.282 0.33 0.444 0.03
 * 28 0.013 0.02 0.005 0.039 0.014 29 0.0 0.0 0.0 0.0 0.0 CS Length 21 0.688
 * 0.942 0.941 0.946 0.0 22 0.627 0.794 0.788 0.805 0.0 23 0.51 0.503 0.586 0.63
 * 0.007 24 0.367 0.139 0.362 0.432 0.023 25 0.243 0.031 0.159 0.271 0.021 26
 * 0.387 0.087 0.361 0.441 0.024 Resource Access 21 0.642 0.535 0.71 0.728 0.003
 * 26 0.373 0.436 0.452 0.524 0.014 211 0.261 0.326 0.324 0.389 0.013 216 0.185
 * 0.294 0.256 0.337 0.009 221 0.134 0.251 0.2 0.27 0.004 226 0.109 0.206 0.162
 * 0.223 0.005 231 0.095 0.189 0.14 0.2 0.003 Parallel 24 0.995 0.999 0.999
 * 0.999 0.0 26 0.975 0.979 0.98 0.981 0.0 28 0.921 0.946 0.946 0.952 0.0 210
 * 0.87 0.911 0.904 0.922 0.0 212 0.753 0.81 0.81 0.84 0.001 214 0.603 0.633
 * 0.683 0.719 0.002 216 0.478 0.489 0.565 0.619 0.003 218 0.333 0.308 0.404
 * 0.469 0.022 220 0.241 0.181 0.308 0.359 0.017 222 0.167 0.133 0.226 0.276
 * 0.021
 */

public class ProtocolsCombined {

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

		int Dcombine = 0;
		int Dnew = 0;
		int fnp = 0;
		int fp = 0;
		int mrsp = 0;

		public synchronized void incfnp() {
			fnp++;
		}

		public synchronized void incfp() {
			fp++;
		}

		public synchronized void incmrsp() {
			mrsp++;
		}

		public synchronized void incDcombine() {
			Dcombine++;
		}

		public synchronized void incDnew() {
			Dnew++;
		}

		public synchronized void initResults() {
			mrsp = 0;
			fp = 0;
			fnp = 0;
			Dcombine = 0;
			Dnew = 0;
		}

	}

	public static void main(String[] args) throws InterruptedException {
		ProtocolsCombined test = new ProtocolsCombined();

		// for (int i = 8; i < 9; i++) {
		// final int count = i;
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Counter counter = test.new Counter();
		// counter.initResults();
		// test.parallelExperimentIncreasingWorkload(count, counter);
		// downLatch.countDown();
		// }
		// }).start();
		//
		// }

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

		// for (int i = 1; i < 42; i = i + 5) {
		// final int count = i;
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Counter counter = test.new Counter();
		// counter.initResults();
		// test.parallelExperimentIncreasingAccess(count, counter);
		// downLatch.countDown();
		// }
		// }).start();
		// }
		// for (int i = 4; i < 23; i = i + 2) {
		// final int count = i;
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Counter counter = test.new Counter();
		// counter.initResults();
		// test.parallelExperimentIncreasingPartitions(count, counter);
		// downLatch.countDown();
		// }
		// }).start();
		// }

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

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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
					ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();
					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 5, 1, 0.5,
							0.01, 5, 5, true, true);

					Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incmrsp();

					Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfnp();

					Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfp();

					if (solver.checkSchedulability(true, true) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 1) {
							counter.incDnew();
							System.out.println("counter: " + counter.Dnew);
						}
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

		String result = (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dcombine / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 2 2 " + cslen, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingAccess(int NoA, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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
					ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();
					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, GENERATIONS, POPULATION, 5, 1, 0.5,
							0.1, 5, 5, true, true);

					Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incmrsp();

					Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfnp();

					Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfp();

					if (solver.checkSchedulability(true, true) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 1) {
							counter.incDnew();
						}
					}

					System.out.println(Thread.currentThread().getName() + " F");
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

		String result = (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dcombine / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 3 2 " + NoA, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingPartitions(int NoP, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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
					ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();
					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, GENERATIONS, POPULATION, 5, 1, 0.5,
							0.1, 5, 5, true, true);

					Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incmrsp();

					Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfnp();

					Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfp();

					if (solver.checkSchedulability(true, true) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 1) {
							counter.incDnew();
						}
					}

					System.out.println(Thread.currentThread().getName() + " F");
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

		String result = (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dcombine / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 4 2 " + NoP, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingrsf(int resourceSharingFactor, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 1;
		double rsf;
		switch (resourceSharingFactor) {
		case 1:
			rsf = 0.2;
			break;
		case 2:
			rsf = 0.3;
			break;
		case 3:
			rsf = 0.4;
			break;
		case 4:
			rsf = 0.5;
			break;
		case 5:
			rsf = 0.6;
			break;
		default:
			rsf = 0;
			break;
		}

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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
							TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION, rsf, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);
					ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();
					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, GENERATIONS, POPULATION, 5, 1, 0.5,
							0.1, 5, 5, true, true);

					Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incmrsp();

					Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfnp();

					Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfp();

					if (solver.checkSchedulability(true, true) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 1) {
							counter.incDnew();
						}
					}

					System.out.println(Thread.currentThread().getName() + " F");
					downLatch.countDown();
				}
			});
			worker.setName("5 " + resourceSharingFactor + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dcombine / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 5 2 " + resourceSharingFactor, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingWorkload(int NoT, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		double rsf = 0.2;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NoT, rsf, range,
							RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);
					ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

					long[][] Ris;
					MrsP mrsp = new MrsP();
					FIFOP fp = new FIFOP();
					FIFONP fnp = new FIFONP();
					GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, GENERATIONS, POPULATION, 5, 1, 0.5,
							0.1, 5, 5, true, true);

					Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incmrsp();

					Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfnp();

					Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasks, Ris))
						counter.incfp();

					if (solver.checkSchedulability(true, true) == 1) {
						counter.incDcombine();
						if (solver.bestProtocol == 0 || solver.bestAllocation > 4 || solver.bestPriority > 1) {
							counter.incDnew();
						}
					}

					System.out.println(Thread.currentThread().getName() + " F");
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

		String result = (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.Dcombine / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.Dnew / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 1 2 " + NoT, result);
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
