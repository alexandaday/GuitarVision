package guitarvision.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import sun.net.www.content.audio.x_aiff;

public class EdgeDetector {	
	private int blurKernelSize = 3;
	
	private int cannyLowerThreshold = 0;
	//Severe error - malloc problems, when this value is small up to 143 (with OpenCV 3.1)
	private int cannyUpperThreshold = 144;
	
	private int houghThreshold = 300;
	
	public Mat getEdges(Mat image)
	{
		Mat blurredImage = image.clone();

		//Imgproc.blur(blurredImage, blurredImage, new Size(blurKernelSize, blurKernelSize));
		Imgproc.GaussianBlur(image, blurredImage,  new Size(blurKernelSize, blurKernelSize), 1);
		//Imgproc.medianBlur(image, blurredImage, 3);

		Mat detectedEdges = new Mat();

		Imgproc.Canny(blurredImage, detectedEdges, cannyLowerThreshold, cannyUpperThreshold);
		
		return detectedEdges;
	}

	public Mat houghTransform(Mat edgeImage)
	{
		Mat edges = new Mat();

		Imgproc.HoughLines(edgeImage, edges, 1, Math.PI/360, houghThreshold);

		return edges;
	}
	
	public void setBlurFilterSize(int newSize)
	{
		blurKernelSize = newSize;
	}
	
	public void setCannyLowerThreshold(int newThreshold)
	{
		cannyLowerThreshold = newThreshold;
	}
	
	public void setCannyUpperThreshold(int newThreshold)
	{
		cannyUpperThreshold = newThreshold;
	}
	
	public void setHoughThreshold(int newThreshold)
	{
		houghThreshold = newThreshold;
	}
	
	//Currently assumes lines are sorted
	public ArrayList<DetectedLine> evenlyDistribute(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
	{
		HashMap<Integer, ArrayList<DetectedLine>> occurrences = new HashMap<Integer, ArrayList<DetectedLine>>();
		
		//Round each distance to the nearest 2 pixels
		int tolerance = 2;
		
		for(int compareTo = 0; compareTo < lines.size(); compareTo++)
		{
			for(int x = compareTo + 1; x < lines.size(); x++)
			{
				DetectedLine line1 = lines.get(x);
				DetectedLine line2 = lines.get(compareTo);
				double distance = line1.rho - line2.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)

				double roundedDistance = Math.floor((distance / tolerance)) * tolerance;
				
				int difference = x - compareTo;
				
				int normalisedDistance = (int) (roundedDistance / difference);

				if (occurrences.containsKey(normalisedDistance))
				{
					ArrayList<DetectedLine> currentLines = occurrences.get(normalisedDistance);
					if (!currentLines.contains(line1))
					{
						currentLines.add(line1);
					}
					if (!currentLines.contains(line2))
					{
						currentLines.add(line2);
					}
					//Don't need to put because have reference?
					//occurrences.put(normalisedDistance, currentLines);
				}
				else
				{
					ArrayList<DetectedLine> currentLines = new ArrayList<DetectedLine>();
					currentLines.add(line1);
					currentLines.add(line2);
					occurrences.put(normalisedDistance, currentLines);
				}

			}
		}
		
		int modeKey = 0;
		int modeCount = 0;
		
		for (Integer key: occurrences.keySet())
		{
			ArrayList<DetectedLine> matchingLines = occurrences.get(key);
			
			int count = matchingLines.size();
			
			if (modeCount <= count)
			{
				modeKey = key;
				modeCount = count;
			}
		}
		
		ArrayList<DetectedLine> linesToUse = occurrences.get(modeKey);
		
		if (occurrences.keySet().size() == 0) return lines;
		
		Collections.sort(linesToUse);
		
		//Maybe break and loop forever
//		for (int x = 0; x < linesToUse.size() - 1; x++)
//		{
//			DetectedLine currentLine = linesToUse.get(x);
//			DetectedLine nextLine = linesToUse.get(x+1);
//			double expectedRho = currentLine.rho + modeKey;
//			
//			if (!(nextLine.rho < expectedRho + tolerance && nextLine.rho > expectedRho - tolerance))
//			{
//				if (!(nextLine.rho < expectedRho + tolerance))
//				{
//					linesToUse.remove(x+1);
//				}
//				
//				DetectedLine newLine = new DetectedLine(expectedRho, currentLine.theta);
//				
//				
//				linesToUse.add(x+1, newLine);
//			}
//		}
		
		int index = 0;
		
		while (linesToUse.size() < numberOfLinesRequired)
		{
			if (index < (linesToUse.size() - 1))
			{
				DetectedLine currentLine = linesToUse.get(index);
				DetectedLine nextLine = linesToUse.get(index+1);
				double expectedRho = currentLine.rho + modeKey;
				
				if (!(nextLine.rho < expectedRho + tolerance && nextLine.rho > expectedRho - tolerance))
				{
					if (!(nextLine.rho < expectedRho + tolerance))
					{
						linesToUse.remove(index+1);
					}
					
					DetectedLine newLine = new DetectedLine(expectedRho, currentLine.theta);
					
					
					linesToUse.add(index+1, newLine);
				}
				index++;
			}
			else
			{
				DetectedLine finalLine = linesToUse.get(linesToUse.size()-1);
				
				double theta = finalLine.theta;
				double expectedRho = finalLine.rho + modeKey;
				
				for(int x = linesToUse.size(); x < numberOfLinesRequired; x++)
				{
					DetectedLine newLine = new DetectedLine(expectedRho, theta);
					
					linesToUse.add(newLine);
					
					expectedRho += modeKey;
				}
				break;
			}
		}
		
//		if (linesToUse.size() > numberOfLinesRequired)
//		{
//			for(int x = numberOfLinesRequired; x < linesToUse.size(); x++)
//			{
//				linesToUse.remove(x);
//			}
//		}
		
		return linesToUse;
	}
	
