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

public class confidence {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 100;
	public static int NUMBER_OF_TIMES = 100;

	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;
	public static int TOTAL_PARTITIONS = 16;

	int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
	final CS_LENGTH_RANGE range = CS_LENGTH_RANGE.RANDOM;
	final double RSF = 0.3;
	int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;

	public static boolean useRi = true;
	public static boolean btbHit = true;

	public static int GENERATIONS = 100;
	public static int POPULATION = 100;
	public static int ALLOCATION_POLICY = 1;
	public static int PRIORITY_RULE = 1;

	public static boolean useGA = true;
	public static boolean lazy = true;
	public static boolean record = false;

	class Counter {
		int fnp = 0;
		int fp = 0;
		int mrsp = 0;
		int Dcombine = 0;
		int Dnew = 0;

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
		final CountDownLatch downLatch = new CountDownLatch(1);
		confidence test = new confidence();
		int bigTest = Integer.parseInt(args[0]);
		int smallTest = Integer.parseInt(args[1]);

		switch (bigTest) {
		case 2:
			new Thread(new Runnable() {
				@Override
				public void run() {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingCriticalSectionLength(smallTest, counter);
					downLatch.countDown();
				}
			}).start();
			break;
		case 3:
			new Thread(new Runnable() {
				@Override
				public void run() {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingAccess(smallTest, counter);
					downLatch.countDown();
				}
			}).start();
		default:
			break;
		}

		downLatch.await();

		ResultReader.readsuccessF("result", bigTest, smallTest);
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

		final CountDownLatch timesdownLatch = new CountDownLatch(NUMBER_OF_TIMES);
		for (int j = 0; j < NUMBER_OF_TIMES; j++) {
			final int fatherindex = j;
			new Thread(new Runnable() {

				@Override
				public void run() {

					final Counter subcount = new Counter();

					final CountDownLatch numbersdownLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
					for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
						final int childindex = i;
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
								ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
										generator.total_partitions, 0);

								long[][] Ris;
								MrsP mrsp = new MrsP();
								FIFOP fp = new FIFOP();
								FIFONP fnp = new FIFONP();

								GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2,
										2, 0.8, 0.01, 2, 2, record, true);
								solver.name = "GA: " + Thread.currentThread().getName();

								Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incmrsp();
									subcount.incmrsp();
								}

								Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incfnp();
									subcount.incfnp();
								}

								Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incfp();
									subcount.incfp();
								}

								if (solver.checkSchedulability(useGA, lazy) == 1) {
									counter.incDcombine();
									subcount.incDcombine();
									if (solver.bestProtocol == 0) {
										counter.incDnew();
										subcount.incDnew();
									}
								}

								System.out.println(Thread.currentThread().getName() + " Finish.");
								numbersdownLatch.countDown();
							}
						});
						worker.setName("2 " + cslen + " times: " + fatherindex + " numbers: " + childindex);
						worker.start();
					}

					try {
						numbersdownLatch.await();
					} catch (InterruptedException e) {
					}

					String subresult = cslen + "|" + fatherindex + " " + subcount.fnp + " " + subcount.fp + " " + subcount.mrsp + " " + subcount.Dcombine + " "
							+ subcount.Dnew;
					writeSystem("2 2 " + cslen + " " + fatherindex, subresult);

					timesdownLatch.countDown();

				}
			}).start();

		}

		try {
			timesdownLatch.await();
		} catch (InterruptedException e) {
		}

		String result = "cslen-" + cslen + " " + counter.fnp + " " + counter.fp + " " + counter.mrsp + " " + counter.Dcombine + " " + counter.Dnew;
		writeSystem("2 2 " + cslen, result);

	}

	public void parallelExperimentIncreasingAccess(int NoA, Counter counter) {
		final CountDownLatch timesdownLatch = new CountDownLatch(NUMBER_OF_TIMES);
		for (int j = 0; j < NUMBER_OF_TIMES; j++) {
			final int fatherindex = j;
			new Thread(new Runnable() {

				@Override
				public void run() {

					final Counter subcount = new Counter();

					final CountDownLatch numbersdownLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
					for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
						final int childindex = i;
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
								ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources,
										generator.total_partitions, 0);

								long[][] Ris;
								MrsP mrsp = new MrsP();
								FIFOP fp = new FIFOP();
								FIFONP fnp = new FIFONP();

								GASolver solver = new GASolver(tasksToAlloc, resources, generator, ALLOCATION_POLICY, PRIORITY_RULE, POPULATION, GENERATIONS, 2,
										2, 0.8, 0.01, 2, 2, record, true);
								solver.name = "GA: " + Thread.currentThread().getName();

								Ris = mrsp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incmrsp();
									subcount.incmrsp();
								}

								Ris = fnp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incfnp();
									subcount.incfnp();
								}

								Ris = fp.getResponseTimeByDMPO(tasks, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
								if (isSystemSchedulable(tasks, Ris)) {
									counter.incfp();
									subcount.incfp();
								}

								if (solver.checkSchedulability(useGA, lazy) == 1) {
									counter.incDcombine();
									subcount.incDcombine();
									if (solver.bestProtocol == 0) {
										counter.incDnew();
										subcount.incDnew();
									}
								}

								System.out.println(Thread.currentThread().getName() + " Finish.");
								numbersdownLatch.countDown();
							}
						});
						worker.setName("3 " + NoA + " times: " + fatherindex + " numbers: " + childindex);
						worker.start();
					}

					try {
						numbersdownLatch.await();
					} catch (InterruptedException e) {
					}

					String subresult = NoA + "|" + fatherindex + " " + subcount.fnp + " " + subcount.fp + " " + subcount.mrsp + " " + subcount.Dcombine + " "
							+ subcount.Dnew;
					writeSystem("3 2 " + NoA + " " + fatherindex, subresult);

					timesdownLatch.countDown();

				}
			}).start();

		}

		try {
			timesdownLatch.await();
		} catch (InterruptedException e) {
		}

		String result = "access-" + NoA + " " + counter.fnp + " " + counter.fp + " " + counter.mrsp + " " + counter.Dcombine + " " + counter.Dnew;
		writeSystem("3 2 " + NoA, result);
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
