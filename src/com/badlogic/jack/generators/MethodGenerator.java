package com.badlogic.jack.generators;

import soot.SootMethod;
import soot.Type;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Takes a {@link SootMethod} and corresponding {@link ClassInfo}
 * and generates the C++ code for the statements in the method. Also
 * gathers literals and stores them in MethodInfo.
 * @author mzechner
 *
 */
public class MethodGenerator {
	private final SourceWriter writer;
	private final JavaSourceProvider sourceProvider;
	private final ClassInfo info;
	private final SootMethod method;
	
	public MethodGenerator(SourceWriter writer, JavaSourceProvider sourceProvider, ClassInfo info, SootMethod method) {
		this.writer = writer;
		this.sourceProvider = sourceProvider;
		this.method = method;
		this.info = info;
	}
	
	public void generate() {
		if(!method.isConcrete()) {
			if(method.isNative()) {
				new NativeMethodGenerator(writer, info, method);
			}
		} else {
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
			writer.push();
			new MethodBodyGenerator(writer, sourceProvider, info, method).generate();
			writer.pop();
			writer.wl("}");
		}
	}
}
