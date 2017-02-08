package guitarvision.sheetmusic;

public class MusicNote {
	public int note; //between 0 and 11
	public int octave; //between 0 and 8
	public int string; //between 0 and 5
	
	public Integer startingFrame;
	private Integer endingFrame = null;
	
	public MusicNote(int note, int octave, int string, Integer startingFrame)
	{
		this.note = note;
		this.octave = octave;
		this.string = string;
		
		this.startingFrame = startingFrame;
	}
	
	public void setEndingFrame(Integer endingFrame)
	{
		this.endingFrame = endingFrame;
	}
	
	public Integer getEndingFrame()
	{
		return endingFrame;
	}
}
