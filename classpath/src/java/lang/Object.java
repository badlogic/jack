package java.lang;

import java.lang.annotation.DirectNative;

// FIXME vm
@DirectNative
public class Object {
	protected Object clone() {
		return null;
	}
	
	public boolean equals(Object obj) {
		return false;
	}
	
	protected void finalize() {
	}
	
	public Class getClass() {
		return null;
	}
	
	public native int hashCode();
	
	public void notify() {
	}
	
	public void notifyAll() {
	}

	public String toString() {
		return null;
	}
	
	public void wait() {
	}
	
	public void wait(long timeout) {
	}
	
	public void wait(long timeout, int nanos) {
	}
}