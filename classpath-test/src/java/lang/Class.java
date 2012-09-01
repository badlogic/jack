/* Copyright (c) 2008-2011, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// FIXME reflection
public final class Class<T> {
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

	public T newInstance() throws IllegalAccessException,
			InstantiationException {
		throw new UnsupportedOperationException();
	}

	public static Class forName(String name) throws ClassNotFoundException {
		throw new UnsupportedOperationException();
	}

	public static Class forCanonicalName(String name) {
		throw new UnsupportedOperationException();
	}

	public Class getComponentType() {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	public boolean isArray() {
		throw new UnsupportedOperationException();
	}

	public boolean isInstance(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean isPrimitive() {
		throw new UnsupportedOperationException();
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