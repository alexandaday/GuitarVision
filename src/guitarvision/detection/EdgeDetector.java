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
	
	private final double luthierConstant = 17.871 / 16.871;
	
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
				double distance = line1.getRho() - line2.getRho(); //line1.getIntercept(intercept) - line2.getIntercept(intercept)

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
//			double expectedRho = currentLine.getRho() + modeKey;
//			
//			if (!(nextLine.getRho() < expectedRho + tolerance && nextLine.getRho() > expectedRho - tolerance))
//			{
//				if (!(nextLine.getRho() < expectedRho + tolerance))
//				{
//					linesToUse.remove(x+1);
//				}
//				
//				DetectedLine newLine = new DetectedLine(expectedRho, currentLine.getTheta());
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
				double expectedRho = currentLine.getRho() + modeKey;
				
				if (!(nextLine.getRho() < expectedRho + tolerance && nextLine.getRho() > expectedRho - tolerance))
				{
					if (!(nextLine.getRho() < expectedRho + tolerance))
					{
						linesToUse.remove(index+1);
					}
					
					DetectedLine newLine = new DetectedLine(expectedRho, currentLine.getTheta());
					
					
					linesToUse.add(index+1, newLine);
				}
				index++;
			}
			else
			{
				DetectedLine finalLine = linesToUse.get(linesToUse.size()-1);
				
				double theta = finalLine.getTheta();
				double expectedRho = finalLine.getRho() + modeKey;
				
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
				double distance = line1.getRho() - line2.getRho(); //line1.getIntercept(intercept) - line2.getIntercept(intercept)
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
				double distance = line2.getRho() - line1.getRho(); //line1.getIntercept(intercept) - line2.getIntercept(intercept)

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
			rhoValues[x] = line.getRho();
			thetaValues[x] = line.getTheta();
			rhoSum += line.getRho();
			x++;
		}
		
		double meanRhoValue = rhoSum /filteredLines.size();
		
		//double middleThetaValue = (thetaValues[thetaValues.length - 1] + thetaValues[0])/2;

		//double[] newRhoValues = new double[rhoValues.length];
		
		int middleIndex = (int) Math.floor(rhoValues.length / 2);
		
		//double startRho = meanRhoValue - (medianDistance/2);
		
		double curRho = filteredLines.get(middleIndex).getRho();//startRho;
		double curTheta = filteredLines.get(middleIndex).getTheta();
		
		double tolerance = averageDistance/2;
		
		for (int i = middleIndex; i >= 0; i--)
		{
			double expectedRho = curRho;
			
			double curStringRho = filteredLines.get(i).getRho();
			
			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
			
			if (!stringExists)
			{
				//System.out.println("String doesn't exist");
				filteredLines.get(i).setRho(expectedRho);
				filteredLines.get(i).setTheta((filteredLines.get(i).getTheta() + curTheta) / 2);
			}
			
			
			
			//filteredLines.get(i).getRho() = curRho;
			//filteredLines.get(i).getTheta() = middleThetaValue;
			curRho -= medianDistance;
		}
		
		curRho = filteredLines.get(middleIndex).getRho() + medianDistance;
		
		
		for (int i = middleIndex + 1; i < rhoValues.length; i++)
		{
			double expectedRho = curRho;
			
			double curStringRho = filteredLines.get(i).getRho();
			
			boolean stringExists = (curStringRho < expectedRho + tolerance) && (curStringRho > expectedRho - tolerance);
			
			if (!stringExists)
			{
				//System.out.println("String doesn't exist");
				filteredLines.get(i).setRho(expectedRho);
				filteredLines.get(i).setTheta((filteredLines.get(i).getTheta() + curTheta) / 2);
			}
			
			//filteredLines.get(i).getRho() = curRho;
			//filteredLines.get(i).getTheta() = middleThetaValue;
			curRho += medianDistance;
		}
	
		
		
		return filteredLines;
		
	}
	
	private double angleFactorAllowance = 0.2;
	private double distanceFactorAllowance = 2;
	
	public ArrayList<DetectedLine> evenlyDistributeLinesExponential(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
	{
		
		if (lines.size() <= 1) return null;
		
		double totalAngle = 0;
		double[] angles = new double[lines.size()];
		
		for (DetectedLine line : lines)
		{
			double angle = line.getTheta();
			angle = (angle > Math.PI) ? angle - (Math.PI * 2) : angle;
			angles[lines.indexOf(line)] = angle;
			totalAngle += line.getTheta();
		}
		
		double averageAngle = totalAngle / lines.size();
		double medianAngle = angles[(int) Math.floor((angles.length)/2)];
		
		ArrayList<DetectedLine> filteredByAngle = new ArrayList<DetectedLine>();
		
		for (DetectedLine line : lines)
		{
			double curTheta = line.getTheta();
			curTheta = (curTheta > Math.PI) ? curTheta - (Math.PI * 2) : curTheta;
			
			if (!((curTheta < medianAngle - angleFactorAllowance) || (curTheta > medianAngle + angleFactorAllowance)))
			{
				filteredByAngle.add(line);	
			}
		}
		
		lines = filteredByAngle;
		
		//Calculate mean and median string separation, for exponential spacings

		
		
		ArrayList<DetectedLine> filteredBySeparation = new ArrayList<DetectedLine>();

		
		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
			if (!(lines.get(compareTo + 1).getRho() - lines.get(compareTo).getRho() < 10))
			{
				filteredBySeparation.add(lines.get(compareTo));
			}
		}
		
		lines = filteredBySeparation;
		
		double totalDistance = 0;
		
		if (lines.size() == 0) return new ArrayList<DetectedLine>();
		
		double[] distances = new double[lines.size() - 1];
		HashMap<Double, Integer> distanceIndexes = new HashMap<Double, Integer>();
		
		for (int cur =  0; cur < lines.size() - 1; cur++)
		{
			DetectedLine line1 = lines.get(cur);
			DetectedLine line2 = lines.get(cur+1);
			
			double distance = line2.getRho() - line1.getRho();
			distances[cur] = distance;
			distanceIndexes.put(distance, cur);
			
			//System.out.println(distance);
		}
		
//		Arrays.sort(distances);
//		
//		double medianDistance = distances[(int) Math.round(distances.length / 2)];
//		
//		int indexMedian = distanceIndexes.get(medianDistance);
//		
//		double halfMedianDistance = medianDistance / 2;
//		
//		
//		//ArrayList<DetectedLine> evenlySpaced = new ArrayList<DetectedLine>();
//		
//		DetectedLine medianLine = lines.get(indexMedian);
//		DetectedLine followingLine = lines.get(indexMedian + 1);
//		
//		for(int offset = -5; offset < 6; offset++)
//		{
//			double halfMedianTest = halfMedianDistance + offset;
//			if (distanceIndexes.containsKey(halfMedianTest))
//			{
//				medianDistance = halfMedianTest;
//				indexMedian = distanceIndexes.get(halfMedianTest);
//				medianLine = lines.get(indexMedian);
//				followingLine = lines.get(indexMedian+1);
//				break;
//				
//			}
//		}
		
		
		ArrayList<DetectedLine> linesToAdd = new ArrayList<DetectedLine>();
		
		double previousDistance = Integer.MAX_VALUE;
		
		//System.out.println("STARTING");
		
		for (int index = 0; index < lines.size() - 1; index++)
		{
			DetectedLine currentLine = lines.get(index);
			double nextDistance = distances[index];
			
			//System.out.println(nextDistance);
			//System.out.println(previousDistance);

			if (nextDistance > 3.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 4),currentLine.getTheta());
				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 4),currentLine.getTheta());
				DetectedLine newLine3 = new DetectedLine(currentLine.getRho() + (nextDistance * 3 / 4),currentLine.getTheta());
				linesToAdd.add(newLine);
				linesToAdd.add(newLine2);
				linesToAdd.add(newLine3);
				previousDistance = nextDistance / 4;
				//System.out.println("FOUR TIMES");
			}
			else if (nextDistance > 2.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 3),currentLine.getTheta());
				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 3),currentLine.getTheta());
				linesToAdd.add(newLine);
				linesToAdd.add(newLine2);
				previousDistance = nextDistance / 3;
				//System.out.println("THREE TIMES");
			}
			else if (nextDistance > 1.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 2),currentLine.getTheta());
				linesToAdd.add(newLine);
				previousDistance = nextDistance / 2;
				//System.out.println("TWO TIMES");
			}
			else
			{
				previousDistance = nextDistance;
			}
		}
		
		lines.addAll(linesToAdd);
		
		Collections.sort(lines);
		
		linesToAdd = new ArrayList<DetectedLine>();
		
		previousDistance = Integer.MAX_VALUE;
		
		ArrayList<DetectedLine> initialToRemove = new ArrayList<DetectedLine>();
		
		//Filter initial frets
		for (int index = 0; index < lines.size() - 2; index++)
		{
			DetectedLine currentLine = lines.get(index);
			DetectedLine nextLine = lines.get(index + 1);
			DetectedLine furtherLine = lines.get(index + 2);
			
			double distance = nextLine.getRho() - currentLine.getRho();
			double nextDistance = furtherLine.getRho() - nextLine.getRho();
			
			if (distance > nextDistance * 1.9)
			{
				initialToRemove.add(currentLine);
			}
			else
			{
				break;
			}
		}
		
		for(DetectedLine line: initialToRemove)
		{
			lines.remove(line);
		}
		
		
		//Reduce down to 20 frets
		for(int x = lines.size() - 1; x >= numberOfLinesRequired; x--)
		{
			lines.remove(lines.get(x));
		}
		
		//System.out.println("Number strings");
		//System.out.println(lines.size());
		
