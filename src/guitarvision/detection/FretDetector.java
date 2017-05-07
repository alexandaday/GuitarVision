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

public class FretDetector {
	public double angleAllowance = 0.05;
	public int numberFretsToDetect = 20;
	
	public ArrayList<DetectedLine> getGuitarFrets(Mat originalImage, Mat imageToAnnotate, ArrayList<GuitarString> guitarStrings, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
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
