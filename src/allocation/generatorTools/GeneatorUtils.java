package allocation.generatorTools;

public class GeneatorUtils {
	
	/* define how long the critical section can be */
	public static enum CS_LENGTH_RANGE {
		LONG_CSLEN, MEDIUM_CS_LEN, RANDOM, SHORT_CS_LEN, VERY_LONG_CSLEN, VERY_SHORT_CS_LEN
	};

	/* define how many resources in the system */
	public static enum RESOURCES_RANGE {
		DOUBLE_PARTITIONS, /* partitions / 2 us */
		HALF_PARITIONS, /* partitions us */
		PARTITIONS, /* partitions * 2 us */
	};

}
