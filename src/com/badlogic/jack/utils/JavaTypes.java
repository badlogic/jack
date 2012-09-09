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
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.Type;

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

	public static SootClass getClassFromType(Type type) {
		if(type instanceof ArrayType) type = ((ArrayType)type).baseType;
		if(type instanceof BooleanType) return Scene.v().loadClassAndSupport("java.lang.Boolean");
		if(type instanceof ByteType) return Scene.v().loadClassAndSupport("java.lang.Byte");
		if(type instanceof CharType) return Scene.v().loadClassAndSupport("java.lang.Character");
		if(type instanceof DoubleType) return Scene.v().loadClassAndSupport("java.lang.Double");
		if(type instanceof FloatType) return Scene.v().loadClassAndSupport("java.lang.Float");
		if(type instanceof IntType) return Scene.v().loadClassAndSupport("java.lang.Integer");
		if(type instanceof LongType) return Scene.v().loadClassAndSupport("java.lang.Long");
		if(type instanceof ShortType) return Scene.v().loadClassAndSupport("java.lang.Short");
		return Scene.v().loadClassAndSupport(type.toString());
	}
}
