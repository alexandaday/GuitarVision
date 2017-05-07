package guitarvision;

//import java.util.ArrayList;

import guitarvision.gui.UserInterface;
//import guitarvision.sheetmusic.ObjectAlignment;

import org.opencv.core.Core;

public class Main
{
	public static void main(String[] args)
	{
		//Load OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.out.println("OpenCV Loaded");
		
//		ArrayList<Byte> pitches1 = new ArrayList<Byte>();
//		ArrayList<Byte> pitches2 = new ArrayList<Byte>();
//		
//		pitches1.add(new Byte((byte) 65));
//		pitches1.add(new Byte((byte) 24));
//		pitches1.add(new Byte((byte) 53));
//		pitches1.add(new Byte((byte) 65));
//		
//		pitches2.add(new Byte((byte) 64));
//		pitches2.add(new Byte((byte) 64));
//		
//		System.out.println(new Byte((byte) 65).equals(new Byte((byte) 65)));
//		
//		ObjectAlignment<Byte> alignPieces = new ObjectAlignment<Byte>(pitches1, pitches2);
//		
//		alignPieces.setMatchScore(1);
//		alignPieces.setMismatchPenalty(0);
//		alignPieces.setInsertDeletePenalty(0);
//		
//		byte val = 5;
//		alignPieces.setMatchTolerance(new Byte(val));
//		System.out.println("SCORE");
//		System.out.println(alignPieces.computeLongestMatchScore());
//		
//		
//		
		//Launch GUI
		UserInterface.launchInterface(args);
	}
}