//		for (int index = lines.size() - 1; index >= 1; index--)
//		{
//			DetectedLine currentLine = lines.get(index);
//			double nextDistance = currentLine.getRho() - lines.get(index - 1).getRho();//distances[index];
//			
//			System.out.println(nextDistance);
//			//System.out.println(previousDistance);
//
//			if (nextDistance > 3.9 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 4),currentLine.getTheta());
//				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 4),currentLine.getTheta());
//				DetectedLine newLine3 = new DetectedLine(currentLine.getRho() + (nextDistance * 3 / 4),currentLine.getTheta());
//				linesToAdd.add(newLine);
//				linesToAdd.add(newLine2);
//				linesToAdd.add(newLine3);
//				previousDistance = nextDistance / 4;
//				System.out.println("FOUR TIMES");
//			}
//			else if (nextDistance > 2.9 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 3),currentLine.getTheta());
//				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 3),currentLine.getTheta());
//				linesToAdd.add(newLine);
//				linesToAdd.add(newLine2);
//				previousDistance = nextDistance / 3;
//				System.out.println("THREE TIMES");
//			}
//			else if (nextDistance > 1.9 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 2),currentLine.getTheta());
//				linesToAdd.add(newLine);
//				previousDistance = nextDistance / 2;
//				System.out.println("TWO TIMES");
//			}
//			else
//			{
//				previousDistance = nextDistance;
//			}
//		}
		
