package guitarvision.detection;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class EdgeDetector {
	private int blurKernelSize = 4;
	
	private int cannyLowerThreshold = 0;
	private int cannyUpperThreshold = 255;
	
	private int houghThreshold = 300;
	
	public Mat getEdges(Mat image)
	{
		Mat blurredImage = image.clone();

		Imgproc.blur(image, blurredImage, new Size(blurKernelSize, blurKernelSize));

		Mat detectedEdges = new Mat();

		Imgproc.Canny(blurredImage, detectedEdges, cannyLowerThreshold, cannyUpperThreshold);

		return detectedEdges;
	}

	public Mat houghTransform(Mat edgeImage)
	{
		Mat edges = new Mat();

		Imgproc.HoughLines(edgeImage, edges, 1, Math.PI/360, houghThreshold);

		return edges;
	}
	
	public void setBlurFilterSize(int newSize)
	{
		blurKernelSize = newSize;
	}
	
	public void setCannyLowerThreshold(int newThreshold)
	{
		cannyLowerThreshold = newThreshold;
	}
	
	public void setCannyUpperThreshold(int newThreshold)
	{
		cannyUpperThreshold = newThreshold;
	}
	
	public void setHoughThreshold(int newThreshold)
	{
		houghThreshold = newThreshold;
	}
}
