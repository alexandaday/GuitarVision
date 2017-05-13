package guitarvision;

//import java.util.ArrayList;

import guitarvision.gui.UserInterface;
//import guitarvision.sheetmusic.ObjectAlignment;

import guitarvision.sheetmusic.MusicNote;
import guitarvision.sheetmusic.SheetMusic;

import org.opencv.core.Core;

public class Main
{
	public static void main(String[] args)
	{
		//Load OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.out.println("OpenCV Loaded");
		
//		SheetMusic sheetMusic = new SheetMusic();
//		
//		MusicNote note1 = new MusicNote(0, 4, 0, 48);
//		note1.setEndingFrame(48 + 24);
//		MusicNote note2 = new MusicNote(0, 5, 0, 48+24);
//		note2.setEndingFrame(48 + 24 + 48);
//		
//		MusicNote note3 = new MusicNote(2, 7, 0, 48+48);
//		note3.setEndingFrame(48 + 24 + 48 + 48);
//		
//		MusicNote note4 = new MusicNote(2, 7, 0, 48+(24 * 8));
//		note4.setEndingFrame(48+(24 * 8)+ (24*8));
//		
//		
//		sheetMusic.addNote(note1);
//		sheetMusic.addNote(note2);
//		sheetMusic.addNote(note3);
//		sheetMusic.addNote(note4);
//		
//		sheetMusic.writeFile("miditest");
//		
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