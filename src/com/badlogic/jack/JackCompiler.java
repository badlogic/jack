package com.badlogic.jack;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import com.badlogic.jack.build.FileDescriptor;
import com.badlogic.jack.generators.HeaderGenerator;
import com.badlogic.jack.generators.ImplementationGenerator;
import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.Mangling;

/**
 * Takes a directory containing .class files and generates C++ header and implementation
 * files. Additionally adds Java source as comments to the C++ header to more easily
 * understand the generated code.
 * 
 * @author mzechner
 *
 */
public class JackCompiler {
	private final String classPath;
	private final String sourcePath;
	private final String outputPath;
	private final boolean incremental;
	private Set<SootClass> classes;
	private final Map<SootClass, ClassInfo> classInfos = new HashMap<SootClass, ClassInfo>();
	private JavaSourceProvider sourceProvider;
	
	/**
	 * Creates a new compiler, setting the classpath directory,
	 * the source path and the output path. Loads all classes
	 * based on the input parameters. The incremental flag
	 * defines whether classes should be translated incrementally, that
	 * is only if they class file is newer than the last .h/.cpp file
	 * generated for them. This improves compilation times for the C++
	 * code.
	 * 
	 * @param classPath the directory containing the .class files
	 * @param sourcePath the directory containing the Java source files
	 * @param outputPath the output directory
	 * @param incremental whether to incrementally translate files
	 */
	public JackCompiler(String classPath, String sourcePath, String outputPath, boolean incremental) {
		this.classPath = classPath.endsWith("/")? classPath: classPath + "/";
		this.sourcePath = sourcePath.endsWith("/")? sourcePath: sourcePath + "/";
		this.outputPath = outputPath.endsWith("/")? outputPath: outputPath + "/";
		this.incremental = incremental;
		
		// FIXME for testing only
		new FileDescriptor(outputPath).deleteDirectory();
		new FileDescriptor(outputPath).mkdirs();
	}
	
	/**
	 * Sets up Soot and loads the classes from the classpath directory. If
	 * incremental builds are enabled, only classes who's classfile is newer
	 * than the last generated .h/.cpp file will be translated.
	 *  
	 * @return the loaded {@link SootClass} instances
	 */
	private Set<SootClass> loadClasses() {		
		Options.v().set_keep_line_number(true);
		Options.v().set_process_dir(Arrays.asList(classPath));
		Scene.v().setSootClassPath(classPath);
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		
		Set<SootClass> classes = new HashSet<SootClass>();
		for(SootClass clazz: Scene.v().getClasses()) {			
			if(!incremental) {
				classes.add(clazz);
			} else {
				String classFile = classPath + clazz.getName().replace(".", "/") + ".class";
				String headerFile = outputPath + Mangling.mangle(clazz) + ".h";
				if(new File(classFile).lastModified() > new File(headerFile).lastModified()) {
					classes.add(clazz);
				}
			}			
		}
		return classes;
	}
	
	/**
	 * Generates .h/.cpp files for each class found in the classpath
	 */
	public void compile() {
		// load the classes and source files
		classes = loadClasses();
		sourceProvider = new JavaSourceProvider();
		sourceProvider.load(new FileDescriptor(sourcePath));

		generateClassInfo();
		generateHeaders();
		generateImplementations();
		generateAuxiliary();
	}

	/**
	 * Generates {@link ClassInfo} instances for each class. These
	 * contain additional information needed for the translation
	 * process.
	 */
	private void generateClassInfo() {
		for(SootClass clazz: classes) {
			classInfos.put(clazz, new ClassInfo(clazz));
		}
	}
	
	/**
	 * Generates the header file for each class
	 */
	private void generateHeaders() {
		for(SootClass clazz: classes) {
			ClassInfo info = classInfos.get(clazz);
			HeaderGenerator headerGenerator = new HeaderGenerator(clazz, info, outputPath + info.mangledName + ".h");
			headerGenerator.generate();
		}
	}
	
	/**
	 * Generates the implementation file for each class
	 */
	private void generateImplementations() {
		for(SootClass clazz: classes) {
			ClassInfo info = classInfos.get(clazz);
			ImplementationGenerator implGenerator = new ImplementationGenerator(clazz, info, outputPath + info.mangledName + ".cpp");
			implGenerator.generate();
		}
	}
	
	/**
	 * Generates auxiliary file, such as the implementation
	 * of class initialization.
	 */
	private void generateAuxiliary() {
	}
	
	public static void main(String[] args) {
		JackCompiler compiler = new JackCompiler("classpath/bin", "classpath/src", "native/classes", false);
		compiler.compile();
	}
}
