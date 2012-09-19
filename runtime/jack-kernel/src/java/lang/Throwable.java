package java.lang;

import java.io.PrintStream;
import java.io.PrintWriter;

public class Throwable {
	private String message;
	private Throwable cause;
	private StackTraceElement[] stackTrace;
	
	public Throwable() {
	}
	
	public Throwable(String message) {
		this(message, null);
	}
	
	public Throwable(String message, Throwable cause) {
		this.message = message;
		this.cause = cause;
	}
	
	public Throwable(Throwable cause) {
		this(null, cause);
	}
	
	public Throwable fillInStackTrace() {
		// FIXME exceptions
		return this;
	}
	
	public Throwable getCause() {
		return cause;
	}
	
	public String getLocalizedMessage() {
		// FIXME exceptions
		return getMessage();
	}
	
	public String getMessage() {
		return message;
	}
	
	public StackTraceElement[] getStackTraces() {
		// FIXME exceptions
		return stackTrace;
	}
	
	public void printStackTrace() {
		// FIXME exceptions
	}
	
	public void printStackTrace(PrintStream s) {
		// FIXME exceptions
	}
	
	public void printStackTrace(PrintWriter s) {
		// FIXME exceptions
	}
	
	public void setStackTrace(StackTraceElement[] stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	public String toString() {
		// FIXME exceptions
		return super.toString();
	}
}
