package javaToC;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;

public class MIPSolverC {
	/** test methods **/
	public native void helloFromC();

	public native long solveMIP(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, int taskSize, boolean isPreemptable);

	static {
		System.loadLibrary("mipsolverc");
	}
}
