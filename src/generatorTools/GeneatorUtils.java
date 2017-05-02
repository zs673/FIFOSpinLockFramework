package generatorTools;

public class GeneatorUtils {
	
	/* define how long the critical section can be */
	public static enum CS_LENGTH_RANGE {
		VERY_LONG_CSLEN, LONG_CSLEN, MEDIUM_CS_LEN, SHORT_CS_LEN, VERY_SHORT_CS_LEN, RANDOM
	};

	/* define how many resources in the system */
	public static enum RESOURCES_RANGE {
		HALF_PARITIONS, /* partitions / 2 us */
		PARTITIONS, /* partitions us */
		DOUBLE_PARTITIONS, /* partitions * 2 us */
	};

}
