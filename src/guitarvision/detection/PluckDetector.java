package guitarvision.detection;

import java.util.ArrayList;

public class PluckDetector {
	public ArrayList<GuitarString> initialStrings;
	
	private double scaleFactor = 6;
	
	
	public PluckDetector(ArrayList<GuitarString> initialStrings)
	{
		this.initialStrings = initialStrings;
	}
	
	public boolean[] detectStringsBeingPlayed(ArrayList<GuitarString> strings)
	{
		int numberStrings = StringDetector.numberStringsToDetect;
		
		boolean[] stringsBeingPlayed = new boolean [numberStrings];
		
		if ((strings.size() != initialStrings.size()) || (numberStrings != strings.size()))
		{
			return null;
		}
		else
		{
			for(int x = 0; x < numberStrings; x++)
			{
				GuitarString initialString = initialStrings.get(x);
				GuitarString currentString = strings.get(x);
				
//				System.out.println("Current thickness");
//				System.out.println(currentString.thickness);
//				System.out.println("Target thickness");
//				System.out.println(initialString.thickness * scaleFactor);
				
				
				if ((currentString.thickness > initialString.thickness * scaleFactor) && (initialString.thickness != 0))
				{
//					System.out.println("String thickness");
//					System.out.println(currentString.thickness);
//					System.out.println("Initial thickness * sf");
//					System.out.println(initialString.thickness * scaleFactor);
					stringsBeingPlayed[x] = true;
				}
			}
		}
		
		return stringsBeingPlayed;
	}
}
