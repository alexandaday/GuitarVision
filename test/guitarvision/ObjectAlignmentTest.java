package guitarvision;

import static org.junit.Assert.*;

import java.util.ArrayList;

import guitarvision.sheetmusic.ObjectAlignment;

import org.junit.Test;

public class ObjectAlignmentTest {
	@Test
	public void testAlignment()
	{
		ArrayList<String> list1 = new ArrayList<String>();
		ArrayList<String> list2 = new ArrayList<String>();
		
		list1.add("A");
		list1.add("T");
		list1.add("G");
		list1.add("T");
		list1.add("T");
		list1.add("A");
		list1.add("T");
		list1.add("A");
		
		list2.add("A");
		list2.add("T");
		list2.add("C");
		list2.add("G");
		list2.add("T");
		list2.add("C");
		list2.add("C");
		
		
		ObjectAlignment<String> alignment = new ObjectAlignment<String>(list1, list2);
		
		alignment.setInsertDeletePenalty(0);
		alignment.setMismatchPenalty(0);
		alignment.setMatchScore(1);
		
		int result = alignment.computeLongestMatchScore();
		
		System.out.println(result);
		
		assertEquals(4, result);
	}
}
