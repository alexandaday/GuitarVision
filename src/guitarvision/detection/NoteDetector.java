package guitarvision.detection;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import guitarvision.sheetmusic.MusicNote;

/**
 * @author Alex Day
 * Detection class which determines which note is being played given a vibrating string, a set of frets and detected skin.
 */
public class NoteDetector {
	
	/**
	 * Get an object containing the note being played
	 * @param copy of image to annotate frets which are being played
	 * @param current frame number in video to store with note object
	 * @param copy of image with only skin pixels set
	 * @param number of the string which is vibrating, to store with the note object
	 * @param list of detected guitar strings
	 * @param list of detected guitar frets
	 * @return object containing the note being played
	 */
	public MusicNote getNote(Mat frameToAnnotate, int frameNumber, Mat skin, int vibratingStringNo, ArrayList<GuitarString> guitarStrings, ArrayList<DetectedLine> guitarFrets)
	{
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
		Scalar colourOverlap = new Scalar(255,255,255);
		
		Scalar curColour;
		
		for(int x = 0; x < guitarFrets.size() - 1; x++)
		{
			DetectedLine currentFret = guitarFrets.get(x);
			DetectedLine nextFret = guitarFrets.get(x+1);
			
			Point startPoint = string.getCollisionPoint(currentFret);
			Point endPoint = string.getCollisionPoint(nextFret);
			
			if (alternateColours == 0)
			{
				curColour = colour1;
			}
			else if (alternateColours == 1)
			{
				curColour = colour2;
			}
			else
			{
				curColour = colour3;
			}

			alternateColours = (alternateColours + 1) % 3;
			
			overlapping = SkinDetector.fretOverlapSkin(skin, startPoint, endPoint);
			
			if (overlapping)
			{
				startedOverlapping = true;
				Imgproc.line(frameToAnnotate, startPoint, endPoint, colourOverlap);
			}
			else
			{
				Imgproc.line(frameToAnnotate, startPoint, endPoint, curColour);
			}
			
			if (startedOverlapping && !overlapping)
			{
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
