package com.badlogic.jack.generators;

import com.badlogic.jack.utils.SourceWriter;

/**
 * Creates a function jack_register_signal_handlers that registers signal 
 * handlers that throw exceptions for SIGSEG. 
 * @author mzechner
 *
 */
public class SignalHandlerGenerator {
	public void generate(SourceWriter writer) {
		writer.wl("#include <signal.h>");
		writer.wl("#include \"classes/java_lang_NullPointerException.h\"");
		writer.wl("void signal_handler(int code) { signal(SIGSEGV, signal_handler); throw new java_lang_NullPointerException(); }\n");
		writer.wl("void jack_register_signal_handlers() { signal(SIGSEGV, signal_handler); }");		
	}
}
