package guitarvision.sheetmusic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.*;

public class SheetMusic {
	//Assume notes play one after the other
	ArrayList<MusicNote> notes = new ArrayList<MusicNote>();
	
	int ticksPerBeat = 4;
	public int framesPerBeat = 24;
	public int globalStartFrame = 48;

	public void setInitialFrame(int value)
	{
		globalStartFrame = value;
	}
	
	public void setFramesPerBeat(int value)
	{
		framesPerBeat = value;
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
			Sequence midiSequence = new Sequence(javax.sound.midi.Sequence.PPQ, ticksPerBeat);
			
			Track midiTrack = midiSequence.createTrack();
			
			long count = 0;
			
			for(MusicNote note: notes)
			{	
				ShortMessage noteMessage = new ShortMessage();
				
				int midiNote = note.note + note.octave * 12;
				int startFrame = note.startingFrame;
				int endFrame = note.getEndingFrame();
				
				int frames = endFrame - startFrame;
				
				int ticks = (int) Math.round(((double)frames / (double) framesPerBeat) * ticksPerBeat);
				
				int initialTick = (int) Math.round(((double) (startFrame - globalStartFrame) / (double) framesPerBeat) * ticksPerBeat);
				
				//System.out.println(ticks);
				//System.out.println(initialTick);
				
				
				//Start of note
				noteMessage.setMessage(0x90, midiNote, 127);
				
				MidiEvent noteEvent = new MidiEvent(noteMessage, initialTick);
				
				midiTrack.add(noteEvent);

				//End of note
				noteMessage = new ShortMessage();
				noteMessage.setMessage(0x80, midiNote, 127);
				
				noteEvent = new MidiEvent(noteMessage, initialTick + ticks);
				
				midiTrack.add(noteEvent);
				
				//increment based on note length
				count += ticksPerBeat;
			}
			
			//End track
			MetaMessage endMessage = new MetaMessage();
			endMessage.setMessage(0x2F, new byte[] {}, 0);
			MidiEvent finalEvent = new MidiEvent(endMessage, count + 120);
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
