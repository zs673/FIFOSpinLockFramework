package generatorTools;

public class GeneatorUtils {

	/* define how long the critical section can be */
	public static enum CS_LENGTH_RANGE {
		EXTREME_SHORT_CSLEN, VERY_SHORT_CS_LEN, SHORT_CS_LEN, MEDIUM_CS_LEN, LONG_CSLEN, VERY_LONG_CSLEN, EXTREME_LONG_CSLEN, RANDOM
	};

	/* define how many resources in the system */
	public static enum RESOURCES_RANGE {
		HALF_PARITIONS, /* partitions us */
		PARTITIONS, /* partitions * 2 us */
		DOUBLE_PARTITIONS, /* partitions / 2 us */
	};

	/* define how long the critical section can be */
	public static enum ALLOCATION_POLICY {
		FIRST_FIT, BEST_FIT, WORST_FIT, NEXT_FIT, RESOURCE_REQUEST_TASKS_FIT, RESOURCE_LENGTH_DECREASE_FIT, RESOURCE_LOCAL_FIT, RESOURCE_LENGTH_INCREASE_FIT
	};
}