//		for (int index = indexMedian - 1; index >= 0; index--)
//		{
//			DetectedLine currentLine = lines.get(index);
//			double nextDistance = distances[index];
//			
//			System.out.println(nextDistance);
//			System.out.println(previousDistance);
//
//			if (nextDistance > 3.5 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine((currentLine.getRho()  - nextDistance) / 4,currentLine.getTheta());
//				DetectedLine newLine2 = new DetectedLine((currentLine.getRho()  - nextDistance) * 2 / 4,currentLine.getTheta());
//				DetectedLine newLine3 = new DetectedLine((currentLine.getRho()  - nextDistance) * 3 / 4,currentLine.getTheta());
//				linesToAdd.add(newLine);
//				linesToAdd.add(newLine2);
//				linesToAdd.add(newLine3);
//				previousDistance = nextDistance / 4;
//			}
//			else if (nextDistance > 2.5 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine((currentLine.getRho()  - nextDistance) / 3,currentLine.getTheta());
//				DetectedLine newLine2 = new DetectedLine((currentLine.getRho()  - nextDistance) * 2 / 3,currentLine.getTheta());
//				linesToAdd.add(newLine);
//				linesToAdd.add(newLine2);
//				previousDistance = nextDistance / 3;
//			}
//			else if (nextDistance > 1.5 * previousDistance)
//			{
//				DetectedLine newLine = new DetectedLine((currentLine.getRho()  - nextDistance) / 2,currentLine.getTheta());
//				linesToAdd.add(newLine);
//				previousDistance = nextDistance / 2;
//			}
//			else
//			{
//				previousDistance = nextDistance;
//			}
//		}
		
		lines.addAll(linesToAdd);
		
		//System.out.println(linesToAdd.size());
		
		Collections.sort(lines);
		
		
		//linesToAdd.add(medianLine);
		//lines = linesToAdd;
