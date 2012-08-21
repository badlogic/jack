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
		// FIXME
		return this;
	}
	
	public Throwable getCause() {
		return cause;
	}
	
	public String getLocalizedMessage() {
		// FIXME
		return getMessage();
	}
	
	public String getMessage() {
		return message;
	}
	
	public StackTraceElement[] getStackTraces() {
		// FIXME
		return stackTrace;
	}
	
	public void printStackTrace() {
		// FIXME
	}
	
	public void printStackTrace(PrintStream s) {
		// FIXME
	}
	
	public void printStackTrace(PrintWriter s) {
		// FIXME
	}
	
	public void setStackTrace(StackTraceElement[] stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	public String toString() {
		// FIXME
		return super.toString();
	}
}
