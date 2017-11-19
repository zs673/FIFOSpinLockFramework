package GeneticAlgorithmFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import analysis.CombinedAnalysis;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.AnalysisUtils;

public class GASolver {
	SystemGenerator geneator;
	ArrayList<SporadicTask> tasks;
	ArrayList<Resource> resources;
	CombinedAnalysis framework = new CombinedAnalysis();
	AllocationGeneator allocGeneator = new AllocationGeneator();

	Random ran = new Random(System.currentTimeMillis());

	public int[] bestGene = null;

	int[][] nextGenes;
	int[][] parentGenes;
	int[][] elitismGene;
	long[] rtFitness;
	long[] schedFitness;

	/****************** GA Properties ******************/
	int ALLOCATION_POLICY_NUMBER;
	int PRIORITY_SCHEME_NUMBER;
	int PROTOCOL_SIZE = 3;
	boolean isPrint;
	int population;
	int elitismSize;
	double crossoverRate;
	double mutationRate;
	int mutationBound;
	int maxGeneration;
	int randomBound = 65535;
	public int currentGeneration = 0;
	int toumamentSize1, toumamentSize2;
	ArrayList<Integer> allocations = new ArrayList<>();

	public int allocation = -1;
	public int protocol = -1; // 1 MrsP; 2 FIFONP; 3 FIFOP; 0 combined.
	public int bestPriority = -1;
	// public int priority = -1;

	/****************** GA Properties ******************/

	public GASolver(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources, SystemGenerator geneator, int ALLOCATION_POLICY_NUMBER,
			int PRIORITY_SCHEME_NUMBER, int population, int maxGeneration, int elitismSize, double crossoverRate, double mutationRate, int mutationBound,
			int toumamentSize1, int toumamentSize2, boolean isPrint) {
		this.isPrint = isPrint;
		this.tasks = tasks;
		this.resources = resources;
		this.geneator = geneator;
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.PRIORITY_SCHEME_NUMBER = PRIORITY_SCHEME_NUMBER;

		this.population = population;
		this.maxGeneration = maxGeneration;
		this.elitismSize = elitismSize;
		this.crossoverRate = crossoverRate;
		this.mutationRate = mutationRate;
		this.mutationBound = mutationBound;
		this.toumamentSize1 = toumamentSize1;
		this.toumamentSize2 = toumamentSize2;

		nextGenes = new int[population][resources.size() + 1];
		parentGenes = new int[population][resources.size() + 1];
		elitismGene = new int[elitismSize][resources.size() + 1];

		schedFitness = new long[population];
		rtFitness = new long[population];
	}

