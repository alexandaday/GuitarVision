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
	public int numberStringsToDetect = 6;
	
	public ArrayList<PolarLine> getGuitarStrings(Mat image, EdgeDetector edgeDetector, ImageProcessingOptions processingOptions)
	{
		if (edgeDetector == null)
		{
			edgeDetector = new EdgeDetector();
			edgeDetector.setCannyLowerThreshold(0);
			edgeDetector.setCannyUpperThreshold(255);
			edgeDetector.setHoughThreshold(300);
		}
		
		Mat cannyProcessedImage = edgeDetector.getEdges(image);
		
		Mat houghLineParameters = edgeDetector.houghTransform(cannyProcessedImage);
		
		ArrayList<PolarLine> initialLines = getLinesFromParameters(houghLineParameters);
		
		ArrayList<PolarLine> parallelLines = filterGuitarStrings(initialLines);
		
		int noGroups = numberStringsToDetect;
		
		ArrayList<ArrayList<PolarLine>> stringGroupings = clusterGuitarStrings(parallelLines, noGroups);
		
		ArrayList<PolarLine> selectedStrings = selectGuitarString(stringGroupings);
		
		if((processingOptions == ImageProcessingOptions.DRAWSTRINGS) || (processingOptions == ImageProcessingOptions.DRAWGROUPINGS))
		{
			for(PolarLine string : selectedStrings)
			{
				Imgproc.line(image, string.getPoint1(), string.getPoint2(), new Scalar(255,255,255));
			}
		}
		
		Random randomGenerator = new Random();
		
		if(processingOptions == ImageProcessingOptions.DRAWGROUPINGS)
		{
			for(ArrayList<PolarLine> stringGroup: stringGroupings)
			{
				Scalar colour = new Scalar(randomGenerator.nextInt(255),randomGenerator.nextInt(255),randomGenerator.nextInt(255));
				
				for(PolarLine string: stringGroup)
				{
					Imgproc.line(image, string.getPoint1(), string.getPoint2(), colour);
				}
			}
		}
		
		return selectedStrings;
	}
	
	private ArrayList<PolarLine> getLinesFromParameters(Mat houghLines)
	{
		ArrayList<PolarLine> guitarStrings = new ArrayList<PolarLine>();

		for (int lineIndex = 0; lineIndex < houghLines.rows(); lineIndex++)
		{
			double[] polarLineParameters = houghLines.get(lineIndex, 0);

			PolarLine currentString = new PolarLine(polarLineParameters[0], polarLineParameters[1]);

			guitarStrings.add(currentString);
		}

		return guitarStrings;
	}

	private ArrayList<PolarLine> filterGuitarStrings(ArrayList<PolarLine> candidateStrings)
	{
		double totalAngle = 0;

		for(PolarLine curString : candidateStrings)
		{
			totalAngle += curString.theta;
		}

		double averageAngle = totalAngle/candidateStrings.size();

		ArrayList<PolarLine> filteredStrings = new ArrayList<PolarLine>();

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

	private ArrayList<ArrayList<PolarLine>> clusterGuitarStrings(ArrayList<PolarLine> filteredStrings, int noGroups)
	{
		//Create matrix to cluster with the y intercepts of all lines
		Mat linesToCluster = new Mat(filteredStrings.size(), 1, CvType.CV_32F);

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			PolarLine curString = filteredStrings.get(a);
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
			return new ArrayList<ArrayList<PolarLine>>();
		}
		
		
		Core.kmeans(linesToCluster, noGroups, clusterLabels, new TermCriteria(TermCriteria.COUNT, 50, 1), 10, Core.KMEANS_RANDOM_CENTERS);
		
		ArrayList<ArrayList<PolarLine>> groupedStrings = new ArrayList<ArrayList<PolarLine>>();

		for(int c = 0; c < noGroups; c++)
		{
			groupedStrings.add(new ArrayList<PolarLine>());
		}

		for(int a = 0; a < filteredStrings.size(); a++)
		{
			int group = (int) (clusterLabels.get(a, 0)[0]);

			groupedStrings.get(group).add(filteredStrings.get(a));
		}
		
		return groupedStrings;
	}

	private ArrayList<PolarLine> selectGuitarString(ArrayList<ArrayList<PolarLine>> groupedStrings)
	{	
		
		ArrayList<PolarLine> finalStrings = new ArrayList<PolarLine>();
		
		for(int b = 0; b < groupedStrings.size(); b++)
		{
			ArrayList<Double> rhoValues = new ArrayList<Double>();
			
			for(PolarLine s : groupedStrings.get(b))
			{
				rhoValues.add((Double) s.rho);
			}

			if (rhoValues.size() > 0)
			{
				Collections.sort(rhoValues);

				Double middleValue = rhoValues.get((int) Math.floor(rhoValues.size() / 2));

				for(PolarLine s : groupedStrings.get(b))
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
		numberStringsToDetect = newNumber;
	}
}
