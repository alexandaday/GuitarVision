package guitarvision.detection;

import guitarvision.Engine;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

/**
 * @author Alex Day
 * Class for detecting when a string is vibrating
 */
public class PluckDetector {
	private ArrayList<GuitarString> initialStrings;
	private double thicknessThresholdFactor = 1.2;
	
	public void setInitialStrings(ArrayList<GuitarString> initialStrings)
	{
		this.initialStrings = initialStrings;
	}
	
	public ArrayList<GuitarString> getInitialStrings()
	{
		return initialStrings;
	}
	
	/**
	 * Determine the thickness of strings in the image, and store them in the string objects
	 * @param list of guitar strings
	 * @param list of frets, for isolating the string image to around the fretboard
	 * @param original image to extract strings images from
	 */
	public void getStringThicknesses(ArrayList<GuitarString> strings, ArrayList<DetectedLine> frets, Mat originalImage)
	{
		for(int stringIndex = 0; stringIndex < strings.size(); stringIndex++)
		{
			//Extract string from image by performing perspective transform on a rectangle surrounding the string
			double curRho = strings.get(stringIndex).getRho();
			double nextRho;
			double difference;
			if (stringIndex == strings.size()-1)
			{
				nextRho = strings.get(stringIndex-1).getRho();
				difference = curRho - nextRho;
				
			}
			else
			{
				nextRho = strings.get(stringIndex+1).getRho();
				difference = nextRho - curRho;
			}
			
			double fretSpanStart = 0;
			double fretSpanEnd = 0;
			
			//Find where the outer frets collide with the string, for creating the bounding rectangle
			if (frets != null && frets.size() > 0)
			{
				GuitarString curString = strings.get(stringIndex);
				
				Point collideWithFret1 = frets.get(frets.size()-1).getCollisionPoint(curString);
				Point collideWithFret2 = frets.get(0).getCollisionPoint(curString);
				
				double width = Math.abs(collideWithFret2.x - collideWithFret1.x);
				
				fretSpanStart = collideWithFret1.x - (width / 3);
				
				if (fretSpanStart < 0) fretSpanStart = 0;
				
				fretSpanEnd = collideWithFret2.x - (width / 3);
				
				if (fretSpanStart - fretSpanEnd == 0)
				{
					fretSpanStart = 0;
					fretSpanEnd = Engine.getInstance().processingResolution.width;
				}

			}
			
			//Find parallel lines halfway between the strings on either side
			//to form the bounding rectangle for the perspective transform
			DetectedLine startStringLine = new DetectedLine(curRho - (difference / 2), strings.get(stringIndex).getTheta());
			DetectedLine endStringLine = new DetectedLine(curRho + (difference / 2), strings.get(stringIndex).getTheta());
			
			List<Point> sourcePoints = new ArrayList<Point>();
			
			double height = Engine.getInstance().processingResolution.height / 2;
			double width = Math.abs(fretSpanEnd - fretSpanStart);
			
			DetectedLine rightSideOfRectangle = new DetectedLine(fretSpanEnd, 0.0);
			
			DetectedLine leftSideOfRectangle = new DetectedLine(fretSpanStart, 0.0);
			
			Point bottomRightPoint = startStringLine.getCollisionPoint(rightSideOfRectangle);

			Point topRightPoint = endStringLine.getCollisionPoint(rightSideOfRectangle);
			
			Point bottomLeftPoint = startStringLine.getCollisionPoint(leftSideOfRectangle);

			Point topLeftPoint = endStringLine.getCollisionPoint(leftSideOfRectangle);
			
			Point point1 = new Point(fretSpanStart,bottomLeftPoint.y);
			Point point2 = new Point(fretSpanEnd,bottomRightPoint.y);
			Point point3 = new Point(fretSpanEnd,topRightPoint.y);
			Point point4 = new Point(fretSpanStart,topLeftPoint.y);
			sourcePoints.add(point1);
			sourcePoints.add(point2);
			sourcePoints.add(point3);
			sourcePoints.add(point4);
			Mat source = Converters.vector_Point2f_to_Mat(sourcePoints);

			List<Point> destPoints = new ArrayList<Point>();
			Point pointD1 = new Point(0,0);
			Point pointD2 = new Point(width,0);
			Point pointD3 = new Point(width,height);
			Point pointD4 = new Point(0,height);
			destPoints.add(pointD1);
			destPoints.add(pointD2);
			destPoints.add(pointD3);
			destPoints.add(pointD4);
			Mat dest = Converters.vector_Point2f_to_Mat(destPoints);
			
			Mat warpMat = Imgproc.getPerspectiveTransform(source, dest);

			Mat result = new Mat();
			Imgproc.warpPerspective(originalImage, result, warpMat, new Size(width, height));
			
			double newThickness = getThicknessFromStringImage(result);

			strings.get(stringIndex).thickness = newThickness;
		}
	}
	
