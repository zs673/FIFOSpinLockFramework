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

public class confidence2 {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	class Counter {
		int basicS = 0;
		int newS = 0;

		int count = 0;

		public synchronized void incbasicS() {
			basicS++;
		}

		public synchronized void incnewS() {
			newS++;
		}

		public synchronized void incCount() {
			count++;
		}

		public synchronized void initResults() {
			basicS = 0;
			newS = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		confidence2 test = new confidence2();
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
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, 22, 22 * 4, 0.3, CS_LENGTH_RANGE.RANDOM,
							RESOURCES_RANGE.PARTITIONS, 3, false);
					ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasksToAlloc, resources);

					long[][] Ris;
					CombinedAnalysis analysis = new CombinedAnalysis();

					boolean isBasicSchedulable = false;
					boolean isNewSchedulable = false;

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
						for (int i = 0; i < 5; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}
						}

						for (int i = 5; i < 8; i++) {
							ArrayList<ArrayList<SporadicTask>> allocTask = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
									generator.total_partitions, i);
							
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeByDMPO(allocTask, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, true, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}
							
							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 1;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 2;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}

							for (int j = 0; j < resources.size(); j++) {
								resources.get(j).protocol = 3;
							}
							Ris = analysis.getResponseTimeBySimpleSBPO(allocTask, resources, false);
							if (isSystemSchedulable(allocTask, Ris)) {
								isNewSchedulable = true;
								break;
							}
						}
					}

					int newsched = 0, oldsched = 0;
					if (isBasicSchedulable) {
						counter.incbasicS();
						oldsched = 1;
					}
					if (isNewSchedulable) {
						counter.incnewS();
						newsched = 1;
					}
					if (isBasicSchedulable && isNewSchedulable) {
						System.out.println("ERROR!");
						System.exit(-1);
					}

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

					String result = oldsched + " " + newsched;
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
