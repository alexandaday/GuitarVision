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
		
		alignment.setInsertDeleteScore(0);
		alignment.setMismatchScore(0);
		alignment.setMatchScore(1);
		
		int result = alignment.computeLongestMatchScore();
		
		
		ArrayList<String> list3 = new ArrayList<String>();
		ArrayList<String> list4 = new ArrayList<String>();
		
		list3.add("A");
		list3.add("A");
		list3.add("B");
		list3.add("B");
		list3.add("C");
		list3.add("C");
		
		list4.add("D");
		list4.add("D");
		list4.add("A");
		list4.add("C");
		list4.add("B");
		list4.add("B");
		ObjectAlignment<String> alignment2 = new ObjectAlignment<String>(list3, list4);
		
		alignment2.setInsertDeleteScore(-1);
		alignment2.setMismatchScore(-1);
		alignment2.setMatchScore(4);
		
		int result2 = alignment2.computeLongestMatchScore();
		
		
		ArrayList<Byte> list5 = new ArrayList<Byte>();
		ArrayList<Byte> list6 = new ArrayList<Byte>();
		
		list5.add(new Byte((byte) 0x8));

		list6.add(new Byte((byte) 0x1));
		list6.add(new Byte((byte) 0x2));
		list6.add(new Byte((byte) 0x3));
		list6.add(new Byte((byte) 0x10));
		list6.add(new Byte((byte) 0x9));
		list6.add(new Byte((byte) 0x2));
		list6.add(new Byte((byte) 0x1));

		
		ObjectAlignment<Byte> alignment3 = new ObjectAlignment<Byte>(list5, list6);
		
		alignment3.setInsertDeleteScore(0);
		alignment3.setMismatchScore(-1);
		alignment3.setMatchScore(5);
		alignment3.setMatchTolerance(new Byte((byte) 1));
		
		int result3 = alignment3.computeLongestMatchScore();
		
		
		ArrayList<String> list7 = new ArrayList<String>();
		ArrayList<String> list8 = new ArrayList<String>();
		
		list7.add("X");
		list7.add("X");
		
		list8.add("Y");
		list8.add("X");
		
		ObjectAlignment<String> alignment4 = new ObjectAlignment<String>(list7, list8);
		
		alignment4.setInsertDeleteScore(-1);
		alignment4.setMismatchScore(-2);
		alignment4.setMatchScore(2);
		
		int result4 = alignment4.computeLongestMatchScore();
		
		assertEquals(4, result);
		assertEquals(7, result2);
		assertEquals(5, result3);
		assertEquals(0, result4);
	}
}
