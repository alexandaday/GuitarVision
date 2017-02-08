package guitarvision.sheetmusic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.*;

public class SheetMusic {
	//Assume notes play one after the other
	ArrayList<MusicNote> notes = new ArrayList<MusicNote>();
	
	public void addNote(MusicNote note)
	{
		notes.add(note);
	}
	
	public void writeFile(String fileName)
	{
		try
		{
			Sequence midiSequence = new Sequence(javax.sound.midi.Sequence.PPQ, 24);
			
			Track midiTrack = midiSequence.createTrack();
			
			long count = 0;
			
			for(MusicNote note: notes)
			{
				ShortMessage noteMessage = new ShortMessage();
				
				int midiNote = note.note + note.octave * 12;
				
				noteMessage.setMessage(0x90, midiNote, 0x60);
				
				
				MidiEvent noteEvent = new MidiEvent(noteMessage, count);
				midiTrack.add(noteEvent);
				
				//increment based on note length
				count += 120;
			}
			
			//End track
			MetaMessage endMessage = new MetaMessage();
			endMessage.setMessage(0x2F, new byte[] {}, 0);
			MidiEvent finalEvent = new MidiEvent(endMessage, count + 120);
			midiTrack.add(finalEvent);
			
			//Write file
			File sheetMusicFile = new File(fileName + ".mid");
			MidiSystem.write(midiSequence, 1, sheetMusicFile);
		}
		catch (InvalidMidiDataException e) {
			System.out.println(e.getMessage());
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
}
