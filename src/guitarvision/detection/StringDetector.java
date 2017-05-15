package guitarvision.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

public class StringDetector {	
	private double angleAllowance = 0.35;

	private int numberInitialLinesToDetect = 8;
	private int numberStringsToDetect = 6;
	
	private Scalar stringColour = new Scalar(0,255,0);
	
	private double previousStringsWeighting = 0.95;
	
	public ArrayList<GuitarString> getAccurateGuitarStrings(Mat originalImage, Mat imageToAnnotate, ArrayList<GuitarString> previousStrings, ImageProcessingOptions processingOptions)
	{
		HashMap<Integer, ArrayList<GuitarString>> allStrings = new HashMap<Integer, ArrayList<GuitarString>>();
		Integer highestScore = null;
		int bestCanny = 0;
		
		//Find best scoring canny upper threshold
		for (int cannyUpper = 50; cannyUpper < 400; cannyUpper+=50)
		{
			//System.out.println("CANNY THRESHOLD");
			//System.out.println(cannyUpper);
			
			EdgeDetector edgeDetector = new EdgeDetector();
			edgeDetector.setCannyLowerThreshold(0);
			edgeDetector.setCannyUpperThreshold(cannyUpper);
			edgeDetector.setHoughThreshold(300);
			
			ArrayList<GuitarString> curStrings = getGuitarStrings(originalImage, imageToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.NOPROCESSING);
		
			int curScore = getSpacingScore(curStrings);
			
			allStrings.put(curScore, curStrings);

			if ((highestScore == null) || (curScore > highestScore))
			{
				highestScore = curScore;
				bestCanny = cannyUpper;
			}
		}
		
		//Find best scoring hough threshold for the canny threshold
		for (int houghUpper = 300; houghUpper < 400; houghUpper+=50)
		{
			EdgeDetector edgeDetector = new EdgeDetector();
			edgeDetector.setCannyLowerThreshold(0);
			edgeDetector.setCannyUpperThreshold(bestCanny);
			edgeDetector.setHoughThreshold(houghUpper);
			
			ArrayList<GuitarString> curStrings = getGuitarStrings(originalImage, imageToAnnotate, edgeDetector, previousStrings, ImageProcessingOptions.NOPROCESSING);
		
			int curScore = getSpacingScore(curStrings);
			
			allStrings.put(curScore, curStrings);

			if ((highestScore == null) || (curScore > highestScore))
			{
				highestScore = curScore;
			}
		}
		
		ArrayList<GuitarString> result = null;
		
		if ((highestScore != null) && (allStrings.containsKey(highestScore)))
		{
			result = allStrings.get(highestScore);
		}
		
		if((processingOptions == ImageProcessingOptions.DRAWSELECTEDLINES) || (processingOptions == ImageProcessingOptions.DRAWCLUSTERS))
		{
			for(DetectedLine string : result)
			{
				Imgproc.line(imageToAnnotate, string.getPoint1(), string.getPoint2(), stringColour);
			}
		}
		
		
		return result;
	}
	
	public int getSpacingScore(ArrayList<GuitarString> strings)
	{
		//Assume strings are sorted
		
		int toleranceInPixels = 3;
		
		HashMap<Integer, Integer> binCount = new HashMap<Integer,Integer>();
		
		for(int index = 0; index < strings.size() - 1; index++)
		{
			double curRho = strings.get(index).getYIntercept();
			double nextRho = strings.get(index+1).getYIntercept();
			
			//THEY AREN'T SORTED BY Y INTERCEPT
			
			double difference = nextRho - curRho;
			
			//System.out.println("Difference");
			//System.out.println(difference);
			
			//Plus one to avoid quantising to 0
			Integer differenceQuantised = (int) (difference - (difference % toleranceInPixels)) + 1;
			
			//Ignore strings which aren't separated
			if (difference == 0) differenceQuantised = 0;
			
			if (binCount.containsKey(differenceQuantised))
			{
				binCount.put(differenceQuantised, binCount.get(differenceQuantised)+1);
			}
			else
			{
				binCount.put(differenceQuantised, 1);
			}
			//Compare each to next in sorted order
			//Create count of number of each distance
			//Disregard 0 pixels
			//Take maximum similar - weighted of others
		}
		
		//Returns how many lines are separated by the mode length
		int maxBin = 0;
		
		for(Integer difference: binCount.keySet())
		{
			if (difference != 0)
			{
				int count = binCount.get(difference);
				if (count > maxBin) maxBin = count;
			}
		}
		
		//System.out.println("Score");
		//System.out.println(maxBin);
		
		return maxBin;
	}
	
