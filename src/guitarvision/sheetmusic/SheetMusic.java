package guitarvision.sheetmusic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.*;

public class SheetMusic {
	//Assume notes play one after the other
	ArrayList<MusicNote> notes = new ArrayList<MusicNote>();
	
	public int framesPerTick = 24;
	public int globalStartFrame = 48;

	public void setInitialFrame(int value)
	{
		globalStartFrame = value;
	}
	
	public void setFramesPerBeat(int value)
	{
		framesPerTick = value;
	}
	
	public void addNote(MusicNote note)
	{
		notes.add(note);
	}
	
	public File writeFile(String fileName)
	{
		File sheetMusicFile = null;
		try
		{
			Sequence midiSequence = new Sequence(javax.sound.midi.Sequence.PPQ, 4);
			
			Track midiTrack = midiSequence.createTrack();
			
			int finalTick = 0;
			
			for(MusicNote note: notes)
			{	
				ShortMessage noteMessage = new ShortMessage();
				
				int midiNote = note.note + note.octave * 12;
				int stringUsed = note.string;
				int startFrame = note.startingFrame;
				int endFrame = note.getEndingFrame();
				
				int frames = endFrame - startFrame;
				
				int ticks = (int) Math.round(((double)frames / (double) framesPerTick));
				
				if (ticks == 0) ticks = 1;
				
				int initialTick = (int) Math.round(((double) (startFrame - globalStartFrame) / (double) framesPerTick));
			
				if (initialTick < 0) initialTick = 0;
				
				if (ticks + initialTick > finalTick)
				{
					finalTick = ticks + initialTick;
				}
				
				//Annotate midi with string used to play note
				MetaMessage metaMessage = new MetaMessage();
				metaMessage.setMessage(0x01, (Integer.toString(stringUsed)).getBytes(), 1);
				
				MidiEvent noteEventMeta = new MidiEvent(metaMessage, initialTick);
				midiTrack.add(noteEventMeta);
				
				//Start of note
				noteMessage.setMessage(0x90, midiNote, 127);
				
				MidiEvent noteEvent = new MidiEvent(noteMessage, initialTick);
				
				midiTrack.add(noteEvent);

				//End of note
				noteMessage = new ShortMessage();
				noteMessage.setMessage(0x80, midiNote, 127);
				
				noteEvent = new MidiEvent(noteMessage, initialTick + ticks);
				
				midiTrack.add(noteEvent);
			}
			
			//End track
			MetaMessage endMessage = new MetaMessage();
			endMessage.setMessage(0x2F, new byte[] {}, 0);
			MidiEvent finalEvent = new MidiEvent(endMessage, finalTick);
			midiTrack.add(finalEvent);
			
			if (fileName == null) fileName = "GuitarVisionTranscription";
			
			//Write file
			sheetMusicFile = new File(fileName + ".mid");
			MidiSystem.write(midiSequence, 1, sheetMusicFile);
		}
		catch (InvalidMidiDataException e) {
			System.out.println(e.getMessage());
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
		
		return sheetMusicFile;
	}
}