//		
//		evenlySpaced.add(medianLine);
//		evenlySpaced.add(followingLine);
//		
//		double curRho = followingLine.getRho();
//		double theta = (followingLine.getTheta() + medianLine.getTheta()) / 2;
//		
//		int originalIndex = indexMedian;
//		int curIndex = originalIndex + 2;
//		
//		int offsetAllowance = 10;
//		
//		double curDistance = medianDistance;
//		
//		for (int x = 0; x < 20; x++)
//		{
//			if (curIndex < lines.size())
//			{
//				DetectedLine actualLine = lines.get(curIndex);
//				
//				DetectedLine nextLine;
//				
//				System.out.println("Actual");
//				System.out.println(actualLine.getRho());
//				System.out.println("Expected");
//				System.out.println(curRho);
//				
//				if (!((actualLine.getRho() < curRho - offsetAllowance) || (actualLine.getRho() > curRho + offsetAllowance)))
//				{
//					nextLine = actualLine;
//					
//					//Update separation distance
//					curDistance = curDistance + (actualLine.getRho() - curRho);
//					
//					System.out.println("Use");
//				}
//				else
//				{
//					nextLine = new DetectedLine(curRho, theta);
//					System.out.println("New");
//				}
//
//				evenlySpaced.add(nextLine);
//
//				curDistance = curDistance / Math.pow(luthierConstant, x + 1);
//				curRho += curDistance;
//				curIndex++;
//			}
//			else
//			{
//				break;
//			}
//		}
//		
//		curIndex = originalIndex - 1;
//		curDistance = medianDistance;
//		curRho = medianLine.getRho();
//		
//		
//		for (int x = 0; x < 20; x++)
//		{
//			curRho -= (medianDistance * Math.pow(luthierConstant, x + 1));
//			
//			DetectedLine nextLine = new DetectedLine(curRho, theta);
//			
//			if (curIndex > 0)
//			{
//				DetectedLine actualLine = lines.get(curIndex);
//				
//				if (!((actualLine.getRho() < curRho - offsetAllowance) || (actualLine.getRho() > curRho + offsetAllowance)))
//				{
//					nextLine = actualLine;
//					
//					//Update separation distance
//					curDistance = curDistance + (actualLine.getRho() - curRho);
//				}
//				else
//				{
//					nextLine = new DetectedLine(curRho, theta);
//				}
//
//				evenlySpaced.add(nextLine);
//
//				curDistance = curDistance * Math.pow(luthierConstant, x + 1);
//				curRho -= curDistance;
//				curIndex--;
//			}
//			else
//			{
//				break;
//			}
//		}
		
		
		//lines = evenlySpaced;

		return lines;//filteredLines;
		
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
				double distance = line1.getRho() - line2.getRho(); //line1.getIntercept(intercept) - line2.getIntercept(intercept)

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
//			double expectedRho = currentLine.getRho() + modeKey;
//			
//			if (!(nextLine.getRho() < expectedRho + tolerance && nextLine.getRho() > expectedRho - tolerance))
//			{
//				if (!(nextLine.getRho() < expectedRho + tolerance))
//				{
//					linesToUse.remove(x+1);
//				}
//				
//				DetectedLine newLine = new DetectedLine(expectedRho, currentLine.getTheta());
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
				double expectedRho = currentLine.getRho() + modeKey;
				
				if (!(nextLine.getRho() < expectedRho + tolerance && nextLine.getRho() > expectedRho - tolerance))
				{
					if (!(nextLine.getRho() < expectedRho + tolerance))
					{
						linesToUse.remove(index+1);
					}
					
					DetectedLine newLine = new DetectedLine(expectedRho, currentLine.getTheta());
					
					
					linesToUse.add(index+1, newLine);
				}
				index++;
			}
			else
			{
				DetectedLine finalLine = linesToUse.get(linesToUse.size()-1);
				
				double theta = finalLine.getTheta();
				double expectedRho = finalLine.getRho() + modeKey;
				
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
