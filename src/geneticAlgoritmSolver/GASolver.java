package geneticAlgoritmSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import analysis.IACombinedProtocol;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator2;
import generatorTools.SystemGenerator2.CS_LENGTH_RANGE;
import generatorTools.SystemGenerator2.RESOURCES_RANGE;

public class GASolver {
	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;

	IACombinedProtocol framework = new IACombinedProtocol();

	/****************** GA Properties ******************/
	Random ran = new Random(System.currentTimeMillis());

	int PROTOCOL_SIZE = 3;
	int population = 1000;
	int randomBound = 65535;
	int elitismSize = 2;
	int maxGeneration = 100;
	double crossoverRate = 0.5;
	double mutationRate = 0.1;
	int mutationBound = 5;
	int toumamentSize1 = 5, toumamentSize2 = 5;

	int currentGeneration = 0;
	int[][] elitismGene;
	int[][] nextGenes;
	int[][] parentGenes;
	int[] bestGene = null;
	long[] schedFitness;
	long[] rtFitness;

	/****************** GA Properties ******************/

	public GASolver(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources) {
		this.tasks = tasks;
		this.resources = resources;

		nextGenes = new int[population][resources.size()];
		parentGenes = new int[population][resources.size()];

		schedFitness = new long[population];
		rtFitness = new long[population];
//		for (int i = 0; i < population; i++) {
//			schedFitness[i] = 0;
//			rtFitness[i] = 0;
//		}
		elitismGene = new int[elitismSize][resources.size()];
		
//		for(int i=0;i<resources.size();i++){
//			System.out.print("R" + resources.get(i).id +": " + resources.get(i).csl + "    ");
//		}
//		System.out.println();
	}

	public int findSchedulableProtocols(boolean useGA) {
		int initial = new PreGASolver(tasks, resources, false).initialCheck();
		if (initial != 0)
			return initial;

//		int result = solve(useGA);
//		return result;
		
		return -1;
	}

	int solve(boolean useGA) {
		getFirstGene();
		getFitness(nextGenes);
		if (bestGene != null) {
			System.out.println("new combination schedulable");
			return 0;
		}

		while (currentGeneration <= maxGeneration) {
			currentGeneration++;
			for (int i = 0; i < population; i++) {
				for (int j = 0; j < resources.size(); j++) {
					parentGenes[i][j] = nextGenes[i][j];
				}
			}
			if (useGA) {
				int nextGeneIndex = elitismSize;
				for (int i = 0; i < elitismSize; i++) {
					for (int j = 0; j < resources.size(); j++) {
						nextGenes[i][j] = elitismGene[i][j];
					}
				}
				for (int i = nextGeneIndex; i < population; i++) {
					ArrayList<ArrayList<Long>> toumament1 = new ArrayList<>();
					ArrayList<ArrayList<Long>> toumament2 = new ArrayList<>();

					for (int j = 0; j < toumamentSize1; j++) {
						int randomIndex = ran.nextInt(population);
						ArrayList<Long> randomGeneFitness = new ArrayList<>();
						randomGeneFitness.add(schedFitness[randomIndex]);
						randomGeneFitness.add(rtFitness[randomIndex]);
						randomGeneFitness.add((long) randomIndex);
						toumament1.add(randomGeneFitness);
					}
					for (int j = 0; j < toumamentSize2; j++) {
						int randomIndex = ran.nextInt(population);
						ArrayList<Long> randomGeneFitness = new ArrayList<>();
						randomGeneFitness.add(schedFitness[randomIndex]);
						randomGeneFitness.add(rtFitness[randomIndex]);
						randomGeneFitness.add((long) randomIndex);
						toumament2.add(randomGeneFitness);
					}
					toumament1.sort((l1, l2) -> compareFitness(l1, l2));
					toumament2.sort((l1, l2) -> compareFitness(l1, l2));
					long index1 = toumament1.get(0).get(2), index2 = toumament2.get(0).get(2);

					double crossover = ran.nextDouble();
					if (crossover <= crossoverRate) {
						int crosspoint = ran.nextInt(resources.size() - 1) + 1;
						int[] newGene = new int[resources.size()];
						for (int j = 0; j < resources.size(); j++) {
							if (j < crosspoint)
								newGene[j] = parentGenes[(int) index1][j];
							else
								newGene[j] = parentGenes[(int) index2][j];
						}
						nextGenes[i] = newGene;

					} else {
						if (compareFitness(toumament1.get(0), toumament2.get(0)) <= 0) {
							long index = toumament1.get(0).get(2);
							nextGenes[i] = parentGenes[(int) index];
						} else {
							long index = toumament2.get(0).get(2);
							nextGenes[i] = parentGenes[(int) index];
						}
					}

					double mute = ran.nextDouble();
					if (mute < mutationRate) {
						int muteCount = ran.nextInt(mutationBound);
						for (int j = 0; j < muteCount; j++) {
							int muteindex1 = ran.nextInt(resources.size());
							int muteindex2 = ran.nextInt(resources.size());
							int temp = nextGenes[i][muteindex1];
							nextGenes[i][muteindex1] = nextGenes[i][muteindex2];
							nextGenes[i][muteindex2] = temp;

						}
					}
				}
			} else {
				for (int i = 0; i < nextGenes.length; i++) {
					for (int j = 0; j < nextGenes[i].length; j++) {
						nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
					}
				}
			}
			getFitness(nextGenes);
			if (bestGene != null) {
				System.out.println("new combination schedulable");
				return 0;
			}

		}
		System.out.println("not schedulable. GA finish");
		return -1;
	}

