package evaluation;

import java.util.ArrayList;
import java.util.Random;

import analysis.CombinedAnalysis;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.AllocationGeneator;
import generatorTools.SystemGenerator;
import utils.GeneatorUtils.CS_LENGTH_RANGE;
import utils.GeneatorUtils.RESOURCES_RANGE;

public class NewOPATest {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	static int NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE = 3;
	static int NUMBER_OF_TASKS_ON_EACH_PARTITION = 4;

	static CS_LENGTH_RANGE range = CS_LENGTH_RANGE.SHORT_CS_LEN;
	static double RESOURCE_SHARING_FACTOR = 0.2;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 100000;
	public static int TOTAL_PARTITIONS = 16;

	public static void main(String[] args) throws Exception {
		NewOPATest test = new NewOPATest();
		test.experimentIncreasingCriticalSectionLength(6);
	}

	public void experimentIncreasingCriticalSectionLength(int cs_len) {
		final CS_LENGTH_RANGE cs_range;
		switch (cs_len) {
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

		SystemGenerator generator = new SystemGenerator(MIN_PERIOD, MAX_PERIOD, true, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NUMBER_OF_TASKS_ON_EACH_PARTITION,
				RESOURCE_SHARING_FACTOR, cs_range, RESOURCES_RANGE.PARTITIONS, NUMBER_OF_MAX_ACCESS_TO_ONE_RESOURCE, false);

		CombinedAnalysis combined = new CombinedAnalysis();

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			generator.generateResourceUsage(tasksToAlloc, resources);
			ArrayList<ArrayList<SporadicTask>> tasks = new AllocationGeneator().allocateTasks(tasksToAlloc, resources, generator.total_partitions, 0);

			for (int k = 0; k < resources.size(); k++) {
				resources.get(k).protocol = new Random().nextInt(65535) % 3 + 1;
			}
			combined.getResponseTimeBySBPO(tasks, resources, false);

			System.out.println(2 + " " + 1 + " " + cs_len + " times: " + i);
		}

	}

}
