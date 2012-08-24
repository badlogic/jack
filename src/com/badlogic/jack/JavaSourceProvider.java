package com.badlogic.jack;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.jack.build.FileDescriptor;

/**
 * Reads all .java files in a directory and it's subdirectories,
 * splits them up into lines and allows a client to retrieve
 * individual lines
 * @author mzechner
 *
 */
public class JavaSourceProvider {
	Map<String, String[]> files = new HashMap<String, String[]>();
	
	/**
	 * Loads lines of each .java file in the given directory. The 
	 * directory i assumed to contain the root package of the classes
	 * to be loaded.
	 * 
	 * @param directory to be loaded
	 */
	public void load(FileDescriptor dir) {
		loadDir(dir, dir.path() + "/");
	}
	
	public void loadDir(FileDescriptor dir, String baseDir) {
		FileDescriptor[] list = dir.list();
		for(FileDescriptor file: list) {
			if(file.isDirectory()) {
				loadDir(file, baseDir);
			} else {
				if(file.extension().equals("java")) {
					loadFile(file, baseDir);
				}
			}
		}
	}
	
	private void loadFile(FileDescriptor file, String baseDir) {
		String fullName = getFullClassName(file, baseDir);
		String[] lines = file.readString().split("\n");
		files.put(fullName, lines);
	}
	
	private String getFullClassName(FileDescriptor file, String baseDir) {
		String name = file.path().replace(baseDir, "").replace('/', '.').replace('\\', '.').replace(".java", "");
		return name;
	}

	/**
	 * Returns a line for a class previously loaded via
	 * {@link #load(FileDescriptor)}.
	 * @param fullClassName the fully qualified name of the class, e.g. java.lang.Object
	 * @param line the line number, starting at 0
	 * @return the line or null
	 */
	public String getLine(String fullClassName, int line) {
		String[] lines = files.get(fullClassName);
		if(lines == null) return null;
		else return lines[line];
	}
	
	public static void main(String[] args) {
		new JavaSourceProvider().load(new FileDescriptor("classpath/src"));
	}
}
