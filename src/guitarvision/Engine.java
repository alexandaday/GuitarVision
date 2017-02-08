package guitarvision;

import java.io.File;
import java.util.ArrayList;

import guitarvision.detection.EdgeDetector;
import guitarvision.detection.FretDetector;
import guitarvision.detection.GuitarString;
import guitarvision.detection.ImageProcessingOptions;
import guitarvision.detection.DetectedLine;
import guitarvision.detection.NoteDetector;
import guitarvision.detection.PluckDetector;
import guitarvision.detection.SkinDetector;
import guitarvision.detection.StringDetector;
import guitarvision.sheetmusic.MusicNote;
import guitarvision.sheetmusic.SheetMusic;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

public class Engine {
	private static Engine instance = null;
	
	public static Engine getInstance()
	{
		if (instance == null)
		{
			instance = new Engine();
		}
		return instance;
	}
	
	public void exportImage(Mat image, String fileName)
	{
		//System.out.println("Exporting: " + fileName);
		
		Imgcodecs.imwrite(fileName, image);
	}
	
	public void processVideo(File videoFile)
	{
		VideoCapture guitarVideo = new VideoCapture(videoFile.getPath());
		
		Mat currentFrame = new Mat();
		
		String fileName = videoFile.getName();
		String name = fileName.substring(0, fileName.lastIndexOf("."));
		String extension = fileName.substring(fileName.lastIndexOf("."));
		
		double width = guitarVideo.get(Videoio.CAP_PROP_FRAME_WIDTH);
		double height = guitarVideo.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		double fps = guitarVideo.get(Videoio.CAP_PROP_FPS);
		double codecToUse = guitarVideo.get(Videoio.CAP_PROP_FOURCC);
		
		String outputFileName = name +"_processed"+extension;
		
		File outputFile = new File(outputFileName);
		
		if (outputFile.exists()) outputFile.delete();
		
		VideoWriter outputVideo = new VideoWriter(outputFileName, (int)codecToUse, fps, new Size(width,height), true);
		
		int frameNo = 0;
		
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setCannyUpperThreshold(200);
		edgeDetector.setHoughThreshold(300);
		
		EdgeDetector fretEdgeDetector = new EdgeDetector();
		fretEdgeDetector.setCannyLowerThreshold(0);
		fretEdgeDetector.setCannyUpperThreshold(255);
		fretEdgeDetector.setHoughThreshold(75);
		
		StringDetector stringDetector = new StringDetector();
		
		FretDetector fretDetector = new FretDetector();
		
		PluckDetector pluckDetector = null;
		
		SkinDetector skinDetector = new SkinDetector();
		
		NoteDetector noteDetector = new NoteDetector();
		
		
		SheetMusic transcribedMusic = new SheetMusic();
//		
//		Store Thickness of string in Polar line class - maybe rename?
		
		boolean firstFrame = true;
		
		ArrayList<MusicNote> currentlyHeldNotes = new ArrayList<MusicNote>();
		
		for(int x = 0; x < StringDetector.numberStringsToDetect; x++)
		{
			currentlyHeldNotes.add(null);
		}
		
		Mat frameToAnnotate;
		
		//Process remaining frames
		while (guitarVideo.read(currentFrame))
		{
			frameToAnnotate =  currentFrame.clone();
			
			ArrayList<GuitarString> guitarStrings = stringDetector.getGuitarStrings(currentFrame, frameToAnnotate, edgeDetector, ImageProcessingOptions.DRAWCLUSTERS);
			ArrayList<DetectedLine> guitarFrets = fretDetector.getGuitarFrets(currentFrame, frameToAnnotate, guitarStrings, fretEdgeDetector, ImageProcessingOptions.DRAWCLUSTERS);

			if (!firstFrame)
			{
				//Recallibrate if better frame
				if (pluckDetector.initialStrings.size() == 0 && guitarStrings.size() == StringDetector.numberStringsToDetect)
				{
					pluckDetector.initialStrings = guitarStrings;
				}
				
				Mat skin = skinDetector.getSkin(currentFrame);
				
				
				Core.addWeighted(frameToAnnotate, 0.6, skin, 0.4, 1.0, frameToAnnotate);
				
				boolean[] stringsPlayed = pluckDetector.detectStringsBeingPlayed(guitarStrings);
				
				if (stringsPlayed != null)
				{	
					for(int x = 0; x < stringsPlayed.length; x++)
					{
						if (stringsPlayed[x] && currentlyHeldNotes.get(x) == null)
						{
							//create note

							MusicNote notePlayed = noteDetector.getNote(currentFrame, frameNo, skin, x, guitarStrings, guitarFrets);

							currentlyHeldNotes.set(x, notePlayed);
						}
						else if (!stringsPlayed[x] && currentlyHeldNotes.get(x) != null)
						{
							//finish note
							MusicNote currentNote = currentlyHeldNotes.get(x);

							currentNote.setEndingFrame(frameNo);

							transcribedMusic.addNote(currentNote);

							currentlyHeldNotes.set(x, null);
						}
					}
				}
				
				int scaleFactor = 12;
				if (stringsPlayed == null) stringsPlayed = new boolean[StringDetector.numberStringsToDetect];
				
				for(int x = 0; x < StringDetector.numberStringsToDetect; x++)
				{
					int fret = 0;
					if (currentlyHeldNotes.get(x) != null)
					{
						MusicNote note = currentlyHeldNotes.get(x);
						fret = note.note;
					}

					Imgproc.putText(frameToAnnotate, "String "+Integer.toString(x)+": " + stringsPlayed[x] , new Point((currentFrame.rows() / scaleFactor) * 1 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 2.0, new Scalar(255,255,255), 2);
					Imgproc.putText(frameToAnnotate, "Note : " + fret , new Point((currentFrame.rows() / scaleFactor) * 8 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 2.0, new Scalar(255,255,255), 2);
					
				}
			}
			else
			{
				//First frame initialisation
				pluckDetector = new PluckDetector(guitarStrings);
				firstFrame = false;
			}

			outputVideo.write(frameToAnnotate);
			frameNo++;
			if (frameNo >= 350) break;
		}
		
		guitarVideo.release();
		outputVideo.release();
		
		transcribedMusic.writeFile("test");
		
		//Process first frame
		//Using altering canny upper and hough threshold
		//Each time calculate mode separation of strings, and number of pairs with this mode
		//change thresholds to maximum number of pairs with mode
		
		//Then then interpolate for missing strings
		
		System.out.println("DONE");
	}
	
	//Variables and Methods for development/testing
	
	private String fileName = "../images/guitar.png";
	
	public Mat getOriginalImage()
	{	
//		Mat image = Imgcodecs.imread(getClass().getResource("../images/plucking.png").getPath());
//		
//		//Skin Classifier
//		
//		SkinDetector skinDetector = new SkinDetector();
//		
//		return skinDetector.getSkin(image);
		
		return getProcessedImage(100, 255, false);
		
	}
	
	public Mat getProcessedImage(int argument, int argument2, boolean showEdges)
	{	
		Mat imageToProcess = Imgcodecs.imread(getClass().getResource(fileName).getPath());
		
		Mat imageToAnnotate = imageToProcess.clone();
		
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setCannyUpperThreshold(argument);
		edgeDetector.setHoughThreshold(argument2);
		
		if (showEdges)
		{
			return edgeDetector.getEdges(imageToProcess);
		}
		else
		{
			StringDetector stringDetector = new StringDetector();
			
			ArrayList<GuitarString> guitarStrings = stringDetector.getGuitarStrings(imageToProcess, imageToAnnotate, edgeDetector, ImageProcessingOptions.NOPROCESSING);
			
			FretDetector fretDetector = new FretDetector();
			EdgeDetector fretEdgeDetector = new EdgeDetector();
			fretEdgeDetector.setCannyLowerThreshold(0);
			fretEdgeDetector.setCannyUpperThreshold(255);
			fretEdgeDetector.setHoughThreshold(75);
			
			ArrayList<DetectedLine> guitarFrets = fretDetector.getGuitarFrets(imageToProcess, imageToAnnotate, guitarStrings,fretEdgeDetector,ImageProcessingOptions.NOPROCESSING);
			
			SkinDetector skinDetector = new SkinDetector();
			
			Mat skin = skinDetector.getSkin(imageToProcess);
			
			Core.addWeighted(imageToProcess, 0.6, skin, 0.4, 1.0, imageToProcess);
			
			NoteDetector noteDetector = new NoteDetector();
			
			for (int x = 0; x < StringDetector.numberStringsToDetect; x++)
			{
				MusicNote notePlayed = noteDetector.getNote(imageToProcess, 0, skin, x, guitarStrings, guitarFrets);
				
				if (notePlayed != null)
				{
					System.out.println("Detected note string:");
					System.out.println(x);
					System.out.println("Note is:");
					System.out.println(notePlayed.note);
				}
			}
			
		}
		return imageToProcess;
	}
}
