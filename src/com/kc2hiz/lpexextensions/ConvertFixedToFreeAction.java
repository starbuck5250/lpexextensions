package com.kc2hiz.lpexextensions; 

import com.ibm.lpex.core.LpexAction;
import com.ibm.lpex.core.LpexView;
import com.ibm.lpex.core.LpexLog;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Convert Fixed-specification to fully free format
 * <p>Intended to convert one spec at a time to allow for easier review of the conversion.
 * <p>for use as an Lpex User Action.
 * @author buck
 * @version 01.00.00 Initial
 * @version 01.00.01 Add H-spec
 * @version 01.00.02 Add global variable for column to start free-form in
 *
 */ 
public class ConvertFixedToFreeAction implements LpexAction {

	public ConvertFixedToFreeAction() {
		// empty constructor
	}
	
	
	
	// default start column where free-form code will be placed
	int startColumn = 1;
	int padColumns = startColumn - 1;
	
	/**
	 * Check to see if we should be allowed to perform the D to free conversion
	 * @param view LpexView to operate on
	 * @return true if action is available for this view
	 * @see com.ibm.lpex.core.LpexAction#available(com.ibm.lpex.core.LpexView)
	 */
	@Override
	public boolean available(LpexView view) {
		  return view.currentElement() > 0 &&
			         !view.queryOn("readonly"); /* &&
			         view.queryOn("block.anythingSelected"); */
	}

	/**
	 * Converts fixed specifications to fully free
	 * @param view LpexView to operate on
	 * @see com.ibm.lpex.core.LpexAction#doAction(com.ibm.lpex.core.LpexView)
	 */
	@Override
	public void doAction(LpexView view) {
		
		// work with the line the cursor is on
		int thisLine = view.currentElement();
		String sourceStmt = view.elementText(thisLine);

		// leave if no text
		if (sourceStmt.length() == 0) {
		    view.doCommand("set messageText Empty text");			
			return;
		}

		// need to at least see 6 columns or we don't possibly have a fixed form spec
		if (sourceStmt.length() <= 5) {
			view.doCommand("set messageText Line too short");
			return;
		}

		// one routine for each spec we're dealing with
		String specType = getSpecFromText(sourceStmt);
		
		switch (specType) {
			case "h":
				hToFree(view, sourceStmt, thisLine);
				break;
			case "d":
				dToFree(view, sourceStmt, thisLine);
				break;
			case "p":
				dToFree(view, sourceStmt, thisLine);
				break;
			default:
			    view.doCommand("set messageText specType is" + specType);
			    break;
		}
		
		return;
	}
	

	
	// method to handle converting D-specs to fully free
	private void dToFree(LpexView view, String sourceStmt, int thisLine) {
		// Instantiate a DSpec object.  The constructor will break out the columns.
		DSpec dspec = new DSpec(view, sourceStmt, thisLine);

		// leave if we're not looking at a D- or P-specification
		if (!dspec.spec.equals("d") && 
				!dspec.spec.equals("p")) {
		    view.doCommand("set messageText Not a D- or P-spec");			
			return;
		}
		
		// Handle the various definition types we know about
		if (dspec.spec.equals("d") || dspec.spec.equals("p")) {
			if (dspec.defType.trim().equals("b") ||
					dspec.defType.trim().equals("c") ||
					dspec.defType.trim().equals("e") ||
					dspec.defType.equals("ds") ||
					dspec.defType.equals("pi") ||
					dspec.defType.equals("pr") ||
					dspec.defType.trim().equals("s")) {
				convertSubfieldsToFree(view, dspec);
				// position the cursor to the top of the area we converted from
				view.doDefaultCommand("locate element " + thisLine);
				view.doDefaultCommand("set position 1");
			} else {
				view.doCommand("set messageText Unusable D-spec. In the middle of a structure? " + dspec.defType);
			}	
		}

	}


