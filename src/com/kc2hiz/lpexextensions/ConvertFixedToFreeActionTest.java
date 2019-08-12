package com.kc2hiz.lpexextensions;

import static org.junit.Assert.*;
import org.junit.Test;

import com.ibm.lpex.core.LpexView;

public class ConvertFixedToFreeActionTest {

	@Test
	public void testGetSpecFromTextNaive() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("Null input", "?", c.getSpecFromTextNaive(""));
		assertEquals("comment", "*", c.getSpecFromTextNaive("      *comment"));
		assertEquals("comment free 1", "*", c.getSpecFromTextNaive("//comment"));
		assertEquals("H", "h", c.getSpecFromTextNaive("     h debug"));
		assertEquals("F", "f", c.getSpecFromTextNaive("     fqsysprt"));
		assertEquals("E", "e", c.getSpecFromTextNaive("     earray"));
		assertEquals("L", "l", c.getSpecFromTextNaive("     lqsysprt"));
		assertEquals("I", "i", c.getSpecFromTextNaive("     i 01   25"));
		assertEquals("C", "c", c.getSpecFromTextNaive("     c   begin   tag"));
		assertEquals("O", "o", c.getSpecFromTextNaive("     o udate y"));
		assertEquals("D", "d", c.getSpecFromTextNaive("     d variable 10i 0"));
		assertEquals("P", "p", c.getSpecFromTextNaive("     p   b"));
		assertEquals("/copy", " ", c.getSpecFromTextNaive("      /copy qprotosrc"));
		assertEquals("**", "?", c.getSpecFromTextNaive("**  Compile-time table"));
        assertEquals("free form short stmt", ";", c.getSpecFromTextNaive("dcl-s;"));
	}
	
	
	// TODO I don't know how to mock this one up
    @Test
    public void testGetSpecFromView() {
        ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();

        LpexView view = new LpexView();
        view.doCommand("insert       *comment");
        
        // Pretend the cursor is on line 1
        int thisLine = 0;
        
        assertEquals("Null input", "?", c.getSpecFromView(view, thisLine));
        assertEquals("comment", "*", c.getSpecFromView(view, thisLine));
        assertEquals("comment free 1", "*", c.getSpecFromView(view, thisLine));
        assertEquals("H", "h", c.getSpecFromView(view, thisLine));
        assertEquals("F", "f", c.getSpecFromView(view, thisLine));
        assertEquals("E", "e", c.getSpecFromView(view, thisLine));
        assertEquals("L", "l", c.getSpecFromView(view, thisLine));
        assertEquals("I", "i", c.getSpecFromView(view, thisLine));
        assertEquals("C", "c", c.getSpecFromView(view, thisLine));
        assertEquals("O", "o", c.getSpecFromView(view, thisLine));
        assertEquals("D", "d", c.getSpecFromView(view, thisLine));
        assertEquals("P", "p", c.getSpecFromView(view, thisLine));
        assertEquals("/copy", " ", c.getSpecFromView(view, thisLine));
        assertEquals("**", " ", c.getSpecFromView(view, thisLine));
        assertEquals("free form short stmt", ";", c.getSpecFromView(view, thisLine));
    }

	@Test
	public void testIsComment() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("Null input", false, c.isComment(""));
		assertEquals("Not comment", false, c.isComment("     c    movel x y"));
		assertEquals("Not comment, free", false, c.isComment("y = 100;"));
		assertEquals("Comment", true, c.isComment("     c* this is a comment"));
		assertEquals("Comment free", true, c.isComment("// this is a comment"));
		assertEquals("Comment free 8", true, c.isComment("       // this is a comment"));
		assertEquals("**", false, c.isComment("**      // part of the compile time table"));
	}

	@Test
	public void testGetComment() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("Null input", "", c.getComment(""));
		assertEquals("Not comment", "", c.getComment("     c    movel x y"));
		assertEquals("Not comment, free", "", c.getComment("y = 100;"));
		assertEquals("Comment", " this is a comment", c.getComment("     c* this is a comment"));
		assertEquals("Comment free", " this is a comment", c.getComment("// this is a comment"));
		assertEquals("Comment free 8", " this is a comment", c.getComment("       // this is a comment"));
		// does not do RH comments
		assertNotEquals("RH comment, free", "comment here", c.getComment("y = 100;     //comment here"));
	}

	@Test
	public void testGetRhCommentFromText() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("null input", "", c.getRhCommentFromText(""));
		assertEquals("short input", "", c.getRhCommentFromText("     d FILE_NAME              83     92"));
		assertEquals("empty comment", "", c.getRhCommentFromText("     d FILE_NAME              83     92                                                           "));
		assertEquals("File name", "* File name", c.getRhCommentFromText("     d FILE_NAME              83     92                                         * File name"));
	}

	@Test
	public void testLog() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		c.log("JUnit test");
	}

	@Test
	public void testPadLeft() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("Null input", "", c.padLeft("", 0));
		assertEquals("Null input, pad 7", "       ", c.padLeft("", 7));
		assertEquals("Pad 0", "dcl-s", c.padLeft("dcl-s", 0));
		assertEquals("Pad 1", " dcl-s", c.padLeft("dcl-s", 1));
		assertEquals("Long pad 0", "dcl-s aardvark char(256)", c.padLeft("dcl-s aardvark char(256)", 0));
		assertEquals("Long pad 7", "       dcl-s aardvark char(256)", c.padLeft("dcl-s aardvark char(256)", 7));
	}
	
	@Test
	public void testGetDataTypeKeyword() {
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		assertEquals("Null input", "", c.getDataTypeKeyword("", "", "", "", ""));
		assertEquals("10 ", "char(10)", c.getDataTypeKeyword("", "10", "", "", ""));
		assertEquals("10a ", "char(10)", c.getDataTypeKeyword("", "10", "a", "", ""));
		assertEquals("10 pos(1)", "char(10) pos(1)", c.getDataTypeKeyword("1", "10", "", "", ""));
		assertEquals("10a pos(1) ", "char(10) pos(1)", c.getDataTypeKeyword("1", "10", "a", "", ""));
		assertEquals("10 (p)", "packed(10: 0)", c.getDataTypeKeyword("", "10", "", "0", ""));
		assertEquals("5 int", "int(5)", c.getDataTypeKeyword("", "5", "i", "0", ""));
	}

}
