package test;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class TestGeneatorTasks {

	public static void main(String[] args) {
		int NUMBER_OF_SYSTEMS = 100000;

		int MAX_PERIOD = 1000;
		int MIN_PERIOD = 1;
		int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 2;
		int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;
		CS_LENGTH_RANGE range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
		double RESOURCE_SHARING_FACTOR = 0.2;
		int TOTAL_PARTITIONS = 16;

		AllocationGeneator allocGeneator = new AllocationGeneator();

		SystemGenerator geneator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, NUMBER_OF_TASKS_ON_EACH_PARTITION * TOTAL_PARTITIONS,
				RESOURCE_SHARING_FACTOR, range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, -1, false);

		for (int j = 0; j < NUMBER_OF_SYSTEMS; j++) {
			ArrayList<SporadicTask> tasks = geneator.generateTasks();
			ArrayList<Resource> resources = geneator.generateResources();
			geneator.generateResourceUsage(tasks, resources);

			System.out.println(" WORST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksWF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 0);
			geneator.PrintAllocatedSystem(tasksWF, resources);

			System.out.println("\n\n");
			System.out.println(" BEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksBF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 1);
			geneator.PrintAllocatedSystem(tasksBF, resources);

			System.out.println("\n\n");
			System.out.println(" FIRST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksFF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 2);
			geneator.PrintAllocatedSystem(tasksFF, resources);

			System.out.println("\n\n");
			System.out.println(" NEXT FIT");
			ArrayList<ArrayList<SporadicTask>> tasksNF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 3);
			geneator.PrintAllocatedSystem(tasksNF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE REQUEST FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRRF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 4);
			geneator.PrintAllocatedSystem(tasksRRF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE LOCAL FIT");
			ArrayList<ArrayList<SporadicTask>> tasksRLF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 5);
			geneator.PrintAllocatedSystem(tasksRLF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscslendF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 6);
			geneator.PrintAllocatedSystem(taskscslendF, resources);

			System.out.println("\n\n");
			System.out.println(" RESOURCE Length FIT");
			ArrayList<ArrayList<SporadicTask>> taskscsleniF = allocGeneator.allocateTasks(tasks, resources, geneator.total_partitions, 7);
			geneator.PrintAllocatedSystem(taskscsleniF, resources);

			System.err.println("\n\n " + j + " \n\n");

			System.out.println((int) Math.round(0.4));
		}

	}

}
