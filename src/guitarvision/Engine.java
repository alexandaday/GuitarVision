package guitarvision;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;

import guitarvision.detection.EdgeDetector;
import guitarvision.detection.FretDetector;
import guitarvision.detection.ImageProcessingOptions;
import guitarvision.detection.PolarLine;
import guitarvision.detection.StringDetector;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
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
		System.out.println("Exporting: " + fileName);
		
		Imgcodecs.imwrite(fileName, image);
	}
	
	public void loadVideo(File videoFile)
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
		
		int framesToProcess = 255;
		
		EdgeDetector edgeDetector = new EdgeDetector();
		//edgeDetector.setCannyUpperThreshold(30);
		
		StringDetector stringDetector = new StringDetector();
		
		while (guitarVideo.read(currentFrame))
		{
			stringDetector.getGuitarStrings(currentFrame, edgeDetector, ImageProcessingOptions.DRAWGROUPINGS);

			outputVideo.write(currentFrame);
			framesToProcess--;
			if (framesToProcess <= 0) break;
		}
		
		guitarVideo.release();
		outputVideo.release();
		
		System.out.println("DONE");
	}
	
	//Variables and Methods for development/testing
	
	private String fileName = "../images/guitar.png";
	
	public Mat getOriginalImage()
	{	
		Mat image = Imgcodecs.imread(getClass().getResource(fileName).getPath());
		
		return image;
	}
	
	public Mat getProcessedImage(int argument, int argument2, boolean showEdges)
	{	
		Mat imageToProcess = Imgcodecs.imread(getClass().getResource(fileName).getPath());
		
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
			
			ArrayList<PolarLine> guitarStrings = stringDetector.getGuitarStrings(imageToProcess, edgeDetector, ImageProcessingOptions.DRAWGROUPINGS);
			
			//FretDetector fretDetector = new FretDetector();
			//EdgeDetector fretEdgeDetector = new EdgeDetector();
			//fretEdgeDetector.setHoughThreshold(75);
			
			//guitarStrings = fretDetector.getGuitarFrets(imageToProcess,guitarStrings,fretEdgeDetector,ImageProcessingOptions.DRAWGROUPINGS);
			
		}
		return imageToProcess;
	}
}
