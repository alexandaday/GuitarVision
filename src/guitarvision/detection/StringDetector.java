package guitarvision.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

public class StringDetector {	
	private double angleAllowance = 0.35;

	private int numberLinesToDetectInitial = 8;
	private int numberLinesToDetectFinal = 6;
	
	private Scalar stringColour = new Scalar(0,255,0);
	
	private double previousStringsWeight = 0.95;
	
	/**
	 * Find guitar strings in image
	 * @param image containing guitar
	 * @param copy of image to annotate strings for user
	 * @param edge detector to use for finding the strings
	 * @param list of strings from previous frame
	 * @param processing option object, whether to draw strings and clusters on the annotated image
	 * @return
	 */
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
		
		ArrayList<DetectedLine> parallelLines = filterGuitarStringsByAngle(initialLines);
		
		int noGroups = numberLinesToDetectInitial;
		
		ArrayList<ArrayList<DetectedLine>> stringGroupings = clusterGuitarStrings(parallelLines, noGroups);
		
		ArrayList<DetectedLine> selectedStrings = selectCentralLinesFromClusters(stringGroupings);
		
		Collections.sort(selectedStrings);
		
		ArrayList<DetectedLine> filteredStrings = evenlyDistributeStrings(selectedStrings, numberLinesToDetectFinal);
	
		if (filteredStrings == null)
		{
			filteredStrings = new ArrayList<DetectedLine>();
		}

		ArrayList<GuitarString> guitarStringsFiltered = new ArrayList<GuitarString>();
		
		for(DetectedLine line : filteredStrings)
		{
			if (line instanceof GuitarString)
			{
				guitarStringsFiltered.add((GuitarString) line);
			}
		}
		
		ArrayList<GuitarString> finalStrings = performPreviousStringWeighting(guitarStringsFiltered, previousStrings);

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

	private ArrayList<DetectedLine> filterGuitarStringsByAngle(ArrayList<DetectedLine> candidateStrings)
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
		//Cluster lines by y intercept
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

	private ArrayList<DetectedLine> selectCentralLinesFromClusters(ArrayList<ArrayList<DetectedLine>> groupedStrings)
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

	//Assumes the lines are already sorted by rho value
	public ArrayList<DetectedLine> evenlyDistributeStrings(ArrayList<DetectedLine> lines, int numberOfLines)
	{
		if (lines.size() == 0) return null;

		//Calculate mean and median string separation
		ArrayList<DetectedLine> filteredLines = new ArrayList<DetectedLine>();

		double totalDistance = 0;
		double[] distances = new double[lines.size()-1];

		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
			int x = compareTo + 1;

			DetectedLine line1 = lines.get(x);
			DetectedLine line2 = lines.get(compareTo);
			double distance = line1.getRho() - line2.getRho();
			totalDistance += distance;
			distances[compareTo] = distance;
		}

		double averageDistance = totalDistance / lines.size();

		Arrays.sort(distances);

		double medianDistance = distances[(int) Math.floor(distances.length /2)];

		boolean prevAddedBoth = false;

		//Remove a string from pairs which are separated by more than double or less than half the median distance
		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
			int x = compareTo + 1;

			DetectedLine line1 = lines.get(compareTo);
			DetectedLine line2 = lines.get(x);
			double distance = line2.getRho() - line1.getRho();

			if (!((distance < medianDistance / 2) || (distance > medianDistance * 2)))
			{
				if (!prevAddedBoth) filteredLines.add(line1);
				filteredLines.add(line2);
				prevAddedBoth = true;
			}
			else
			{
				prevAddedBoth = false;
			}
		}

		//Remove a string from pairs with smallest separation when there are more than numberOfLinesRequired strings
		while (filteredLines.size () > numberOfLines)
		{
			double smallestDistance = Double.MAX_VALUE;
			int smallestIndex = 0;

			for(int compareTo = 0; compareTo < filteredLines.size() - 1; compareTo++)
			{
				int x = compareTo + 1;

				DetectedLine line1 = filteredLines.get(compareTo);
				DetectedLine line2 = filteredLines.get(x);
				double distance = line2.getRho() - line1.getRho();

				if (distance < smallestDistance)
				{
					smallestDistance = distance;
					smallestIndex = compareTo;
				}
			}

			filteredLines.remove(smallestIndex);
		}

		//Evenly space strings
		//Start with lower middle string and work outwards using expected positions calculated from the median rho separation

		double[] rhoValues = new double[filteredLines.size()];
		double[] thetaValues = new double[filteredLines.size()];
		int x =  0;
		for(DetectedLine line : filteredLines)
		{
			rhoValues[x] = line.getRho();
			thetaValues[x] = line.getTheta();
			x++;
		}

		int middleIndex = (int) Math.floor(rhoValues.length / 2);

		double curRho = filteredLines.get(middleIndex).getRho();;
		double curTheta = filteredLines.get(middleIndex).getTheta();

		double tolerance = averageDistance/2;

		for (int index = middleIndex; index >= 0; index--)
		{
			double expectedRho = curRho;

			double curStringRho = filteredLines.get(index).getRho();

			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);

			if (!stringExists)
			{
				filteredLines.get(index).setRho(expectedRho);
				filteredLines.get(index).setTheta((filteredLines.get(index).getTheta() + curTheta) / 2);
			}

			curRho -= medianDistance;
		}

		curRho = filteredLines.get(middleIndex).getRho() + medianDistance;

		for (int index = middleIndex + 1; index < rhoValues.length; index++)
		{
			double expectedRho = curRho;

			double curStringRho = filteredLines.get(index).getRho();

			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);

			if (!stringExists)
			{
				filteredLines.get(index).setRho(expectedRho);
				filteredLines.get(index).setTheta((filteredLines.get(index).getTheta() + curTheta) / 2);
			}
			curRho += medianDistance;
		}
		return filteredLines;

	}

	public ArrayList<GuitarString> performPreviousStringWeighting(ArrayList<GuitarString> curStrings, ArrayList<GuitarString> previousStrings)
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
				curStrings.get(x).setRho((curStrings.get(x).getRho() * (1 - previousStringsWeight)) + (previousStrings.get(x).getRho() * previousStringsWeight));
				curStrings.get(x).setTheta((curStrings.get(x).getTheta() * (1 - previousStringsWeight)) + (previousStrings.get(x).getTheta() * previousStringsWeight));
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
		numberLinesToDetectFinal = newNumber;
	}
	
	public int getNumberOfStringsToDetect()
	{
		return numberLinesToDetectFinal;
	}
}

