package guitarvision.sheetmusic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public class SheetComparator {
	/**
	 * Perform sequence alignment on the note pitches from the two midi files and compare the number of notes
	 * @param first midi file
	 * @param second midi file
	 * @return statistic about the two files
	 */
	public MusicStatistics compareFiles(File file1, File file2)
	{
		MusicStatistics result = new MusicStatistics();
		
		result.name = file2.getPath();
		
		if (!file1.exists())
		{
			System.out.println("FILE DOES NOT EXIST");
			System.out.println(file1.getAbsolutePath());
		}
		
		if (!file2.exists())
		{
			System.out.println("FILE DOES NOT EXIST");
			System.out.println(file2.getAbsolutePath());
		}

		try
		{
			Sequence sequence1 = MidiSystem.getSequence(file1);
			Sequence sequence2 = MidiSystem.getSequence(file2);
			
			if (sequence1.getTracks().length >= 1 && sequence2.getTracks().length >= 1)
			{
				//Read music from first track (there should only be one track in each midi file - the guitar track)
				Track track1 = sequence1.getTracks()[0];
				Track track2 = sequence2.getTracks()[0];
				
				double size1 = track1.size();
				double size2 = track2.size();
				
				ArrayList<MidiEvent> notes1 = new ArrayList<MidiEvent>();
				ArrayList<MidiEvent> notes2 = new ArrayList<MidiEvent>();
				
				ArrayList<Byte> pitches1 = new ArrayList<Byte>();
				ArrayList<Byte> pitches2 = new ArrayList<Byte>();
				
				for(int x = 0; x < (int) size1; x++)
				{
					MidiEvent event = track1.get(x);
					byte[] message = event.getMessage().getMessage();
					Byte pitchData = null;

					//There should be a status byte and two data bytes
					//We only consider the pitch of note starting events
					if (event.getMessage().getStatus() == 0x90 && message.length >= 2)
					{
						pitchData = message[1];
						notes1.add(event);
						pitches1.add(pitchData);
					}
				}
				
				for(int x = 0; x < (int) size2; x++)
				{
					MidiEvent event = track2.get(x);
					byte[] message = event.getMessage().getMessage();
					Byte pitchData = null;
					if (event.getMessage().getStatus() == 0x90 && message.length >= 2)
					{
						pitchData = message[1];
						notes2.add(event);
						pitches2.add(pitchData);
					}
				}
				
				ObjectAlignment<Byte> alignPieces = new ObjectAlignment<Byte>(pitches1, pitches2);
				
				alignPieces.setMatchScore(100);
				alignPieces.setMismatchScore(-25);
				alignPieces.setInsertDeleteScore(-1);
				byte val = 0;
				alignPieces.setMatchTolerance(new Byte(val));
				
				result.alignmentScore = alignPieces.computeLongestMatchScore();
				
				result.proportionNotes = size1 / size2;
				
				return result;
			}
		}
		catch (IOException e) {
			System.out.println("Error reading midi file");
		}
		catch (InvalidMidiDataException e) {
			System.out.println("Error reading midi file data");
		}
		
		return result;
	}
}
