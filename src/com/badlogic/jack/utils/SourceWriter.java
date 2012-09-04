package com.badlogic.jack.utils;

/**
 * Stores indentation levels that can be pushed/popped and
 * allows to output lines.
 * @author mzechner
 *
 */
public class SourceWriter {
	int indent;
	final StringBuffer buffer = new StringBuffer();
	
	public SourceWriter() {		
	}

	private String i() {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < indent; i++) {
			buffer.append("\t");
		}
		return buffer.toString();
	}
	
	public void wl(String message) {
		buffer.append(i());
		buffer.append(message);
		buffer.append("\n");
	}
	
	public void push() {
		indent++;
	}
	
	public void pop() {
		indent--;
		if(indent < 0) indent = 0;
	}
	
	public String toString() {
		return buffer.toString();
	}
}
