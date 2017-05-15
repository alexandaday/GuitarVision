package guitarvision.detection;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import guitarvision.sheetmusic.MusicNote;

public class NoteDetector {
	public NoteDetector()
	{
		
	}
	
	public MusicNote getNote(Mat frame, Mat frameToAnnotate, int frameNumber, Mat skin, int vibratingStringNo, ArrayList<GuitarString> guitarStrings, ArrayList<DetectedLine> guitarFrets)
	{
		//Return open string for me
		//MusicNote note = new MusicNote(0, 4, 2, frameNumber);
		
//		System.out.println("STRING IS");
//		System.out.println(vibratingStringNo);
		
		if (guitarStrings.size() <= vibratingStringNo)
		{
			return null;
		}
		
		GuitarString string = guitarStrings.get(vibratingStringNo);
		
		boolean startedOverlapping = false;
		boolean overlapping;
		
		int fretPlaying = 0;
		
		int alternateColours = 0;
		
		Scalar colour1 = new Scalar(255,0,0);
		Scalar colour2 = new Scalar(0,255,0);
		Scalar colour3 = new Scalar(0,0,255);
		Scalar colour4 = new Scalar(255,255,255);
		
		Scalar colour;
		
		for(int x = 0; x < guitarFrets.size() - 1; x++)
		{
			DetectedLine currentFret = guitarFrets.get(x);
			DetectedLine nextFret = guitarFrets.get(x+1);
			
			Point startPoint = string.getCollisionPoint(currentFret);
			Point endPoint = string.getCollisionPoint(nextFret);
			
			if (alternateColours == 0)
			{
				colour = colour1;
			}
			else if (alternateColours == 1)
			{
				colour = colour2;
			}
			else
			{
				colour = colour3;
			}

			alternateColours = (alternateColours + 1) % 3;
			
			overlapping = SkinDetector.fretOverlapSkin(skin, startPoint, endPoint);
			
			if (overlapping)
			{
				startedOverlapping = true;
//				System.out.println("Overlapping on fret");
//				System.out.println(x);
				Imgproc.line(frameToAnnotate, startPoint, endPoint, colour4);
			}
			else
			{
				Imgproc.line(frameToAnnotate, startPoint, endPoint, colour);
			}
			if (startedOverlapping && !overlapping)
			{
//				System.out.println("Drawing");
				fretPlaying = x;
				
				break;
			}
		}
		
		int note;
		int octave;
		
		switch(vibratingStringNo)
		{
		case 0:
			note = 4;
			octave = 4;
			break;
		case 1:
			note = 9;
			octave = 4;
			break;
		case 2:
			note = 2;
			octave = 5;
			break;
		case 3:
			note = 7;
			octave = 5;
			break;
		case 4:
			note = 11;
			octave = 5;
			break;
		case 5:
			note = 4;
			octave = 6;
			break;
		default:
			note = 0;
			octave = 0;
			break;
		}
		
		int additionalNotes = (fretPlaying % 12);
		int additionalOctaves = Math.floorDiv(fretPlaying, 12);
		
		note = (note + additionalNotes) % 12;
		octave += additionalOctaves;
		
		MusicNote musicNote = new MusicNote(note, octave, vibratingStringNo, frameNumber);
		
		
		return musicNote;
	}
}
