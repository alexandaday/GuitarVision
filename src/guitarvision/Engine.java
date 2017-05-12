package guitarvision;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import guitarvision.detection.EdgeDetector;
import guitarvision.detection.FaceDetector;
import guitarvision.detection.FretDetector;
import guitarvision.detection.GuitarHeadDetector;
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
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

public class Engine {
	private static Engine instance = null;
	
	public final Size processingResolution = new Size(960,540);
	//private Size processingResolution = new Size(1920,1080);
	
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
	
	/**
	 * The core method of the project, produces MIDI transcriptions from videos
	 * @param videoFile - the address of the video to process
	 * @param numberFramesToProcess - the number of frames from the video to process
	 * @param outputDirectoryName - relative path to place the video file
	 * @param writeAnnotatedVideo - whether to produce a video showing the detection modules and processing 
	 * @return Object containing references to the generated MIDI and video files
	 */
	public ProcessedFiles transcribeFromVideo(File videoFile, Integer numberFramesToProcess, String outputDirectoryName, boolean writeAnnotatedVideo)
	{
		VideoCapture guitarVideo = new VideoCapture(videoFile.getPath());
		
		Mat currentFrame = new Mat();
		Mat frameToAnnotate = null;
		
		String fileName = videoFile.getName();
		String name = fileName.substring(0, fileName.lastIndexOf("."));
		String extension = fileName.substring(fileName.lastIndexOf("."));
		
		//double width = guitarVideo.get(Videoio.CAP_PROP_FRAME_WIDTH);
		//double height = guitarVideo.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		double width = processingResolution.width;
		double height = processingResolution.height;
		double fps = guitarVideo.get(Videoio.CAP_PROP_FPS);
		double codecToUse = guitarVideo.get(Videoio.CAP_PROP_FOURCC);
		//double fps = guitarVideo.get(Videoio.CAP_PROP_FPS);
		//double codecToUse = guitarVideo.get(Videoio.CAP_PROP_FOURCC);
		
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
		
		//Gather and set up the required detection objects
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setCannyUpperThreshold(70);
		edgeDetector.setHoughThreshold(300);
		
		EdgeDetector fretEdgeDetector = new EdgeDetector();
		fretEdgeDetector.setCannyLowerThreshold(0);
		fretEdgeDetector.setCannyUpperThreshold(380);
		fretEdgeDetector.setHoughThreshold(45);
		
		StringDetector stringDetector = new StringDetector();
		
		FretDetector fretDetector = new FretDetector();
		
		//To be initialised once the thickness of the strings has been determined
		PluckDetector pluckDetector = null;
		
		SkinDetector skinDetector = new SkinDetector();
		
		NoteDetector noteDetector = new NoteDetector();
		
		SheetMusic transcribedMusic = new SheetMusic();
		
		if (numberFramesToProcess == null)
		{
			numberFramesToProcess = 1000;
		}
		
		
		//numberFramesToProcess = 1000;
		

		boolean firstFrame = true;
		
		//List tracking which strings are currently being played in the frame
		ArrayList<MusicNote> currentlyHeldNotes = new ArrayList<MusicNote>();
		
		for(int x = 0; x < stringDetector.getNumberOfStringsToDetect(); x++)
		{
			currentlyHeldNotes.add(null);
		}
		
		ArrayList<GuitarString> previousStrings = null;
		
		//Process frames one by one
		while (guitarVideo.read(currentFrame))
		{
			//Resize image
			Imgproc.resize(currentFrame, currentFrame, processingResolution);
			
			frameToAnnotate =  currentFrame.clone();
			
			//FaceDetector faceDetector = new FaceDetector();
			
			//frameToAnnotate = faceDetector.getFaces(frameToAnnotate);
			
			//METHOD OF TURNING ON AND OFF DETECTION STAGES TO TRY AND IMPROVE SPEED
			//GIVE USER FEEDBACK AND IMPROVE DETECTION
			
			//Detect strings and frets in frame
			ArrayList<GuitarString> guitarStrings = stringDetector.getGuitarStrings(currentFrame, frameToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.DRAWCLUSTERS);
			
			//ArrayList<GuitarString> guitarStrings = stringDetector.getAccurateGuitarStrings(currentFrame, frameToAnnotate, ImageProcessingOptions.DRAWSELECTEDLINES);
			
			previousStrings = guitarStrings;
			
			ArrayList<DetectedLine> guitarFrets = fretDetector.getGuitarFrets(currentFrame, frameToAnnotate, guitarStrings, fretEdgeDetector, ImageProcessingOptions.NOPROCESSING);

			Mat skin = skinDetector.getSkin(currentFrame);
			
			//Core.addWeighted(frameToAnnotate, 0.6, skin, 0.4, 1.0, frameToAnnotate);
			
			//Processing of frames after the first frame
			if (!firstFrame)
			{
				//Re-calibrate pluck detector if frame used didn't detect all guitar strings
				if (pluckDetector.initialStrings.size() < stringDetector.getNumberOfStringsToDetect() && guitarStrings.size() > pluckDetector.initialStrings.size())
				{
					pluckDetector.initialStrings = guitarStrings;
				}

				boolean[] stringsPlayed = pluckDetector.detectStringsBeingPlayed(guitarStrings);
				
				if (stringsPlayed != null)
				{	
					for(int x = 0; x < stringsPlayed.length; x++)
					{
						if (stringsPlayed[x] && currentlyHeldNotes.get(x) == null)
						{
							//Create new note object
							MusicNote notePlayed = noteDetector.getNote(currentFrame, frameNo, skin, x, guitarStrings, guitarFrets);

							currentlyHeldNotes.set(x, notePlayed);
						}
						else if (!stringsPlayed[x] && currentlyHeldNotes.get(x) != null)
						{
							//Finish note as it is no longer vibrating
							MusicNote currentNote = currentlyHeldNotes.get(x);

							currentNote.setEndingFrame(frameNo);

							transcribedMusic.addNote(currentNote);

							currentlyHeldNotes.set(x, null);
						}
					}
				}

				//Annotate frame with detected strings
				if (writeAnnotatedVideo)
				{
					int scaleFactor = 12;
					if (stringsPlayed == null) stringsPlayed = new boolean[stringDetector.getNumberOfStringsToDetect()];

					
					MusicNote currentNote;
					for(int x = 0; x < stringDetector.getNumberOfStringsToDetect(); x++)
					{
						String currentFret = "NONE";
						
						if (currentlyHeldNotes.get(x) != null)
						{
							currentNote = currentlyHeldNotes.get(x);
							currentFret = Integer.toString(currentNote.note);
						}

						Imgproc.putText(frameToAnnotate, "String "+Integer.toString(x)+": " + stringsPlayed[x] , new Point((currentFrame.rows() / scaleFactor) * 1 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 1.0, new Scalar(255,255,255), 2);
						Imgproc.putText(frameToAnnotate, "Note : " + currentFret , new Point((currentFrame.rows() / scaleFactor) * 8 ,(currentFrame.cols() / (scaleFactor)) * (x+1)), Core.FONT_ITALIC, 1.0, new Scalar(255,255,255), 2);
					}
				}
			}
			//Processing of the first frame (initialisation of pluck detection)
			else
			{
				pluckDetector = new PluckDetector(guitarStrings);
				firstFrame = false;
			}
			
			if (writeAnnotatedVideo)
			{
				outputVideo.write(frameToAnnotate);
			}
			
			frameNo++;
			if ((numberFramesToProcess!= null) && (frameNo >= numberFramesToProcess)) break;
		}
		
		guitarVideo.release();
		if (writeAnnotatedVideo)
		{
			outputVideo.release();
		}
		
		File midiFile = transcribedMusic.writeFile(outputMidiFileName);
		
		//Return references to the created files
		ProcessedFiles results = new ProcessedFiles(outputFile, midiFile);
		
		System.out.println("Processing Video Complete");
		
		return results;
	}
	
	//Variables and Methods for development/testing
	
	String filePath = "resources/images/guitar1.png";
	
	public Mat getProcessedImage(int argument, int argument2, boolean showEdges, int imageNumber, boolean autoLevels)
	{
		switch(imageNumber)
		{
		case 1:
			filePath = "resources/images/vibrate.png";
			break;
		case 2:
			filePath = "resources/images/guitar2.png";
			break;
		case 3:
			filePath = "resources/images/guitar3.png";
			break;
		case 4:
			filePath = "resources/images/guitar4.png";
			break;
		case 5:
			filePath = "resources/images/guitar5.png";
			break;
		case 6:
			filePath = "resources/images/guitar6.png";
			break;
		}
		
		File file = new File(filePath);
		
		Mat imageToProcess = Imgcodecs.imread(file.getPath());
		
		
		//Resize image	
		Imgproc.resize(imageToProcess, imageToProcess, processingResolution);
		
		//Mat result = new Mat();
		//Imgproc.grabCut(imageToProcess, result, new Rect(new Point(0,0), processingResolution), new Mat(), new Mat(), 5);
		
		Mat imageToAnnotate = imageToProcess.clone();
				
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setCannyUpperThreshold(argument);
		edgeDetector.setHoughThreshold(argument2);
		
		
		
		
		Mat hsvImage = new Mat();
		
		
		
		
		ArrayList<GuitarString> previousStrings = null;
		
		if (showEdges)
		{
			return edgeDetector.getEdges(imageToProcess);
		}
		else
		{
			StringDetector stringDetector = new StringDetector();

			ArrayList<GuitarString> guitarStrings = null;
			
			if (!autoLevels)
			{
				guitarStrings = stringDetector.getGuitarStrings(imageToProcess, imageToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.DRAWSELECTEDLINES);
			}
			else
			{
				guitarStrings = stringDetector.getAccurateGuitarStrings(imageToProcess, imageToAnnotate, previousStrings, ImageProcessingOptions.DRAWSELECTEDLINES);	
			}
			
			previousStrings = guitarStrings;
			
			
			PluckDetector pluckDetector = new PluckDetector(guitarStrings);
			
			
			pluckDetector.detectStringBlur(guitarStrings, imageToProcess);
			
			
			//Imgproc.line(imageToAnnotate, guitarStrings.get(2).getPoint1(), guitarStrings.get(2).getPoint2(), new Scalar(255,0,0));
			//Imgproc.line(imageToAnnotate, guitarStrings.get(5).getPoint1(), guitarStrings.get(5).getPoint2(), new Scalar(255,0,0));
			
			
			//guitarStrings.get(0).getPoint1()// .remove(0);//.get(0);
			//guitarStrings.remove(guitarStrings.size()-1);//.get(guitarStrings.size()-1);
			
			if (guitarStrings.size() == 0) return imageToAnnotate;
			
			GuitarString endString1 = guitarStrings.get(0);
			GuitarString endString2 = guitarStrings.get(guitarStrings.size()-1);
			
			List<Point> sourcePoints = new ArrayList<Point>();
			
			//Point point1 = new Point(0,0);
			//Point point2 = new Point(processingResolution.width,0);
			//Point point3 = new Point(processingResolution.width,processingResolution.height);
			//Point point4 = new Point(0,processingResolution.height);
			
//			System.out.println(endString1.getYAtXValue((int) processingResolution.width));
			
			DetectedLine otherLine = new DetectedLine(processingResolution.width, 0.0);
			
//			System.out.println("COLLIDE");
			Point collideP = endString1.getCollisionPoint(otherLine);
//			System.out.println(collideP.x);
//			System.out.println(collideP.y);
			
//			System.out.println("COLLIDE2");
			Point collideP2 = endString2.getCollisionPoint(otherLine);
//			System.out.println(collideP2.x);
//			System.out.println(collideP2.y);
			
			Point point1 = new Point(0,endString1.getYIntercept());
			Point point2 = new Point(processingResolution.width,collideP.y);
			Point point3 = new Point(processingResolution.width,collideP2.y);
			Point point4 = new Point(0,endString2.getYIntercept());		//350, 440  (960,130), (960, 175)
			sourcePoints.add(point1);
			sourcePoints.add(point2);
			sourcePoints.add(point3);
			sourcePoints.add(point4);
			Mat source = Converters.vector_Point2f_to_Mat(sourcePoints);

			List<Point> destPoints = new ArrayList<Point>();
			Point pointD1 = new Point(0,0);
			Point pointD2 = new Point(processingResolution.width,0);
			Point pointD3 = new Point(processingResolution.width,processingResolution.height);
			Point pointD4 = new Point(0,processingResolution.height);
			destPoints.add(pointD1);
			destPoints.add(pointD2);
			destPoints.add(pointD3);
			destPoints.add(pointD4);
			Mat dest = Converters.vector_Point2f_to_Mat(destPoints);

			
			Mat warpMat = Imgproc.getPerspectiveTransform(source, dest);
			Mat inverseWarpMat = warpMat.inv();//Imgproc.getPerspectiveTransform(dest, source);
			
//			System.out.println("MATRIX TRANSFORM");
//			System.out.println(warpMat.size().width);
//			System.out.println(warpMat.size().height);
			
			Mat result = new Mat();
			Imgproc.warpPerspective(imageToProcess, result, warpMat, processingResolution);
			
			
			
			
			
			
			FretDetector fretNeckDetector = new FretDetector();
			EdgeDetector fretNeckEdgeDetector = new EdgeDetector();
			fretNeckEdgeDetector.setCannyLowerThreshold(0);
			fretNeckEdgeDetector.setCannyUpperThreshold(70);
			fretNeckEdgeDetector.setHoughThreshold(220);
			
			
			//stringDetector.getGuitarStrings(result, result, edgeDetector, ImageProcessingOptions.DRAWCLUSTERS);
			
			//Use this
			ArrayList<DetectedLine> guitarNeckFrets = fretNeckDetector.getGuitarFretsFromNeck(imageToProcess, imageToAnnotate, inverseWarpMat, result, fretNeckEdgeDetector, ImageProcessingOptions.DRAWSELECTEDLINES);

			
			Mat revertBack = new Mat();
			Imgproc.warpPerspective(result, revertBack, inverseWarpMat, processingResolution);
			
			
			
			
			
			GuitarHeadDetector headDetector = new GuitarHeadDetector();
			
			result = headDetector.getFilterOutNeck(result);
			
			
			
			exportImage(revertBack, "reverted.png");
			
			
			
			exportImage(result, "warped.png");
			
			
			
//			System.out.println("done");
			
			
			
			
			
			
			
			//imageToProcess = result;
			
			//imageToAnnotate = result;
			
			
			
			
			ArrayList<DetectedLine> guitarFrets = guitarNeckFrets;
			
			//FretDetector fretDetector = new FretDetector();
			//EdgeDetector fretEdgeDetector = new EdgeDetector();
			//fretEdgeDetector.setCannyLowerThreshold(0);
			//fretEdgeDetector.setCannyUpperThreshold(380);
			//fretEdgeDetector.setHoughThreshold(45);
			
			//ArrayList<DetectedLine> guitarFrets = fretDetector.getGuitarFrets(imageToProcess, imageToAnnotate, guitarStrings,fretEdgeDetector, ImageProcessingOptions.DRAWSELECTEDLINES);
			
			
			
			
			
			SkinDetector skinDetector = new SkinDetector();
			
			Mat skin = skinDetector.getSkin(imageToProcess);
			
			Mat skinGrey = new Mat();
			
			Imgproc.cvtColor(skin, skinGrey, Imgproc.COLOR_RGB2GRAY);
			
			Mat components = new Mat();
			
			//System.out.println("Number of labels " + Integer.toString(Imgproc.connectedComponents(skinGrey, components)));
			
			//Core.addWeighted(imageToAnnotate, 0.6, skin, 0.4, 1.0, imageToAnnotate);
			
			FaceDetector faceDetector = new FaceDetector();
			
			imageToAnnotate = faceDetector.getFaces(imageToAnnotate);
			
			
			NoteDetector noteDetector = new NoteDetector();
			
			for (int x = 0; x < stringDetector.getNumberOfStringsToDetect(); x++)
			{
				MusicNote notePlayed = noteDetector.getNote(imageToProcess, 0, skin, x, guitarStrings, guitarFrets);
				
				if (notePlayed != null)
				{
					//System.out.println("Detected note string:");
					//System.out.println(x);
					//System.out.println("Note is:");
					//System.out.println(notePlayed.note);
				}
			}
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
