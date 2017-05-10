package guitarvision.detection;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class GuitarHeadDetector {
	private static int detectedColour = 255;
	
	public Mat getFilterOutNeck(Mat image)
	{
		Mat hsvImage = new Mat();
		Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);
		
		for(int row = 0; row < hsvImage.rows(); row++)
		{
			for(int column = 0; column < hsvImage.cols(); column++)
			{
				double[] pixel = hsvImage.get(row, column);
				
//				if ((pixel[0] > 150) || (pixel[0] < 20))
//				{
//					hsvImage.put(row, column, new double[] {detectedColour,detectedColour,detectedColour});
//				}
//				else
//				{
//					hsvImage.put(row, column, new double[] {0.0,0.0,0.0});
//				}
				
				if ((pixel[2] > 76) && (!((pixel[0] > 150) || (pixel[0] < 20))))
				{
					hsvImage.put(row, column, new double[] {detectedColour,detectedColour,detectedColour});
				}
				else
				{
					hsvImage.put(row, column, new double[] {0.0,0.0,0.0});
				}
			}
		}
		
		return erodeImage(hsvImage);
	}
	
	public Mat getFrets(Mat image)
	{
		Mat hsvImage = new Mat();
		Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);
		
		for(int row = 0; row < hsvImage.rows(); row++)
		{
			for(int column = 0; column < hsvImage.cols(); column++)
			{
				double[] pixel = hsvImage.get(row, column);
				
//				if ((pixel[0] > 150) || (pixel[0] < 20))
//				{
//					hsvImage.put(row, column, new double[] {detectedColour,detectedColour,detectedColour});
//				}
//				else
//				{
//					hsvImage.put(row, column, new double[] {0.0,0.0,0.0});
//				}
				
				if (!(pixel[2] > 125))
				{
					hsvImage.put(row, column, new double[] {0.0,0.0,0.0});
				}
			}
		}
		
		Imgproc.cvtColor(hsvImage, hsvImage, Imgproc.COLOR_HSV2BGR);
		
		return (hsvImage);
	}
	
	public Mat erodeImage(Mat image)
	{
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(12,12));
		
		Mat result = new Mat();
		
		Imgproc.erode(image, result, kernel);
		
		return result;
	}
}