	// method to handle converting H-specs to fully free
	private void hToFree(LpexView view, String sourceStmt, int thisLine) {
		// Instantiate a DSpec object.  The constructor will break out the columns.
		HSpec hspec = new HSpec(view, sourceStmt, thisLine);

		int lastSubfieldNumber = view.currentElement();	
		ArrayList<String> dsLines = new ArrayList<String>();
		String dsDclTemp = "";
		ConvertFixedToFreeAction c = new ConvertFixedToFreeAction();
		dsDclTemp = c.padLeft("ctl-opt", padColumns);
				
		// if there are keywords, append them
		if (hspec.keywords.length() != 0) {
			dsDclTemp = dsDclTemp.concat(" " + hspec.keywords);
		}

		// append the semicolon
		dsDclTemp = dsDclTemp.concat(";");

		// right hand comments (if any) come after the semicolon
		if (hspec.rhComment.length() != 0) {
			dsDclTemp = dsDclTemp.concat(" // " + hspec.rhComment);
		}
		
		// now that we have a fully formed line, add it to the array of lines
		dsLines.add(dsDclTemp);

		// position cursor AFTER the block we just read
		view.doCommand("locate line " + (lastSubfieldNumber));
		
		// loop through the array and write the contents out
		for (String dsLine: dsLines) {
			if (!dsLine.isEmpty()) {
				view.doDefaultCommand("insert " + dsLine);
			}
		}
		
		// re-position the cursor to the top of the area we converted from
		view.doDefaultCommand("locate element " + thisLine);
		view.doDefaultCommand("set position 1");
		
	}	