	//Assume sorted
	public ArrayList<DetectedLine> evenlyDistributeLines(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
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
				double distance = line1.rho - line2.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)
				totalDistance += distance;
				distances[compareTo] = distance;
		}
		
		double averageDistance = totalDistance / lines.size();
		
		Arrays.sort(distances);
		
		double medianDistance = distances[(int) Math.floor(distances.length /2)];
		
		boolean prevAddedBoth = false;
		
		
		//Remove line from pairs which are separated by more than double or less than half the median distance
		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
				int x = compareTo + 1;
						
				DetectedLine line1 = lines.get(compareTo);
				DetectedLine line2 = lines.get(x);
				double distance = line2.rho - line1.rho;
				
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
		
		//Remove those with smallest separation when more than numberOfLinesRequired strings
		while (filteredLines.size () > numberOfLinesRequired)
		{
			double smallestDistance = Double.MAX_VALUE;
			int smallestIndex = 0;

			for(int compareTo = 0; compareTo < filteredLines.size() - 1; compareTo++)
			{
				int x = compareTo + 1;

				DetectedLine line1 = filteredLines.get(compareTo);
				DetectedLine line2 = filteredLines.get(x);
				double distance = line2.rho - line1.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)

				if (distance < smallestDistance)
				{
					smallestDistance = distance;
					smallestIndex = compareTo;
				}

				//boolean removedLine = false;
			}
			
			filteredLines.remove(smallestIndex);
		}
		
		
		//Make strings evenly spaced apart
		
		double[] rhoValues = new double[filteredLines.size()];
		double[] thetaValues = new double[filteredLines.size()];
		double rhoSum = 0;
		int x =  0;
		for(DetectedLine line : filteredLines)
		{
			rhoValues[x] = line.rho;
			thetaValues[x] = line.theta;
			rhoSum += line.rho;
			x++;
		}
		
		double meanRhoValue = rhoSum /filteredLines.size();
		
		//double middleThetaValue = (thetaValues[thetaValues.length - 1] + thetaValues[0])/2;

		//double[] newRhoValues = new double[rhoValues.length];
		
		int middleIndex = (int) Math.floor(rhoValues.length / 2);
		
		//double startRho = meanRhoValue - (medianDistance/2);
		
		double curRho = filteredLines.get(middleIndex).rho;//startRho;
		double curTheta = filteredLines.get(middleIndex).theta;
		
		double tolerance = averageDistance/2;
		
		for (int i = middleIndex; i >= 0; i--)
		{
			double expectedRho = curRho;
			
			double curStringRho = filteredLines.get(i).rho;
			
			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
			
			if (!stringExists)
			{
				//System.out.println("String doesn't exist");
				filteredLines.get(i).rho = expectedRho;
				filteredLines.get(i).theta = (filteredLines.get(i).theta + curTheta) / 2;
			}
			
			
			
			//filteredLines.get(i).rho = curRho;
			//filteredLines.get(i).theta = middleThetaValue;
			curRho -= medianDistance;
		}
		
		curRho = filteredLines.get(middleIndex).rho + medianDistance;
		
		
		for (int i = middleIndex + 1; i < rhoValues.length; i++)
		{
			double expectedRho = curRho;
			
			double curStringRho = filteredLines.get(i).rho;
			
			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
			
			if (!stringExists)
			{
				System.out.println("String doesn't exist");
				filteredLines.get(i).rho = expectedRho;
				filteredLines.get(i).theta = (filteredLines.get(i).theta + curTheta) / 2;
			}
			
			//filteredLines.get(i).rho = curRho;
			//filteredLines.get(i).theta = middleThetaValue;
			curRho += medianDistance;
		}
	
		
		
		return filteredLines;
		
	}
	
	public ArrayList<DetectedLine> evenlyDistributeLinesExponential(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
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
				double distance = line1.rho - line2.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)
				totalDistance += distance;
				distances[compareTo] = distance;
		}
		
		double averageDistance = totalDistance / lines.size();
		
		Arrays.sort(distances);
		
		double medianDistance = distances[(int) Math.floor(distances.length /2)];
		
		boolean prevAddedBoth = false;
		
		
		//Remove line from pairs which are separated by more than double or less than half the median distance
		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
				int x = compareTo + 1;
						
				DetectedLine line1 = lines.get(compareTo);
				DetectedLine line2 = lines.get(x);
				double distance = line2.rho - line1.rho;
				
				if (!((distance < medianDistance / 8) || (distance > medianDistance * 6)))
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
		
		//Remove those with smallest separation when more than numberOfLinesRequired strings
		while (filteredLines.size () > numberOfLinesRequired)
		{
			System.out.println("REMOVING");
			double smallestDistance = Double.MAX_VALUE;
			int smallestIndex = 0;

			for(int compareTo = 0; compareTo < filteredLines.size() - 1; compareTo++)
			{
				int x = compareTo + 1;

				DetectedLine line1 = filteredLines.get(compareTo);
				DetectedLine line2 = filteredLines.get(x);
				double distance = line2.rho - line1.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)

				if (distance < smallestDistance)
				{
					smallestDistance = distance;
					smallestIndex = compareTo;
				}

				//boolean removedLine = false;
			}
			
			filteredLines.remove(smallestIndex);
		}
		
		
		//Make strings evenly spaced apart
		
