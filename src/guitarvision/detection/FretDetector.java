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
	public double angleAllowanceNeck = 0.5;
	public int initialNumberFretsToDetect = 30;
	public int numberFretsToDetect = 20;
	
	private double previousFretsWeighting = 0.9999;
	
	public ArrayList<DetectedLine> getGuitarFrets(Mat imageToProcess, Mat imageToAnnotate, ArrayList<GuitarString> guitarStrings, EdgeDetector edgeDetector, ArrayList<DetectedLine> previousFrets, ImageProcessingOptions processingOptions)
	{
		double width = imageToProcess.width();
		double height = imageToProcess.height()/4;
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
		
		ArrayList<DetectedLine> finalFrets = trackFrets(guitarNeckFrets, previousFrets);
		
		
		
		//Engine.getInstance().exportImage(guitarNeckImage, "heck.png");
		
		if ((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
		{
			int x = 0;
			Scalar colour = null;
			
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
		
		return finalFrets;
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
		
		
		Mat hsvImage = new Mat();
		Imgproc.cvtColor(guitarNeckImage, hsvImage, Imgproc.COLOR_BGR2HSV);
		//Engine.getInstance().exportImage(hsvImage, "neckImage.png");

		Mat cannyProcessedImage = edgeDetector.getEdges(guitarNeckImage);
		
		
		
		Mat houghLineParameters = edgeDetector.houghTransform(cannyProcessedImage);
		
		ArrayList<DetectedLine> initialLines = getLinesFromParameters(houghLineParameters);
		
		//System.out.println("Number of lines detected: ");
		//System.out.println(initialLines.size());
		
		ArrayList<DetectedLine> parallelLines = filterGuitarNeckFrets(initialLines);
		
		
		
		
		int noGroups = initialNumberFretsToDetect;
		
		ArrayList<ArrayList<DetectedLine>> fretGroupings = clusterGuitarFrets(parallelLines, noGroups);
		
		//System.out.println("Groupigns");
		
//		for (ArrayList<DetectedLine> grouping : fretGroupings)
//		{
//			System.out.println(grouping.size());
//		}
		
		ArrayList<DetectedLine> selectedFrets = selectEachGuitarFret(fretGroupings);
		
		//System.out.println(selectedFrets.size());
		
		Collections.sort(selectedFrets);
		
		ArrayList<DetectedLine> finalFrets = edgeDetector.evenlyDistributeLinesExponential(selectedFrets, numberFretsToDetect, Intercept.XINTERCEPT);//edgeDetector.evenlyDistribute(selectedFrets, numberFretsToDetect, Intercept.XINTERCEPT);
		
		//System.out.println("FINAL");
		//System.out.println(selectedFrets.size());
		
		//System.out.println("NO frets generated:"+finalFrets.size());
		
		if (finalFrets == null)
		{
			finalFrets = new ArrayList<DetectedLine>();
		}
		
			//Engine.getInstance().exportImage(cannyProcessedImage, "usingnecknow_without.png");
		
//			Mat exportColour = new Mat();
//			Imgproc.cvtColor(cannyProcessedImage, exportColour, Imgproc.COLOR_GRAY2RGB);
//				for(DetectedLine fret : parallelLines)
//				{
//					Imgproc.line(exportColour, fret.getPoint1(), fret.getPoint2(), new Scalar(200,0,255));
//				}
//				for(DetectedLine fret : selectedFrets)
//				{
//					Imgproc.line(exportColour, fret.getPoint1(), fret.getPoint2(), new Scalar(255,200,0));
//				}
//				
//				Engine.getInstance().exportImage(exportColour, "usingnecknow.png");
				
//				Mat result = new Mat();
//				Imgproc.warpPerspective(exportColour, result, inverseNeckWarp, new Size(960,540));
				//Engine.getInstance().exportImage(result, "warpedimage.png");
		
		
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
			totalStringAngle += curString.getTheta();
		}

		double averageStringAngle = totalStringAngle/guitarStrings.size();
		
		double perpendicularAllowance = 1.3;
		
		//Frets
		//Get only perpendicular strings
		
		ArrayList<DetectedLine> perpendicularLines = new ArrayList<DetectedLine>();

		for(DetectedLine curString : candidateFrets)
		{
			if ((curString.getTheta() > averageStringAngle + perpendicularAllowance) || (curString.getTheta() < averageStringAngle - perpendicularAllowance))
			{
				perpendicularLines.add(curString);
			}
		}
		
		//Get parallel frets
		double totalAngle = 0;

		for(DetectedLine curString : perpendicularLines)
		{
			totalAngle += curString.getTheta();
		}

		double averageAngle = totalAngle/perpendicularLines.size();

		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		if (perpendicularLines.size() > 0)
		{

			for(int a = 0; a < perpendicularLines.size(); a++)
			{
				if (!((perpendicularLines.get(a).getTheta() > averageAngle + angleAllowance) || (perpendicularLines.get(a).getTheta() < averageAngle - angleAllowance)))
				{
					filteredStrings.add(perpendicularLines.get(a));
				}
			}
		}

		return filteredStrings;
	}
	
	private ArrayList<DetectedLine> filterGuitarNeckFrets(ArrayList<DetectedLine> candidateFrets)
	{
		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		for(int a = 0; a < candidateFrets.size(); a++)
		{
			double curAngle = candidateFrets.get(a).getTheta();
			double currentAngleAroundZero = (curAngle > Math.PI / 2) ? curAngle - (Math.PI) : curAngle;
			if (!((currentAngleAroundZero > angleAllowanceNeck) || (currentAngleAroundZero < -angleAllowanceNeck)))
			{
				filteredStrings.add(candidateFrets.get(a));
			}
		}

		return filteredStrings;
	}

	private ArrayList<ArrayList<DetectedLine>> clusterGuitarFrets(ArrayList<DetectedLine> filteredStrings, int noGroups)
	{
		//Create matrix to cluster with the rho of all lines
		Mat linesToCluster = new Mat(filteredStrings.size(), 1, CvType.CV_32F);

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			DetectedLine curString = filteredStrings.get(a);
			double rhoValue = curString.getRho();// .getXIntercept();
			linesToCluster.put(a, 0, rhoValue);
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
		
		
		Core.kmeans(linesToCluster, noGroups, clusterLabels, new TermCriteria(TermCriteria.COUNT, 5, 1), 5, Core.KMEANS_RANDOM_CENTERS);
		
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
				rhoValues.add((Double) s.getRho());
			}

			if (rhoValues.size() > 0)
			{
				Collections.sort(rhoValues);

				Double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(DetectedLine s : groupedStrings.get(b))
				{
					if ((double) middleValue == s.getRho())
					{
						finalStrings.add(s);
						break;
					}
				}
			}
		}
		
		return finalStrings;
	}
	
	public ArrayList<DetectedLine> trackFrets(ArrayList<DetectedLine> curFrets, ArrayList<DetectedLine> previousFrets)
	{
		if (previousFrets == null)
		{
			return curFrets;
		}
		
		Collections.sort(curFrets);
		Collections.sort(previousFrets);
		
		ArrayList<DetectedLine> newFrets = new ArrayList<DetectedLine>();
		
		if (curFrets.size() >= previousFrets.size())
		{
			for(int x = 0; x < curFrets.size() && x < previousFrets.size(); x++)
			{
				
				double newRho = (curFrets.get(x).getRho() * (1 - previousFretsWeighting)) + (previousFrets.get(x).getRho() * previousFretsWeighting);
				double newTheta = (curFrets.get(x).getTheta() * (1 - previousFretsWeighting)) + (previousFrets.get(x).getTheta() * previousFretsWeighting);

				DetectedLine newFret = new DetectedLine(newRho, newTheta);
				newFrets.add(newFret);
			}
		}
		else
		{
			newFrets = previousFrets;
		}
		
		return newFrets;
	}
	
	public void setAngleAllowance(int newAngle)
	{
		angleAllowance = newAngle;
	}
	
	public void setNumberOfStringsToDetect(int newNumber)
	{
		initialNumberFretsToDetect = newNumber;
	}
}
