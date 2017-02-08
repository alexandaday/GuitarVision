package guitarvision.detection;

public class GuitarString extends DetectedLine{
	public double thickness;
	
	public GuitarString(double thickness, DetectedLine lineInfo)
	{
		super(lineInfo.rho, lineInfo.theta, lineInfo.length);
		this.thickness = thickness;
	}
}