	/**
	 * Convert one or more field definitions from fixed to free
	 * If a standalone or constant, converts just the one line
	 * If a DS, PI or PR, converts the entire structure
	 * @param view LpexView - the current view we're working on
	 * @param dspec String - the first line of the structure (DS, PR, PI)
	 */
private void convertSubfieldsToFree(LpexView view, DSpec dspec) {
		int e = 0;
		int lastSubfieldNumber = view.currentElement();	
		int specLineNumber = view.currentElement();	
		String dsTemp = "";
		ArrayList<String> dsLines = new ArrayList<String>();
		String dsDclTemp = "";
				
		// the declare uses the definition type
		// unless b, which is a P-spec and s/b 'proc'
		// unless e, which gets nothing for the declaration
		if (!dspec.defType.trim().equals("e")) {
			if (!dspec.defType.trim().equals("b")) {
				dsDclTemp = "        dcl-" + dspec.defType.trim() + " "	+ dspec.name;
			} else {
				dsDclTemp = "        dcl-proc " + dspec.name;
			}
				
			// if there is a datatype, append it
			String subfieldDataType = getDataTypeKeyword(dspec.fromPos, dspec.len, dspec.dataType, dspec.decimals, dspec.keywords);
			if (subfieldDataType.length() != 0) {
				dsDclTemp = dsDclTemp.concat(" " + subfieldDataType);
			}

			// if there are keywords, append them
			// first, strip out procptr
			if (dspec.keywords.length() != 0) {
				dspec.keywords = dspec.keywords.replace("procptr", "");
				dsDclTemp = dsDclTemp.concat(" " + dspec.keywords);
			}

			// append the semicolon
			dsDclTemp = dsDclTemp.concat(";");

			// right hand comments (if any) come after the semicolon
			if (dspec.rhComment.length() != 0) {
				dsDclTemp = dsDclTemp.concat(" // " + dspec.rhComment);
			}
		
			// now that we have a fully formed line, add it to the array of lines
			dsLines.add(dsDclTemp);
		}

		
		// loop forward through the next set of source lines
		// until the end of the structure is found
		// note that for standalone and constant lines, the very next spec terminates the 'structure'
		for (e = specLineNumber + 1; e <= view.elements(); e++) {
			
			String dsSubfield = view.elementText(e);
			
			// if blank line, assume end of struc
			int textLengthDS = dsSubfield.length();
			if (textLengthDS == 0) {
				break;
			}

			// need to at least see 6 columns or we don't possibly have a d-spec
			if (textLengthDS <= 5) {
				break;
			} 

			// make a new DSpec object which will break out the columns
			DSpec subfield = new DSpec(view, dsSubfield, e);

			// comments have no fields to parse, but  
			// carry the comments forward into the converted block
			if (subfield.isComment) {
				dsTemp = "       // " + subfield.longComment.trim();
			} else {
				
				// read the next line if this one is a continuation
				if (dsSubfield.matches(".*\\.\\.\\.")) {
					continue;
				}

				// leave the loop if we've reached the end of the subfields
				if (subfield.spec.equals("d") && !subfield.defType.equals("  ")) {
					break;
				}

				// leave if we're not looking at a D-specification
				if (!subfield.spec.equals("d")) {
					break;
				}

				// now generate the keywords for data type
				String dataTypeKwdDS = getDataTypeKeyword(subfield.fromPos, subfield.len, subfield.dataType, subfield.decimals, subfield.keywords);

				dsTemp =  "           " + 
						subfield.name + " " + dataTypeKwdDS;
				// keywords are optional; don't leave a trailing space if none needed
				// also, strip procptr
				if (subfield.keywords.length() != 0) {
					subfield.keywords = subfield.keywords.replace("procptr", "");
					dsTemp = dsTemp.concat(" " + subfield.keywords);
				}

				// add the terminating semicolon
				dsTemp = dsTemp.concat(";");

				// if we have a right hand comment, carry it forward
				if (subfield.rhComment.length() != 0) {
					dsTemp = dsTemp.concat(" // " + subfield.rhComment.trim());
				}
				
				// save the element number of the last subfield we actually processed
				lastSubfieldNumber = e;
			}
			dsLines.add(dsTemp);
			
		};
		
		// ...and the end
		// not needed for standalone and constant
		if (!dspec.defType.trim().equals("c") && 
			!dspec.defType.trim().equals("s") &&
			!dspec.defType.trim().equals("b") &&
			!dspec.defType.trim().equals("p")) {
			
			String endDsTemp = "";
			// the declare uses the definition type
			// unless b/e, which are P-specs and s/b 'proc'
			if (!dspec.defType.trim().equals("e")) {
				endDsTemp = "        end-"  + dspec.defType + ";";
			} else {
				endDsTemp = "        end-proc;";
			}
			
			dsLines.add(endDsTemp);
		}

		// position cursor AFTER the block we just read
		view.doCommand("locate line " + (lastSubfieldNumber));
		
		// loop through the array and write the contents out
		for (String dsLine: dsLines) {
			if (!dsLine.isEmpty()) {
				view.doDefaultCommand("insert " + dsLine);
			}
		}
	}


/**
 * build up the data type keyword based on the data type
 *   which has been partially parsed out of the d-spec
 * @param fromPos String
 * @param len String
 * @param dataType String
 * @param decimals String
 * @param keywords String
 * @return dataTypeKwd String
 */
String getDataTypeKeyword(String fromPos, String len, String dataType, String decimals, String keywords) {
	String dataTypeKwd = "";
	int tempLen = 0;

	// the data type can be blank and the RPG compiler will
	// supply a rational default.  We need to do the same thing.
	// note that pointer data types have no length
	if (len.length() != 0 && dataType != "*") {
		if (dataType.length() == 0) {
			if (decimals.length() == 0) {
				dataType = "a";
			} else {
				dataType = "p";
			}
		}
	}

	// have a length adjustment.  No data type keyword for this.
	// instead, append the adjustment to the LIKE keyword
	// in the getKeywordsFromText method
	if (!len.matches(".*\\+.*")) {
		// old style from/to specs
		// convert length 
		String tempLenChar = len;
		if (fromPos.length() != 0) {
			tempLen = Integer.parseInt(len) - Integer.parseInt(fromPos) + 1;
			tempLenChar = String.valueOf(tempLen);
		}

		// data type
		// TODO is this all the possible data types?
		switch (dataType) {
		case "":	// data structures
			dataTypeKwd = "";
			break;
		case "a":
			dataTypeKwd = "char(" + tempLenChar + ")";
			break;
		case "f":
			dataTypeKwd = "float(" + tempLenChar + ")";
			break;
		case "i":
			dataTypeKwd = "int(" + tempLenChar + ")";
			break;
		case "p":
			dataTypeKwd = "packed(" + tempLenChar + ": " + decimals + ")";
			break;
		case "s":
			dataTypeKwd = "zoned(" + tempLenChar + ": " + decimals + ")";
			break;
		case "u":
			dataTypeKwd = "uns(" + tempLenChar + ")";
			break;
		case "*":
			// the procptr keyword becomes pointer(*proc)
			int i = keywords.indexOf("procptr");
			String procKwd = "";
			if (i != -1) {
				procKwd = "(*proc)";
			}
			dataTypeKwd = "pointer" + procKwd;
			break;
		default:
			dataTypeKwd = "unk(" + dataType + ") tempLenChar("
					+ tempLenChar + ")";
			break;
		}

		// we may have an old style from-to situation
		// use the POS keyword to tell the compiler where the subfield starts
		if (fromPos.length() != 0) {
			dataTypeKwd = dataTypeKwd.concat(" pos(" + fromPos + ")");
		}
	}

	return dataTypeKwd;
}

//utility methods
/**
* Extract RPG specification type (D, P, C, F) from a line of source code
* @param sourceStmt String raw text
* @return spec String lower case spec (c, d, f, p, etc or blank if not a spec we care about)
* 
* We don't need to worry about free-form declarations, because they're already free :-)
*/
String getSpecFromText(String sourceStmt) {
	String spec = " ";
	
	if (sourceStmt.length() > 5 && isComment(sourceStmt) == false) {
		if (!sourceStmt.substring(0, 2).equals("**")) {
			spec = sourceStmt.substring(5, 6).toLowerCase();
		}
	}
	return spec;
}


/**
* is this entire spec a comment line?
* @param sourceStmt String - raw D-specification
* @param d DSpec - the parsed d-spec object
* @return true if entire line is a comment
*/
boolean isComment(String sourceStmt) {
boolean isComment = false;

if (sourceStmt.length() >= 8) {

	// comments can be either a * in column 7 or
	// a pair of slashes preceded by optional white space
	// the first is easy:
	if (sourceStmt.substring(6, 7).equals("*")) {
		isComment = true;
	}

	// the second is a bit harder: 
	if (sourceStmt.matches(" *//.*")) {
		isComment = true;
	}
}
return isComment;
}

/**
* extract comment from a comment line
* @param sourceStmt String - raw D-specification
* @return comment String
*/
String getComment(String sourceStmt) {
String comment = "";

if (isComment(sourceStmt)) {

	// comments can be either a * in column 7 or
	// a pair of slashes preceded by optional white space
	// the first is easy:
	if (sourceStmt.substring(6, 7).equals("*")) {
		comment = sourceStmt.substring(7, sourceStmt.length());
	}

	// the second is a bit harder: 
	if (sourceStmt.matches(" *//.*")) {
		String[] parts = sourceStmt.split(" *//");
		comment = parts[1];
	}
}
return comment;
}


/**
* Extract right hand comment from raw d-spec
* @param sourceStmt String raw d-spec
* @return rhComment String right-hand comment
*/
String getRhCommentFromText(String sourceStmt) {
String rhComment = "";

if (sourceStmt.length() > 81) {
	int i = 100;
	if (sourceStmt.length() <= i) {
		i = sourceStmt.length();
	}
	rhComment = sourceStmt.substring(80, i).trim();
}
return rhComment;
}

/**
* Log debugging text to the error log
* @param message String 
*/
void log(String message) {
final boolean DEBUG = false;

if (DEBUG) {
	LpexLog.log(message);
}	


}

/**
 * Left pad a string
 * returns the original string with 1-based space characters on the left
 * @parm spec String
 */
public String padLeft(String spec, int padLength) {
	String pad = new String("");
	if (padLength > 0) {
		for (int i = 1; i <= padLength; i++)
			pad += " ";}
	return pad + spec;
}


/**
* This stores the various column based fields for an h-spec
* This object tokenises the line with the definitions.  It will also move backward 
* in the view's text lines to accumulate names continued from earlier lines.
* @param view LpexView 
* @param dSpec String a single raw d-spec (field / subfield)
* @author buck
*
*/
class HSpec {
public String spec = "";
public String keywords = "";
public String rhComment = "";
public boolean isComment = false;
public String longComment = "";

// constructor
public HSpec(LpexView view, String sourceStmt, int thisLine) {
	
	// I prefer lower case, so everything except the name will be monocased
	// RDi trims each line, so they don't all equal 100 bytes...
	// must test for out of bounds before substringing!
	
	/*
	 *  1 -  5 sequence number / text
	 *  6 -  6 hSpec 
	 *  7 - 80 keywords
	 * 81 -100 comment
	 * 
	 * Alternatively, there can be a comment of the form:
	 *  6 -  6 hSpec 
	 *  7 -  7 *
	 *  8 -100 comment
	 *  
	 * Alternatively, there can be a comment of the form:  
	 * 6 -  7 blanks
	 * 8 -100 // comment.  Note that there can be any number of blanks 
	 *                     preceding the //.
	 *                     
	 * Alternatively, there can be a continuation line of the form:
	 * 6 -  6 d
	 * 7 - 80 someName...                     
	 * 
	 */
		
	spec = getSpecFromText(sourceStmt);
	
	isComment = isComment(sourceStmt);
	
	// early exit if a comment
	if (isComment) {
		longComment = getComment(sourceStmt);
		return;
	}

	keywords = getKeywordsFromHSpec(sourceStmt);
	rhComment = getRhCommentFromText(sourceStmt);

}
/**
 * Extract keywords from raw h-spec
 * @param sourceStmt hSpec String raw h-spec
 * @return keywords String keywords
 */
private String getKeywordsFromHSpec(String sourceStmt) {
	String keywords = "";

	log("start getKeywordsFromTextString " + sourceStmt);

	if (sourceStmt.length() > 7) {
		int i = 80;
		if (sourceStmt.length() <= i) {
			i = sourceStmt.length();
		}
		keywords = sourceStmt.substring(7, i).toLowerCase().trim();

	}
	return keywords;
}

}


/**
 * This stores the various column based fields for a d-spec
 * This object tokenises the line with the definitions.  It will also move backward 
 * in the view's text lines to accumulate names continued from earlier lines.
 * 
 * TODO: Break apart the Lpexview dependency
 * 
 * @param view LpexView 
 * @param dSpec String a single raw d-spec (field / subfield)
 * @author buck
 *
 */
class DSpec {
	public String spec = "";
	public String name = "";
	public String extType = "";
	public String dsType = "";
	public String defType = "";
	public String fromPos = "";
	public String len = "";
	public String dataType = "";
	public String decimals = "";
	public String keywords = "";
	public String rhComment = "";
	public String dataTypeKwd = "";
	public boolean isComment = false;
	public String longComment = "";
	
