package com.badlogic.jack.generators;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.info.MethodInfo;
import com.badlogic.jack.utils.SourceWriter;

import soot.SootMethod;

/**
 * Generates the C++ translation for a {@link SootMethod} and fills
 * in additional information such as literals in a {@link ClassInfo}
 * and {@link MethodInfo} instance. Assumes that the signature of
 * the method was already emitted, e.g. by {@link ClinitGenerator}
 * or {@link MethodGenerator}
 * @author mzechner
 *
 */
public class MethodBodyGenerator {
	private final SourceWriter writer;
	private final ClassInfo info;
	private final SootMethod method;
	
	public MethodBodyGenerator(SourceWriter writer, ClassInfo info, SootMethod method) {
		this.writer = writer;
		this.info = info;
		this.method = method;
	}
	
	public void generate() {
		
	}
}
