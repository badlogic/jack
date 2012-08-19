package com.badlogic.jack;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import com.badlogic.jack.build.FileDescriptor;

public class Compiler {
	static int ident;
	
	public static void main(String[] args) {
		
		Scene.v().setSootClassPath("classpath/bin/;");
		Scene.v().loadClassAndSupport("jack.Main");
		
		new FileDescriptor("native/classes/").deleteDirectory();
		new FileDescriptor("native/classes/").mkdirs();
		
		StringBuffer buffer = new StringBuffer();
		wl(buffer, "#ifndef jack_all_classes");
		wl(buffer, "#define jack_all_classes");
		
		for(SootClass c: Scene.v().getClasses()) {
			// skip interfaces
			if(c.isInterface()) {
				System.out.println("skipping interface " + c.getName());
				continue;
			}
			
			System.out.println("procesing " + c.getName());
			String header = generateHeader(c);
			String cFile = generateCFile(c);
			new FileDescriptor("native/classes/" + nor(c.getName()) + ".h").writeString(header, false);
			new FileDescriptor("native/classeS/" + nor(c.getName()) + ".cpp").writeString(cFile, false);
		
			wl(buffer, "#include \"classes/" + nor(c.getName()) + ".h\"");
		}
		
		wl(buffer, "#endif");
		new FileDescriptor("native/classes/classes.h").writeString(buffer.toString(), false);
	}
	
	public static String generateHeader(SootClass clazz) {
		StringBuffer buffer = new StringBuffer();
		String fullName = nor(clazz.getName());
		String clazzName = nor(clazz.getShortName());
		String packageName = nor(clazz.getPackageName());
		
		// include guards
		wl(buffer, "#ifndef " + fullName + "_h");
		wl(buffer, "#define " + fullName + "_h");
		wl(buffer, "");
		
		// include common headers
		wl(buffer, "#include \"vm/types.h\"");

		// include super class header
		if(clazz.hasSuperclass()) {
			wl(buffer, "#include \"classes/" + nor(clazz.getSuperclass().getName()) + ".h\"");
		}
		wl(buffer, "");
		
		// include forward declarations of types used
		// as method params and fields
		outputForwardDeclarations(buffer, clazz);
		
		// struct
		wl(buffer, "struct " + fullName + " {");
		push();
		
		// add super class if any
		if(clazz.hasSuperclass()) {
			wl(buffer, nor(clazz.getSuperclass().getName()) + " super;");
		}
		
		// add fields
		for(SootField field: clazz.getFields()) {
			generateField(buffer, field);
		}
		
		pop();
		wl(buffer, "};");
		
		wl(buffer, "");
		wl(buffer, "#endif");
		return buffer.toString();
	}
	
	private static void outputForwardDeclarations(StringBuffer buffer, SootClass clazz) {
		// output field forward decls.
		for(SootField field: clazz.getFields()) {
			if(field.getType() instanceof RefType) {
				wl(buffer, "struct " + nor(field.getType().toString()) + ";");
			}
		}
		
		// go through each method signature and
		// output forward decls. for return types and parameters
		for(SootMethod method: clazz.getMethods()) {
			method.retrieveActiveBody();
			for(Object type: method.getParameterTypes()) {
				if(type instanceof RefType) {
					wl(buffer, "struct " + nor(type.toString()) + ";");
				}
			}
			if(method.getReturnType() instanceof RefType) {
				wl(buffer, "struct " + nor(method.getReturnType().toString()) + ";");
			}
		}
		wl(buffer, "");
	}
	
	private static void generateField(StringBuffer buffer, SootField field) {
		String fieldName = nor(field.getName());
		
		// determine type and convert to C type
		if(field.getType() instanceof RefType) {
			wl(buffer, nor(field.getType().toString()) + "* " + nor(field.getName()) + ";");			
		} else {
			String fieldString = "";
			if(field.getType() instanceof BooleanType) fieldString += "j_bool";
			else if(field.getType() instanceof ByteType) fieldString += "j_byte";
			else if(field.getType() instanceof CharType) fieldString += "j_char";
			else if(field.getType() instanceof ShortType) fieldString += "j_short";
			else if(field.getType() instanceof IntType) fieldString += "j_int";
			else if(field.getType() instanceof LongType) fieldString += "j_long";
			else if(field.getType() instanceof FloatType) fieldString += "j_float";
			else if(field.getType() instanceof DoubleType) fieldString += "j_double";
			else throw new RuntimeException("Unknown primitive type " + field.getType());
			wl(buffer, fieldString + " " + nor(field.getName()) + ";");
		}
	}
	
	public static String generateCFile(SootClass clazz) {
		StringBuffer buffer = new StringBuffer();
		
		return buffer.toString();
	}
	
	private static String nor(String name) {
		return name.replace('.', '_');
	}
	
	private static void wl(StringBuffer buffer, String message) {
		for(int i = 0; i < ident; i++) {
			buffer.append("\t");
		}
		buffer.append(message);
		buffer.append("\n");
	}
	
	private static void push() {
		ident++;
	}
	
	private static void pop() {
		ident--;
		if(ident < 0) ident = 0;
	}
	
	private static void w(StringBuffer buffer, String message) {
		buffer.append(message);
	}
}
