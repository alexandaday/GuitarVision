package guitarvision;

import java.io.File;

public class ProcessedFiles {
	private File videoFile;
	private File midiFile;
	
	public ProcessedFiles(File videoFile, File midiFile)
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
