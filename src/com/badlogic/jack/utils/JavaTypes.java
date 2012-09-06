package com.badlogic.jack.utils;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.PrimType;
import soot.ShortType;

/**
 * Converts Java canonical names to 
 * @author mzechner
 *
 */
public class JavaTypes {
	public static  String toClassName(ArrayType type) {
		String name = "";
		for(int i = 0; i < type.numDimensions; i++) {
			name += "[";
		}
		if(type.baseType instanceof PrimType) {
			name += toAbbreviatedType((PrimType)type.baseType);
		} else {
			name += type.toString() + ";";
		}
		return name;
	}
	
	public static String toAbbreviatedType(PrimType type) {
		if(type instanceof BooleanType) return "Z";
		if(type instanceof ByteType) return "B";
		if(type instanceof CharType) return "C";
		if(type instanceof DoubleType) return "D";
		if(type instanceof FloatType) return "F";
		if(type instanceof IntType) return "I";
		if(type instanceof LongType) return "J";
		if(type instanceof ShortType) return "S";
		throw new RuntimeException("Unknown primitive type " + type);
	}
}