	/**
	 * Try to search for a feasible solution
	 * 
	 * @param useGA
	 *            If true, GA is applied to search. Otherwise we randomly search
	 *            for a solution.
	 * @return True: the system is feasible. False: the system is not feasible
	 *         within the given generation and population size.
	 */
	public int checkSchedulability(boolean useGA) {
		PreGASolver preSovler = new PreGASolver(tasks, resources, geneator, PROTOCOL_SIZE, ALLOCATION_POLICY_NUMBER, PRIORITY_SCHEME_NUMBER, isPrint);
		int initial = preSovler.initialCheck(true);

		if (initial != 0) {
			this.allocation = preSovler.allocation;
			this.bestPriority = preSovler.priority;
			this.protocol = preSovler.protocol;
			return initial;
		}

		for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
			if (allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, i) != null) {
				allocations.add(i);
			}
		}

		if (allocations.size() == 0)
			return -1;

		return solve(useGA);
	}

	private int solve(boolean useGA) {
		getFirstGene();
		getFitness(nextGenes);
		if (bestGene != null) {
			if (isPrint)
				System.out.println(
						"new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene) + " priority: " + bestPriority);
			return 1;
		}

		while (currentGeneration <= maxGeneration) {
			currentGeneration++;
			for (int i = 0; i < population; i++) {
				for (int j = 0; j < resources.size() + 1; j++) {
					parentGenes[i][j] = nextGenes[i][j];
				}
			}
			if (useGA) {
				int nextGeneIndex = elitismSize;
				for (int i = 0; i < elitismSize; i++) {
					for (int j = 0; j < resources.size() + 1; j++) {
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
						int[] newGene = new int[resources.size() + 1];
						for (int j = 0; j < resources.size(); j++) {
							if (j < crosspoint)
								newGene[j] = parentGenes[(int) index1][j];
							else
								newGene[j] = parentGenes[(int) index2][j];
						}

						if (compareFitness(toumament1.get(0), toumament2.get(0)) <= 0) {
							newGene[resources.size()] = parentGenes[(int) index1][resources.size()];
						} else {
							newGene[resources.size()] = parentGenes[(int) index2][resources.size()];
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

						nextGenes[i][resources.size()] = ran.nextInt(randomBound) % allocations.size();
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
				if (isPrint)
					System.out.println(
							"new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene) + " priority: " + bestPriority);
				return 1;
			}

		}
		if (isPrint)
			System.out.println("not schedulable with in " + maxGeneration + " generations. GA finish");
		return -1;
	}

	private void getFirstGene() {
		for (int i = 0; i < PROTOCOL_SIZE * allocations.size(); i++) {
			for (int j = 0; j < resources.size(); j++) {
				nextGenes[i][j] = i % 3 + 1;
			}
			nextGenes[i][resources.size()] = i / PROTOCOL_SIZE;
		}

		for (int i = PROTOCOL_SIZE * allocations.size(); i < nextGenes.length; i++) {
			for (int j = 0; j < resources.size(); j++) {
				nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
			}
			nextGenes[i][resources.size()] = allocations.get(ran.nextInt(randomBound) % allocations.size());
		}

		System.out.println();
	}

	void getFitness(int[][] gene) {
		ArrayList<ArrayList<Long>> fitness = new ArrayList<>();

		for (int i = 0; i < gene.length; i++) {
			long[] fit = computeFitness(gene[i]);
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
			System.out.println("Generation " + currentGeneration + "   maxsched: " + fitness.get(0).get(0) + " maxrt: " + fitness.get(0).get(1) + "    GENE: "
					+ Arrays.toString(nextGenes[(int) maxindex]));
	}

	private long[] computeFitness(int[] gene) {
		long[] fitness = new long[2];
		if (gene.length != resources.size() + 1 || gene[resources.size()] >= 8) {
			System.err.println(" gene length error! or alloc gene error");
			System.exit(-1);
		}
		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}

		ArrayList<ArrayList<SporadicTask>> tasksWithAllocation = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions,
				gene[resources.size()]);
		long[][] Ris = framework.getResponseTimeByDMPO(tasksWithAllocation, resources, AnalysisUtils.extendCalForGA, false, true, true, true, false);

		if (Ris == null) {
			int NoT = 0;
			for (int i = 0; i < tasks.size(); i++) {
				NoT++;
			}
			fitness[0] = NoT;
			fitness[1] = Long.MAX_VALUE;
		} else {
			int sched_fitness = 0;
			long rt_fitness = 0;
			for (int i = 0; i < tasksWithAllocation.size(); i++) {
				for (int j = 0; j < tasksWithAllocation.get(i).size(); j++) {
					if (tasksWithAllocation.get(i).get(j).deadline < Ris[i][j]) {
						sched_fitness++;
						rt_fitness += Ris[i][j] - tasksWithAllocation.get(i).get(j).deadline;
					}
				}
			}
			fitness[0] = sched_fitness;
			fitness[1] = rt_fitness;
		}

		if (fitness[0] == 0) {
			bestPriority = 1;
			protocol = 0;
			allocation = gene[resources.size()];
		} else {
			if (PRIORITY_SCHEME_NUMBER > 1) {
				long[][] RisOPA = framework.getResponseTimeByOPA(tasksWithAllocation, resources, true, false);
				if (isSystemSchedulable(tasksWithAllocation, RisOPA)) {
					fitness[0] = 0;
					fitness[1] = 0;
					bestPriority = 2;
					protocol = 0;
					allocation = gene[resources.size()];
				}

				if (PRIORITY_SCHEME_NUMBER > 2) {
					long[][] RisSBPO = framework.getResponseTimeBySBPO(tasksWithAllocation, resources, false);
					if (isSystemSchedulable(tasksWithAllocation, RisSBPO)) {
						fitness[0] = 0;
						fitness[1] = 0;
						bestPriority = 3;
						protocol = 0;
						allocation = gene[resources.size()];
					}
				}
			}
		}

		return fitness;
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

	boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		if (tasks == null || tasks.size() == 0 || Ris == null || Ris.length == 0)
			return false;

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}

}
