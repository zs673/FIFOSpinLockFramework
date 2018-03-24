package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

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

public class TypicalApproach {
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

	class Counter {
		int fnpWF = 0;
		int fpWF = 0;
		int mrspWF = 0;

		int count = 0;

		public synchronized void incfnpWF() {
			fnpWF++;
		}

		public synchronized void incfpWF() {
			fpWF++;
		}

		public synchronized void incmrspWF() {
			mrspWF++;
		}

		public synchronized void incCount() {
			count++;
		}

		public synchronized void initResults() {
			mrspWF = 0;
			fpWF = 0;
			fnpWF = 0;

			count = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {

		TypicalApproach test = new TypicalApproach();

		final CountDownLatch tasksdownLatch = new CountDownLatch(4);
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 1; i < 10; i++) {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingWorkload(i, counter);

				}
				tasksdownLatch.countDown();
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 1; i < 7; i++) {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingCriticalSectionLength(i, counter);

				}
				tasksdownLatch.countDown();
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 1; i < 42; i = i + 5) {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingAccess(i, counter);

				}
				tasksdownLatch.countDown();
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 4; i < 23; i = i + 2) {
					Counter counter = test.new Counter();
					counter.initResults();
					test.parallelExperimentIncreasingPartitions(i, counter);

				}
				tasksdownLatch.countDown();
			}
		}).start();

		tasksdownLatch.await();

		ResultReader.schedreader("result");
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

					boolean mrspOK = false, fnpOK = false, fpOK = false;

					ArrayList<ArrayList<SporadicTask>> tasksWF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
					Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksBF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
					Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksFF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
					Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksNF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
					Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksSPA = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions,
							4);
					Ris = mrsp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fpOK = true;

					if (mrspOK)
						counter.incmrspWF();
					if (fnpOK)
						counter.incfnpWF();
					if (fpOK)
						counter.incfpWF();

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);

				}
			});
			worker.setName("2 " + cslen + " " + i);
			worker.run();
		}

		String result = (double) counter.fnpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrspWF / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("2 2 " + cslen, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingWorkload(int NoT, Counter counter) {

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

					boolean mrspOK = false, fnpOK = false, fpOK = false;

					ArrayList<ArrayList<SporadicTask>> tasksWF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
					Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksBF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
					Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksFF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
					Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksNF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
					Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksSPA = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions,
							4);
					Ris = mrsp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fpOK = true;

					if (mrspOK)
						counter.incmrspWF();
					if (fnpOK)
						counter.incfnpWF();
					if (fpOK)
						counter.incfpWF();

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
				}
			});
			worker.setName("1 " + NoT + " " + i);
			worker.run();
		}

		String result = (double) counter.fnpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrspWF / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("1 2 " + NoT, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingAccess(int NoA, Counter counter) {

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

					boolean mrspOK = false, fnpOK = false, fpOK = false;

					ArrayList<ArrayList<SporadicTask>> tasksWF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
					Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksBF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
					Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksFF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
					Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksNF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
					Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksSPA = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions,
							4);
					Ris = mrsp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fpOK = true;

					if (mrspOK)
						counter.incmrspWF();
					if (fnpOK)
						counter.incfnpWF();
					if (fpOK)
						counter.incfpWF();
					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
				}
			});
			worker.setName("3 " + NoA + " " + i);
			worker.run();
		}

		String result = (double) counter.fnpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrspWF / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("3 2 " + NoA, result);
		System.out.println(result);
	}

	public void parallelExperimentIncreasingPartitions(int NoP, Counter counter) {

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

					boolean mrspOK = false, fnpOK = false, fpOK = false;

					ArrayList<ArrayList<SporadicTask>> tasksWF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);
					Ris = mrsp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksWF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksWF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksBF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 1);
					Ris = mrsp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksBF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksBF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksFF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 2);
					Ris = mrsp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksFF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksFF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksNF = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 3);
					Ris = mrsp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksNF, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksNF, Ris))
						fpOK = true;

					ArrayList<ArrayList<SporadicTask>> tasksSPA = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions,
							4);
					Ris = mrsp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						mrspOK = true;

					Ris = fnp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fnpOK = true;

					Ris = fp.getResponseTimeByDMPO(tasksSPA, resources, AnalysisUtils.extendCalForStatic, true, btbHit, useRi, false);
					if (isSystemSchedulable(tasksSPA, Ris))
						fpOK = true;

					if (mrspOK)
						counter.incmrspWF();
					if (fnpOK)
						counter.incfnpWF();
					if (fpOK)
						counter.incfpWF();

					counter.incCount();
					System.out.println(Thread.currentThread().getName() + " F, count: " + counter.count);
				}
			});
			worker.setName("4 " + NoP + " " + i);
			worker.run();
		}
		String result = (double) counter.fnpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) counter.fpWF / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) counter.mrspWF / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("4 2 " + NoP, result);
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