	// constructor
	public DSpec(LpexView view, String sourceStmt, int thisLine) {
		
		// I prefer lower case, so everything except the name will be monocased
		// RDi trims each line, so they don't all equal 100 bytes...
		// must test for out of bounds before substringing!
		
		/*
		 *  1 -  5 sequence number / text
		 *  6 -  6 dSpec 
		 *  7 - 21 name
		 * 22 - 22 external type (' ', 'e')
		 * 23 - 23 DS type (' ', 's', 'u')
		 * 24 - 25 def type (' ', 'c', 'ds', 'pi', 'pr')
		 * 26 - 32 from
		 * 33 - 39 len / to
		 * 40 - 40 type (a, p, i, z, *, etc)
		 * 41 - 42 decimal
		 * 44 - 80 keywords
		 * 81 -100 comment
		 * 
		 * Alternatively, there can be a comment of the form:
		 *  6 -  6 dSpec 
		 *  7 -  7 *
		 *  8 -100 comment
		 *  
		 * Alternatively, there can be a comment of the form:  
		 * 6 -  7 blanks
		 * 8 -100 // comment.  Note that there can be any number of blanks 
		 *                     preceding the //.
		 *                     
		 * Alternatively, there can be a continuation line of the form:
		 * 6 -  6 d
		 * 7 - 80 someName...                     
		 * 
		 */
			
		spec = getSpecFromText(sourceStmt);
		
		isComment = isComment(sourceStmt);
		
		// early exit if a comment
		if (isComment) {
			longComment = getComment(sourceStmt);
			return;
		}

		extType = getExtTypeFromText(sourceStmt);
		dsType = getDsTypeFromText(sourceStmt);
		defType = getDefTypeFromText(sourceStmt);
		fromPos = getFromPosFromText(sourceStmt);
		len = getLenFromText(sourceStmt);
		dataType = getDataTypeFromText(sourceStmt);
		decimals = getDecimalsFromText(sourceStmt);
		keywords = getKeywordsFromDSpec(sourceStmt, len);
		rhComment = getRhCommentFromText(sourceStmt);
		// do this last to load the rest of the spec columns before it
		name = getNameFromText(view, sourceStmt, thisLine);

	}

