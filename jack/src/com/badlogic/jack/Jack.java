package com.badlogic.jack;

import java.io.File;
import java.io.FilenameFilter;
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
import com.badlogic.jack.generators.RuntimeGenerator;
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
public class Jack {
	private final String[] classPath;
	private final String[] sourcePath;
	private final String outputPath;
	private Set<SootClass> classes;
	private final Map<SootClass, ClassInfo> classInfos = new HashMap<SootClass, ClassInfo>();
	private JavaSourceProvider sourceProvider;
	private Set<String> generatedFiles = new HashSet<String>();
	
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
	public Jack(String classPath, String sourcePath, String outputPath, boolean incremental) {
		this.classPath = parsePath(classPath);
		this.sourcePath = parsePath(sourcePath);
		this.outputPath = outputPath.endsWith("/")? outputPath: outputPath + "/";
	}
	
	private String[] parsePath(String path) {
		String[] paths = path.split(";");
		for(int i = 0; i < paths.length; i++) {
			paths[i] = paths[i].endsWith("/")? paths[i]: paths[i] + "/";
		}
		return paths;
	}
	
	private String concatenatePath(String[] paths) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < paths.length; i++) {
			buffer.append(paths[i]);
			if(i != paths.length -1) buffer.append(";");
		}
		return buffer.toString();
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
		Scene.v().setSootClassPath(concatenatePath(classPath));
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		
		Set<SootClass> classes = new HashSet<SootClass>();
		for(SootClass clazz: Scene.v().getClasses()) {		
			generatedFiles.add(Mangling.mangle(clazz) + ".h");
			generatedFiles.add(Mangling.mangle(clazz) + ".cpp");
			classes.add(clazz);
			ClassInfo info = new ClassInfo(clazz);
			classInfos.put(clazz, info);
		}
		generatedFiles.add("classes.h");
		generatedFiles.add("classes.cpp");
		return classes;
	}
	
	/**
	 * Generates .h/.cpp files for each class found in the classpath
	 */
	public void compile() {
		// load the classes and source files
		classes = loadClasses();
		sourceProvider = new JavaSourceProvider();
		for(String path: sourcePath) {
			sourceProvider.load(new FileDescriptor(path));
		}

		generateHeaders();
		generateImplementations();
		generateAuxiliary();
		
		// delete all files that aren't in the classpath
		for(String f: new File(outputPath).list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.endsWith(".h") || name.endsWith(".cpp");
			}
		})) {
			if(!generatedFiles.contains(f)) {
				System.out.println("deleting " + outputPath + f);
				new File(outputPath + f).delete();
			}
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
			info.gatherDependencies();
			ImplementationGenerator implGenerator = new ImplementationGenerator(clazz, sourceProvider, info, outputPath + info.mangledName + ".cpp");
			implGenerator.generate();
		}
	}
	
	/**
	 * Generates auxiliary file, such as the implementation
	 * of class initialization.
	 */
	private void generateAuxiliary() {		
		new RuntimeGenerator(classes, classInfos, outputPath).generate();
	}
	
	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Usage: Jack <classpath> <sources> <outputdir>");
			System.exit(0);
		}			
		
		String classpath = args[0].endsWith("/")? args[0]: args[0] + "/";
		String sources = args[1].endsWith("/")? args[1]: args[1] + "/";
		String outputDir = args[2].endsWith("/")? args[2]: args[2] + "/";
		
		Jack compiler = new Jack(classpath, sources, outputDir, false);
		compiler.compile();
	}
}
