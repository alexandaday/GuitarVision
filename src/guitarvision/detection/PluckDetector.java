package guitarvision.detection;

import guitarvision.Engine;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class PluckDetector {
	public ArrayList<GuitarString> initialStrings;
	
	private double scaleFactor = 2;
	
	
	public PluckDetector(ArrayList<GuitarString> initialStrings)
	{
		this.initialStrings = initialStrings;
	}
	
	public Double getBlurScore(Mat image, int x)
	{
		//Mat convertedImage = new Mat();
//		Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2HSV);
//		
//		
//		for(int x1 = 0; x1 < image.rows(); x1 ++)
//		{
//			for(int y = 0; y < image.cols(); y ++)
//			{
//				double h = image.get(x1, y)[2];
//				double s = 0;//image.get(x, y)[1];
//				double v = 0;//image.get(x, y)[2];
//				//imageToAnnotate2.get(x, y)[1] = 0;
////				if (image.get(x1, y)[2] < 100){
////					h = 0;
////				}
//				image.put(x1, y, new double[] {h,s,v});
//			}
//
//		}
		
		Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
		
		Imgproc.GaussianBlur(image, image,  new Size(3, 3), 1);
		

		Mat sobelOutput = new Mat();
		Imgproc.Sobel(image, sobelOutput, CvType.CV_8UC1, 0, 1);
		
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5));
		
		Imgproc.dilate(sobelOutput, sobelOutput, kernel);
		
		
		for(int x1 = 0; x1 < sobelOutput.rows(); x1 ++)
		{
			for(int y = 0; y < sobelOutput.cols(); y ++)
			{
				double g = sobelOutput.get(x1, y)[0];

				if (g > 10)
				{
					g = 255;
				}
				else
				{
					g = 0;
				}
				
				image.put(x1, y, new double[] {g});
			}

		}
		
		EdgeDetector edgeDetector = new EdgeDetector();
		
		Mat lines = edgeDetector.houghTransform(sobelOutput);
		
		//CLUSTER INTO TWO GROUPS, IF SUFFICIENT FAR AWAY VIBRATING
		
		System.out.println(lines.size());
		
		System.out.println(Core.sumElems(sobelOutput));
		
		Engine.getInstance().exportImage(sobelOutput, "sobel_"+x+".png");
		
		Mat laplacianResult = new Mat();
		Imgproc.Laplacian(sobelOutput, laplacianResult, CvType.CV_16S);
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble stdDev = new MatOfDouble();
		Core.meanStdDev(laplacianResult, mean, stdDev);
		
		Double result = stdDev.empty() ? null : stdDev.get(0, 0)[0];
		
		System.out.println(result);
		
		return null;
	}
	
	public boolean[] detectStringBlur(ArrayList<GuitarString> strings, Mat originalImage)
	{
		for(int x = 0; x < strings.size(); x++)
		{
			double curRho = strings.get(x).rho;
			double nextRho;
			double difference;
			if (x == strings.size()-1)
			{
				nextRho = strings.get(x-1).rho;
				difference = curRho - nextRho;
				
			}
			else
			{
				nextRho = strings.get(x+1).rho;
				difference = nextRho - curRho;
			}
			
			DetectedLine startStringLine = new DetectedLine(curRho - (difference / 2), strings.get(x).theta);
			DetectedLine endStringLine = new DetectedLine(curRho + (difference / 2), strings.get(x).theta);
			
			List<Point> sourcePoints = new ArrayList<Point>();
			
			double height = Engine.getInstance().processingResolution.height / 4;
			double width = Engine.getInstance().processingResolution.width;
			
			DetectedLine otherLine = new DetectedLine(width, 0.0);
			
			Point collideP = startStringLine.getCollisionPoint(otherLine);

			Point collideP2 = endStringLine.getCollisionPoint(otherLine);

			Point point1 = new Point(0,startStringLine.getYIntercept());
			Point point2 = new Point(width,collideP.y);
			Point point3 = new Point(width,collideP2.y);
			Point point4 = new Point(0,endStringLine.getYIntercept());
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
			Mat inverseWarpMat = warpMat.inv();//Imgproc.getPerspectiveTransform(dest, source);
			
//			System.out.println("MATRIX TRANSFORM");
//			System.out.println(warpMat.size().width);
//			System.out.println(warpMat.size().height);
			
			Mat result = new Mat();
			Imgproc.warpPerspective(originalImage, result, warpMat, new Size(width, height));
			
			getBlurScore(result, x);
			
			Engine.getInstance().exportImage(result, "string_"+x+".png");
			
		}
		
		return null;
	}
	
	public boolean[] detectStringsBeingPlayed(ArrayList<GuitarString> strings)
	{
		int numberStrings = strings.size();//StringDetector.numberStringsToDetect;
		
		boolean[] stringsBeingPlayed = new boolean [numberStrings];
		
		if (strings.size() != initialStrings.size())
		{
			return null;
		}
		else
		{
			for(int x = 0; x < numberStrings; x++)
			{
				GuitarString initialString = initialStrings.get(x);
				GuitarString currentString = strings.get(x);
				
//				System.out.println("Current thickness");
//				System.out.println(currentString.thickness);
//				System.out.println("Target thickness");
//				System.out.println(initialString.thickness * scaleFactor);
				
				
				if ((currentString.thickness > initialString.thickness * scaleFactor) && (initialString.thickness != 0))
				{
//					System.out.println("String thickness");
//					System.out.println(currentString.thickness);
//					System.out.println("Initial thickness * sf");
//					System.out.println(initialString.thickness * scaleFactor);
					stringsBeingPlayed[x] = true;
				}
			}
		}
		
		return stringsBeingPlayed;
	}
}
