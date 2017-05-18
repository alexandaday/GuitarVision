package guitarvision;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

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

/**
 * @author Alex Day
 * Core functionality of AMT system
 * Singleton class
 */
public class Engine {
	private static Engine instance = null;
	
	public final Size processingResolution = new Size(960,540);
	
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
		Imgcodecs.imwrite(fileName, image);
	}
	
	public String intToMusicalNote(int value)
	{
		switch(value)
		{
		case 0:
			return "C";
		case 1:
			return "C#";
		case 2:
			return "D";
		case 3:
			return "D#";
		case 4:
			return "E";
		case 5:
			return "F";
		case 6:
			return "F#";
		case 7:
			return "G";
		case 8:
			return "G#";
		case 9:
			return "A";
		case 10:
			return "A#";
		case 11:
			return "B";
		default:
			return Integer.toString(value);
		}
	}

	/**
	 * The core method of the project, produces MIDI transcriptions from guitar videos
	 * @param videoFile - file object pointing to the video to be processed
	 * @param numberFramesToProcess - the number of frames from the video to process, starting from frame 0
	 * @param outputDirectoryName - path of directory to write the output files to
	 * @param firstNoteDuration - number of ticks that the first note detected lasts (four ticks is one musical beat)
	 * @param poorLighting - whether to use a lower Canny upper threshold due to a scene with poor lighting
	 * @param writeAnnotatedVideo - whether to write the output annotated video file
	 * @return instance of OutputFileReferences containing file object pointing to the generated output files
	 */
	public OutputFileReferences transcribeFromVideo(File videoFile, Integer numberFramesToProcess, String outputDirectoryName, int firstNoteDuration, boolean poorLighting, boolean writeAnnotatedVideo)
	{
		VideoCapture guitarVideo = new VideoCapture(videoFile.getPath());
		
		Mat currentFrame = new Mat();
		Mat frameToAnnotate = null;
		
		String fileName = videoFile.getName();
		String name = fileName.substring(0, fileName.lastIndexOf("."));
		String extension = fileName.substring(fileName.lastIndexOf("."));

		double width = processingResolution.width;
		double height = processingResolution.height;
		double fps = guitarVideo.get(Videoio.CAP_PROP_FPS);
		double codecToUse = guitarVideo.get(Videoio.CAP_PROP_FOURCC);

		//Determine the names and relative paths of the output files
		String outputVideoFileNameWithExtension = "";
		String outputMidiFileName = "";
		
		if (!(outputDirectoryName == null))
		{
			 outputVideoFileNameWithExtension += outputDirectoryName + java.io.File.separator;
			 outputMidiFileName += outputDirectoryName + java.io.File.separator;
		}

		outputVideoFileNameWithExtension += name + "_processed"+extension;
		outputMidiFileName += name;
		
		File outputFile = new File(outputVideoFileNameWithExtension);
		
		if (outputFile.exists()) outputFile.delete();
		
		VideoWriter outputVideo = null;
		
		if (writeAnnotatedVideo)
		{
			outputVideo = new VideoWriter(outputVideoFileNameWithExtension, (int)codecToUse, fps, new Size(width,height), true);
		}
		
		int frameNo = 0;
		
		//Set up the required detection objects
		EdgeDetector edgeDetector = new EdgeDetector();
		if (poorLighting)
		{
			edgeDetector.setCannyUpperThreshold(32);
		}
		else 
		{
			edgeDetector.setCannyUpperThreshold(70);
		}
		
		edgeDetector.setHoughThreshold(300);

		StringDetector stringDetector = new StringDetector();
		
		FretDetector fretDetector = new FretDetector();

		PluckDetector pluckDetector = new PluckDetector();
		
		SkinDetector skinDetector = new SkinDetector();
		
		NoteDetector noteDetector = new NoteDetector();
		
		SheetMusic transcribedMusic = new SheetMusic();
		
		if (numberFramesToProcess == null)
		{
			numberFramesToProcess = 1000;
		}

		boolean firstFrame = true;
		
		ArrayList<MusicNote> currentlyHeldNotes = new ArrayList<MusicNote>();
		
		for(int x = 0; x < stringDetector.getNumberOfStringsToDetect(); x++)
		{
			currentlyHeldNotes.add(null);
		}
		
		ArrayList<GuitarString> previousStrings = null;
		ArrayList<DetectedLine> previousFrets = null;
		
		boolean firstNote = true;
		
		//Process frames one by one
		while (guitarVideo.read(currentFrame))
		{
			//Resize image
			Imgproc.resize(currentFrame, currentFrame, processingResolution);
			
			frameToAnnotate =  currentFrame.clone();
			
			//Detect strings and frets in frame
			ArrayList<GuitarString> guitarStrings = stringDetector.getGuitarStrings(currentFrame, frameToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.DRAWSELECTEDLINES);

			previousStrings = guitarStrings;

			EdgeDetector fretEdgeDetector = new EdgeDetector();
			fretEdgeDetector.setCannyLowerThreshold(0);
			fretEdgeDetector.setCannyUpperThreshold(250);
			fretEdgeDetector.setHoughThreshold(70);

			ArrayList<DetectedLine> guitarFrets = fretDetector.getGuitarFrets(currentFrame, frameToAnnotate, guitarStrings, fretEdgeDetector, previousFrets, ImageProcessingOptions.DRAWSELECTEDLINES);
			
			Collections.reverse(guitarFrets);
			
			//Extract string thicknesses and store them in the string objects
			pluckDetector.getStringThicknesses(guitarStrings, guitarFrets, currentFrame);
			
			previousFrets = guitarFrets;

			//Detect skin
			Mat skin = skinDetector.getSkin(currentFrame);
			
			Core.addWeighted(frameToAnnotate, 0.8, skin, 0.2, 1.0, frameToAnnotate);
			
			//Plucking and note detection for frames after the first frame
			if (!firstFrame)
			{
				//Re-calibrate pluck detector if previous frames didn't detect all guitar strings
				if (pluckDetector.getInitialStrings().size() < stringDetector.getNumberOfStringsToDetect() && guitarStrings.size() > pluckDetector.getInitialStrings().size())
				{
					pluckDetector.setInitialStrings(guitarStrings);
				}
				
				//Detect which strings are vibrating
				boolean[] stringsPlayed = pluckDetector.getVibratingStrings(guitarStrings);
				
				
				if (stringsPlayed != null)
				{	
					for(int x = 0; x < stringsPlayed.length && x < currentlyHeldNotes.size(); x++)
					{
						if (stringsPlayed[x] && currentlyHeldNotes.get(x) == null)
						{
							//Create new note object
							MusicNote notePlayed = noteDetector.getNote(frameToAnnotate, frameNo, skin, x, guitarStrings, guitarFrets);

							currentlyHeldNotes.set(x, notePlayed);
						}
						else if (!stringsPlayed[x] && currentlyHeldNotes.get(x) != null)
						{
							//Finish note as the string is no longer vibrating and add to sheet music
							MusicNote currentNote = currentlyHeldNotes.get(x);

							currentNote.setEndingFrame(frameNo);
							
							//Use the first note to determine the relationship between number of frames and number of music ticks
							//given the specified number of ticks of the first note
							if (firstNote == true)
							{
								int initialFrame = currentNote.startingFrame;
								int frameDuration = currentNote.getEndingFrame() - initialFrame;
								int framesInTick = (int) Math.round((double) frameDuration / (double) firstNoteDuration);
								if (framesInTick < 1) framesInTick = 1;
								transcribedMusic.setInitialFrame(initialFrame);
								transcribedMusic.setFramesPerBeat(framesInTick);
							}
							
							firstNote = false;
							transcribedMusic.addNote(currentNote);

							currentlyHeldNotes.set(x, null);
						}
					}
				}

				//Annotate frame to show which notes are currently detected
				//Display which fret is held down for each string even if it is not vibrating
				if (writeAnnotatedVideo)
				{
					int scaleFactor = 12;
					if (stringsPlayed == null) stringsPlayed = new boolean[stringDetector.getNumberOfStringsToDetect()];

					
					MusicNote currentNote;
					for(int x = 0; x < stringDetector.getNumberOfStringsToDetect(); x++)
					{
						String currentSemitone = "NONE";
						
						if (currentlyHeldNotes.get(x) != null)
						{
							currentNote = currentlyHeldNotes.get(x);
							currentSemitone = intToMusicalNote(currentNote.note);
						}
						else
						{
							Mat nonVibratingAnnotations = currentFrame.clone();
							MusicNote currentFretNote = noteDetector.getNote(nonVibratingAnnotations, frameNo, skin, x, guitarStrings, guitarFrets);
							if (!(currentFretNote == null))
							{
								currentSemitone = intToMusicalNote(currentFretNote.note);
							}
						}
						
						
						boolean beingPlayed = false;
						if (x < stringsPlayed.length)
						{
							beingPlayed = stringsPlayed[x];
						}

						Imgproc.putText(frameToAnnotate, "String "+Integer.toString(x)+": " + beingPlayed , new Point((currentFrame.rows() / scaleFactor) * 1 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 1.0, new Scalar(255,255,255), 2);
						Imgproc.putText(frameToAnnotate, "Note : " + currentSemitone , new Point((currentFrame.rows() / scaleFactor) * 8 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 1.0, new Scalar(255,255,255), 2);
					}
				}
			}
			//Initialise plucking detection from the strings in the first frame
			else
			{
				pluckDetector.setInitialStrings(guitarStrings);
				firstFrame = false;
			}
			
			if (writeAnnotatedVideo)
			{
				outputVideo.write(frameToAnnotate);
			}
			
			frameNo++;
			if ((numberFramesToProcess!= null) && (frameNo >= numberFramesToProcess)) break;
			
			//Skip every other frame to reduce processing time
			guitarVideo.read(currentFrame);
		}
		
		guitarVideo.release();
		if (writeAnnotatedVideo)
		{
			outputVideo.release();
		}
		
		File midiFile = transcribedMusic.writeFile(outputMidiFileName);
		
		//Return references to the created files
		OutputFileReferences results = new OutputFileReferences(outputFile, midiFile);
		
		System.out.println("Processing Video Complete");
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		System.out.println(format.format(date));
		
		return results;
	}
	
	//Methods for development and testing
	
	public Mat getProcessedImage(int argument, int argument2, boolean showEdges, String filePath)
	{
		//Perform string, fret and skin detection on the image
		File file = new File(filePath);
		
		Mat imageToProcess = Imgcodecs.imread(file.getPath());

		Imgproc.resize(imageToProcess, imageToProcess, processingResolution);

		Mat imageToAnnotate = imageToProcess.clone();
				
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setCannyUpperThreshold(argument);
		edgeDetector.setHoughThreshold(argument2);

		ArrayList<GuitarString> previousStrings = null;
		
		if (showEdges)
		{
			return edgeDetector.getEdges(imageToProcess);
		}
		else
		{
			StringDetector stringDetector = new StringDetector();

			ArrayList<GuitarString> guitarStrings = null;
			
			guitarStrings = stringDetector.getGuitarStrings(imageToProcess, imageToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.DRAWSELECTEDLINES);
			
			previousStrings = guitarStrings;

			if (guitarStrings.size() == 0) return imageToAnnotate;

			FretDetector fretDetector = new FretDetector();
			EdgeDetector fretEdgeDetector = new EdgeDetector();
			fretEdgeDetector.setCannyLowerThreshold(0);
			fretEdgeDetector.setCannyUpperThreshold(250);
			fretEdgeDetector.setHoughThreshold(70);

			fretDetector.getGuitarFrets(imageToProcess, imageToAnnotate, guitarStrings, fretEdgeDetector, null, ImageProcessingOptions.DRAWSELECTEDLINES);
		}
		
		return imageToAnnotate;
	}
	
	public void printMatrix(Mat mat)
	{
		System.out.print("Matrix ("+mat.height()+"x"+mat.width()+"):");
		for(int x = 0; x < mat.height(); x++)
		{
			for(int y = 0; y < mat.width(); y++)
			{
				Object element = mat.get(x, y);
				if (element != null)
				{
					System.out.print(mat.get(x, y)[0]);
				}
				else
				{
					System.out.print(mat.get(x, y));
				}
				System.out.print("");
			}
			System.out.println();
		}
	}
}
