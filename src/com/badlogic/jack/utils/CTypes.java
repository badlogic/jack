package com.badlogic.jack.utils;

import java.util.List;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.ShortType;
import soot.Type;
import soot.VoidType;

/**
 * Contains static helper methods to convert Soot types to C/C++ types.
 * 
 * @author mzechner
 *
 */
public class CTypes {
	/**
	 * Converts a Soot {@link Type} to C/C++ type string.
	 * @param type the Type
	 * @return the C/C++ type string
	 */
	public static String toCType(Type type) {
		if(type instanceof RefType) {
			return Mangling.mangle(type) + "*";
		} else if(type instanceof ArrayType) {
			ArrayType t = (ArrayType)type;			
			String elementType = toCType(t.baseType);
			String array = generateArraySignature(elementType, t.numDimensions) + "*";
			return array;			
		} else {		
			if(type instanceof BooleanType) return "j_bool";
			else if(type instanceof ByteType) return "j_byte";
			else if(type instanceof CharType) return "j_char";
			else if(type instanceof ShortType) return "j_short";
			else if(type instanceof IntType) return "j_int";
			else if(type instanceof LongType) return "j_long";
			else if(type instanceof FloatType) return "j_float";
			else if(type instanceof DoubleType) return "j_double";
			else if(type instanceof VoidType) return "void";
			else if(type instanceof NullType) return "0";
			else throw new RuntimeException("Unknown primitive type " + type);
		}
	}
	
	public static String toUnsignedCType(Type type) {
		if(type instanceof PrimType) {	
			if(type instanceof ByteType) return "j_ubyte";			
			if(type instanceof ShortType) return "j_ushort";
			if(type instanceof IntType) return "j_uint";
			if(type instanceof LongType) return "j_ulong";
			if(type instanceof CharType) return "j_char";
		}
		throw new RuntimeException("Can't create unsigned primitive type of " + type);			
	}
	
	/**
	 * Generates the C++ signature of an array type
	 * @param elementType
	 * @param numDimensions
	 * @return
	 */
	public static String generateArraySignature(String elementType, int numDimensions) {
		String array = "";
		for(int i = 0; i < numDimensions; i++) array += "Array<";							
		array += elementType;
		for(int i = 0; i < numDimensions - 1; i++) array += ">*";
		array += ">";
		return array;
	}
	
	public static String generateArray(int size, String elementType, boolean isPrimitive) {
		return generateArray(Integer.toString(size), elementType, isPrimitive);
	}
	
	public static String generateArray(String size, String elementType, boolean isPrimitive) {
		String clazz = getElementTypeClass(elementType);
		return "new Array<" + elementType + ">(" + size + ", " + isPrimitive + ", 1, " + clazz + ")";
	}
	
	private static String getElementTypeClass(String elementType) {
		if(elementType.equals("j_byte")) return "&java_lang_Byte::f_TYPE";
		else if(elementType.equals("j_bool")) return "&java_lang_Boolean::f_TYPE";
		else if(elementType.equals("j_char")) return "&java_lang_Character::f_TYPE"; 
		else if(elementType.equals("j_short")) return "&java_lang_Short::f_TYPE";
		else if(elementType.equals("j_int")) return "&java_lang_Integer::f_TYPE";
		else if(elementType.equals("j_long")) return "&java_lang_Long::f_TYPE";
		else if(elementType.equals("j_float")) return "&java_lang_Float::f_TYPE";
		else if(elementType.equals("j_double")) return "&java_lang_Double::f_TYPE";
		else {			
			return "&" + elementType.replace("*", "") + "::clazz";
		}
	}
	
	/**
	 * Generates C++ code to instantiate a multidimensional array
	 * @param target the variable to assign the array to
	 * @param elementType the element type
	 * @param isPrimitive whether it's a primitive type array
	 * @param sizes array containing the sizes for each dimensions
	 * @return
	 */
	public static String generateMultiArray(String target, String elementType, boolean isPrimitive, List<String> sizes) {
		String clazz = getElementTypeClass(elementType);
		String newMultiArray = target + " = new " + generateArraySignature(elementType, sizes.size()) + "(" + sizes.get(0) + ", false, " + sizes.size() + ", " + clazz  + ");\n";
		String counter = target + "_c0";
		int depth = 0;
		for(int i = 0; i < sizes.size() - 1; i++) {
			newMultiArray += indent(depth) + "for(int " + counter + " = 0; " + counter + " < " + sizes.get(i) + "; " + counter + "++) {\n";
			depth++;
			String subArray = generateArraySignature(elementType, sizes.size() - i - 1); 
			newMultiArray += indent(depth);
			for(int j = 0; j < i + 1; j++) {
				newMultiArray += "(*";
			}
			newMultiArray += target + ")";
			for(int j = 0; j < i + 1; j++) {
				if(j < i)
					newMultiArray += "[" + target + "_c" + j + "])"; 
				else
					newMultiArray += "[" + target + "_c" + j + "]";
			}
			if(i == sizes.size() - 2) {
				newMultiArray += " = new " + subArray + "(" + sizes.get(i+1) + ", " + isPrimitive + ", " +  + ((sizes.size() - 1) - i) + ", " + clazz + ");\n";
			} else {
				newMultiArray += " = new " + subArray + "(" + sizes.get(i+1) + ", false, " + ((sizes.size() - 1) - i) + ", " + clazz + ");\n";
			}
			counter = target + "_c" + (i+1);
		}
		for(int i = 0; i < sizes.size() - 1; i++) {
			depth--;
			newMultiArray += indent(depth) + "}\n";
		}
		return newMultiArray;
	}
	
	private static String indent(int depth) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < depth; i++) {
			buffer.append("\t");
		}
		return buffer.toString();
	}
}
