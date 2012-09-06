package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// FIXME reflection
public final class Class<T> {
	private static Map<String, Class> classes = new HashMap<String, Class>();
	
	public static Class forName(String name) throws ClassNotFoundException {
		throw new UnsupportedOperationException();
	}
	
	String name;
	Class superClass;
	Class[] interfaces;
	boolean isPrimitive = false;
	boolean isInterface = false;
	boolean isArray = false;
	Class componentType;
	
	public String toString() {
		return getName();
	}

	public String getName() {
		throw new UnsupportedOperationException();
	}

	public String getCanonicalName() {
		throw new UnsupportedOperationException();
	}

	public String getSimpleName() {
		throw new UnsupportedOperationException();
	}

	public T newInstance() throws IllegalAccessException, InstantiationException {
		throw new UnsupportedOperationException();
	}

	public Class getComponentType() {
		return componentType;
	}

	public boolean isAssignableFrom(Class c) {
		throw new UnsupportedOperationException();
	}

	public Field getDeclaredField(String name) throws NoSuchFieldException {
		throw new UnsupportedOperationException();
	}

	public Field getField(String name) throws NoSuchFieldException {
		throw new UnsupportedOperationException();
	}

	public Method getDeclaredMethod(String name, Class... parameterTypes)
			throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Method getMethod(String name, Class... parameterTypes)
			throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Constructor getConstructor(Class... parameterTypes)
			throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Constructor getDeclaredConstructor(Class... parameterTypes)
			throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}
	
	public Constructor[] getDeclaredConstructors() {
		throw new UnsupportedOperationException();
	}

	public Constructor[] getConstructors() {
		throw new UnsupportedOperationException();
	}

	public Field[] getDeclaredFields() {
		throw new UnsupportedOperationException();
	}

	public Field[] getFields() {
		throw new UnsupportedOperationException();
	}

	public Field[] getAllFields() {
		throw new UnsupportedOperationException();
	}

	public Method[] getDeclaredMethods() {
		throw new UnsupportedOperationException();
	}

	public Method[] getMethods() {
		throw new UnsupportedOperationException();
	}

	public Class[] getInterfaces() {
		return interfaces;
	}

	public T[] getEnumConstants() {
		throw new UnsupportedOperationException();
	}

	public int getModifiers() {
		return 0;
	}

	public boolean isInterface() {
		throw new UnsupportedOperationException();
	}

	public Class getSuperclass() {
		return superClass;
	}

	public boolean isArray() {
		return isArray;
	}

	public boolean isInstance(Object o) {
		if(this == o.getClass()) return true;
		if(superClass != null && superClass != Object.class) {
			if(superClass.isInstance(o)) return true;
		}
		for(Class itf: interfaces) {
			if(itf.isInstance(o)) return true;
		}
		return false;
	}

	public boolean isPrimitive() {
		return isPrimitive;
	}
	
	public boolean desiredAssertionStatus() {
		throw new UnsupportedOperationException();
	}

	public <T> Class<? extends T> asSubclass(Class<T> c) {
		throw new UnsupportedOperationException();
	}

	public T cast(Object o) {
		return (T) o;
	}

	// FIXME reflection
//	public Package getPackage() {
//		throw new UnsupportedOperationException();
//	}	
}