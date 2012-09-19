package com.badlogic.jack.generators;

import java.util.TreeMap;
import java.util.TreeSet;

import soot.SootClass;
import soot.SootField;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.Tag;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Emits all static variables and string literals of a class as well
 * as the header section of the .cpp file.
 * @author mzechner
 *
 */
public class StaticsGenerator {
	private final SourceWriter writer;
	private final ClassInfo info;
	
	public StaticsGenerator(SourceWriter writer, ClassInfo info) {
		this.writer = writer;
		this.info = info;
	}
	
	public void generate() {
		// default include files
		writer.wl("#include <math.h>"); 
		writer.wl("#include <limits>");
		writer.wl("#include \"vm/Array.h\"");
		writer.wl("#include \"classes/java_lang_Class.h\"");
		writer.wl("#include \"classes/java_lang_String.h\"");
		
		// include the header for this class and its dependencies
		// we sort the dependencies and remove duplicates. This is
		// needed so the checks whether a .cpp file needs updating
		// works.
		writer.wl("#include \"classes/" + info.mangledName + ".h\"");
		TreeMap<String, SootClass> dependencies = new TreeMap<String, SootClass>();
		for(SootClass dependency: info.dependencies) {
			String name = Mangling.mangle(dependency);
			if(!dependencies.containsKey(name)) {
				dependencies.put(name, dependency);
			}
		}
		for(SootClass dependency: dependencies.values()) {
			writer.wl("#include \"classes/" + Mangling.mangle(dependency) + ".h\"");			
		}
		writer.wl("");
		
		// generate static fields and initialize them with constant values
		for(SootField field: info.clazz.getFields()) {
			if(field.isStatic()) {
				String cType = CTypes.toCType(field.getType());
				String constantValue = null;
				
				for(Tag tag: field.getTags()) {
					if(tag instanceof FloatConstantValueTag) constantValue = Mangling.mangleFloat(Float.toString(((FloatConstantValueTag)tag).getFloatValue()));
					if(tag instanceof DoubleConstantValueTag) constantValue = Mangling.mangleDouble(Double.toString(((DoubleConstantValueTag)tag).getDoubleValue()));
					if(tag instanceof IntegerConstantValueTag) constantValue = Integer.toString(((IntegerConstantValueTag)tag).getIntValue());
					if(tag instanceof LongConstantValueTag) constantValue = Long.toString(((LongConstantValueTag)tag).getLongValue());
					if(constantValue != null) break;
				}
				
				if(constantValue == null)
					writer.wl(cType + " " + info.mangledName + "::" + Mangling.mangle(field) + " = 0;");
				else
					writer.wl(cType + " " + info.mangledName + "::" + Mangling.mangle(field) + " = " + constantValue + ";");
			}
		}
		
		// generate the synthetic static fields for the java.lang.Class and clinit guard
		writer.wl("java_lang_Class* " + info.mangledName + "::clazz = 0;");
		writer.wl("bool " + info.mangledName + "::clinit = 0;");
		writer.wl("");
		
		// output string literal array and java.lang.String declarations.
		info.literals.generateDeclarations(writer);
		
		writer.wl("");
	}
}
