package guitarvision.detection;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class SkinDetector {
	private static int detectedColour = 255;
	
	private static final int overlapThreshold = 3;
	
	//Skin analysis algorithm from paper by Christophe Garcia and Georgios Tziritas
	//Face detection using quantized skin color regions merging and wavelet packet analysis. IEEE Transactions on Multimedia, 1(3):264.277, 1999.
	public Mat getSkin(Mat image)
	{
		Mat hsvImage = new Mat();
		Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);
		
		for(int row = 0; row < hsvImage.rows(); row++)
		{
			for(int column = 0; column < hsvImage.cols(); column++)
			{
				double[] pixel = hsvImage.get(row, column);
				
				double h = pixel[0];
				double s = pixel[1];
				double v = pixel[2];
				
				h = (h * 360.0) / 179.0;
				s = (s * 100.0) / 255.0;
				v = (v * 100.0) / 255.0;
				
				if (h > 180)
				{
					h = h - 360.0;
				}
				
				boolean skin = false;
				
				if ((s >= 10) && (v >= 40) && (s <= (-1 * h) + (-1 * 0.1 * v) + 110) && (h <= (-1 * 0.4 * v) + 75))
				{
					if (h >= 0)
					{
						if (s <= (0.08 * (100 - v) * h) + (0.5 * v))
						{
							skin = true;
						}
					}
					
					if (h <= 0)
					{
						if (s <= (0.5 * h) + 35)
						{
							skin = true;
						}
					}
				}
				
				if (skin)
				{
					hsvImage.put(row, column, new double[] {detectedColour,detectedColour,detectedColour});
				}
				else
				{
					hsvImage.put(row, column, new double[] {0.0,0.0,0.0});
				}
			}
		}
		
		return smoothDetectedSkin(hsvImage);
	}
	
	/**
	 * Erode and then dilate the detected skin image to remove noise
	 * @param image to process
	 * @return processed image
	 */
	public Mat smoothDetectedSkin(Mat image)
	{
		Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5));
		Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(8,8));
		
		Mat result = new Mat();
		
		result = image;

		Imgproc.erode(image, result, kernelErode);

		Imgproc.dilate(result, result, kernelDilate);
		
		return result;
	}

	
	/**
	 * Detect whether the given line overlaps with the isolated skin image. 
	 * The overlap must be over 'overlapThreshold' pixels.
	 * @param skin image
	 * @param start point of line
	 * @param end point of line
	 * @return whether the line overlaps with the skin image
	 */
	public static boolean fretOverlapSkin(Mat skin, Point startPoint, Point endPoint)
	{
		Mat lineImage = new Mat(skin.rows(), skin.cols(), skin.type(), new Scalar(0,0,0));

		Imgproc.line(lineImage, startPoint, endPoint, new Scalar(detectedColour,detectedColour,detectedColour));

		Mat result = new Mat();
		
		Core.subtract(lineImage, skin, result);

		Core.subtract(lineImage, result, result);

		Mat greyImage = new Mat();

		Imgproc.cvtColor(result, greyImage, Imgproc.COLOR_BGR2GRAY);

		boolean overlap = !(Core.countNonZero(greyImage) < overlapThreshold);
		
		return (overlap);
	}
}
