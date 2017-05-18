package guitarvision.detection;

import guitarvision.Engine;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * @author Alex Day
 * Class that stores the information for a line, given in Polar Coordinates (defined by rho and theta values).
 * Two line end points may be generated for drawing the line.
 * Functionality for finding the intercepts of the line, intersections with other lines and applying matrix transforms.
 */
public class DetectedLine implements Comparable<DetectedLine>
{
	//The polar coordinate parameters
	private double rho;
	private double theta;
	
	//The distance to extend the line on either side of (x0, y0)
	protected int length = (int) Engine.getInstance().processingResolution.width * 2;
	
	//Store the two end points once they have been calculated
	private Point point1 = null;
	private Point point2 = null;
	
	/**
	 * We define lines in polar coordinates by the angle and distance to the origin from a point on the line.
	 * Where the point forms a perpendicular line when joined to the origin.
	 * @param The rho distance value used to define the line
	 * @param The angle used to define the line
	 */
	public DetectedLine(Double rho, Double theta)
	{
		this.rho = rho;
		this.theta = theta;
	}
	
	/**
	 * Constructor with optional parameter to set the length to extend the line by on either side of (x0,y0)
	 * @param rho
	 * @param theta
	 * @param length
	 */
	public DetectedLine(Double rho, Double theta, int length)
	{
		this(rho, theta);
		this.length = length;
	} 
	
	/**
	 * Method for calculating two points of the polar coordinate line
	 */
	private void computePoints()
	{
		point1 = new Point();
		point2 = new Point();
		
		double cosTheta = Math.cos(theta);
		double sinTheta = Math.sin(theta);
		
		//Coordinates of the point that forms a perpendicular line with the origin
		double x0 = rho * cosTheta;
		double y0 = rho * sinTheta;
		
		//Determine two points along the line given by the Polar equation
		point1.x = Math.rint(x0 + length * (-sinTheta));
		point1.y = Math.rint(y0 + length * (cosTheta));
		
		point2.x = Math.rint(x0 - length * (-sinTheta));
		point2.y = Math.rint(y0 - length * (cosTheta));
	}

	/**
	 * Lazily retrieve the first point of the line
	 * @return The first point of the line
	 */
	public Point getPoint1()
	{
		if (point1 == null)
		{
			computePoints();
		}
		return point1;
	}
	
	
	/**
	 * Lazily retrieve the second point of the line
	 * @return The second point of the line
	 */
	public Point getPoint2()
	{
		if (point2 == null)
		{
			computePoints();
		}
		return point2;
	}
	
	public double getRho()
	{
		return rho;
	}
	
	public double getTheta()
	{
		return theta;
	}
	
	public void setRho(double val)
	{
		rho = val;
		computePoints();
	}
	
	public void setTheta(double val)
	{
		theta = val;
		computePoints();
	}
	
	public double getGradient()
	{
		return Math.tan(this.theta);
	}
	
	public double getXIntercept()
	{
		return (-1 * getYIntercept() / getGradient());
	}
	
	public double getYIntercept()
	{
		return this.rho / Math.sin(this.theta);
	}

	/**
	 * Get the y value where this line intersects with the line x = xValue
	 * @param the x coordinate along the line
	 * @return the corresponding y coordinate
	 */
	public double getYAtXValue(int xValue)
	{
		return (getGradient() * xValue) + getYIntercept();
	}
	
	public double getIntercept(Intercept intercept)
	{
		switch(intercept)
		{
		case XINTERCEPT:
			return getXIntercept();
		case YINTERCEPT:
			return getYIntercept();
		default:
			return getXIntercept();
		}
	}
	
	/**
	 * Get the point where this line intersects with another line
	 * @param line to intersect with
	 * @return intersection point
	 */
	public Point getCollisionPoint(DetectedLine otherLine)
	{
		Point p1 = getPoint1();
		Point p2 = getPoint2();
		
		Point o1 = otherLine.getPoint1();
		Point o2 = otherLine.getPoint2();
	
		double m1 = (p1.y - p2.y) / (p1.x - p2.x);
		double m2 = (o1.y - o2.y) / (o1.x - o2.x);
		
		double c1 = p1.y - (m1 * p1.x);
		double c2 = o1.y - (m2 * o1.x);
		
		Point intersection = new Point();
		
		if ((otherLine.theta == 0) && (theta == 0))
		{
			return null;
		}
		else if (otherLine.theta == theta)
		{
			return null;
		}
		else if (otherLine.theta == 0)
		{
			intersection.x = otherLine.rho;
			intersection.y = (m1 * otherLine.rho) + c1;
			return intersection;
		}
		else if (theta == 0)
		{
			intersection.x = rho;
			intersection.y = (m2 * rho) + c2;
			return intersection;
		}
		else
		{
			intersection.x = (c2 - c1)  / (m1 - m2);
			
			intersection.y = ((c1 * m2) - (c2 * m1))  / (m2 - m1);
			
			return intersection;
		}
	}
	
	/**
	 * Apply matrix transform to the line.
	 * This updates the polar coordinate parameters and the two points describing the line.
	 * @param the 3 X 3 matrix transform to apply
	 */
	public void applyWarp(Mat warp)
	{
		Point p1 = getPoint1();
		Point p2 = getPoint2();
		
		Mat point1Vector = new Mat();
		
		point1Vector.create(3, 1, CvType.CV_64F);
		point1Vector.put(0, 0, p1.x);
		point1Vector.put(1, 0, p1.y);
		point1Vector.put(2, 0, 1);
		
		Mat point2Vector = new Mat();
		
		point2Vector.create(3, 1, CvType.CV_64F);
		point2Vector.put(0, 0, p2.x);
		point2Vector.put(1, 0, p2.y);
		point2Vector.put(2, 0, 1);
		
		Mat point1Transformed = new Mat(1, 3, CvType.CV_64F);
		Mat point2Transformed = new Mat(1, 3, CvType.CV_64F);
		
		Core.gemm(warp, point1Vector, 1, new Mat(), 0, point1Transformed);
		Core.gemm(warp, point2Vector, 1, new Mat(), 0, point2Transformed);

		double normalisefactor = point1Transformed.get(2, 0)[0];
		
		point1 = new Point();
		point1.x = point1Transformed.get(0, 0)[0] / normalisefactor;
		point1.y = point1Transformed.get(1, 0)[0] / normalisefactor;
		
		double normalisefactor2 = point2Transformed.get(2, 0)[0];
		
		point2 = new Point();
		point2.x = point2Transformed.get(0, 0)[0] / normalisefactor2;
		point2.y = point2Transformed.get(1, 0)[0] / normalisefactor2;
		
		double newGradient = (point2.y - point1.y) / (point2.x - point1.x);
		
		double newTheta = Math.atan(-1 / newGradient);
		double newRho = Math.abs(point1.y - (newGradient * point1.x)) / Math.sqrt((newGradient * newGradient) + 1);	
		theta = newTheta;
		rho = newRho;
		computePoints();
	}
	
	//Method used for sorting lists of lines by rho value
	@Override
	public int compareTo(DetectedLine otherLine)
	{
		double thisRho = this.rho;
		double otherRho = otherLine.rho;
		
		if (thisRho == otherRho)
		{
			return 0;
		}
		else if (thisRho < otherRho)
		{
			return -1;
		}
		else
		{
			return 1;
		}	
	}
}