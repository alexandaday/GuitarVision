package guitarvision.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class EdgeDetector {
	private int blurKernelSize = 3;
	
	private int cannyLowerThreshold = 0;
	//Severe error - malloc problems, when this value is small up to 143
	private int cannyUpperThreshold = 144;
	
	private int houghThreshold = 300;
	
	public Mat getEdges(Mat image)
	{
		Mat blurredImage = image.clone();

		Imgproc.blur(image, blurredImage, new Size(blurKernelSize, blurKernelSize));
		//Imgproc.GaussianBlur(image, blurredImage, new Size(3, 3), 1);
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
}
