package guitarvision.sheetmusic;

public class MusicNote {
	//Store the pitch of the note (ranges between 0 and 11 representing notes from C to B)
	public int note;
	//Store the octave of the note (ranges between 0 and 8)
	public int octave;
	//Store the string used to generate the note (ranges between 0 and 5)
	public int string;
	
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
