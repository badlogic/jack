package com.badlogic.jack.info;

import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.Mangling;

import soot.SootField;

public class FieldInfo {
	public final SootField field;
	public final String mangledName;
	public final String cType;
	
	public FieldInfo(SootField field) {
		this.field = field;
		this.mangledName = Mangling.mangle(field);
		cType = CTypes.toCType(field.getType());
	}
}