	public ArrayList<GuitarString> getGuitarStrings(Mat originalImage, Mat imageToAnnotate, EdgeDetector edgeDetector, ArrayList<GuitarString> previousStrings, ImageProcessingOptions processingOptions)
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
		
		int noGroups = numberInitialLinesToDetect;
		
		ArrayList<ArrayList<DetectedLine>> stringGroupings = clusterGuitarStrings(parallelLines, noGroups);
		
		ArrayList<DetectedLine> selectedStrings = selectCenterGuitarString(stringGroupings);
		
		Collections.sort(selectedStrings);
		
		//ArrayList<ArrayList<DetectedLine>> stringGroupings2 = clusterGuitarStrings(selectedStrings, 6);
		
		//ArrayList<DetectedLine> selectedStrings2 = selectCenterGuitarString(stringGroupings2);
		
		
		ArrayList<DetectedLine> filteredStrings = edgeDetector.evenlyDistributeLines(selectedStrings, numberStringsToDetect, Intercept.YINTERCEPT);
	
		if (filteredStrings == null)
		{
			filteredStrings = new ArrayList<DetectedLine>();
		}
		
		//PERFORM EVEN DISTRIBUTION BEFORE SORT STRINGS
		
		ArrayList<GuitarString> guitarStringsFiltered = new ArrayList<GuitarString>();
		
		for(DetectedLine l : filteredStrings)
		{
			if (l instanceof GuitarString)
			{
				guitarStringsFiltered.add((GuitarString) l);
			}
		}
		
		ArrayList<GuitarString> finalStrings = trackStrings(guitarStringsFiltered, previousStrings);
		
		
		
		
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
		
		return finalStrings;
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
			totalAngle += curString.getTheta();
		}

		double averageAngle = totalAngle/candidateStrings.size();

		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		if (candidateStrings.size() > 0)
		{

			for(int a = 0; a < candidateStrings.size(); a++)
			{
				if (!((candidateStrings.get(a).getTheta() > averageAngle + angleAllowance) || (candidateStrings.get(a).getTheta() < averageAngle - angleAllowance)))
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
		
		//Maybe use the centers returned
		//Mat centers = new Mat();
		//Core.kmeans(linesToCluster, noGroups, clusterLabels, new TermCriteria(TermCriteria.COUNT, 50, 1), 10, Core.KMEANS_RANDOM_CENTERS, centers);
		
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

	private ArrayList<DetectedLine> selectCenterGuitarString(ArrayList<ArrayList<DetectedLine>> groupedStrings)
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
				
				double smallestValue = rhoValues.get(0);
				double largestValue = rhoValues.get(rhoValues.size() - 1);
				
				double thickness = largestValue - smallestValue;

				double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(DetectedLine s : groupedStrings.get(b))
				{
					if ((double) middleValue == s.getRho())
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
	
	public ArrayList<GuitarString> trackStrings(ArrayList<GuitarString> curStrings, ArrayList<GuitarString> previousStrings)
	{
		if (previousStrings == null)
		{
			return curStrings;
		}
		
		Collections.sort(curStrings);
		Collections.sort(previousStrings);
		
		if (curStrings.size() >= previousStrings.size())
		{
			for(int x = 0; x < curStrings.size() && x < previousStrings.size(); x++)
			{
				curStrings.get(x).setRho((curStrings.get(x).getRho() * (1 - previousStringsWeighting)) + (previousStrings.get(x).getRho() * previousStringsWeighting));
				curStrings.get(x).setTheta((curStrings.get(x).getTheta() * (1 - previousStringsWeighting)) + (previousStrings.get(x).getTheta() * previousStringsWeighting));
			}
		}
		else
		{
			curStrings = previousStrings;
		}
		
		return curStrings;
	}
	
	public void setAngleAllowance(int newAngle)
	{
		angleAllowance = newAngle;
	}
	
	public double getAngleAllowance()
	{
		return angleAllowance;
	}
	
	public void setNumberOfStringsToDetect(int newNumber)
	{
		numberStringsToDetect = newNumber;
	}
	
	public int getNumberOfStringsToDetect()
	{
		return numberStringsToDetect;
	}
}

