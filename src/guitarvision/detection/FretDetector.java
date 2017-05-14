package guitarvision.detection;

import guitarvision.Engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class FretDetector {
	public double angleAllowance = 0.05;
	public double angleAllowanceNeck = 0.02;
	public int numberFretsToDetect = 20;
	
	public ArrayList<DetectedLine> getGuitarFrets(Mat imageToProcess, Mat imageToAnnotate, ArrayList<GuitarString> guitarStrings, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
	{
		double width = imageToProcess.width();
		double height = imageToProcess.height();
		Size resolution = new Size(width, height);
		
		GuitarString endString1 = guitarStrings.get(0);
		GuitarString endString2 = guitarStrings.get(guitarStrings.size()-1);
		
		List<Point> sourcePoints = new ArrayList<Point>();
		
		DetectedLine otherLine = new DetectedLine(width, 0.0);
		
		Point collideP = endString1.getCollisionPoint(otherLine);
		Point collideP2 = endString2.getCollisionPoint(otherLine);
		
		Point point1 = new Point(0,endString1.getYIntercept());
		Point point2 = new Point(width,collideP.y);
		Point point3 = new Point(width,collideP2.y);
		Point point4 = new Point(0,endString2.getYIntercept());
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
		Mat inverseWarpMat = warpMat.inv();

		Mat guitarNeckImage = new Mat();
		Imgproc.warpPerspective(imageToProcess, guitarNeckImage, warpMat, resolution);

		ArrayList<DetectedLine> guitarNeckFrets = getGuitarFretsFromNeckImage(imageToProcess, imageToAnnotate, inverseWarpMat, guitarNeckImage, edgeDetector, ImageProcessingOptions.DRAWSELECTEDLINES);

		//Engine.getInstance().exportImage(guitarNeckImage, "heck.png");
		
		if ((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
		{
			int x = 0;
			Scalar colour = null;
			for(DetectedLine fret : guitarNeckFrets)
			{
				if (x % 3 == 0)
				{
					colour = new Scalar(255,0,0);
				}
				else if (x % 3 == 1)
				{
					colour = new Scalar(0,255,0);
				}
				else if (x % 3 == 2)
				{
					colour = new Scalar(0,0,255);
				}
				else
				{
					colour = new Scalar(255,0,255);
				}
				Imgproc.line(imageToAnnotate, fret.getPoint1(), fret.getPoint2(), colour);
				x++;
			}
		}
		
		return guitarNeckFrets;
	}
	
	
	private ArrayList<DetectedLine> getGuitarFretsFromNeckImage(Mat originalImage, Mat imageToAnnotate, Mat inverseNeckWarp, Mat guitarNeckImage, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
	{
		if (edgeDetector == null)
		{
			edgeDetector = new EdgeDetector();
			edgeDetector.setCannyLowerThreshold(0);
			edgeDetector.setCannyUpperThreshold(255);
			edgeDetector.setHoughThreshold(300);
		}
		
		
		
		
//		Mat originalImageToAnnotate = imageToAnnotate;
		
		
		
		
		
		
		
//		originalImage = guitarNeckImage;
//		imageToAnnotate = guitarNeckImage.clone();
//		Mat imageToAnnotate2 = guitarNeckImage.clone();
		
//		GuitarHeadDetector headDet = new GuitarHeadDetector();
		
//		originalImage = headDet.getFrets(originalImage);
//		imageToAnnotate = headDet.getFrets(imageToAnnotate);
		

//		Imgproc.cvtColor(imageToAnnotate2, imageToAnnotate2, Imgproc.COLOR_BGR2HSV);
//		
//		
//		
//		//Looking for first fret
//		
//		for(int x = 0; x < imageToAnnotate2.rows(); x ++)
//		{
//			for(int y = 0; y < imageToAnnotate2.cols(); y ++)
//			{
//				double h = imageToAnnotate2.get(x, y)[0];
//				double s = imageToAnnotate2.get(x, y)[1];
//				double v = imageToAnnotate2.get(x, y)[2];
//				imageToAnnotate2.put(x, y, new double[] {h,s,v});
//			}
//
//		}
//		
//		double maxTotal = 0;
//		int maxCol = imageToAnnotate2.cols()-1;
//		for(int y = (int) Math.floor(imageToAnnotate2.cols() /2); y < imageToAnnotate2.cols(); y ++)
//		{
//			double columnTotal = 0;
//			for(int x = 0; x < imageToAnnotate2.rows(); x ++)
//			{
//				double h = 0;
//				double s = 0;
//				double v = imageToAnnotate2.get(x, y)[2];
//				columnTotal += v;
//			}
//			if (columnTotal > maxTotal)
//			{
//				maxTotal = columnTotal;
//				maxCol = y;
//			}
//		}
//		
//		for(int x = 0; x < imageToAnnotate2.rows(); x ++)
//		{			
//			imageToAnnotate2.put(x, maxCol, new double[] {0,0,0});
//		}
//		
		
		
		
		
		
		//Engine.getInstance().exportImage(imageToAnnotate2, "hsv_one_layer.png");
		
		//FIRST FILTER BY ABOVE CERTAIN BRIGHTNESS/VALUE
		
		
		
//		Mat verticalLineTest = imageToAnnotate2.clone();
		//Imgproc.cvtColor(verticalLineTest, verticalLineTest, Imgproc.COLOR_RGB2GRAY);
		
		//Imgproc.GaussianBlur(verticalLineTest, verticalLineTest,  new Size(3, 3), 1);

//		Mat sobelOutput = new Mat();
//		Imgproc.Sobel(verticalLineTest, sobelOutput, CvType.CV_8UC1, 1, 1);
		
		
		//Engine.getInstance().exportImage(sobelOutput, "vertical_edge_fretboard.png");
		
		
		Mat cannyProcessedImage = edgeDetector.getEdges(guitarNeckImage);
		
		
		
		Mat houghLineParameters = edgeDetector.houghTransform(cannyProcessedImage);
		
		ArrayList<DetectedLine> initialLines = getLinesFromParameters(houghLineParameters);
		
		//System.out.println("Number of lines detected: ");
		//System.out.println(initialLines.size());
		
		ArrayList<DetectedLine> parallelLines = filterGuitarNeckFrets(initialLines);
		
		
		
		
		
		//FIND FIRST FRET, cluster and get median separation work out
		
		//USE MEDIAN COLOUR OF DETECTED STRINGS, only accept max with this colour - or reject lines no this colour
		
		
		
		
		
		
		int noGroups = numberFretsToDetect;
		
		ArrayList<ArrayList<DetectedLine>> fretGroupings = clusterGuitarFrets(parallelLines, noGroups);
		
		ArrayList<DetectedLine> selectedFrets = selectEachGuitarFret(fretGroupings);
		
		Collections.sort(selectedFrets);
		
		ArrayList<DetectedLine> finalFrets = initialLines;//edgeDetector.evenlyDistributeLinesExponential(selectedFrets, noGroups, Intercept.XINTERCEPT);//edgeDetector.evenlyDistribute(selectedFrets, numberFretsToDetect, Intercept.XINTERCEPT);
		
		if (finalFrets == null)
		{
			finalFrets = new ArrayList<DetectedLine>();
		}
		
			Engine.getInstance().exportImage(cannyProcessedImage, "usingnecknow_without.png");
		
				for(DetectedLine fret : finalFrets)
				{
					Imgproc.line(cannyProcessedImage, fret.getPoint1(), fret.getPoint2(), new Scalar(150,255,255));
				}
				
				Engine.getInstance().exportImage(cannyProcessedImage, "usingnecknow.png");
		
		
		//Sort based on x intercept
		
		
		//TESTING
		//ArrayList<DetectedLine> finalFrets = parallelLines;
		//ArrayList<DetectedLine> selectedFrets = parallelLines;
		//ArrayList<ArrayList<DetectedLine>> fretGroupings = null;
		//TESTING
		
		
		
		
		
		
//		if((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
//		{
//			Scalar colour;
//			int x = 0;
//			for(DetectedLine fret : finalFrets)
//			{
//				if (x % 3 == 0)
//				{
//					colour = new Scalar(255,0,0);
//				}
//				else if (x % 3 == 1)
//				{
//					colour = new Scalar(0,255,0);
//				}
//				else if (x % 3 == 2)
//				{
//					colour = new Scalar(0,0,255);
//				}
//				else
//				{
//					colour = new Scalar(255,0,255);
//				}
//				Imgproc.line(imageToAnnotate, fret.getPoint1(), fret.getPoint2(), colour);
//				x++;
//			}
//		}
//		
//		Random randomGenerator = new Random();
//		
//		if(processingOptions == ImageProcessingOptions.DRAWCLUSTERS)
//		{
//			for(ArrayList<DetectedLine> stringGroup: fretGroupings)
//			{
//				Scalar colour = new Scalar(randomGenerator.nextInt(255),randomGenerator.nextInt(255),randomGenerator.nextInt(255));
//				
//				for(DetectedLine string: stringGroup)
//				{
//					Imgproc.line(imageToAnnotate, string.getPoint1(), string.getPoint2(), colour);
//				}
//			}
//		}
		
		//ArrayList<DetectedLine> transformedFrets = new ArrayList<DetectedLine>();
		
		
//		GuitarHeadDetector headDetector = new GuitarHeadDetector();
//		
//		Mat neckImage = headDetector.getFilterOutNeck(originalImage);
//
//		ArrayList<DetectedLine> fretsToRemove = new ArrayList<DetectedLine>();
//		
//		for(DetectedLine fret : finalFrets)
//		{
//			if (!SkinDetector.fretOverlapSkin(neckImage, fret.getPoint1(), fret.getPoint2()))
//			{
//				//fretsToRemove.add(fret);
//			}
//		}
//		
//		for(DetectedLine fret : fretsToRemove)
//		{
//			finalFrets.remove(fret);
//		}

		
		for(DetectedLine fret : finalFrets)
		{
			fret.applyWarp(inverseNeckWarp);
		}

		return finalFrets;
	}
	
	public ArrayList<DetectedLine> getGuitarFretsWithoutIsolatingNeck(Mat originalImage, Mat imageToAnnotate, ArrayList<GuitarString> guitarStrings, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
	{
		if (edgeDetector == null)
		{
			edgeDetector = new EdgeDetector();
			edgeDetector.setCannyLowerThreshold(0);
			edgeDetector.setCannyUpperThreshold(255);
			edgeDetector.setHoughThreshold(300);
		}
		
		Mat cannyProcessedImage = edgeDetector.getEdges(originalImage);
		
		Mat houghLineParameters = edgeDetector.houghTransform(cannyProcessedImage);
		
		ArrayList<DetectedLine> initialLines = getLinesFromParameters(houghLineParameters);
		
		ArrayList<DetectedLine> parallelLines = filterGuitarFrets(initialLines, guitarStrings);
		
		//int noGroups = numberFretsToDetect;
		
		//ArrayList<ArrayList<DetectedLine>> fretGroupings = clusterGuitarFrets(parallelLines, noGroups);
		
		//ArrayList<DetectedLine> selectedFrets = selectEachGuitarFret(fretGroupings);
		
		//ArrayList<DetectedLine> finalFrets = selectedFrets;//edgeDetector.evenlyDistribute(selectedFrets, numberFretsToDetect, Intercept.XINTERCEPT);
		
		//Sort based on x intercept
		
		
		//TESTING
		ArrayList<DetectedLine> finalFrets = parallelLines;
		ArrayList<DetectedLine> selectedFrets = parallelLines;
		ArrayList<ArrayList<DetectedLine>> fretGroupings = null;
		//TESTING
		
		
		
		
		Collections.sort(selectedFrets);
		
		if((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
		{
			Scalar colour;
			int x = 0;
			for(DetectedLine fret : finalFrets)
			{
				if (x % 3 == 0)
				{
					colour = new Scalar(255,0,0);
				}
				else if (x % 3 == 1)
				{
					colour = new Scalar(0,255,0);
				}
				else if (x % 3 == 2)
				{
					colour = new Scalar(0,0,255);
				}
				else
				{
					colour = new Scalar(255,0,255);
				}
				Imgproc.line(imageToAnnotate, fret.getPoint1(), fret.getPoint2(), colour);
				x++;
			}
		}
		
		Random randomGenerator = new Random();
		
		if(processingOptions == ImageProcessingOptions.DRAWCLUSTERS)
		{
			for(ArrayList<DetectedLine> stringGroup: fretGroupings)
			{
				Scalar colour = new Scalar(randomGenerator.nextInt(255),randomGenerator.nextInt(255),randomGenerator.nextInt(255));
				
				for(DetectedLine string: stringGroup)
				{
					Imgproc.line(imageToAnnotate, string.getPoint1(), string.getPoint2(), colour);
				}
			}
		}
		
		return finalFrets;
	}
	
	private ArrayList<DetectedLine> getLinesFromParameters(Mat houghLines)
	{
		ArrayList<DetectedLine> guitarStrings = new ArrayList<DetectedLine>();

		for (int lineIndex = 0; lineIndex < houghLines.rows(); lineIndex++)
		{
			double[] polarLineParameters = houghLines.get(lineIndex, 0);

			DetectedLine currentString = new DetectedLine(polarLineParameters[0], polarLineParameters[1]);

			guitarStrings.add(currentString);
		}

		return guitarStrings;
	}

	private ArrayList<DetectedLine> filterGuitarFrets(ArrayList<DetectedLine> candidateFrets, ArrayList<GuitarString> guitarStrings)
	{
		//Strings
		double totalStringAngle = 0;

		for(DetectedLine curString : guitarStrings)
		{
			totalStringAngle += curString.theta;
		}

		double averageStringAngle = totalStringAngle/guitarStrings.size();
		
		double perpendicularAllowance = 1.3;
		
		//Frets
		//Get only perpendicular strings
		
		ArrayList<DetectedLine> perpendicularLines = new ArrayList<DetectedLine>();

		for(DetectedLine curString : candidateFrets)
		{
			if ((curString.theta > averageStringAngle + perpendicularAllowance) || (curString.theta < averageStringAngle - perpendicularAllowance))
			{
				perpendicularLines.add(curString);
			}
		}
		
		//Get parallel frets
		double totalAngle = 0;

		for(DetectedLine curString : perpendicularLines)
		{
			totalAngle += curString.theta;
		}

		double averageAngle = totalAngle/perpendicularLines.size();

		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		if (perpendicularLines.size() > 0)
		{

			for(int a = 0; a < perpendicularLines.size(); a++)
			{
				if (!((perpendicularLines.get(a).theta > averageAngle + angleAllowance) || (perpendicularLines.get(a).theta < averageAngle - angleAllowance)))
				{
					filteredStrings.add(perpendicularLines.get(a));
				}
			}
		}

		return filteredStrings;
	}
	
	private ArrayList<DetectedLine> filterGuitarNeckFrets(ArrayList<DetectedLine> candidateFrets)
	{
		double totalAngle = 0;

		for(DetectedLine curFret : candidateFrets)
		{
			double curAngle = curFret.theta;
			//double curAngleAroundZero = (curAngle > Math.PI) ? curAngle - (2 * Math.PI) : curAngle;
			totalAngle += curFret.theta;//curAngleAroundZero;
		}

		double averageAngle = totalAngle % (Math.PI * 2)/candidateFrets.size();
		System.out.println(averageAngle);

		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		for(int a = 0; a < candidateFrets.size(); a++)
		{
			double curAngle = candidateFrets.get(a).theta;
			double currentAngleAroundZero = (curAngle > Math.PI) ? curAngle - (2 * Math.PI) : curAngle;
			if (!((currentAngleAroundZero > averageAngle + angleAllowanceNeck) || (currentAngleAroundZero < averageAngle - angleAllowanceNeck)))
			{
				filteredStrings.add(candidateFrets.get(a));
			}
		}

		return filteredStrings;
	}

	private ArrayList<ArrayList<DetectedLine>> clusterGuitarFrets(ArrayList<DetectedLine> filteredStrings, int noGroups)
	{
		//Create matrix to cluster with the y intercepts of all lines
		Mat linesToCluster = new Mat(filteredStrings.size(), 1, CvType.CV_32F);

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			DetectedLine curString = filteredStrings.get(a);
			double yIntercept = curString.rho / Math.sin(curString.theta);
			linesToCluster.put(a, 0, yIntercept);
		}

		Mat clusterLabels = new Mat();

		if (!(filteredStrings.size() > noGroups))
		{
			noGroups = filteredStrings.size();
		}

		if (filteredStrings.size() == 0)
		{
			return new ArrayList<ArrayList<DetectedLine>>();
		}
		
		
		Core.kmeans(linesToCluster, noGroups, clusterLabels, new TermCriteria(TermCriteria.COUNT, 50, 1), 10, Core.KMEANS_RANDOM_CENTERS);
		
		ArrayList<ArrayList<DetectedLine>> groupedStrings = new ArrayList<ArrayList<DetectedLine>>();

		for(int c = 0; c < noGroups; c++)
		{
			groupedStrings.add(new ArrayList<DetectedLine>());
		}

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			int group = (int) (clusterLabels.get(a, 0)[0]);

			if ((group >= 0) && (group < groupedStrings.size()) && (a < filteredStrings.size()))
			{
				groupedStrings.get(group).add(filteredStrings.get(a));
			}
		}
		
		return groupedStrings;
	}

	private ArrayList<DetectedLine> selectEachGuitarFret(ArrayList<ArrayList<DetectedLine>> groupedStrings)
	{	
		
		ArrayList<DetectedLine> finalStrings = new ArrayList<DetectedLine>();
		
		for(int b = 0; b < groupedStrings.size(); b++)
		{
			ArrayList<Double> rhoValues = new ArrayList<Double>();
			
			for(DetectedLine s : groupedStrings.get(b))
			{
				rhoValues.add((Double) s.rho);
			}

			if (rhoValues.size() > 0)
			{
				Collections.sort(rhoValues);

				Double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(DetectedLine s : groupedStrings.get(b))
				{
					if ((double) middleValue == s.rho)
					{
						finalStrings.add(s);
						break;
					}
				}
			}
		}
		
		return finalStrings;
	}
	
	public void setAngleAllowance(int newAngle)
	{
		angleAllowance = newAngle;
	}
	
	public void setNumberOfStringsToDetect(int newNumber)
	{
		numberFretsToDetect = newNumber;
	}
}