	// ==========================================================
	// individual methods to extract the columns


	/**
	 * Extract name from raw d-spec
	 * @param view LpexView
	 * @param sourceStmt raw d-spec
	 * @param thisLine int line number passed in
	 * @return name String
	 */
	private String getNameFromText(LpexView view, String sourceStmt, int thisLine ) {
		// name is variable length, ending anywhere from 7 to 21
		// so we could potentially have a name like i which would
		// only be in column 7.
		String name = "";
		StringBuilder sbname = new StringBuilder();

		// line too short to hold a name
		if (sourceStmt.length() < 6) {
			return "";
		}
		
		int i = 21;
		if (sourceStmt.length() <= i) {
			i = sourceStmt.length();
		}	
		sbname = sbname.append(sourceStmt.substring(6, i).trim());
		
		
		// names can be continued on another line
		// we started on the specification line;
		// back up until we run out of continuation lines
		// go backward from the line with the specs
		for (int j = thisLine - 1; j != 0; j--) {
			String sourceStmtPrior = view.elementText(j);
			String specPrior = "";
			String namePrior = "";
			String defTypePrior = "";
			String defTypePrior1 = "";
			boolean isCommentPrior = false;
			
			specPrior = getSpecFromText(sourceStmtPrior);
			isCommentPrior = isComment(sourceStmtPrior);

			// read another line if this is a comment
			if (isCommentPrior) {
				continue;
			}

			// line too short to hold a name
			if (sourceStmtPrior.length() <= 6) {
				continue;
			}
			
			i = 21;
			if (sourceStmtPrior.length() <= i) {
				i = sourceStmtPrior.length();
			}	
			namePrior = sourceStmtPrior.substring(6, i).trim();
				
			defTypePrior1 = defTypePrior;
			defTypePrior = getDefTypeFromText(sourceStmtPrior);
			
			// is the name continued?
			if (sourceStmtPrior.matches(".*\\.\\.\\.")) {
				sbname.insert(0, sourceStmtPrior.substring(6, sourceStmtPrior.length() - 3).trim());
				continue;
			} else {
				// if d or p spec AND
				// name is blank AND
				// defType (s, pi etc) doesn't change THEN
				// it's legal and read another
				if ((specPrior.equals("d") || specPrior.equals("p")) &&
					(namePrior.length() == 0) &&
					(defTypePrior.equals(defTypePrior1))) {
					continue;
				}
				break;
			}
		}

		
		// convert the StringBuilder name to the permanent String
		name = sbname.toString();
		return name;
	}

