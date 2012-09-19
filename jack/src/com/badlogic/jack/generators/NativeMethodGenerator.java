package com.badlogic.jack.generators;

import java.util.List;

import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.tagkit.Tag;

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
	
	private boolean isDirect(List<Tag> tags) {
		for(Tag tag: tags) {
			if(tag.toString().contains("DirectNative")) return true;
		}
		return false;
	}
	
	public void generate() {
		// if this method or it's class is tagged with
		// @DirectNative, omit generation of the native
		// method wrapper.
		if(isDirect(method.getTags()) || isDirect(method.getDeclaringClass().getTags())) return;
		
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
		info.dependencies.add(Scene.v().getSootClass("java.lang.UnsupportedOperationException"));
		writer.push();
		writer.wl("throw new java_lang_UnsupportedOperationException();");
		writer.pop();
		writer.wl("}");
	}
}
