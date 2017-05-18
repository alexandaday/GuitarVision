package guitarvision.detection;

public class GuitarString extends DetectedLine{
	public double thickness;
	
	public GuitarString(double thickness, DetectedLine lineInfo)
	{
		super(lineInfo.getRho(), lineInfo.getTheta(), lineInfo.length);
		this.thickness = thickness;
	}
}