package framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.SystemGenerator;
import generatorTools.SystemGenerator.CS_LENGTH_RANGE;
import generatorTools.SystemGenerator.RESOURCES_RANGE;

public class GAProtocolsFinder {
	public static int PROTOCOL_SIZE = 3;
	public static int POPULATION = 50;
	public static int MAX_GENERATION = 100;

	ArrayList<ArrayList<SporadicTask>> tasks;
	ArrayList<Resource> resources;
	boolean print;

	FIFOSpinLocksFramework framework = new FIFOSpinLocksFramework();;
	Random ran = new Random(System.currentTimeMillis());
	int randomBound = 65535;

	int[] init_fitness;
	int[][] initial_generation;
	long[][][] init_rt;

	int[] bestSolution = null;
	int expect_fitness;

	int[][] parent_generation;
	int[][] next_generation;
	int[] fitness;

	public GAProtocolsFinder(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean print) {
		this.tasks = tasks;
		this.resources = resources;
		this.print = print;
	}

	public int[] findSchedulableProtocols() {
		
		// check response time of each time with each single protocol
		init();
		checkInitProtocols();
		if (print) {
			System.out.println("Init Done, expect fitness: " + expect_fitness);
			for (int i = 0; i < initial_generation.length; i++) {
				System.out.println(Arrays.toString(initial_generation[i]));
				System.out.println(Arrays.deepToString(init_rt[i]));
			}
			System.out.println(Arrays.toString(init_fitness));
			
			System.out.println("random generated protocols");
			for(int i=0;i<POPULATION;i++){
				System.out.println(Arrays.toString(next_generation[i]));
			}
		}
		if(bestSolution != null)
			return bestSolution;

		// processing initial data
		processGene();
		evolve();
		
		
		
		return null;
	}

	private void processGene(){
		
	}
	
	private void evolve() {
		// copy the next generation to the parent generation
		for (int i = 0; i < next_generation.length; i++) {
			for (int j = 0; j < next_generation[i].length; j++) {
				parent_generation[i][j] = next_generation[i][j];
			}
		}
	}

	private void init() {
		bestSolution = null;

		expect_fitness = 0;
		for (int i = 0; i < tasks.size(); i++) {
			expect_fitness += tasks.get(i).size();
		}

		fitness = new int[POPULATION];
		for (int i = 0; i < POPULATION; i++) {
			fitness[i] = 0;
		}

		parent_generation = new int[POPULATION][resources.size()];
		next_generation = new int[POPULATION][resources.size()];
		for (int i = 0; i < next_generation.length; i++) {
			for (int j = 0; j < next_generation[i].length; j++) {
				next_generation[i][j] = ran.nextInt(randomBound) % 3 + 1;
			}
		}

		initial_generation = new int[PROTOCOL_SIZE][resources.size()];
		for (int i = 0; i < initial_generation.length; i++) {
			for (int j = 0; j < initial_generation[i].length; j++) {
				initial_generation[i][j] = i + 1;
			}
		}
		init_rt = new long[PROTOCOL_SIZE][][];
		init_fitness = new int[PROTOCOL_SIZE];
	}

	private void checkInitProtocols() {
		for (int i = 0; i < PROTOCOL_SIZE; i++) {
			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = initial_generation[i][k];
			}
			init_rt[i] = framework.calculateResponseTime(tasks, resources, false, false);
			init_fitness[i] = isSystemSchedulable(init_rt[i]);
		}
	}

	private int isSystemSchedulable(long[][] Ris) {
		int fitness = 0;
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline >= Ris[i][j])
					fitness++;
			}
		}
		return fitness;
	}

	public static void main(String args[]) {
		int TOTAL_PARTITIONS = 16;
		int MIN_PERIOD = 1;
		int MAX_PERIOD = 1000;
		CS_LENGTH_RANGE cs_len_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RSF = 0.2;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION = 6;
		int NUMBER_OF_SYSTEMS = 1;
		boolean print = true;

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, 0.1 * (double) NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, TOTAL_PARTITIONS,
				NUMBER_OF_MAX_TASKS_ON_EACH_PARTITION, true, cs_len_range, RESOURCES_RANGE.PARTITIONS, RSF, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE);

		for (int i = 0; i < NUMBER_OF_SYSTEMS; i++) {
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasks, resources);
			GAProtocolsFinder finder = new GAProtocolsFinder(tasks, resources, print);
			finder.findSchedulableProtocols();
		}
	}
}
