package com.badlogic.jack.generators;

import soot.SootMethod;
import soot.Type;
import soot.VoidType;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Generates the body of a native method.
 * @author mzechner
 *
 */
public class NativeMethodGenerator {
	private final SourceWriter writer;
	private final ClassInfo info;
	private final SootMethod method;
	
	public NativeMethodGenerator(SourceWriter writer, ClassInfo info, SootMethod method) {
		this.writer = writer;
		this.info = info;
		this.method = method;
	}
	
	public void generate() {
		// output the signature
		String methodSig = "";
		methodSig += CTypes.toCType(method.getReturnType());
		methodSig += " " + info.mangledName + "::" + Mangling.mangle(method) + "(";
		
		int i = 0;
		for(Object paramType: method.getParameterTypes()) {
			if(i > 0) methodSig += ", ";
			methodSig += CTypes.toCType((Type)paramType);
			methodSig += " param" + i;
			i++;
		}
		
		methodSig +=") {";
		writer.wl(methodSig);
		
		// FIXME JNI, add function pointer loading and invocation
		// output the body 
		writer.push();
		if(method.getReturnType() instanceof VoidType) {
			writer.wl("return;");
		} else {
			writer.wl("return 0;");
		}
		writer.pop();
		writer.wl("}");
	}
}
