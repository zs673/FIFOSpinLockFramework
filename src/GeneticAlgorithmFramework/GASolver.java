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
	public String name = "";
	Random ran = new Random(System.currentTimeMillis());

	SystemGenerator geneator;
	ArrayList<SporadicTask> tasks;
	ArrayList<Resource> resources;
	CombinedAnalysis framework = new CombinedAnalysis();
	AllocationGeneator allocGeneator = new AllocationGeneator();

	public int[] bestGene = null;

	int[][] nextGenes;
	int[][] parentGenes;

	int[][] elitismGene;
	int[] elitismGeneIndex;

	long[] rtFitness;
	long[] schedFitness;

	/****************** GA Properties ******************/
	int ALLOCATION_POLICY_NUMBER;
	int PRIORITY_SCHEME_NUMBER;
	int PROTOCOL_SIZE = 3;
	boolean isPrint;
	int population;
	int elitismSize;
	int crossoverPoint;
	double crossoverRate;
	double mutationRate;
	int maxGeneration;
	int randomBound = 65535;
	public int currentGeneration = 0;
	int toumamentSize1, toumamentSize2;
	ArrayList<Integer> allocations = new ArrayList<>();
	ArrayList<AllocatedSystem> allocatedSystems = new ArrayList<>();

	public int bestAllocation = -1;
	public int bestProtocol = -1; // 1 MrsP; 2 FIFONP; 3 FIFOP; 0 combined.
	public int bestPriority = -1;

	boolean record;

	public ArrayList<Double> resultRecorder = new ArrayList<>();

	/****************** GA Properties ******************/

	public GASolver(ArrayList<SporadicTask> tasks, ArrayList<Resource> resources, SystemGenerator geneator, int ALLOCATION_POLICY_NUMBER,
			int PRIORITY_SCHEME_NUMBER, int population, int maxGeneration, int elitismSize, int crossoverPoint, double crossoverRate, double mutationRate,
			int toumamentSize1, int toumamentSize2, boolean record, boolean isPrint) {
		this.isPrint = isPrint;
		this.tasks = tasks;
		this.resources = resources;
		this.geneator = geneator;
		this.ALLOCATION_POLICY_NUMBER = ALLOCATION_POLICY_NUMBER;
		this.PRIORITY_SCHEME_NUMBER = PRIORITY_SCHEME_NUMBER;

		this.population = population;
		this.maxGeneration = maxGeneration;
		this.elitismSize = elitismSize;
		this.crossoverPoint = crossoverPoint;
		this.crossoverRate = crossoverRate;
		this.mutationRate = mutationRate;
		this.toumamentSize1 = toumamentSize1;
		this.toumamentSize2 = toumamentSize2;

		nextGenes = new int[population][resources.size() + 1];
		parentGenes = new int[population][resources.size() + 1];
		elitismGene = new int[elitismSize][resources.size() + 1];
		elitismGeneIndex = new int[elitismSize];

		schedFitness = new long[population];
		rtFitness = new long[population];

		for (int i = 0; i < population; i++) {
			schedFitness[i] = -1;
		}
		this.record = record;
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
	public int checkSchedulability(boolean useGA, boolean lazy) {
		for (int i = 0; i < ALLOCATION_POLICY_NUMBER; i++) {
			ArrayList<ArrayList<SporadicTask>> alloced = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, i);
			if (alloced != null) {
				allocations.add(i);
				AllocatedSystem allocsys = new AllocatedSystem(alloced);
				allocatedSystems.add(allocsys);
			}
		}

		if (allocations.size() == 0) {
			System.out.println("the task set is not allocatable!, name: " + name);
			return -1;
		}

		if (lazy) {
			PreGASolver preSovler = new PreGASolver(tasks, resources, geneator, PROTOCOL_SIZE, ALLOCATION_POLICY_NUMBER, PRIORITY_SCHEME_NUMBER, allocations,
					allocatedSystems, isPrint);
			int initial = preSovler.initialCheck(lazy, true);

			if (initial != 0) {
				this.bestAllocation = preSovler.allocation;
				this.bestPriority = preSovler.priority;
				this.bestProtocol = preSovler.protocol;
				System.out.println("the GA is not needed!, name: " + name);
				return initial;
			}
		}

		System.out.println("GA starts, name: " + name);
		return solve(useGA);
	}

	private int solve(boolean useGA) {
		getFirstGene();
		getFitness(nextGenes, schedFitness, rtFitness);
		if (bestGene != null) {

			bestProtocol = 1;
			int firstchorm = bestGene[0];
			for (int i = 1; i < resources.size(); i++) {
				if (bestGene[i] != firstchorm) {
					bestProtocol = 0;
					break;
				}
			}
			if (isPrint)
				System.out.println(name + " " + "new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene)
						+ " protocol: " + bestProtocol + " allocation: " + bestAllocation + " priority: " + bestPriority);
			return 1;
		}

		while (currentGeneration <= maxGeneration) {
			long[] sched_temp = new long[population];
			long[] rt_temp = new long[population];
			for (int i = 0; i < population; i++) {
				sched_temp[i] = -1;
			}
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
						sched_temp[i] = schedFitness[elitismGeneIndex[i]];
						rt_temp[i] = rtFitness[elitismGeneIndex[i]];
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
						int crosspoint1 = ran.nextInt(resources.size() - 1) + 1;
						int crosspoint2 = ran.nextInt(resources.size() - 1) + 1;

						int[] gene1 = parentGenes[(int) index1];
						int[] gene2 = parentGenes[(int) index2];

						int[] newGene1 = new int[resources.size() + 1];
						int[] newGene2 = new int[resources.size() + 1];

						if (crossoverPoint == 1 || crosspoint1 == crosspoint2) {
							for (int j = 0; j < resources.size() + 1; j++) {
								if (j < crosspoint1) {
									newGene1[j] = gene1[j];
									newGene2[j] = gene2[j];
								} else {
									newGene1[j] = gene2[j];
									newGene2[j] = gene1[j];
								}
							}
						} else {
							int a = -1, b = -1;
							if (crosspoint1 < crosspoint2) {
								a = crosspoint1;
								b = crosspoint2;
							} else {
								a = crosspoint2;
								b = crosspoint1;
							}

							for (int j = 0; j < resources.size() + 1; j++) {
								if (j < a) {
									newGene1[j] = gene1[j];
									newGene2[j] = gene2[j];
								} else if (j >= a && j <= b) {
									newGene1[j] = gene2[j];
									newGene2[j] = gene1[j];
								} else {
									newGene1[j] = gene1[j];
									newGene2[j] = gene2[j];
								}
							}
						}

						long[] Fit1 = computeFristFitness(newGene1);
						long[] Fit2 = computeFristFitness(newGene2);
						ArrayList<Long> gene1fitness = new ArrayList<>();
						ArrayList<Long> gene2fitness = new ArrayList<>();
						gene1fitness.add(Fit1[0]);
						gene1fitness.add(Fit1[1]);
						gene1fitness.add((long) 1);
						gene2fitness.add(Fit2[0]);
						gene2fitness.add(Fit2[1]);
						gene2fitness.add((long) 2);

						if (compareFitness(gene1fitness, gene2fitness) <= 0) {
							nextGenes[i] = newGene1;
							sched_temp[i] = Fit1[0];
							rt_temp[i] = Fit1[1];
						} else {
							nextGenes[i] = newGene2;
							sched_temp[i] = Fit2[0];
							rt_temp[i] = Fit2[1];
						}

					} else {
						if (compareFitness(toumament1.get(0), toumament2.get(0)) <= 0) {
							long index = toumament1.get(0).get(2);
							nextGenes[i] = parentGenes[(int) index];
							sched_temp[i] = schedFitness[(int) index];
							rt_temp[i] = rtFitness[(int) index];
						} else {
							long index = toumament2.get(0).get(2);
							nextGenes[i] = parentGenes[(int) index];
							sched_temp[i] = schedFitness[(int) index];
							rt_temp[i] = rtFitness[(int) index];
						}
					}

					double mute = ran.nextDouble();
					if (mute < mutationRate) {
						int muteindex1 = ran.nextInt(resources.size());
						int muteindex2 = ran.nextInt(resources.size());
						int temp = nextGenes[i][muteindex1];
						nextGenes[i][muteindex1] = nextGenes[i][muteindex2];
						nextGenes[i][muteindex2] = temp;
						sched_temp[i] = -1;
					}
				}
			} else {
				for (int i = 0; i < nextGenes.length; i++) {
					for (int j = 0; j < nextGenes[i].length - 1; j++) {
						nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
					}
					nextGenes[i][resources.size()] = allocations.get(ran.nextInt(randomBound) % allocations.size());
				}
			}

			getFitness(nextGenes, sched_temp, rt_temp);

			if (bestGene != null) {

				bestProtocol = 1;
				int firstchorm = bestGene[0];
				for (int i = 1; i < resources.size(); i++) {
					if (bestGene[i] != firstchorm) {
						bestProtocol = 0;
						break;
					}
				}
				if (isPrint)
					System.out.println(name + " " + "new combination schedulable   Gene: " + currentGeneration + "   Sol: " + Arrays.toString(bestGene)
							+ " protocol: " + bestProtocol + " allocation: " + bestAllocation + " priority: " + bestPriority);
				return 1;
			}

		}
		if (isPrint)
			System.out.println(name + " " + "not schedulable with in " + maxGeneration + " generations. GA finish");
		return -1;
	}

	private void getFirstGene() {
		for (int i = 0; i < PROTOCOL_SIZE * allocations.size(); i++) {
			for (int j = 0; j < resources.size(); j++) {
				nextGenes[i][j] = i % 3 + 1;
			}
			nextGenes[i][resources.size()] = allocations.get(i / PROTOCOL_SIZE);
		}

		for (int i = PROTOCOL_SIZE * allocations.size(); i < nextGenes.length; i++) {
			for (int j = 0; j < resources.size(); j++) {
				nextGenes[i][j] = ran.nextInt(randomBound) % 3 + 1;
			}
			nextGenes[i][resources.size()] = allocations.get(ran.nextInt(randomBound) % allocations.size());
		}
	}

	void getFitness(int[][] gene, long[] sched, long[] rt) {
		ArrayList<ArrayList<Long>> fitness = new ArrayList<>();

		for (int i = 0; i < gene.length; i++) {
			// System.out.println("Compute " + i + "th gene in generation " +
			// currentGeneration);
			long fit[] = null;
			if (sched[i] == -1)
				fit = computeFristFitness(gene[i]);
			else {
				fit = new long[2];
				fit[0] = sched[i];
				fit[1] = rt[i];
			}
			schedFitness[i] = fit[0];
			rtFitness[i] = fit[1];

			if (schedFitness[i] == 0) {
				bestGene = gene[i];
				if (!record)
					return;
			}

			ArrayList<Long> fitnessofGene = new ArrayList<>();
			fitnessofGene.add(fit[0]);
			fitnessofGene.add(fit[1]);
			fitnessofGene.add((long) i);
			fitness.add(fitnessofGene);
		}

		fitness.sort((l1, l2) -> compareFitness(l1, l2));

		if (record) {
			if (currentGeneration == 0) {
				resultRecorder.add((double) fitness.get(0).get(0));
				resultRecorder.add((double) fitness.get(0).get(1));

				resultRecorder.add((double) fitness.get(fitness.size() - 1).get(0));
				resultRecorder.add((double) fitness.get(fitness.size() - 1).get(1));

				long avgsched = 0, avgrt = 0;
				for (int i = 0; i < fitness.size(); i++) {
					avgsched += fitness.get(i).get(0);
					avgrt += fitness.get(i).get(1);
				}

				resultRecorder.add((double) avgsched / (double) population);
				resultRecorder.add((double) avgrt / (double) population);
				System.out.println(this.name + "  generation 0 recorded! ");
			}
			if (currentGeneration == maxGeneration || bestGene != null) {
				resultRecorder.add((double) fitness.get(0).get(0));
				resultRecorder.add((double) fitness.get(0).get(1));

				resultRecorder.add((double) fitness.get(fitness.size() - 1).get(0));
				resultRecorder.add((double) fitness.get(fitness.size() - 1).get(1));

				long avgsched = 0, avgrt = 0;
				for (int i = 0; i < fitness.size(); i++) {
					avgsched += fitness.get(i).get(0);
					avgrt += fitness.get(i).get(1);
				}

				resultRecorder.add((double) avgsched / (double) population);
				resultRecorder.add((double) avgrt / (double) population);

				System.out.println(this.name + " last generation recorded! last generation: " + currentGeneration + " max generation: " + maxGeneration
						+ " bestGene?: " + (bestGene != null));
			}

			if (bestGene != null)
				return;
		}

		if (PRIORITY_SCHEME_NUMBER > 1) {
			for (int i = 0; i < elitismSize; i++) {
				long index = fitness.get(i).get(2);
				long fit = computeSBPO(gene[(int) index]);
				if (fit == 0) {
					bestGene = gene[(int) index];
					return;
				}
			}
		}

		for (int i = 0; i < elitismSize; i++) {
			long index = fitness.get(i).get(2);
			elitismGene[i] = nextGenes[(int) index];
			elitismGeneIndex[i] = (int) index;
		}

		long maxindex = fitness.get(0).get(2);
		if (isPrint)
			System.out.println(name + " " + "Generation " + currentGeneration + "   maxsched: " + fitness.get(0).get(0) + " maxrt: " + fitness.get(0).get(1)
					+ "    GENE: " + Arrays.toString(nextGenes[(int) maxindex]));

	}

	private long[] computeFristFitness(int[] gene) {
		int sched_fitness = 0;
		long rt_fitness = 0;
		if (gene.length != resources.size() + 1 || gene[resources.size()] >= 8) {
			System.err.println(" gene length error! or alloc gene error");
			System.exit(-1);
		}
		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}

		ArrayList<ArrayList<SporadicTask>> tasksWithAllocation = null;

		// try {
		int allocation = gene[resources.size()];
		int allocation_index = allocations.indexOf(allocation);
		if (allocation_index < 0) {
			System.out.println("error!");
		}
		tasksWithAllocation = allocatedSystems.get(allocation_index).tasks;
		// } catch (Exception e) {
		// System.out.println("error!");
		// }

		if (tasksWithAllocation != null) {
			for (int i = 0; i < tasksWithAllocation.size(); i++) {
				if (tasksWithAllocation.get(i).size() == 0) {
					tasksWithAllocation.remove(i);
					i--;
				}
			}

			for (int i = 0; i < tasksWithAllocation.size(); i++) {
				for (int j = 0; j < tasksWithAllocation.get(i).size(); j++) {
					tasksWithAllocation.get(i).get(j).partition = i;
				}
			}

			if (resources != null && resources.size() > 0) {
				for (int i = 0; i < resources.size(); i++) {
					Resource res = resources.get(i);
					res.isGlobal = false;
					res.partitions.clear();
					res.requested_tasks.clear();
				}

				/* for each resource */
				for (int i = 0; i < resources.size(); i++) {
					Resource resource = resources.get(i);

					/* for each partition */
					for (int j = 0; j < tasksWithAllocation.size(); j++) {

						/* for each task in the given partition */
						for (int k = 0; k < tasksWithAllocation.get(j).size(); k++) {
							SporadicTask task = tasksWithAllocation.get(j).get(k);

							if (task.resource_required_index.contains(resource.id - 1)) {
								resource.requested_tasks.add(task);
								if (!resource.partitions.contains(task.partition)) {
									resource.partitions.add(task.partition);
								}
							}
						}
					}

					if (resource.partitions.size() > 1)
						resource.isGlobal = true;
				}
			}

		}

		long[][] Ris = framework.getResponseTimeByDMPO(tasksWithAllocation, resources, AnalysisUtils.extendCalForGA, false, true, true, true, false);

		for (int i = 0; i < tasksWithAllocation.size(); i++) {
			for (int j = 0; j < tasksWithAllocation.get(i).size(); j++) {
				if (tasksWithAllocation.get(i).get(j).deadline < Ris[i][j]) {
					sched_fitness++;
					rt_fitness += Ris[i][j] - tasksWithAllocation.get(i).get(j).deadline;
				}
			}
		}

		if (sched_fitness == 0) {
			bestPriority = 0;
			bestAllocation = gene[resources.size()];
		}

		long[] fitness = new long[2];
		fitness[0] = sched_fitness;
		fitness[1] = rt_fitness;

		return fitness;
	}

	private long computeSBPO(int[] gene) {

		for (int i = 0; i < resources.size(); i++) {
			resources.get(i).protocol = gene[i];
		}

		ArrayList<ArrayList<SporadicTask>> tasksWithAllocation = allocatedSystems.get(allocations.indexOf(gene[resources.size()])).tasks;

		if (tasksWithAllocation != null) {
			for (int i = 0; i < tasksWithAllocation.size(); i++) {
				if (tasksWithAllocation.get(i).size() == 0) {
					tasksWithAllocation.remove(i);
					i--;
				}
			}

			for (int i = 0; i < tasksWithAllocation.size(); i++) {
				for (int j = 0; j < tasksWithAllocation.get(i).size(); j++) {
					tasksWithAllocation.get(i).get(j).partition = i;
				}
			}

			if (resources != null && resources.size() > 0) {
				for (int i = 0; i < resources.size(); i++) {
					Resource res = resources.get(i);
					res.isGlobal = false;
					res.partitions.clear();
					res.requested_tasks.clear();
				}

				/* for each resource */
				for (int i = 0; i < resources.size(); i++) {
					Resource resource = resources.get(i);

					/* for each partition */
					for (int j = 0; j < tasksWithAllocation.size(); j++) {

						/* for each task in the given partition */
						for (int k = 0; k < tasksWithAllocation.get(j).size(); k++) {
							SporadicTask task = tasksWithAllocation.get(j).get(k);

							if (task.resource_required_index.contains(resource.id - 1)) {
								resource.requested_tasks.add(task);
								if (!resource.partitions.contains(task.partition)) {
									resource.partitions.add(task.partition);
								}
							}
						}
					}

					if (resource.partitions.size() > 1)
						resource.isGlobal = true;
				}
			}

		}

		// Get 1st Fitness Values.
		long[][] Ris = framework.getResponseTimeBySBPO(tasksWithAllocation, resources, false);
		int sched_fitness = 0;
		for (int i = 0; i < tasksWithAllocation.size(); i++) {
			for (int j = 0; j < tasksWithAllocation.get(i).size(); j++) {
				if (tasksWithAllocation.get(i).get(j).deadline < Ris[i][j]) {
					sched_fitness++;
				}
			}
		}

		if (sched_fitness == 0) {
			bestPriority = 1;
			bestAllocation = gene[resources.size()];
		}

		return sched_fitness;
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

	class AllocatedSystem {
		public ArrayList<ArrayList<SporadicTask>> tasks = null;

		public AllocatedSystem(ArrayList<ArrayList<SporadicTask>> tsks) {
			this.tasks = new ArrayList<ArrayList<SporadicTask>>(tsks);

		}
	}

}
