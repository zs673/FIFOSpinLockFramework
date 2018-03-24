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
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class LazyMode2 {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;

	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	class Counter {
		int typical = 0;
		int basicS = 0;
		int newS = 0;
		int newAllocation = 0;
		int newPriority = 0;

		int count = 0;

		public synchronized void inctypical() {
			typical++;
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
			typical = 0;

			basicS = 0;
			newS = 0;
			newAllocation = 0;
			newPriority = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		LazyMode2 test = new LazyMode2();

		Counter counter = test.new Counter();
		counter.initResults();
		test.test22(counter);

		System.out.println("FINISHED!!!");
	}

	public void test22(Counter counter) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			final int index = i;
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, 16, 16 * 7, 0.3, CS_LENGTH_RANGE.RANDOM,
							RESOURCES_RANGE.PARTITIONS, 3, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;
					boolean isNewAllocation = false;
					boolean isNewpriority = false;

					for (int i = 0; i < 5; i++) {
						ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
								generator.total_partitions, i);

						for (int j = 0; j < resources.size(); j++) {
							resources.get(j).protocol = 1;
						}
						Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
						if (isSystemSchedulable(allocTask, Ris)) {
							isBasicSchedulable = true;
							break;
						}

						for (int j = 0; j < resources.size(); j++) {
							resources.get(j).protocol = 2;
						}
						Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
						if (isSystemSchedulable(allocTask, Ris)) {
							isBasicSchedulable = true;
							break;
						}

						for (int j = 0; j < resources.size(); j++) {
							resources.get(j).protocol = 3;
						}
						Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
						if (isSystemSchedulable(allocTask, Ris)) {
							isBasicSchedulable = true;
							break;
						}
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

					int newsched = 0, oldsched = 0, newAllocation = 0, newPrio = 0;
					if (isBasicSchedulable) {
						counter.incbasicS();
						oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						newsched = 1;
					}
					if (isNewAllocation) {
						newAllocation = 1;
						counter.incnewA();
					}
					if (isNewpriority) {
						newPrio = 1;
						counter.incnewP();
					}

					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					String result = oldsched + " " + newsched + " " + newAllocation + " " + newPrio;
					writeSystem((2 + " " + 1 + " " + index), result);

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

		String result = (double) counter.basicS / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newS / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.newAllocation / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.newPriority / (double) TOTAL_NUMBER_OF_SYSTEMS
				+ "\n";

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
