package com.badlogic.jack.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.badlogic.jack.utils.SourceWriter;

/**
 * Helps keeping track of literals, able to generate declarations and definitions.</p>
 * 
 * String literals are composed of a static j_char array holding the 16-bit character
 * data, and a java_lang_String, that references that char array.</p>
 * 
 * Adding a new literal can be done via 
 * 
 * To generate the declaration, call {@link #generateDeclarations(SourceWriter)}, to
 * generate the definitions call {@link #generateDefinitions(SourceWriter)}.
 * @author mzechner
 *
 */
public class LiteralsGenerator {
	/** the prefix **/
	private final String prefix;
	/** string literals **/
	private final Map<String, String> literals = new HashMap<String, String>();
	/** ids to literals, so we can output things in order **/
	private final Map<String, String> ids = new TreeMap<String, String>();
	/** next literal id for this class **/
	private int nextLiteralId;
	
	/**
	 * Constructs a new LiteralsGenerator. The prefix is added
	 * to all literal variable names (usually the class name within
	 * which the literal was defined).
	 * @param prefix the prefix added to each literal variable.
	 */
	public LiteralsGenerator(String prefix) {
		this.prefix = prefix;
	}
	
	/**
	 * Adds a new literal, returns a variable name pointing to
	 * a java_lang_String* that can be used in C++ code.
	 * 
	 * @param literal the string literal
	 * @return the variable name.
	 */
	public String addLiteral(String literal) {
		String literalId = literals.get(literal);
		if(literalId == null) {
			literalId = prefix + "_literal" + (nextLiteralId++);
			literals.put(literal, literalId);
			ids.put(literalId, literal);
		}
		return literalId;
	}
	
	/**
	 * Generates the definition of a string. Does not use the constructor
	 * since we use this mechanism to generate reflection data (see {@link ReflectionGenerator})
	 * before the String class is initialized.
	 * @param writer
	 */
	public void generateDefinitions(SourceWriter writer) {
		for(String id: ids.keySet()) {
			String literal = ids.get(id);
			writer.wl(id + " = new java_lang_String();");
			writer.wl(id + "->f_data = new Array<j_char>(" + id + "_array, " + literal.length() + ", true, 1, &java_lang_Character::f_TYPE);");
			writer.wl(id + "->f_offset = 0;");
			writer.wl(id + "->f_length = " + literal.length() + ";");
		}
	}
	
	/**
	 * Generates the declaration of a static j_char[] holding the character data
	 * and a java_lang_String* for each string instance. The string is instantiated in
	 * {@link #generateDefinitions(SourceWriter)} 
	 * @param writer
	 */
	public void generateDeclarations(SourceWriter writer) {
		// need to include classes/java_lang_Character.h since we need to pass the
		// java.lang.Class to the arrays created in generateDefinitions
		writer.wl("#include \"classes/java_lang_Character.h\"");
		
		for(String id: ids.keySet()) {
			String literal = ids.get(id);
			String literalDef = "// literal: \"" + literal.replace("\n", "\\n").replace("\r", "\\r") + "\"\n";
									
			literalDef += "j_char " + id + "_array[] = {";
			if(literal.length() == 0) {
				literalDef += "0";
			} else {
				for(int i = 0; i < literal.length(); i++) {
					if(i > 0) literalDef += ", ";
					literalDef += Short.toString((short)literal.charAt(i)); // FIXME type conversion/promotion
				}
			}
			literalDef += "};\n";
			literalDef += "java_lang_String* " + id + " = 0;";
			writer.wl(literalDef);
		}
	}
}
