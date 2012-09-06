package com.badlogic.jack.generators;

import java.util.Map;

import soot.SootClass;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Generates reflection data for a class.
 * @author mzechner
 *
 */
public class ReflectionGenerator {
	private final SourceWriter writer;
	private final Map<SootClass, ClassInfo> infos;
	private int nextLiteralId = 0;
	
	public ReflectionGenerator(SourceWriter writer, Map<SootClass, ClassInfo> infos) {
		this.writer = writer;
		this.infos = infos;
	}
	
	public void generate() {
		initializeClasses();
		generatePrimitiveClasses();
		
		// fill out their reflection data for each class
		for(SootClass c: infos.keySet()) {
			generateClassReflectionData(c);
		}
		writer.wl("");
		
		addClassesToClassMap();
	}

	/**
	 * Adds the references of each class' Class instance
	 * to the Class#classes map.
	 */
	private void addClassesToClassMap() {
		
	}
	
	private String generateLiteral(String value) {
		return null;
	}

	/**
	 * Creates the xxx::clazz instances. Those have to be created first
	 * as they are referenced later when generating their actual content.
	 */
	private void initializeClasses() {
		// initialize the java_lang_Class* for each class/interface
		for(SootClass c: infos.keySet()) {
			String var = Mangling.mangle(c) + "::clazz";
			writer.wl(var + "= new java_lang_Class();");
		}
		writer.wl("");
	}
	
	/**
	 * Initializes the primitive type classes. 
	 * @return
	 */
	private void generatePrimitiveClasses() {
		writer.wl("error, need to create primitive classes");
	}
	
	private void generateClassReflectionData(SootClass c) {
		String var = Mangling.mangle(c) + "::clazz";
		writer.wl(var + "->m_init();");
		// FIXME reflection
//		writer.wl(var + "->f_isPrimitive = " + )
//		writer.wl(var + "->f_isArray = " + );
		if(c.hasSuperclass()) {
			writer.wl(var + "->f_superClass = " + Mangling.mangle(c.getSuperclass()) + "::clazz;");
		}
		
		// create array for interfaces
		writer.wl(var + "->f_interfaces = new Array<java_lang_Class*>(" + c.getInterfaceCount() + ", false);");
		int i = 0;
		for(SootClass itf: c.getInterfaces()) {
			writer.wl("(*" + var + "->f_interfaces)[" + i + "] = " + Mangling.mangle(itf) + "::clazz;");
			i++;
		}
		writer.wl("");
	}
}
