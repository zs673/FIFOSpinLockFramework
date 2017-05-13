package geneticAlgoritmSolver;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class StaticSolver {

	public static int[] accessRange = { 1, 30 };
	public static int[] periodRange = { 1, 1000 };
	public static int[] processorRange = { 2, 16 };
	public static int[] resourceLengthRange = { 1, 300 };
	public static double[] RSF = { 0.2, 0.6 };
	public static int[] tasksRange = { 1, 10 };

	public int[] solve(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, int NoP, int NoA, boolean print) {
		int[] protocols = new int[resources.size()];
		for (int i = 0; i < resources.size(); i++) {
			Resource res = resources.get(i);
			int protocol = 0;

			if (res.csl <= 60)
				protocol = 1;
			else if (res.csl < 70) {
				if (NoA < 2)
					protocol = 2;
				else
					protocol = 1;
			} 
			
			else if (res.csl <= 100) {
				if (NoP <= 7 || NoA > 13)
					protocol = 1;
				else if (NoA <= 3)
					protocol = 2;
				else
					protocol = 3;
			} 
			
			
			else if (res.csl <= 150){
				if (NoA < 2)
					protocol = 2;
				else
					protocol = 3;
			}	
			else
				protocol = 3;

			if (protocol != 0)
				protocols[i] = protocol;
			else {
				System.err.println("protocol error: " + protocol);
				System.exit(-1);
			}
		}
		return protocols;
	}

}