	/**
	 * Determines the thickness of strings for images of horizontal strings
	 * @param image of the guitar string isolated
	 * @return the perceived thickness of the string in the image
	 */
	private Double getThicknessFromStringImage(Mat image)
	{	
		Mat greyImage = new Mat();
		
		Imgproc.cvtColor(image, greyImage, Imgproc.COLOR_RGB2GRAY);

		Mat sobelOutput = new Mat();
		Imgproc.Sobel(greyImage, sobelOutput, CvType.CV_8UC1, 0, 1);
		
		Mat kernelD = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5));
		
		Imgproc.dilate(sobelOutput, sobelOutput, kernelD);
		
		for(int x = 0; x < image.rows(); x ++)
		{
			for(int y = 0; y < image.cols(); y ++)
			{
				double g = sobelOutput.get(x, y)[0];
				
				if (g > 10)
				{
					g = 255;
				}
				else
				{
					g = 0;
				}
				
				greyImage.put(x, y, new double[] {g});
			}

		}

		image = greyImage;

		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(8,8));
		
		Imgproc.erode(image, image, kernel);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		
		Mat contourImage = image.clone();
		
		for(int x1 = 0; x1 < contourImage.rows(); x1 ++)
		{
			for(int y = 0; y < contourImage.cols(); y ++)
			{
				contourImage.put(x1, y, new double[] {0,0,0});
			}

		}
		
		double maxPerim = 0;
		MatOfPoint maxContour = null;
		for(MatOfPoint contour: contours)
		{
			double perim = Imgproc.contourArea(contour);
			if (perim > maxPerim)
			{
				maxPerim = perim;
				maxContour = contour;
				
			}
		}
		
		if (maxContour != null)
		{
			ArrayList<MatOfPoint> contoursToDraw = new ArrayList<MatOfPoint>();
			contoursToDraw.add(maxContour);
			
			Imgproc.drawContours(contourImage, contoursToDraw, -1, new Scalar(150,150,150));
			
			double thickness = getMeanContourThickness(contourImage);
			
			//Ignore contours that are not sufficiently wide
			if (maxPerim < contourImage.cols() * 5) thickness = 0;
			
			return thickness;
		}
		
		return 0.0;
	}
	
	/**
	 * Given horizontal contour, find the mean thickness of it in the frame
	 * @param image with contour
	 * @return mean thickness
	 */
	private Double getMeanContourThickness(Mat contourImage)
	{
		int count = 0;
		int thicknessTotal = 0;
		for(int x = 0; x < contourImage.cols(); x++)
		{
			int minPoint = contourImage.rows() - 1;
			int maxPoint = 0;
			boolean anyPoints = false;
			for(int y = 0; y < contourImage.rows(); y++)
			{
				if (contourImage.get(y, x)[0] > 0)
				{
					if (y < minPoint) minPoint = y;
					if (y > maxPoint) maxPoint = y;
					anyPoints = true;
				}
			}
			if (anyPoints)
			{
				thicknessTotal += Math.abs(maxPoint - minPoint);
				count++;
			}
		}
		double result = 0;
		if (count > 0) result = (thicknessTotal / count);
		return result;
	}
	
	/**
	 * Get boolean array that indicates which strings are vibrating.
	 * Compares the string thicknesses to the original string thicknesses stored in this detector.
	 * @param strings with their current thicknesses
	 * @return array indicating which strings are vibrating
	 */
	public boolean[] getVibratingStrings(ArrayList<GuitarString> strings)
	{
		int numberStrings = strings.size();
		
		boolean[] stringsBeingPlayed = new boolean [numberStrings];
		
		if (strings.size() != initialStrings.size())
		{
			return null;
		}
		else
		{
			for(int index = 0; index < numberStrings; index++)
			{
				GuitarString initialString = initialStrings.get(index);
				GuitarString currentString = strings.get(index);
				
				if ((currentString.thickness > initialString.thickness * thicknessThresholdFactor) && (currentString.thickness != 0))
				{
					stringsBeingPlayed[index] = true;
				}
			}
		}
		
		return stringsBeingPlayed;
	}
}
