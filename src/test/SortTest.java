package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class SortTest {

	public static void main(String[] args) {
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
