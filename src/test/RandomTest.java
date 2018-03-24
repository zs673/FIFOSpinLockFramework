package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTest {

	static Random ran = new Random(System.currentTimeMillis());

	public static void main(String[] args) {
		randomTest();
	}

	public static void randomTest() {
		ArrayList<Integer> ints = new ArrayList<>();

		for (int i = 0; i < 1000; i++) {
			ints.add(ThreadLocalRandom.current().nextInt(3, 6));
		}

		for (int i = 0; i < ints.size(); i++)
			System.out.println(ints.get(i));
	}

	public static void random() {
		// TODO Auto-generated method stub
		ArrayList<Integer> array = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			array.add(new Random().nextInt(10000));
		}

		array.sort((p1, p2) -> Double.compare(p1, p2));
		System.out.println(Arrays.toString(array.toArray()));

		array.sort((p1, p2) -> Double.compare(p2, p1));
		System.out.println(Arrays.toString(array.toArray()));

		long[][] response_time = new long[10][];
		for (int i = 0; i < response_time.length; i++) {
			response_time[i] = new long[10];
		}

		System.out.println(Arrays.deepToString(response_time));

	}
}