	/**
	 * Get external data type from raw d-spec
	 * @param sourceStmt String raw d-spec
	 * @return extType String
	 */
	private String getExtTypeFromText(String sourceStmt) {
		String extType = "";
		
		if (sourceStmt.length() > 21) {
			extType = sourceStmt.substring(21, 22).toLowerCase();
		}
		
		return extType;
	}

	/**
	 * Extract data structure type from d-spec
	 * @param sourceStmt String raw d-spec
	 * @return dsType String data structure type
	 */
	private String  getDsTypeFromText(String sourceStmt) {
		String dsType = "";
		
		if (sourceStmt.length() > 22) {
			dsType = sourceStmt.substring(22, 23).toLowerCase();
		}
		return dsType;
	}

	/**
	 * Extract definition type from raw d-spec
	 * @param sourceStmt dSpec String raw d-spec
	 * @return defType String definition type
	 */
	private String getDefTypeFromText(String sourceStmt) {
		String defType = "";
		
		if (sourceStmt.length() > 23) {
			int i = 25;
			if (sourceStmt.length() <= i) {
				i = sourceStmt.length();
			}
			defType = sourceStmt.substring(23, i).toLowerCase();
		}
		return defType;
	}

	/**
	 * Extract From position from raw d-spec
	 * @param sourceStmt dSpec String raw d-spec
	 * @return fromPos String From position
	 */
	private String getFromPosFromText(String sourceStmt) {
		String fromPos = "";
		
		if (sourceStmt.length() > 31) {
			fromPos = sourceStmt.substring(25, 32).toLowerCase().trim();
		}
		return fromPos;
	}

