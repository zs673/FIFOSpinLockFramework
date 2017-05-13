package test;

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

public class IOAResultReader {

	public static void main(String[] args) {

		schedreader(null,false);
	}

	public static void schedreader(String title, boolean append) {
		String result = null;
		
		if(title != null)
			result+= title +"\n";
		
		result+= "Work Load\n";
		
		for (int bigSet = 1; bigSet < 6; bigSet++) {
			for (int smallSet = 1; smallSet < 11; smallSet++) {
				String filepath = "result/" + "ioa 1" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}
		}

		result += "CS Length \n";
		for (int bigSet = 1; bigSet < 6; bigSet++) {
			for (int smallSet = 1; smallSet < 301; smallSet++) {
				String filepath = "result/" + "ioa 2" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}
		}

		result += "Resource Access \n";
		for (int bigSet = 1; bigSet < 6; bigSet++) {
			for (int smallSet = 1; smallSet < 32; smallSet++) {
				String filepath = "result/" + "ioa 3" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}
		}

		result += "Parallel \n";
		for (int bigSet = 1; bigSet < 31; bigSet++) {
			for (int smallSet = 2; smallSet < 33; smallSet++) {
				String filepath = "result/" + "ioa 4" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null)
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}
		}

		result += "RSF \n";
		for (int bigSet = 1; bigSet < 100; bigSet++) {
			for (int smallSet = 1; smallSet < 100; smallSet++) {
				String filepath = "result/" + "ioa 5" + " " + bigSet + " " + smallSet + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}

				if (lines != null && !lines.isEmpty())
					result += bigSet + "" + smallSet + " " + lines.get(0) + "\n";
			}
		}
		result += "\n\n\n";

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("result/all.txt"), append));
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
