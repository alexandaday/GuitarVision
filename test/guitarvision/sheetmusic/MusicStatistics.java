package guitarvision.sheetmusic;

public class MusicStatistics {
	public static String tableHeader = "Name,Proportion of notes detected,Accuracy alignment score";
	
	public String name;
	public double proportionNotes;
	public int alignmentScore;
	
	public String tableRecordText()
	{
		return name+","+proportionNotes+","+alignmentScore;
	}
}
