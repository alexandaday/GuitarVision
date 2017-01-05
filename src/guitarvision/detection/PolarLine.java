package guitarvision.detection;

import org.opencv.core.Point;

/**
 * @author Alex Day
 * Class that stores the information for a line, given in Polar Coordinates.
 * Two line end points may be generated for drawing the line.
 * Generally used after a Hough line transform where we have the rho and theta values.
 */

public class PolarLine
{
	//The polar coordinate parameters
	public double rho;
	public double theta;
	
	//The distance to extend the line on either side of (x0, y0)
	private int length = 2500;
	
	//Store the two end points once they have been calculated
	private Point point1 = null;
	private Point point2 = null;
	
	/**
	 * We define lines in polar coordinates by the angle and distance to the origin from a point on the line.
	 * Where the point forms a perpendicular line when joined to the origin.
	 * @param The 'r'/distance value used to define the line
	 * @param The angle used to define the line
	 */
	public PolarLine(Double rho, Double theta)
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
	public PolarLine(Double rho, Double theta, int length)
	{
		this(rho, theta);
		this.length = length;
	} 
	
	/**
	 * Method for calculating the two points of the line on demand
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
}