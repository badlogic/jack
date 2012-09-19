package com.badlogic.jack.utils;

import soot.ArrayType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;

/**
 * Provides static methods to mangle Java class, method and field names to
 * C++ names.
 * @author mzechner
 *
 */
public class Mangling {
	public static final String FIELD_PREFIX = "f_";
	public static final String METHOD_PREFIX = "m_";
	
	/**
	 * Mangles a field name, prepending {@link #FIELD_PREFIX} 
	 * @param field the {@link SootField}
	 * @return the mangled name, prefixed with {@link #FIELD_PREFIX}
	 */
	public static String mangle(SootField field) {
		return FIELD_PREFIX + field.getName().trim();
	}
	
	/**
	 * Mangles a method name. <code>&lt;clinit></code> and <code>&lt;init></code>
	 * are mangled to <code>clinit</code> and <code>init</code> plus the {@link #METHOD_PREFIX}
	 * @param method the {@link SootMethod}
	 * @return the mangled name, prefixed with {@link #METHOD_PREFIX}
	 */
	public static String mangle(SootMethod method) {
		return METHOD_PREFIX + method.getName().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	/**
	 * Mangles a method name. <code>&lt;clinit></code> and <code>&lt;init></code>
	 * are mangled to <code>clinit</code> and <code>init</code> plus the {@link #METHOD_PREFIX}
	 * @param method the {@link SootMethodRef}
	 * @return the mangled name, prefixed with {@link #METHOD_PREFIX}
	 */
	public static String mangle(SootMethodRef methodRef) {
		return "m_" + methodRef.name().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	/**
	 * Mangles a class name. 
	 * @param method the {@link SootClass}
	 * @return the mangled name
	 */	
	public static String mangle(SootClass clazz) {
		return clazz.getName().replace('.', '_').trim();
	}
	
	/**
	 * Mangles a {@link Type} name. This includes class names, array names
	 * and primitive type names. 
	 * @param type the Type to mangle
	 * @return the mangled type name
	 */
	public static String mangle(Type type) {		
		if(type instanceof RefType) {
			return type.toString().replace('.', '_');
		}
		if(type instanceof ArrayType) {			
			return ((ArrayType) type).baseType.toString().replace('.', '_');			
		}
		throw new RuntimeException("Type " + type + " can not be mangled");
	}
	
	/**
	 * Mangles float literals. Infinity and NaN are translated to a corresponding C++ constant.
	 * @param numeric the float literal
	 * @return the mangled float literal
	 */
	public static String mangleFloat(String numeric) {
		if(numeric.equals("Infinity")) return "std::numeric_limits<float>::infinity();";
		if(numeric.equals("-Infinity")) return "-std::numeric_limits<float>::infinity();";
		if(numeric.equals("NaN")) return "std::numeric_limits<float>::signaling_NaN();";
		return numeric;
	}
	
	/**
	 * Mangles double literals. Infinity and NaN are translated to a corresponding C++ constant.
	 * @param numeric the double literal
	 * @return the mangled double literal
	 */
	public static String mangleDouble(String numeric) {
		if(numeric.equals("Infinity")) return "std::numeric_limits<double>::infinity();";
		if(numeric.equals("-Infinity")) return "-std::numeric_limits<double>::infinity();";
		if(numeric.equals("NaN")) return "std::numeric_limits<float>::signaling_NaN();";
		return numeric;
	}
}
