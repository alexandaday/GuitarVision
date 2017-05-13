package guitarvision.detection;

import guitarvision.Engine;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class PluckDetector {
	public ArrayList<GuitarString> initialStrings;
	
	private double scaleFactor = 2;
	
	private double angleAllowance = 0.2;
	
	public PluckDetector(ArrayList<GuitarString> initialStrings)
	{
		this.initialStrings = initialStrings;
	}
	
	//Assume horizontal line
	public Double getMeanStringThickness(Mat contourImage)
	{
		double countWeight = 0.5;
		
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
				thicknessTotal += maxPoint - minPoint;
				count++;
			}
		}
		double result = 0;
		if (count > 0) result = (thicknessTotal / count);
		return result;
	}
	
	public Double getBlurScore(Mat image, int x)
	{	
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

				if (g > 5)
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
		edgeDetector.setCannyUpperThreshold(30);
		edgeDetector.setHoughThreshold(850);
		
		Mat lines = edgeDetector.houghTransform(sobelOutput);
		
		//CLUSTER INTO TWO GROUPS, IF SUFFICIENT FAR AWAY VIBRATING
		
		ArrayList<DetectedLine> stringLines = getLinesFromParameters(lines);
		
		ArrayList<DetectedLine> filteredLines = filterParallelLines(stringLines);
		
		Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10,10));
		
		Imgproc.erode(image, image, kernel2);

		
		
		//Structure analysis perimeter
		
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
			System.out.println("CONVEX");
			System.out.println(Imgproc.isContourConvex(maxContour));
			
			MatOfInt convexHull = new MatOfInt();
			Imgproc.convexHull(maxContour, convexHull);
			
			ArrayList<MatOfPoint> contoursToDraw = new ArrayList<MatOfPoint>();
			contoursToDraw.add(maxContour);
			
			Imgproc.drawContours(contourImage, contoursToDraw, -1, new Scalar(150,150,150));
			
			double thickness = getMeanStringThickness(contourImage);
			
			//remove small items
			if (maxPerim < 4000) thickness = 0;
			
			System.out.println("MEAN THICKNESS");
			System.out.println(thickness);
			
			//Imgproc.drawContours(contourImage, hull, -1, new Scalar(150,150,150));
			
			//System.out.println("Max perim of convex");
			//System.out.println(Imgproc.contourArea(convexHull));
		}
		
		System.out.println("Max perim");
		System.out.println(maxPerim);
		
		//contourImage.
		
		//Imgproc.drawContours(contourImage, contours, -1, new Scalar(150,150,150));
		
		
//		for(MatOfPoint contour: contours)
//		{
//			
//		}
		
		
		
		System.out.println("thresh sum");
		System.out.println(Core.sumElems(image));
		
		for(DetectedLine line: filteredLines)
		{
			//Imgproc.line(image, line.getPoint1(), line.getPoint2(), new Scalar(255,0,0));
		}
		
		Engine.getInstance().exportImage(image, "detectlines"+x+".png");
		Engine.getInstance().exportImage(contourImage, "contours"+x+".png");
		
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
	
	private ArrayList<DetectedLine> getLinesFromParameters(Mat houghLines)
	{
		ArrayList<DetectedLine> stringLines = new ArrayList<DetectedLine>();

		for (int lineIndex = 0; lineIndex < houghLines.rows(); lineIndex++)
		{
			double[] polarLineParameters = houghLines.get(lineIndex, 0);

			DetectedLine currentString = new DetectedLine(polarLineParameters[0], polarLineParameters[1]);

			stringLines.add(currentString);
		}

		return stringLines;
	}
	
	private ArrayList<DetectedLine> filterParallelLines(ArrayList<DetectedLine> candidateLines)
	{
		double totalAngle = 0;

		for(DetectedLine curLine : candidateLines)
		{
			totalAngle += curLine.theta;
		}

		double averageAngle = totalAngle/candidateLines.size();

		ArrayList<DetectedLine> filteredLines = new ArrayList<DetectedLine>();

		if (candidateLines.size() > 0)
		{

			for(int a = 0; a < candidateLines.size(); a++)
			{
				if (!((candidateLines.get(a).theta > averageAngle + angleAllowance) || (candidateLines.get(a).theta < averageAngle - angleAllowance)))
				{
					filteredLines.add(candidateLines.get(a));
				}
			}
		}

		return filteredLines;
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