	private void getFirstGene() {
		for (int i = 0; i < nextGenes.length; i++) {
			if (i < PROTOCOL_SIZE) {
				for (int j = 0; j < nextGenes[i].length; j++) {
					nextGenes[i][j] = i + 1;
				}
			} else {
				for (int j = 0; j < nextGenes[i].length; j++) {
					nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
				}
			}
		}
	}

	void getFitness(int[][] gene) {
		ArrayList<ArrayList<Long>> fitness = new ArrayList<>();

		for (int i = 0; i < gene.length; i++) {
			long[] fit = isSystemSchedulable(gene[i]);
			schedFitness[i] = fit[0];
			rtFitness[i] = fit[1];

			if (schedFitness[i] == 0) {
				bestGene = gene[i];
				return;
			}

			ArrayList<Long> fitnessofGene = new ArrayList<>();
			fitnessofGene.add(fit[0]);
			fitnessofGene.add(fit[1]);
			fitnessofGene.add((long) i);
			fitness.add(fitnessofGene);
		}

		fitness.sort((l1, l2) -> compareFitness(l1, l2));

		for (int i = 0; i < elitismSize; i++) {
			long index = fitness.get(i).get(2);
			elitismGene[i] = nextGenes[(int) index];
		}

		long maxindex = fitness.get(0).get(2);
		System.out.println("Generation " + currentGeneration + "   maxsched: " + fitness.get(0).get(0) + " maxrt: " + fitness.get(0).get(1) + "    GENE: "
				+ Arrays.toString(nextGenes[(int) maxindex]));
	}

	int compareFitness(ArrayList<Long> a, ArrayList<Long> b) {
		long a0 = a.get(0), a1 = a.get(1), a2 = a.get(2);
		long b0 = b.get(0), b1 = b.get(1), b2 = b.get(2);
		if (a0 < b0)
			return -1;
		if (a0 > b0)
			return 1;

		if (a0 == b0) {
			if (a1 < b1)
				return -1;
			if (a1 > b1)
				return 1;

			if (a1 == b1) {
				if (a2 < b2)
					return -1;
				if (a2 > b2)
					return 1;
				if (a2 == b2)
					return 0;
			}
		}

		System.err.println("comparator error!" + " a0:  " + a.get(0) + " a1:  " + a.get(1) + " a2:  " + a.get(2) + " b0:  " + b.get(0) + " b1:  " + b.get(1)
				+ " b2:  " + b.get(2));
		System.err.println(a0 == b0);
		System.err.println(a1 == b1);
		System.err.println(a2 == b2);

		System.exit(-1);
		return 100;
	}

	private long[] isSystemSchedulable(int[] gene) {
		long[] fitness = new long[2];
		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}
		long[][] Ris = framework.calculateResponseTime(tasks, resources, false, false);

		int sched_fitness = 0;
		long rt_fitness = 0;
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j]) {
					sched_fitness++;
					rt_fitness += Ris[i][j] - tasks.get(i).get(j).deadline;
				}
			}
		}
		fitness[0] = sched_fitness;
		fitness[1] = rt_fitness;
		return fitness;
	}

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RSF = 0.4;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 4;
		int NUMBER_OF_SYSTEMS = 1000;

		int fifop = 0, fifonp = 0, mrsp = 0, combine = 0;

		SystemGenerator2 generator = new SystemGenerator2(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		for (int i = 0; i < NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);
			GASolver finder = new GASolver(tasks, resources);
			int result = finder.findSchedulableProtocols(true);

			if (result == 1) {
				fifonp++;
				combine++;
			}
			if (result == 2) {
				fifop++;
				combine++;
			}
			if (result == 3) {
				mrsp++;
				combine++;
			}
			if (result == 0)
				combine++;
			System.out.println(i);
		}
		System.out.println(NUMBER_OF_SYSTEMS + "system:   fifonp: " + fifonp + "   fifop: " + fifop + "   mrsp: " + mrsp + "   combine: " + combine);
	}
}
