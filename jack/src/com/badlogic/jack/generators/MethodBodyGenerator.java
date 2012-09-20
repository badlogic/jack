package com.badlogic.jack.generators;

import soot.Local;
import soot.NullType;
import soot.SootMethod;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.info.MethodInfo;
import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.SourceWriter;

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
	private final JavaSourceProvider sourceProvider;
	private final ClassInfo info;
	private final SootMethod method;
	
	public MethodBodyGenerator(SourceWriter writer, JavaSourceProvider sourceProvider, ClassInfo info, SootMethod method) {
		this.writer = writer;
		this.sourceProvider = sourceProvider;
		this.info = info;
		this.method = method;
	}
	
	public void generate() {
		declareLocals();
		new StatementGenerator(writer, sourceProvider, info, method).generate();
	}

	/**
	 * Declares all local variables plus a variable called _exception that 
	 * holds a pointer to any exception thrown.
	 */
	private void declareLocals() {
		// declare locals
		for(Local local: method.retrieveActiveBody().getLocals()) {
			String cType = null;
			// null types need special treatment, we don't output them
			// we also won't generate statements that use a nullType
			// as a target.
			if(local.getType() instanceof NullType) {
				cType = "java_lang_Object*";
			} else {
				cType = CTypes.toCType(local.getType());
			}
			writer.wl(cType + " " + local.getName() + " = 0;");
		}
		writer.wl("java_lang_Object* _exception = 0;");
	}
}
