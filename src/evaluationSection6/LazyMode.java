package evaluationSection6;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.CombinedAnalysis;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;
import utils.ResultReader;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class LazyMode {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	class Counter {
		int fnp = 0;
		int fp = 0;
		int mrsp = 0;

		int basicS = 0;
		int newS = 0;
		int newAllocation = 0;
		int newPriority = 0;

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

		public synchronized void incbasicS() {
			basicS++;
		}

		public synchronized void incnewS() {
			newS++;
		}

		public synchronized void incnewA() {
			newAllocation++;
		}

		public synchronized void incnewP() {
			newPriority++;
		}

		public synchronized void incCount() {
			count++;
		}

		public synchronized void initResults() {
			fnp = 0;
			fp = 0;
			mrsp = 0;

			basicS = 0;
			newS = 0;
			newAllocation = 0;
			newPriority = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		LazyMode test = new LazyMode();

		Counter counter = test.new Counter();
		counter.initResults();
		test.testaccess(1, counter);

		counter.initResults();
		test.testaccess(6, counter);

		counter.initResults();
		test.testaccess(11, counter);

		counter.initResults();
		test.testaccess(16, counter);

		counter.initResults();
		test.testaccess(21, counter);

		counter.initResults();
		test.testaccess(26, counter);

		counter.initResults();
		test.testaccess(31, counter);

		counter.initResults();
		test.testaccess(36, counter);

		counter.initResults();
		test.testaccess(41, counter);

		counter.initResults();
		test.testtask(1, counter);

		counter.initResults();
		test.testtask(2, counter);

		counter.initResults();
		test.testtask(3, counter);

		counter.initResults();
		test.testtask(4, counter);

		counter.initResults();
		test.testtask(5, counter);

		counter.initResults();
		test.testtask(6, counter);

		counter.initResults();
		test.testtask(7, counter);

		counter.initResults();
		test.testtask(8, counter);

		counter.initResults();
		test.testtask(9, counter);
		
		counter.initResults();
		test.testcslen(1, counter);

		counter.initResults();
		test.testcslen(2, counter);

		counter.initResults();
		test.testcslen(3, counter);

		counter.initResults();
		test.testcslen(4, counter);

		counter.initResults();
		test.testcslen(5, counter);

		counter.initResults();
		test.testcslen(6, counter);

		ResultReader.schedreader("result");

		System.out.println("FINISHED!!!");
	}

	public void testparallelism(int NoP, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			// final int index = i;
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, NoP, NoP * 4, 0.3, CS_LENGTH_RANGE.RANDOM,
							RESOURCES_RANGE.PARTITIONS, 3, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;
					boolean isNewAllocation = false;
					boolean isNewpriority = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfnpS = true;
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfpS = true;
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								ismrspS = true;
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (!isBasicSchedulable) {
						for (int i = 5; i < 8; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}
						}
					}

					if (!isBasicSchedulable && !isNewSchedulable) {
						for (int i = 7; i > -1; i--) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}
						}

					}

					// int newsched = 0, oldsched = 0, newAllocation = 0,
					// newPrio = 0;

					if (isfnpS)
						counter.incfnp();
					if (isfpS)
						counter.incfp();
					if (ismrspS)
						counter.incmrsp();

					if (isBasicSchedulable) {
						counter.incbasicS();
						// oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						// newsched = 1;
					}
					if (isNewAllocation) {
						// newAllocation = 1;
						counter.incnewA();
					}
					if (isNewpriority) {
						// newPrio = 1;
						counter.incnewP();
					}

					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					// String result = oldsched + " " + newsched + " " +
					// newAllocation + " " + newPrio;
					// writeSystem((2 + " " + 1 + " " + index), result);

					downLatch.countDown();
				}
			});
			worker.setName("Thread " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.basicS / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newS / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newAllocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newPriority / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		System.out.println(result);

		writeSystem("4 2 " + NoP, result);
	}

	public void testaccess(int NoA, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			// final int index = i;
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, 16, 16 * 4, 0.3, CS_LENGTH_RANGE.RANDOM,
							RESOURCES_RANGE.PARTITIONS, NoA, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;
					boolean isNewAllocation = false;
					boolean isNewpriority = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfnpS = true;
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfpS = true;
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								ismrspS = true;
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (!isBasicSchedulable) {
						for (int i = 5; i < 8; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}
						}
					}

					if (!isBasicSchedulable && !isNewSchedulable) {
						for (int i = 7; i > -1; i--) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}
						}

					}

					// int newsched = 0, oldsched = 0, newAllocation = 0,
					// newPrio = 0;

					if (isfnpS)
						counter.incfnp();
					if (isfpS)
						counter.incfp();
					if (ismrspS)
						counter.incmrsp();

					if (isBasicSchedulable) {
						counter.incbasicS();
						// oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						// newsched = 1;
					}
					if (isNewAllocation) {
						// newAllocation = 1;
						counter.incnewA();
					}
					if (isNewpriority) {
						// newPrio = 1;
						counter.incnewP();
					}

					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					// String result = oldsched + " " + newsched + " " +
					// newAllocation + " " + newPrio;
					// writeSystem((2 + " " + 1 + " " + index), result);

					downLatch.countDown();
				}
			});
			worker.setName("Thread " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.basicS / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newS / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newAllocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newPriority / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		System.out.println(result);

		writeSystem("3 2 " + NoA, result);
	}

	public void testtask(int NoT, Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			// final int index = i;
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, 16, 16 * NoT, 0.3, CS_LENGTH_RANGE.RANDOM,
							RESOURCES_RANGE.PARTITIONS, 3, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;
					boolean isNewAllocation = false;
					boolean isNewpriority = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfnpS = true;
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfpS = true;
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								ismrspS = true;
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (!isBasicSchedulable) {
						for (int i = 5; i < 8; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}
						}
					}

					if (!isBasicSchedulable && !isNewSchedulable) {
						for (int i = 7; i > -1; i--) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}
						}

					}

					// int newsched = 0, oldsched = 0, newAllocation = 0,
					// newPrio = 0;

					if (isfnpS)
						counter.incfnp();
					if (isfpS)
						counter.incfp();
					if (ismrspS)
						counter.incmrsp();

					if (isBasicSchedulable) {
						counter.incbasicS();
						// oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						// newsched = 1;
					}
					if (isNewAllocation) {
						// newAllocation = 1;
						counter.incnewA();
					}
					if (isNewpriority) {
						// newPrio = 1;
						counter.incnewP();
					}

					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					// String result = oldsched + " " + newsched + " " +
					// newAllocation + " " + newPrio;
					// writeSystem((2 + " " + 1 + " " + index), result);

					downLatch.countDown();
				}
			});
			worker.setName("Thread " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.basicS / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newS / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newAllocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newPriority / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		System.out.println(result);

		writeSystem("1 2 " + NoT, result);
	}

	public void testcslen(int cslen, Counter counter) {
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

		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			// final int index = i;
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, 16, 16 * 4, 0.3, cs_range, RESOURCES_RANGE.PARTITIONS, 3,
							false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isfnpS = false;
					boolean isfpS = false;
					boolean ismrspS = false;

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;
					boolean isNewAllocation = false;
					boolean isNewpriority = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						if (!isfnpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfnpS = true;
							}
						}

						if (!isfpS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								isfpS = true;
							}
						}

						if (!ismrspS) {
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isBasicSchedulable = true;
								ismrspS = true;
							}
						}

						if (isfnpS && isfpS && ismrspS)
							break;
					}

					if (!isBasicSchedulable) {
						for (int i = 5; i < 8; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewAllocation = true;
								break;
							}
						}
					}

					if (!isBasicSchedulable && !isNewSchedulable) {
						for (int i = 7; i > -1; i--) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								isNewpriority = true;
								break;
							}
						}

					}

					// int newsched = 0, oldsched = 0, newAllocation = 0,
					// newPrio = 0;

					if (isfnpS)
						counter.incfnp();
					if (isfpS)
						counter.incfp();
					if (ismrspS)
						counter.incmrsp();

					if (isBasicSchedulable) {
						counter.incbasicS();
						// oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						// newsched = 1;
					}
					if (isNewAllocation) {
						// newAllocation = 1;
						counter.incnewA();
					}
					if (isNewpriority) {
						// newPrio = 1;
						counter.incnewP();
					}

					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					// String result = oldsched + " " + newsched + " " +
					// newAllocation + " " + newPrio;
					// writeSystem((2 + " " + 1 + " " + index), result);

					downLatch.countDown();
				}
			});
			worker.setName("Thread " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) counter.fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.basicS / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newS / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newAllocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newPriority / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		System.out.println(result);

		writeSystem("2 2 " + cslen, result);
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
