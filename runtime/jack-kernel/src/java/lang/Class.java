package java.lang;

import java.lang.annotation.DirectNative;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// FIXME reflection
@DirectNative
public final class Class<T> {
	
	public native static Class forName(String name) throws ClassNotFoundException;
	
	/**
	 * Custom method for creating array classes. Called in Array#m_getClass(), see
	 * vm/array.h.
	 * @param dimensions number of dimensions of the array
	 * @param elementType the element type of the array
	 * @return
	 */
	private native static Class forArray(int dimensions, Class elementType);
	
	/**
	 * Adds a class, used after all class instances are generated, to
	 * register them with the ClassManager, see classmanager.h/.cpp and
	 * ReflectionGenerator.
	 * @param clazz
	 */
	private native static void addClass(Class clazz);
	
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
		return name;
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
	
	/**
	 * Very silly, slow implementation of instanceof. Used for the instanceof 
	 * operator as well, see StatementGenerator in Jack. Follows the specs
	 * at <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.instanceof">http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.instanceof</a>
	 * @param o
	 * @return
	 */
	public boolean isInstance(Object o) {
		if(o == null) return false;
		return isInstance(o.getClass());
	}
	
	private boolean isInstance(Class s) {		
		// if this is a primitive class, bail out, can't compare to primitive classes.
		if(isPrimitive) return false;
				
		Class t = this;
		
		// if S is an ordinary (nonarray) class
		if(!s.isArray && !s.isInterface) {
			// if T is a class type, S must be T or a subclass of T
			if(!t.isArray && !t.isInterface) {
				// fast path, s equals t
				if(s == t) return true;
				// otherwise check superclasses
				Class superClass = s.getSuperclass();
				while(superClass != null) {
					if(superClass == t) return true;
					superClass = superClass.getSuperclass();
				}
				return false;
			}			
			// if T is an interface, S must implement T
			else if(t.isInterface) {
				return implementsInterface(s, t);
			}
			// if T is an array type, we return false as S is not an array class
			else {
				return false;
			}
		}
		// if S is an interface type
		else if(s.isInterface) {
			// if T is a class type, then T must be Object
			if(!t.isArray && !t.isInterface) {
				return t == java.lang.Object.class;
			}
			// if T is an interface type, then T must be the same as S or a superinterface of S
			else if (t.isInterface) {
				return implementsInterface(s, t);
			} 
			// if T is an array, we return false as S is not an array class
			else {
				return false;
			}
		} 
		// If S is an array type
		else {
			// if T is a class type, then T must be Object
			if(!t.isArray && !t.isInterface) {
				return t == java.lang.Object.class;
			}
			// if T is an interface type, then T must be Cloneable or Serializable
			else if(t.isInterface) {
				return t == java.lang.Cloneable.class || t == java.io.Serializable.class;
			}
			// if T is an array type
			else {
				Class sc = s.componentType;
				Class tc = t.componentType;
				
				// if only one of sc or tc are primitive types, return false
				if((sc.isPrimitive && !tc.isPrimitive) || (!sc.isPrimitive && tc.isPrimitive))  return false;  
				
				// if SC and TC are primitive types, they must be the same
				if(sc.isPrimitive && tc.isPrimitive) {
					return sc == tc;
				}
				// else, SC and TC must be reference types and SC must be an instanceof TC 
				else {
					return tc.isInstance(sc);
				}
			}
		}		
	}
	
	/**
	 * Checks whether Class c or any of it's subclasses/interfaces implements interface i 
	 * @param c
	 * @param itf
	 * @return
	 */
	private boolean implementsInterface(Class c, Class itf) {
		// check if c is the interface
		if(c == itf) return true;
		
		// check the interfaces directly implemented by c.
		for(Class oitf: c.interfaces) {
			implementsInterface(oitf, itf);			
		}
		
		// check if the superclass implements itc
		if(c.superClass != null) {
			implementsInterface(c.superClass, itf);
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