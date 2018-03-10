package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ResultReader {

	public static void read(String foldername) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(foldername + "/" + foldername + ".txt"), true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(foldername);

		for (int smallSet = 0; smallSet < 1001; smallSet++) {
			String filepath = foldername + "/" + "1 2 " + smallSet + ".txt";

			List<String> lines = null;
			try {
				lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
			} catch (IOException e) {
			}
			if (lines != null) {
				String result = smallSet + " " + lines.get(0);
				writer.println(result);
			}

		}

		writer.close();
		System.out.println("Crossover Reading Finished ");
	}

	public static void schedreader(String foldername) {
		String result = "Work Load \n";
		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 10; smallSet++) {
				String filepath = foldername + "/" + "1" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n CS Length \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 10; smallSet++) {
				String filepath = foldername + "/" + "2" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n Resource Access \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 42; smallSet++) {
				String filepath = foldername + "/" + "3" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n Parallelism \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 42; smallSet++) {
				String filepath = foldername + "/" + "4" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}

		System.out.println(result);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(foldername + "/" + "all.txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

	public static void schedreader() {
		String result = "Work Load \n";
		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 10; smallSet++) {
				String filepath = "result/" + "1" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n CS Length \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 10; smallSet++) {
				String filepath = "result/" + "2" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n Resource Access \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 42; smallSet++) {
				String filepath = "result/" + "3" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}
		result += "\n \n Parallelism \n";

		for (int bigSet = 1; bigSet < 10; bigSet++) {

			for (int smallSet = 1; smallSet < 42; smallSet++) {
				String filepath = "result/" + "4" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}

			result += "\n";

		}

		System.out.println(result);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("result/all.txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

}
