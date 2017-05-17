package guitarvision.sheetmusic;

import java.util.ArrayList;

public class ObjectAlignment<T> {
	ArrayList<T> firstList;
	ArrayList<T> secondList;
	
	int matchScore = 1;
	int mismatchScore = 0;
	int insertDeleteScore = 0;
	
	T matchTolerance = null;
	
	//Perform string alignment on the notes in two MIDI files
	public ObjectAlignment(ArrayList<T> firstList, ArrayList<T> secondList)
	{
		this.firstList = firstList;
		this.secondList = secondList;
	}
	
	public void setMatchScore(int value)
	{
		matchScore = value;
	}
	
	public void setMismatchScore(int value)
	{
		mismatchScore = value;
	}
	
	public void setInsertDeleteScore(int value)
	{
		insertDeleteScore = value;
	}
	
	public void setMatchTolerance(T value)
	{
		matchTolerance = value;
	}
	
	private boolean nodeExists(int i, int j)
	{
		return ((i >= 0) && (i < firstList.size()) && (j >= 0) && (j < secondList.size()));
	}
	
	public int computeLongestMatchScore()
	{
		int[][] scores = new int[firstList.size()+1][secondList.size()+1];
		for (int i = 0; i <= firstList.size(); i++)
		{
			for (int j = 0; j <= secondList.size(); j++)
			{
				int northI = i - 1;
				int northJ = j;
				int northScore = 0;
				boolean northExists = nodeExists(northI, northJ);
				int westI = i;
				int westJ = j - 1;
				int westScore = 0;
				boolean westExists = nodeExists(westI, westJ);
				int northWestI = i - 1;
				int northWestJ = j - 1;
				int northWestScore = 0;
				boolean northWestExists = nodeExists(northWestI, northWestJ);
				
				if (northWestExists)
				{
					northWestScore = scores[northWestI][northWestJ];
					
					T firstVal = firstList.get(northWestI);
					T secondVal = secondList.get(northWestJ);
					
					boolean matchCondition = false;
					
					if (matchTolerance == null)
					{
						matchCondition = firstVal.equals(secondVal);
					}
					else
					{
						//Hardcoding bytes, but should work with any type with addition
						if (firstVal instanceof Byte && secondVal instanceof Byte)
						{
							Byte tolerance = (Byte) matchTolerance;
							Byte firstByte = (Byte) firstVal;
							Byte secondByte = (Byte) secondVal;
							matchCondition = (firstByte <= secondByte + tolerance) && (firstByte >= secondByte - tolerance);
						}
					}
					
					if (matchCondition)
					{
						northWestScore += matchScore;
					}
					else
					{
						northWestScore += mismatchScore;
					}
				}
				else
				{
					northWestScore = Integer.MIN_VALUE;
				}
				
				if (westExists)
				{
					westScore = scores[westI][westJ];
					westScore += insertDeleteScore;
				}
				else
				{
					westScore = Integer.MIN_VALUE;
				}
				
				if (northExists)
				{
					northScore = scores[northI][northJ];
					northScore += insertDeleteScore;
				}
				else
				{
					northScore = Integer.MIN_VALUE;
				}
				
				int newScore = Integer.max(northWestScore,Integer.max(northScore, westScore));
				
				if (!northWestExists && !westExists && !northExists)
				{
					newScore = 0;
				}
				
				scores[i][j] = newScore;
			}
		}
		return scores[scores.length-1][scores[0].length-1];
	}
}
