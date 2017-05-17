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
	
	public File getDirectory(String name)
	{
		File directory = new File(name);
		
		if (!directory.exists())
		{
			directory.mkdir();
		}
		
		return directory;
	}
	
	public File getOutputFile()
	{
		String outputPath = getDirectory(outputDirectoryName).getPath() + java.io.File.separator + outputFile;
		
		File outputFile = new File(outputPath);
		
		return outputFile;
	}
	
	public void writeOutput(String line)
	{
		try
		{
			if (outputWriter == null)
			{
				//System.out.println("outputfile path");
				//System.out.println(getOutputFile().getPath());

				outputWriter = new BufferedWriter(new FileWriter(getOutputFile()));

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
				// TODO Auto-generated method stub
				return new File(dir, name).isDirectory();
			}
		});
		
		File outputDirectory = getDirectory(outputDirectoryName);
		
		writeOutput(MusicStatistics.tableHeader);
		
		for(String directory : eachVideoDirectory)
		{
			//System.out.println("directory is ");
			//System.out.println(directory);
			
			File curDirectory = new File(sampleVideoDirectory + directory);
			
			File correspondingMIDIFile = new File(sampleMIDIDirectory+directory+".mid");
			
			File outputPieceDirectory = getDirectory(outputDirectory.getPath() + java.io.File.separator + directory);
			
			String[] eachVideoAddress = curDirectory.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
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
				
				//Make into directory structure!
				//Create directory
				String outputMidi = outputPieceDirectory.getPath();// + java.io.File.separator + videoAdress.substring(0, videoAdress.indexOf("."));
				
				//System.out.println("FOUND VIDEO");
				//System.out.println(sampleVideoDirectory + directory + java.io.File.separator + videoAdress);
				
				File videoFile = new File(sampleVideoDirectory + directory + java.io.File.separator + videoAddress);
				
				ProcessedFiles files = Engine.getInstance().transcribeFromVideo(videoFile, 1000, outputMidi, 4, poorLighting, true);
				
				File outputMIDIFile = files.getMidiFile();
				
				if (!(outputMIDIFile == null))
				{
					//System.out.println("OUTPUT PATH");
					//System.out.println(outputMIDIFile.getPath());
					
					compareMIDIFiles(correspondingMIDIFile, outputMIDIFile);
				}
				else
				{
					System.out.println("ERROR WITH GENERATED MIDI FILE");
				}
				
			}
			
			//System.out.println(correspondingMIDIFile.getAbsolutePath());
			
			//for each video
			
			//processVideo(File videoFile);
			
			//Engine.getInstance().processVideo(videoFile);
			
			//put output in special place (maybe an argument)
			
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
		//writeOutput("Test: " + systemTranscription.getAbsolutePath());
		
		SheetComparator musicCompare = new SheetComparator();
		
		MusicStatistics stats = musicCompare.compareFiles(actualTranscription, systemTranscription);
		
		writeOutput(stats.tableRecordText());
	}
}
