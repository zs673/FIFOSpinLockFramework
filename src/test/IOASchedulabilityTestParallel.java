package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysis.IAFIFONP;
import analysis.IAFIFOP;
import analysis.IANewMrsPRTAWithMCNP;
import basicAnalysis.FIFONP;
import basicAnalysis.FIFOP;
import basicAnalysis.NewMrsPRTAWithMCNP;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.GeneatorUtils.CS_LENGTH_RANGE;
import generatorTools.GeneatorUtils.RESOURCES_RANGE;
import generatorTools.SystemGenerator;
import geneticAlgoritmSolver.GADynamicSolver;

public class IOASchedulabilityTestParallel {

	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;
	public static int MIN_PERIOD = 1;
	public static int MAX_PERIOD = 1000;

	int mrsp = 0;
	int fp = 0;
	int fnp = 0;

	int siamrsp = 0;
	int siafp = 0;
	int siafnp = 0;
	int combineL = 0;

	public static void main(String[] args) throws InterruptedException {
		IOASchedulabilityTestParallel test = new IOASchedulabilityTestParallel();
		for (int i = 1; i < 7; i++) {
			test.initResults();
			test.experimentIncreasingCriticalSectionLength(i);
		}

		IOAResultReader.schedreader();
	}

	public void experimentIncreasingCriticalSectionLength(int cslen) {
		final CountDownLatch downLatch = new CountDownLatch(TOTAL_NUMBER_OF_SYSTEMS);
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		final double RSF = 0.3;
		final int cs_len = cslen;
		final CS_LENGTH_RANGE range;
		switch (cslen) {
		case 1:
			range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 2:
			range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 3:
			range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 4:
			range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 5:
			range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		case 6:
			range = CS_LENGTH_RANGE.RANDOM;
			break;
		default:
			range = null;
			break;
		}

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			Thread worker = new Thread(new Runnable() {

				@Override
				public void run() {
					SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_TASKS_ON_EACH_PARTITION,
							TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION, true, range, RESOURCES_RANGE.PARTITIONS, RSF,
							NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);
					ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
					ArrayList<Resource> resources = generator.generateResources();
					generator.generateResourceUsage(tasks, resources);

					long[][] Ris;
					IANewMrsPRTAWithMCNP IOAmrsp = new IANewMrsPRTAWithMCNP();
					IAFIFOP IOAfp = new IAFIFOP();
					IAFIFONP IOAfnp = new IAFIFONP();
					FIFONP fnp = new FIFONP();
					FIFOP fp = new FIFOP();
					NewMrsPRTAWithMCNP mrsp = new NewMrsPRTAWithMCNP();
					GADynamicSolver solverL = new GADynamicSolver(tasks, resources, 500, 200, 2, 0.5, 0.1, 5, 5, 5, true);

					Ris = IOAmrsp.getResponseTime(tasks, resources, true, false);
					if (isSystemSchedulable(tasks, Ris))
						inciamrsp();

					Ris = IOAfnp.NewMrsPRTATest(tasks, resources, true, false);
					if (isSystemSchedulable(tasks, Ris))
						inciafnp();

					Ris = IOAfp.NewMrsPRTATest(tasks, resources, true, false);
					if (isSystemSchedulable(tasks, Ris))
						inciafp();

					Ris = fp.NewMrsPRTATest(tasks, resources, false);
					if (isSystemSchedulable(tasks, Ris))
						incfp();

					Ris = fnp.NewMrsPRTATest(tasks, resources, false);
					if (isSystemSchedulable(tasks, Ris))
						incfnp();

					Ris = mrsp.getResponseTime(tasks, resources, 6, false);
					if (isSystemSchedulable(tasks, Ris))
						incmrsp();

					if (solverL.findSchedulableProtocols(true) >= 0)
						inciacombineL();

					System.out.println(Thread.currentThread().getName() + " F");
					downLatch.countDown();
				}

				public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
					for (int i = 0; i < tasks.size(); i++) {
						for (int j = 0; j < tasks.get(i).size(); j++) {
							if (tasks.get(i).get(j).deadline < Ris[i][j])
								return false;
						}
					}
					return true;
				}
			});
			worker.setName(cs_len + " " + i);
			worker.start();
		}

		try {
			downLatch.await();
		} catch (InterruptedException e) {
		}

		String result = (double) fnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) fp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) mrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siafnp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) siafp / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) siamrsp / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) combineL / (double) TOTAL_NUMBER_OF_SYSTEMS + "\n";

		writeSystem("ioa 2 2 " + cslen, result);
		System.out.println(result);
	}

	public void initResults() {
		mrsp = 0;
		fp = 0;
		fnp = 0;

		siamrsp = 0;
		siafp = 0;
		siafnp = 0;

		combineL = 0;
	}

	public synchronized void incmrsp() {
		mrsp++;
	}

	public synchronized void incfp() {
		fp++;
	}

	public synchronized void incfnp() {
		fnp++;
	}

	public synchronized void inciamrsp() {
		siamrsp++;
	}

	public synchronized void inciafp() {
		siafp++;
	}

	public synchronized void inciafnp() {
		siafnp++;
	}

	public synchronized void inciacombineL() {
		combineL++;
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
