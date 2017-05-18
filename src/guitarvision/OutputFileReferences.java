package guitarvision;

import java.io.File;

public class OutputFileReferences {
	private File videoFile;
	private File midiFile;
	
	public OutputFileReferences(File videoFile, File midiFile)
	{
		this.videoFile = videoFile;
		this.midiFile = midiFile;
	}
	
	public File getVideoFile()
	{
		return videoFile;
	}
	
	public File getMidiFile()
	{
		return midiFile;
	}
}