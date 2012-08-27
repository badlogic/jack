package java.lang;

import java.lang.reflect.Method;

public class Class<T> {
	public static Class forCanonicalName(String name) {
		// FIXME reflection
		return null;
	}

	public String getName() {
		// FIXME reflection
		return null;
	}

	public Class<?> getComponentType() {
		// FIXME reflection
		return null;
	}

	public boolean isInstance(Object o) {
		// FIXME reflection
		return false;
	}
	
	public Class getSuperclass() {
		// FIXME reflection
		return null;
	}
	
	public Method getMethod(String methodSig) {
		// FIXME reflection
		return null;
	}

	public T[] getEnumConstants() {
		// FIXME reflection
		return null;
	}
}
