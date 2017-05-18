package guitarvision;

import guitarvision.gui.UserInterface;

import org.opencv.core.Core;

public class Main
{
	public static void main(String[] args)
	{
		//Load OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.out.println("OpenCV Loaded");
		
		//Launch GUI
		UserInterface.launchInterface(args);
	}
}