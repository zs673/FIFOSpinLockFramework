package test;

import java.util.ArrayList;
import java.util.Random;

public class SortTest {

	public static void main(String[] args) {
		Random r1 = new Random(Long.MAX_VALUE);
		Random r2 = new Random(Long.MAX_VALUE);

		ArrayList<Integer> array1 = new ArrayList<>();
		ArrayList<Integer> array2 = new ArrayList<>();

		for (int i = 0; i < 100; i++) {
			int a = r1.nextInt(65535);
			int b = r2.nextInt(65535);
			array1.add(a);
			array2.add(b);
		}

		System.out.println(array1.toString());
		System.out.println(array2.toString());

	}

}
