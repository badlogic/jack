package com.badlogic.jack.generators;

import java.util.Map;
import java.util.Set;

import soot.SootClass;

import com.badlogic.jack.build.FileDescriptor;
import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Generates code used by the runtime such as a combined include file
 * referencing all classes as well as the class initialization
 * code and reflection metadata. All this code is kept in the classes.h
 * and classes.cpp files.
 * @author mzechner
 *
 */
public class RuntimeGenerator {
	private final Set<SootClass> classes;
	private final Map<SootClass, ClassInfo> infos;
	private final String outputPath;
	
	public RuntimeGenerator(Set<SootClass> classes, Map<SootClass, ClassInfo> infos, String outputPath) {
		this.classes = classes;
		this.infos = infos;
		this.outputPath = outputPath;
	}
	
	public void generate() {
		// generate classes.h
		SourceWriter writer = new SourceWriter();
		writer.wl("#ifndef jack_all_classes");
		writer.wl("#define jack_all_classes");
		
		for(SootClass c: classes) {
			writer.wl("#include \"classes/" + Mangling.mangle(c) + ".h\"");
		}
		
		// add array.h for arrays
		writer.wl("#include \"vm/array.h\"");
		writer.wl("void jack_init();");
		writer.wl("#endif");
		new FileDescriptor(outputPath + "/classes.h").writeString(writer.toString(), false);
		
		// generate classes.cpp
		writer = new SourceWriter();
		writer.wl("#include \"classes/classes.h\"");
		writer.wl("");
		
		// generate signal handlers
		new SignalHandlerGenerator().generate(writer);
		
		// generate all the reflection info
		new ReflectionGenerator(writer, infos).generate();
		
		writer.wl("void jack_init() {");
		writer.push();
		writer.wl("jack_register_signal_handlers();"); // assumed to be written by SignalHandlerGenerator above
		writer.wl("jack_init_reflection();"); // assumed to be written by ReflectionGenerator above
		// call all m_clinit methods, this should cascade
		// start with Object so the base classes are all 
		// initialized
		writer.wl("java_lang_Object::m_clinit();");
		
		for(SootClass c: classes) {
			writer.wl(Mangling.mangle(c) + "::m_clinit();");
		}
		writer.pop();
		writer.wl("}");
		new FileDescriptor(outputPath + "/classes.cpp").writeString(writer.toString(), false);
	}
}