	/**
	 * Extract Length / To position from raw d-spec
	 * @param sourceStmt dSpec String raw d-spec
	 * @return len String Length / To position 
	 */
	private String getLenFromText(String sourceStmt) {
		String len = "";
		
		if (sourceStmt.length() > 38) {
			len = sourceStmt.substring(32, 39).toLowerCase().trim();
		}
		return len;
	}

	/**
	 * Extract data type from raw d-spec
	 * @param sourceStmt dSpec String raw d-spec
	 * @return dataType String data type
	 */
	private String getDataTypeFromText(String sourceStmt) {
		String dataType = "";
		
		if (sourceStmt.length() > 39) {
			dataType = sourceStmt.substring(39, 40).toLowerCase().trim();
		}
		return dataType;
	}

	/**
	 * Extract decimals from raw d-spec
	 * @param sourceStmt dSpec String raw d-spec
	 * @return decimals String decimals
	 */
	private String getDecimalsFromText(String sourceStmt) {
		String decimals = "";
		
		if (sourceStmt.length() > 41) {
			decimals = sourceStmt.substring(40, 42).toLowerCase().trim();
		}
		return decimals;
	}

	/**
	* Extract keywords from raw d-spec
	* @param sourceStmt dSpec String raw d-spec
	* @param len String to / length
	* @return keywords String keywords
	*/
	private String getKeywordsFromDSpec(String sourceStmt, String len) {
	String keywords = "";
	String keywordsAdj = "";

	log("start getKeywordsFromTextString " + sourceStmt);

	if (sourceStmt.length() > 43) {
		int i = 80;
		if (sourceStmt.length() <= i) {
			i = sourceStmt.length();
		}
		keywords = sourceStmt.substring(43, i).toLowerCase().trim();
		
		// do we have a length adjustment?
		if (keywords.length() > 0) {						// have keywords
			log("keywords.length()=" + keywords.length());	
			if (len.matches(".*\\+.*")) {					// have plus
				log("len.matches +");
				if (keywords.matches(".*like(.*).*")) {		// have LIKE()
					log("keywords.matches like(");
					
					// yes, length adjustment
					// delete the spaces
					String lenAdj = len.replace(" ", "").trim();
					log("lenAdj=" + lenAdj);

					// split the LIKE() into two strings by use of regex groups
					// ...LIKE(LIKEVAR
					// ) INZ(12)...
					Pattern likeSplit = Pattern.compile("(.*like\\([^\\)]*)(\\).*)");
					Matcher m = likeSplit.matcher(keywords);
					if (m.matches()) {
						log("group 1=" + m.group(1));
						log("group 1=" + m.group(2));
					
						// re-assemble with the length adjustment inserted
						keywordsAdj = m.group(1) + ": " + lenAdj + m.group(2);
						log("keywordsAdj=" + keywordsAdj);
						
						keywords = keywordsAdj;
					} else {
						log("no match");
					}
				}
			}
			
		}
	}
	return keywords;
	}
	
}

}

