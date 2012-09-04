package com.badlogic.jack.generators;

import soot.SootMethod;

import com.badlogic.jack.info.ClassInfo;
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
	private final ClassInfo info;
	private final SootMethod method;
	
	public MethodGenerator(SourceWriter writer, ClassInfo info, SootMethod method) {
		this.writer = new SourceWriter();
		this.method = method;
		this.info = info;
	}
	
	public void generate() {
		
	}
}