//		System.out.println(filteredLines.size());
//		
//		double[] rhoValues = new double[filteredLines.size()];
//		double[] thetaValues = new double[filteredLines.size()];
//		double rhoSum = 0;
//		int x =  0;
//		for(DetectedLine line : filteredLines)
//		{
//			rhoValues[x] = line.rho;
//			thetaValues[x] = line.theta;
//			rhoSum += line.rho;
//			x++;
//		}
//		
//		double meanRhoValue = rhoSum /filteredLines.size();
//		
//		//double middleThetaValue = (thetaValues[thetaValues.length - 1] + thetaValues[0])/2;
//
//		//double[] newRhoValues = new double[rhoValues.length];
//		
//		int middleIndex = (int) Math.floor(rhoValues.length / 2);
//		
//		//double startRho = meanRhoValue - (medianDistance/2);
//		
//		double curRho = filteredLines.get(middleIndex).rho;//startRho;
//		double curTheta = filteredLines.get(middleIndex).theta;
//		
//		double tolerance = averageDistance/2;
//		
//		for (int i = middleIndex; i >= 0; i--)
//		{
//			double expectedRho = curRho;
//			
//			double curStringRho = filteredLines.get(i).rho;
//			
//			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
//			
//			if (!stringExists)
//			{
//				//System.out.println("String doesn't exist");
//				filteredLines.get(i).rho = expectedRho;
//				filteredLines.get(i).theta = (filteredLines.get(i).theta + curTheta) / 2;
//			}
//			
//			
//			
//			//filteredLines.get(i).rho = curRho;
//			//filteredLines.get(i).theta = middleThetaValue;
//			curRho -= medianDistance;
//		}
//		
//		curRho = filteredLines.get(middleIndex).rho + medianDistance;
//		
//		
//		for (int i = middleIndex + 1; i < rhoValues.length; i++)
//		{
//			double expectedRho = curRho;
//			
//			double curStringRho = filteredLines.get(i).rho;
//			
//			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
//			
//			if (!stringExists)
//			{
//				//System.out.println("String doesn't exist");
//				filteredLines.get(i).rho = expectedRho;
//				filteredLines.get(i).theta = (filteredLines.get(i).theta + curTheta) / 2;
//			}
//			
//			//filteredLines.get(i).rho = curRho;
//			//filteredLines.get(i).theta = middleThetaValue;
//			curRho += medianDistance;
//		}
	
		
		//Use mean theta
		
		double thetaSum = 0;
		for(DetectedLine line : filteredLines)
		{
			thetaSum += line.theta;
		}
		
		double meanThetaValue = thetaSum/filteredLines.size();
		double meanWeight = 0.4;
		
		for (DetectedLine line: filteredLines)
		{
			line.theta = (line.theta * (1 - meanWeight)) + meanThetaValue * meanWeight;
		}
		
		
		return filteredLines;
		
	}
	
	//Currently assumes lines are sorted
	public ArrayList<DetectedLine> evenlyDistributeByPairs(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
	{
		HashMap<Integer, ArrayList<DetectedLine>> occurrences = new HashMap<Integer, ArrayList<DetectedLine>>();
		
		//Round each distance to the nearest 2 pixels
		int tolerance = 2;
		
		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
			//for(int x = compareTo + 1; x < lines.size(); x++)
			//{
				int x = compareTo + 1;
						
				DetectedLine line1 = lines.get(x);
				DetectedLine line2 = lines.get(compareTo);
				double distance = line1.rho - line2.rho; //line1.getIntercept(intercept) - line2.getIntercept(intercept)

				double roundedDistance = Math.floor((distance / tolerance)) * tolerance;
				
				int difference = x - compareTo;
				
				int normalisedDistance = (int) (roundedDistance / difference);

				if (occurrences.containsKey(normalisedDistance))
				{
					ArrayList<DetectedLine> currentLines = occurrences.get(normalisedDistance);
					if (!currentLines.contains(line1))
					{
						currentLines.add(line1);
					}
					if (!currentLines.contains(line2))
					{
						currentLines.add(line2);
					}
					//Don't need to put because have reference?
					//occurrences.put(normalisedDistance, currentLines);
				}
				else
				{
					ArrayList<DetectedLine> currentLines = new ArrayList<DetectedLine>();
					currentLines.add(line1);
					currentLines.add(line2);
					occurrences.put(normalisedDistance, currentLines);
				}

			//}
		}
		
		
		
		int modeKey = 0;
		int modeCount = 0;
		
		for (Integer key: occurrences.keySet())
		{
			ArrayList<DetectedLine> matchingLines = occurrences.get(key);
			
			int count = matchingLines.size();
			
			if (modeCount <= count)
			{
				modeKey = key;
				modeCount = count;
			}
		}
		
		ArrayList<DetectedLine> linesToUse = occurrences.get(modeKey);
		
		if (occurrences.keySet().size() == 0) return lines;
		
		Collections.sort(linesToUse);
		
		//Maybe break and loop forever
//		for (int x = 0; x < linesToUse.size() - 1; x++)
//		{
//			DetectedLine currentLine = linesToUse.get(x);
//			DetectedLine nextLine = linesToUse.get(x+1);
//			double expectedRho = currentLine.rho + modeKey;
//			
//			if (!(nextLine.rho < expectedRho + tolerance && nextLine.rho > expectedRho - tolerance))
//			{
//				if (!(nextLine.rho < expectedRho + tolerance))
//				{
//					linesToUse.remove(x+1);
//				}
//				
//				DetectedLine newLine = new DetectedLine(expectedRho, currentLine.theta);
//				
//				
//				linesToUse.add(x+1, newLine);
//			}
//		}
		
		int index = 0;
		
		while (linesToUse.size() < numberOfLinesRequired)
		{
			if (index < (linesToUse.size() - 1))
			{
				DetectedLine currentLine = linesToUse.get(index);
				DetectedLine nextLine = linesToUse.get(index+1);
				double expectedRho = currentLine.rho + modeKey;
				
				if (!(nextLine.rho < expectedRho + tolerance && nextLine.rho > expectedRho - tolerance))
				{
					if (!(nextLine.rho < expectedRho + tolerance))
					{
						linesToUse.remove(index+1);
					}
					
					DetectedLine newLine = new DetectedLine(expectedRho, currentLine.theta);
					
					
					linesToUse.add(index+1, newLine);
				}
				index++;
			}
			else
			{
				DetectedLine finalLine = linesToUse.get(linesToUse.size()-1);
				
				double theta = finalLine.theta;
				double expectedRho = finalLine.rho + modeKey;
				
				for(int x = linesToUse.size(); x < numberOfLinesRequired; x++)
				{
					DetectedLine newLine = new DetectedLine(expectedRho, theta);
					
					linesToUse.add(newLine);
					
					expectedRho += modeKey;
				}
				break;
			}
		}
		
//		if (linesToUse.size() > numberOfLinesRequired)
//		{
//			for(int x = numberOfLinesRequired; x < linesToUse.size(); x++)
//			{
//				linesToUse.remove(x);
//			}
//		}
		
		return linesToUse;
	}
}
