package guitarvision;

import guitarvision.sheetmusic.MusicStatistics;
import guitarvision.sheetmusic.SheetComparator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PerformanceTest {
	String sampleVideoDirectory = "resources/video/";
	String sampleMIDIDirectory = "resources/midi/";
	
	String outputDirectoryName = "test_output";
	
	String outputFile = "test_stats.csv";
	
	BufferedWriter outputWriter;
	
	public File getDirectoryFromPath(String name)
	{
		File directory = new File(name);
		
		if (!directory.exists())
		{
			directory.mkdir();
		}
		
		return directory;
	}
	
	/**
	 * @return file object pointing to the output .CSV file
	 */
	public File getOutputDataFile()
	{
		String outputPath = getDirectoryFromPath(outputDirectoryName).getPath() + java.io.File.separator + outputFile;
		
		File outputFile = new File(outputPath);
		
		return outputFile;
	}
	
	public void writeOutput(String line)
	{
		try
		{
			if (outputWriter == null)
			{
				outputWriter = new BufferedWriter(new FileWriter(getOutputDataFile()));
			}

			outputWriter.write(line + "\n");
			
			outputWriter.flush();
		}
		catch (IOException e)
		{
			System.out.println("IO Exception");
		}
	}
	
	public void cleanUp()
	{
		File testDirectory = new File(outputDirectoryName);
		
		deleteRecursive(testDirectory);
	}
	
	public void deleteRecursive(File f)
	{
		if (f.isDirectory())
		{
			for(File subFile: f.listFiles())
			{
				deleteRecursive(subFile);
			}
		}
	}
	
	/**
	 * Run system performance test by transcribing all test videos and performing sequence alignment on their output with
	 * the manual transcriptions
	 */
	public void compareToManualTranscriptions()
	{
		System.out.println("Starting performance test");
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		System.out.println(format.format(date));
		
		File videoDirectory = new File(sampleVideoDirectory);
		
		String[] eachVideoDirectory = videoDirectory.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		});
		
		File outputDirectory = getDirectoryFromPath(outputDirectoryName);
		
		writeOutput(MusicStatistics.tableHeader);
		
		for(String directory : eachVideoDirectory)
		{
			File curDirectory = new File(sampleVideoDirectory + directory);
			
			File correspondingMIDIFile = new File(sampleMIDIDirectory+directory+".mid");
			
			File outputPieceDirectory = getDirectoryFromPath(outputDirectory.getPath() + java.io.File.separator + directory);
			
			String[] eachVideoAddress = curDirectory.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					return (new File(dir, name)).isFile() && ((name.toLowerCase().endsWith(".mov")) || (name.toLowerCase().endsWith(".mp4")));
				}
			});
			
			for(String videoAddress : eachVideoAddress)
			{
				boolean poorLighting = false;
				if (videoAddress.contains("scene_poor_lighting"))
				{
					poorLighting = true;
				}

				String outputMidi = outputPieceDirectory.getPath();

				File videoFile = new File(sampleVideoDirectory + directory + java.io.File.separator + videoAddress);
				
				OutputFileReferences files = Engine.getInstance().transcribeFromVideo(videoFile, 1000, outputMidi, 4, poorLighting, true);
				
				File outputMIDIFile = files.getMidiFile();
				
				if (!(outputMIDIFile == null))
				{
					compareMIDIFiles(correspondingMIDIFile, outputMIDIFile);
				}
				else
				{
					System.out.println("ERROR WITH GENERATED MIDI FILE");
				}
				
			}
		}
		
		try
		{
			outputWriter.close();
			outputWriter = null;
		}
		catch (IOException e)
		{
			System.out.println("Error closing output file");
		}
		
		System.out.println("Performance test complete");
		Date endDate = new Date();
		System.out.println(format.format(endDate));
		
	}
	
	public void compareMIDIFiles(File actualTranscription, File systemTranscription)
	{
		SheetComparator musicCompare = new SheetComparator();
		
		MusicStatistics stats = musicCompare.compareFiles(actualTranscription, systemTranscription);
		
		writeOutput(stats.tableRecordText());
	}
}
