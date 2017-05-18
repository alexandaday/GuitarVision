package guitarvision.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
	private double angleAllowanceInitial = 0.5;
	private double angleAllowanceFinal = 0.2;
	private int numberFretsToDetectInitial = 30;
	private int numberFretsToDetectFinal = 20;
	
	private double previousFretsWeighting = 0.999;
	
	/**
	 * Get list of lines containing the positions of the frets in the image 
	 * @param image of guitar in scene
	 * @param image of guitar in scene that is being drawn on in the pipeline, to display to the user
	 * @param list of strings detected in the image
	 * @param edge detector to use on the fretboard for finding frets
	 * @param list of frets from the previous frame for tracking
	 * @param processing option object, whether to draw frets on the annotated image
	 * @return
	 */
	public ArrayList<DetectedLine> getGuitarFrets(Mat imageToProcess, Mat imageToAnnotate, ArrayList<GuitarString> guitarStrings, EdgeDetector edgeDetector, ArrayList<DetectedLine> previousFrets, ImageProcessingOptions processingOptions)
	{
		double width = imageToProcess.width();
		double height = imageToProcess.height()/4;
		Size resolution = new Size(width, height);
		
		//Intersect the outer strings with the left and right sides of the image
		//to obtain rectangle which contains guitar neck
		GuitarString outerString1 = guitarStrings.get(0);
		GuitarString outerString2 = guitarStrings.get(guitarStrings.size()-1);
		
		List<Point> sourcePoints = new ArrayList<Point>();
		
		DetectedLine rightEdgeOfImage = new DetectedLine(width, 0.0);
		
		Point rightEdgeUpperPoint = outerString1.getCollisionPoint(rightEdgeOfImage);
		Point rightEdgeLowerPoint = outerString2.getCollisionPoint(rightEdgeOfImage);
		
		Point point1 = new Point(0,outerString1.getYIntercept());
		Point point2 = new Point(width,rightEdgeUpperPoint.y);
		Point point3 = new Point(width,rightEdgeLowerPoint.y);
		Point point4 = new Point(0,outerString2.getYIntercept());
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
		
		//Perform perspective transform to isolate guitar neck 
		Mat warpMat = Imgproc.getPerspectiveTransform(source, dest);
		Mat inverseWarpMat = warpMat.inv();

		Mat guitarNeckImage = new Mat();
		Imgproc.warpPerspective(imageToProcess, guitarNeckImage, warpMat, resolution);
		
		ArrayList<DetectedLine> guitarNeckFrets = getGuitarFretsFromNeckImage(imageToProcess, imageToAnnotate, inverseWarpMat, guitarNeckImage, edgeDetector, ImageProcessingOptions.DRAWSELECTEDLINES);
		
		ArrayList<DetectedLine> finalFrets = performPreviousFretWeighting(guitarNeckFrets, previousFrets);
		
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

		Mat cannyProcessedImage = edgeDetector.getEdges(guitarNeckImage);

		Mat houghLineParameters = edgeDetector.houghTransform(cannyProcessedImage);
		
		ArrayList<DetectedLine> initialLines = getLinesFromParameters(houghLineParameters);
		
		ArrayList<DetectedLine> parallelLines = filterFretsByAngle(initialLines);

		int noGroups = numberFretsToDetectInitial;
		
		ArrayList<ArrayList<DetectedLine>> fretGroupings = clusterGuitarFrets(parallelLines, noGroups);

		ArrayList<DetectedLine> selectedFrets = selectCentralFretsFromClusters(fretGroupings);
		
		Collections.sort(selectedFrets);
		
		ArrayList<DetectedLine> finalFrets = addMissingFrets(selectedFrets, numberFretsToDetectFinal, Intercept.XINTERCEPT);
		
		if (finalFrets == null)
		{
			finalFrets = new ArrayList<DetectedLine>();
		}
		
		//Convert from fretboard coordinates to original image coordinates
		for(DetectedLine fret : finalFrets)
		{
			fret.applyWarp(inverseNeckWarp);
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
	
	private ArrayList<DetectedLine> filterFretsByAngle(ArrayList<DetectedLine> candidateFrets)
	{
		ArrayList<DetectedLine> filteredStrings = new ArrayList<DetectedLine>();

		for(int index = 0; index < candidateFrets.size(); index++)
		{
			double curAngle = candidateFrets.get(index).getTheta();
			double currentAngleAroundZero = (curAngle > Math.PI / 2) ? curAngle - (Math.PI) : curAngle;
			if (!((currentAngleAroundZero > angleAllowanceInitial) || (currentAngleAroundZero < -angleAllowanceInitial)))
			{
				filteredStrings.add(candidateFrets.get(index));
			}
		}

		return filteredStrings;
	}

	private ArrayList<ArrayList<DetectedLine>> clusterGuitarFrets(ArrayList<DetectedLine> filteredStrings, int noGroups)
	{
		Mat linesToCluster = new Mat(filteredStrings.size(), 1, CvType.CV_32F);

		for(int index = 0; index < filteredStrings.size(); index++)
		{
			DetectedLine curString = filteredStrings.get(index);
			double rhoValue = curString.getRho();
			linesToCluster.put(index, 0, rhoValue);
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

		for(int index = 0; index < noGroups; index++)
		{
			groupedStrings.add(new ArrayList<DetectedLine>());
		}

		for(int index = 0; index < filteredStrings.size(); index++)
		{
			int group = (int) (clusterLabels.get(index, 0)[0]);

			if ((group >= 0) && (group < groupedStrings.size()) && (index < filteredStrings.size()))
			{
				groupedStrings.get(group).add(filteredStrings.get(index));
			}
		}
		
		return groupedStrings;
	}

	private ArrayList<DetectedLine> selectCentralFretsFromClusters(ArrayList<ArrayList<DetectedLine>> groupedStrings)
	{	
		ArrayList<DetectedLine> finalStrings = new ArrayList<DetectedLine>();
		
		for(int groupIndex = 0; groupIndex < groupedStrings.size(); groupIndex++)
		{
			ArrayList<Double> rhoValues = new ArrayList<Double>();
			
			for(DetectedLine line : groupedStrings.get(groupIndex))
			{
				rhoValues.add((Double) line.getRho());
			}

			if (rhoValues.size() > 0)
			{
				Collections.sort(rhoValues);

				Double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(DetectedLine line : groupedStrings.get(groupIndex))
				{
					if ((double) middleValue == line.getRho())
					{
						finalStrings.add(line);
						break;
					}
				}
			}
		}
		
		return finalStrings;
	}
	
	public ArrayList<DetectedLine> performPreviousFretWeighting(ArrayList<DetectedLine> curFrets, ArrayList<DetectedLine> previousFrets)
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
			for(int index = 0; index < curFrets.size() && index < previousFrets.size(); index++)
			{
				double newRho = (curFrets.get(index).getRho() * (1 - previousFretsWeighting)) + (previousFrets.get(index).getRho() * previousFretsWeighting);
				double newTheta = (curFrets.get(index).getTheta() * (1 - previousFretsWeighting)) + (previousFrets.get(index).getTheta() * previousFretsWeighting);

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
	
	public ArrayList<DetectedLine> addMissingFrets(ArrayList<DetectedLine> lines, int numberOfLinesRequired, Intercept intercept)
	{
		//Perform further angle filtering with more restrictive angle
		if (lines.size() <= 1) return null;
		
		double[] angles = new double[lines.size()];
		
		for (DetectedLine line : lines)
		{
			double angle = line.getTheta();
			angle = (angle > Math.PI) ? angle - (Math.PI * 2) : angle;
			angles[lines.indexOf(line)] = angle;
		}
		
		double medianAngle = angles[(int) Math.floor((angles.length)/2)];
		
		ArrayList<DetectedLine> filteredByAngle = new ArrayList<DetectedLine>();
		
		for (DetectedLine line : lines)
		{
			double curTheta = line.getTheta();
			curTheta = (curTheta > Math.PI) ? curTheta - (Math.PI * 2) : curTheta;
			
			if (!((curTheta < medianAngle - angleAllowanceFinal) || (curTheta > medianAngle + angleAllowanceFinal)))
			{
				filteredByAngle.add(line);	
			}
		}
		
		lines = filteredByAngle;

		//Remove one fret from pairs that are separated by small rho value
		ArrayList<DetectedLine> filteredBySeparation = new ArrayList<DetectedLine>();

		for(int compareTo = 0; compareTo < lines.size() - 1; compareTo++)
		{
			if (!(lines.get(compareTo + 1).getRho() - lines.get(compareTo).getRho() < 10))
			{
				filteredBySeparation.add(lines.get(compareTo));
			}
		}
		
		lines = filteredBySeparation;
		
		if (lines.size() == 0) return new ArrayList<DetectedLine>();
		
		//Store array of rho separation values between the filtered fret pairs
		double[] distances = new double[lines.size() - 1];
		HashMap<Double, Integer> distanceIndexes = new HashMap<Double, Integer>();
		
		for (int lineIndex =  0; lineIndex < lines.size() - 1; lineIndex++)
		{
			DetectedLine line1 = lines.get(lineIndex);
			DetectedLine line2 = lines.get(lineIndex+1);
			
			double distance = line2.getRho() - line1.getRho();
			distances[lineIndex] = distance;
			distanceIndexes.put(distance, lineIndex);
		}

		ArrayList<DetectedLine> linesToAdd = new ArrayList<DetectedLine>();
		
		double previousDistance = Integer.MAX_VALUE;

		//Iterate through frets, starting with the first pair
		//Subdivide pairs when their gaps implies more than an integer number of times the previous gap
		//(using 1.4x, 2.4x, and 3.4x the previous gaps as thresholds for inserting 1, 2 and 3 new frets)
		for (int index = 0; index < lines.size() - 1; index++)
		{
			DetectedLine currentLine = lines.get(index);
			double nextDistance = distances[index];

			if (nextDistance > 3.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 4),currentLine.getTheta());
				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 4),currentLine.getTheta());
				DetectedLine newLine3 = new DetectedLine(currentLine.getRho() + (nextDistance * 3 / 4),currentLine.getTheta());
				linesToAdd.add(newLine);
				linesToAdd.add(newLine2);
				linesToAdd.add(newLine3);
				previousDistance = nextDistance / 4;
			}
			else if (nextDistance > 2.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 3),currentLine.getTheta());
				DetectedLine newLine2 = new DetectedLine(currentLine.getRho() + (nextDistance * 2 / 3),currentLine.getTheta());
				linesToAdd.add(newLine);
				linesToAdd.add(newLine2);
				previousDistance = nextDistance / 3;
			}
			else if (nextDistance > 1.4 * previousDistance)
			{
				DetectedLine newLine = new DetectedLine(currentLine.getRho() + (nextDistance / 2),currentLine.getTheta());
				linesToAdd.add(newLine);
				previousDistance = nextDistance / 2;
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
		
		//Remove one fret from initial pairs if their separation is 1.9x the following fret pair
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
		
		//Reduce down to 20 frets, by removing the highest frets (those near the sound hole)
		for(int lineIndex = lines.size() - 1; lineIndex >= numberOfLinesRequired; lineIndex--)
		{
			lines.remove(lines.get(lineIndex));
		}
		
		lines.addAll(linesToAdd);
		
		Collections.sort(lines);

		return lines;
	}
	
	public void setNumberOfFretsToDetect(int newNumber)
	{
		numberFretsToDetectFinal = newNumber;
	}
}
