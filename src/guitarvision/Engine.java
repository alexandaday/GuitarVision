package guitarvision;

import java.util.ArrayList;

import guitarvision.detection.EdgeDetector;
import guitarvision.detection.FretDetector;
import guitarvision.detection.ImageProcessingOptions;
import guitarvision.detection.PolarLine;
import guitarvision.detection.StringDetector;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

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
	
	//Variables and Methods for development/testing
	
	private String fileName = "../images/guitar.png";
	
	public Mat getOriginalImage()
	{	
		Mat image = Imgcodecs.imread(getClass().getResource(fileName).getPath());
		
		return image;
	}
	
	public Mat getProcessedImage(int argument)
	{	
		Mat imageToProcess = Imgcodecs.imread(getClass().getResource(fileName).getPath());
		
		StringDetector stringDetector = new StringDetector();
		EdgeDetector edgeDetector = new EdgeDetector();
		edgeDetector.setHoughThreshold(argument);
		
		ArrayList<PolarLine> guitarStrings = stringDetector.getGuitarStrings(imageToProcess, edgeDetector, ImageProcessingOptions.DRAWGROUPINGS);
		
		FretDetector fretDetector = new FretDetector();
		EdgeDetector fretEdgeDetector = new EdgeDetector();
		fretEdgeDetector.setHoughThreshold(75);
		
		guitarStrings = fretDetector.getGuitarFrets(imageToProcess,guitarStrings,fretEdgeDetector,ImageProcessingOptions.DRAWGROUPINGS);
		
		return imageToProcess;
	}
}
