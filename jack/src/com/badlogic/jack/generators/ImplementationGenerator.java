package com.badlogic.jack.generators;

import com.badlogic.jack.build.FileDescriptor;
import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.info.MethodInfo;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.SourceWriter;

import soot.SootClass;
import soot.SootMethod;

/**
 * Takes a {@link SootClass} instance and a corresponding {@link ClassInfo}
 * instance and outputs a C++ implementation file
 * @author mzechner
 *
 */
public class ImplementationGenerator {
	final SootClass clazz;
	final JavaSourceProvider sourceProvider;
	final ClassInfo info;
	final String fileName;
	
	public ImplementationGenerator(SootClass clazz, JavaSourceProvider sourceProvider, ClassInfo info, String fileName) {
		this.clazz = clazz;
		this.sourceProvider = sourceProvider;
		this.info = info;
		this.fileName = fileName;
	}
	
	public void generate() {
		System.out.println("Generating implementation for " + clazz.getName());
		SourceWriter methodWriter = new SourceWriter();
		// first we generate the methods themselves, as we collect
		// data while emitting the statements, e.g. string literals,
		// labels and so on.
		SootMethod clinitMethod = null;
		for(SootMethod method: clazz.getMethods()) {
			MethodInfo methodInfo = info.methodInfos.get(method);
			// skip any method for which there is no info
			if(methodInfo == null) continue;
			
			// if this is the <clinit> method, we simulate writting that
			// method to collect literals. The actual implementation is
			// generated later, including literal initialization.			
			if(method.getName().equals("<clinit>")) {
				clinitMethod = method;
				new MethodGenerator(new SourceWriter(), sourceProvider, info, method).generate();				
			} else {
				new MethodGenerator(methodWriter, sourceProvider, info, method).generate();
			}
			methodWriter.wl("");
		}
		
		// generate the clinit implementation
		new ClinitGenerator(methodWriter, sourceProvider, info, clinitMethod).generate();
		
		// generate the statics and include section of the .cpp file
		// note that we do this after generating the methods themselves
		// as ClassInfo will have additional data for us, e.g. string literals
		SourceWriter writer = new SourceWriter();
		new StaticsGenerator(writer, info).generate();
		
		// add the methods after the includes and statics
		// and write it to the file
		writer.wl(methodWriter.toString());
		if(needsUpdate(fileName, writer.toString())) {
			new FileDescriptor(fileName).writeString(writer.toString(), false);
		} else {
			System.out.println("Skipped '" + fileName + "', up-to-date");
		}
	}
	
	private boolean needsUpdate(String fileName, String newContent) {
		return !new FileDescriptor(fileName).readString().equals(newContent);
	}
}
