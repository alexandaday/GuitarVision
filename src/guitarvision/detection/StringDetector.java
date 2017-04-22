package guitarvision.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

public class StringDetector {
	public double angleAllowance = 1;
	public static int numberStringsToDetect = 6;
	
	private Scalar stringColour = new Scalar(0,255,0);
	
	public ArrayList<GuitarString> getGuitarStrings(Mat originalImage, Mat imageToAnnotate, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
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
		
		ArrayList<DetectedLine> parallelLines = filterGuitarStrings(initialLines);
		
		int noGroups = numberStringsToDetect;
		
		ArrayList<ArrayList<DetectedLine>> stringGroupings = clusterGuitarStrings(parallelLines, noGroups);
		
		ArrayList<DetectedLine> selectedStrings = selectEachGuitarString(stringGroupings);
		
		ArrayList<DetectedLine> finalStrings = selectedStrings;//= edgeDetector.evenlyDistribute(selectedStrings, 6, Intercept.YINTERCEPT);
		
		
		if((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
		{
			for(DetectedLine string : finalStrings)
			{
				Imgproc.line(imageToAnnotate, string.getPoint1(), string.getPoint2(), stringColour);
			}
		}
		
		Random randomGenerator = new Random();
		randomGenerator.setSeed(50);
		
		if(processingOptions == ImageProcessingOptions.DRAWCLUSTERS)
		{
			for(ArrayList<DetectedLine> stringGroup: stringGroupings)
			{
				Scalar colour = new Scalar(randomGenerator.nextInt(255),randomGenerator.nextInt(255),randomGenerator.nextInt(255));
				
				for(DetectedLine string: stringGroup)
				{
					Imgproc.line(imageToAnnotate, string.getPoint1(), string.getPoint2(), colour);
				}
			}
		}
		
		ArrayList<GuitarString> guitarStringsFinal = new ArrayList<GuitarString>();
		
		for(DetectedLine l : finalStrings)
		{
			if (l instanceof GuitarString)
			{
				guitarStringsFinal.add((GuitarString) l);
			}
		}
		
		return guitarStringsFinal;
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

	private ArrayList<DetectedLine> filterGuitarStrings(ArrayList<DetectedLine> candidateStrings)
	{
		double totalAngle = 0;

		for(DetectedLine curString : candidateStrings)
		{
			totalAngle += curString.theta;
		}

		double averageAngle = totalAngle/candidateStrings.size();

		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		if (candidateStrings.size() > 0)
		{

			for(int a = 0; a < candidateStrings.size(); a++)
			{
				if (!((candidateStrings.get(a).theta > averageAngle + angleAllowance) || (candidateStrings.get(a).theta < averageAngle - angleAllowance)))
				{
					filteredStrings.add(candidateStrings.get(a));
				}
			}
		}

		return filteredStrings;
	}

	private ArrayList<ArrayList<DetectedLine>> clusterGuitarStrings(ArrayList<DetectedLine> filteredStrings, int noGroups)
	{
		//Create matrix to cluster with the y intercepts of all lines
		Mat linesToCluster = new Mat(filteredStrings.size(), 1, CvType.CV_32F);

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			DetectedLine curString = filteredStrings.get(a);
			double yIntercept = curString.getYIntercept();
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
		
		//Maybe use compact value it returns IMPORTANT
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

	private ArrayList<DetectedLine> selectEachGuitarString(ArrayList<ArrayList<DetectedLine>> groupedStrings)
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
				
				double smallestValue = rhoValues.get(0);
				double largestValue = rhoValues.get(rhoValues.size() - 1);
				
				double thickness = largestValue - smallestValue;

				double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(DetectedLine s : groupedStrings.get(b))
				{
					if ((double) middleValue == s.rho)
					{
						GuitarString string = new GuitarString(thickness, s);
						
						finalStrings.add(string);
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
		numberStringsToDetect = newNumber;
	}
}
