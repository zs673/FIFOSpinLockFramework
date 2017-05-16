package geneticAlgoritmSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import analysis.IACombinedProtocol;
import analysis.IOAAnalysisUtils;
import entity.Resource;
import entity.SporadicTask;

public class GADynamicSolver {
	public int[] bestGene = null;
	double crossoverRate;

	public int currentGeneration = 0;

	int[][] elitismGene;

	int elitismSize;
	IACombinedProtocol framework = new IACombinedProtocol();
	boolean isPrint;

	int maxGeneration;
	int mutationBound;
	double mutationRate;
	int[][] nextGenes;
	int[][] parentGenes;
	int population;
	int PROTOCOL_SIZE = 3;

	/****************** GA Properties ******************/
	Random ran = new Random(System.currentTimeMillis());
	int randomBound = 65535;
	ArrayList<Resource> resources;
	long[] rtFitness;
	long[] schedFitness;
	public double similarity = 0;
	int[] staticProtocols;
	ArrayList<ArrayList<SporadicTask>> tasks;
	int toumamentSize1, toumamentSize2;
	boolean withStaticProtocols = false;

	/****************** GA Properties ******************/

	public GADynamicSolver(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, int population, int maxGeneration,
			int elitismSize, double crossoverRate, double mutationRate, int mutationBound, int toumamentSize1, int toumamentSize2, boolean isPrint) {
		this.tasks = tasks;
		this.resources = resources;
		this.population = population;
		this.maxGeneration = maxGeneration;
		this.elitismSize = elitismSize;
		this.crossoverRate = crossoverRate;
		this.mutationRate = mutationRate;
		this.mutationBound = mutationBound;
		this.toumamentSize1 = toumamentSize1;
		this.toumamentSize2 = toumamentSize2;

		this.isPrint = isPrint;

		nextGenes = new int[population][resources.size()];
		parentGenes = new int[population][resources.size()];

		schedFitness = new long[population];
		rtFitness = new long[population];
		elitismGene = new int[elitismSize][resources.size()];

		staticProtocols = new int[resources.size()];
	}

	private double ArrayCompare(int[] a, int[] b) {
		int sameCount = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] == b[i])
				sameCount++;
		}
		return (double) sameCount / (double) a.length;
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

		System.err.println("comparator error!" + " a0:  " + a.get(0) + " a1:  " + a.get(1) + " a2:  " + a.get(2) + " b0:  " + b.get(0) + " b1:  "
				+ b.get(1) + " b2:  " + b.get(2));
		System.err.println(a0 == b0);
		System.err.println(a1 == b1);
		System.err.println(a2 == b2);

		System.exit(-1);
		return 100;
	}

	public int findSchedulableProtocols(boolean useGA, int maxAccess) {
		PreGASolver preSovler = new PreGASolver(tasks, resources, isPrint);
		int initial = preSovler.initialCheck(maxAccess);
		for (int i = 0; i < resources.size(); i++) {
			staticProtocols[i] = preSovler.staticprotocols[i];
		}
		if (initial != 0) {
			if (initial == 4)
				similarity = 1;
			if (initial == 1) {
				int[] protocols = new int[resources.size()];
				for (int i = 0; i < resources.size(); i++) {
					protocols[i] = 1;
				}
				similarity = ArrayCompare(staticProtocols, protocols);
			}
			if (initial == 2) {
				int[] protocols = new int[resources.size()];
				for (int i = 0; i < resources.size(); i++) {
					protocols[i] = 2;
				}
				similarity = ArrayCompare(staticProtocols, protocols);
			}
			if (initial == 3) {
				int[] protocols = new int[resources.size()];
				for (int i = 0; i < resources.size(); i++) {
					protocols[i] = 3;
				}
				similarity = ArrayCompare(staticProtocols, protocols);
			}
			return initial;
		}

		int result = solve(useGA);
		return result;
	}

	private void getFirstGene() {
		int offsite = 0;
		if (withStaticProtocols) {
			for (int j = 0; j < nextGenes[0].length; j++) {
				nextGenes[0][j] = staticProtocols[j];
			}
			offsite = 1;
		}

		for (int i = offsite; i < PROTOCOL_SIZE + offsite; i++) {
			for (int j = 0; j < nextGenes[i].length; j++) {
				nextGenes[i][j] = offsite == 1 ? i : i + 1;
			}
		}

		for (int i = PROTOCOL_SIZE + offsite; i < nextGenes.length; i++) {
			for (int j = 0; j < nextGenes[i].length; j++) {
				nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
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
		if (isPrint)
			System.out.println("Generation " + currentGeneration + "   maxsched: " + fitness.get(0).get(0) + " maxrt: " + fitness.get(0).get(1)
					+ "    GENE: " + Arrays.toString(nextGenes[(int) maxindex]));
	}

	private long[] isSystemSchedulable(int[] gene) {
		long[] fitness = new long[2];
		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}
		long[][] Ris = framework.calculateResponseTime(tasks, resources, false, false, IOAAnalysisUtils.extendCalForGA);

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

	int solve(boolean useGA) {
		getFirstGene();
		getFitness(nextGenes);
		if (bestGene != null) {
			similarity = ArrayCompare(staticProtocols, bestGene);
			if (isPrint)
				System.out.println("new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene) + " similar: "
						+ similarity);
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
				similarity = ArrayCompare(staticProtocols, bestGene);
				if (isPrint)
					System.out.println("new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene)
							+ " similar: " + similarity);
				return 0;
			}

		}
		if (isPrint)
			System.out.println("not schedulable. GA finish");
		return -1;
	}
}
