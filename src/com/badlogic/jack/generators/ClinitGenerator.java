package com.badlogic.jack.generators;

import java.util.Map;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

import soot.SootClass;
import soot.SootMethod;

public class ClinitGenerator {
	private final SourceWriter writer;
	private final JavaSourceProvider sourceProvider;
	private final ClassInfo info;
	private final SootMethod clinitMethod;
	
	public ClinitGenerator(SourceWriter writer, JavaSourceProvider sourceProvider, ClassInfo info, SootMethod clinitMethod) {
		this.writer = writer;
		this.sourceProvider = sourceProvider;
		this.info = info;
		this.clinitMethod = clinitMethod;
	}
	
	public void generate() {
		writer.wl("void " + Mangling.mangle(info.clazz) + "::m_clinit() {");
		writer.push();
		writer.wl("// would enter class monitor for this class' clinit method");
		writer.wl("{");
		writer.push();
		
		// check if clinit was already called and bail out in that case
		writer.wl("if(" + Mangling.mangle(info.clazz) + "::clinit) return;");
		
		// set the clinit flag of this class as a guard
		writer.wl(Mangling.mangle(info.clazz) + "::clinit = true;");
		
		// generate java.lang.String instances for string literals
		Map<String, String> literals = info.literals;
		for(String literal: literals.keySet()) {
			String id = literals.get(literal);
			writer.wl(id + " = new java_lang_String();");
			writer.wl(id + "->m_init(new Array<j_char>(" + id + "_array, " + literal.length() + ", true));");
		}
		
		// emit calls to all classes and interfaces' clinit this class references
		for(SootClass dependency: info.dependencies) {
			writer.wl(Mangling.mangle(dependency) + "::m_clinit();");
		}		
		
		// generate the method body
		if(clinitMethod != null) {
			new MethodBodyGenerator(writer, sourceProvider, info, clinitMethod).generate();
		}
		writer.pop();
		writer.wl("}");
		writer.pop();
		writer.wl("}");
	}	
}
