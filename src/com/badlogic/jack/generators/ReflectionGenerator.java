package com.badlogic.jack.generators;

import java.util.Map;

import soot.SootClass;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Generates reflection data for a class. Writes needed string literals and 
 * a method called jack_init_reflection to the SourceWriter.
 * @author mzechner
 *
 */
public class ReflectionGenerator {
	private final SourceWriter writer;
	private final Map<SootClass, ClassInfo> infos;
	private final LiteralsGenerator literals;
	
	public ReflectionGenerator(SourceWriter writer, Map<SootClass, ClassInfo> infos) {
		this.writer = writer;
		this.infos = infos;
		this.literals = new LiteralsGenerator("jack_reflection_");
	}
	
	public void generate() {
		// write out all classes first, so we can gather literals
		SourceWriter fWriter = new SourceWriter();
				
		fWriter.push();
		initializeClasses(fWriter);
		generatePrimitiveClasses(fWriter);				
		
		// fill out their reflection data for each class
		for(SootClass c: infos.keySet()) {
			generateClassReflectionData(fWriter, c);
		}
		
		// add all the classes to the ClassManager, see classmanager.h/.cpp
		addClassesToClassMap(fWriter);
		fWriter.pop();
		
		// output the jack_initialize_reflection method
		literals.generateDeclarations(writer);
		writer.wl("void jack_init_reflection() {");
		writer.push();
		literals.generateDefinitions(writer);
		writer.wl("");
		writer.pop();
		writer.wl(fWriter.toString());
		writer.wl("}");
	}

	/**
	 * Adds the references of each class' Class instance
	 * to the Class#classes map.
	 */
	private void addClassesToClassMap(SourceWriter writer) {
		for(SootClass c: infos.keySet()) {
			String var = Mangling.mangle(c) + "::clazz";
			writer.wl("java_lang_Class::m_addClass(" + var + ");");
		}
		writer.wl("java_lang_Class::m_addClass(java_lang_Byte::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Character::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Short::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Integer::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Long::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Float::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Double::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Boolean::f_TYPE);");
		writer.wl("java_lang_Class::m_addClass(java_lang_Void::f_TYPE);");
	}

	/**
	 * Creates the xxx::clazz instances. Those have to be created first
	 * as they are referenced later when generating their actual content.
	 */
	private void initializeClasses(SourceWriter writer) {
		// initialize the java_lang_Class* for each class/interface
		for(SootClass c: infos.keySet()) {
			String var = Mangling.mangle(c) + "::clazz";
			writer.wl(var + "= new java_lang_Class();");
			writer.wl(var + "->m_init();");
		}
		writer.wl("");
	}
	
	/**
	 * Initializes the primitive type classes. 
	 * @return
	 */
	private void generatePrimitiveClasses(SourceWriter writer) {
		generatePrimitiveClass(writer, "byte", "java_lang_Byte");
		generatePrimitiveClass(writer, "short", "java_lang_Short");
		generatePrimitiveClass(writer, "int", "java_lang_Integer");
		generatePrimitiveClass(writer, "long", "java_lang_Long");
		generatePrimitiveClass(writer, "float", "java_lang_Float");
		generatePrimitiveClass(writer, "double", "java_lang_Double");
		generatePrimitiveClass(writer, "char", "java_lang_Character");
		generatePrimitiveClass(writer, "boolean", "java_lang_Boolean");
		generatePrimitiveClass(writer, "void", "java_lang_Void");
	}
	
	private void generatePrimitiveClass(SourceWriter writer, String name, String clazz) {
		writer.wl(clazz + "::f_TYPE = new java_lang_Class();");
		writer.wl(clazz + "::f_TYPE->m_init();");
		writer.wl(clazz + "::f_TYPE->f_name = " + literals.addLiteral(name) + ";");
		writer.wl(clazz + "::f_TYPE->f_isPrimitive = true;");
	}
	
	private void generateClassReflectionData(SourceWriter writer, SootClass c) {
		String var = Mangling.mangle(c) + "::clazz";
		writer.wl(var + "->m_init();");
		// FIXME reflection
		writer.wl(var + "->f_name = " + literals.addLiteral(c.getName()) + ";");
		writer.wl(var + "->f_isInterface = " + c.isInterface() + ";");
		if(c.hasSuperclass()) {
			writer.wl(var + "->f_superClass = " + Mangling.mangle(c.getSuperclass()) + "::clazz;");
		}
		
		// create array for interfaces
		writer.wl(var + "->f_interfaces = new Array<java_lang_Class*>(" + c.getInterfaceCount() + ", false, 1, &java_lang_Class::clazz);");
		int i = 0;
		for(SootClass itf: c.getInterfaces()) {
			writer.wl("(*" + var + "->f_interfaces)[" + i + "] = " + Mangling.mangle(itf) + "::clazz;");
			i++;
		}
		writer.wl("");
	}
}
