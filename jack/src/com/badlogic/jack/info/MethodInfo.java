package com.badlogic.jack.info;

import soot.SootClass;
import soot.SootMethod;

import com.badlogic.jack.utils.Mangling;

/**
 * Stores additional information for a {@link SootMethod} such as
 * encountered literals, labels and so on. Determines if the method
 * can be skipped, e.g. if it is a bridge method. The corresponding
 * {@link #skip} field will be set to true in that case.
 * @author mzechner
 *
 */
public class MethodInfo {
	public final SootMethod method;
	public final String mangledName;
	public boolean skip;	
	
	public MethodInfo(SootMethod method) {
		this.method = method;
		this.mangledName = Mangling.mangle(method);
		
		// check if this is a bridge method and whether we 
		// need to emit it. Set the skip flag.
		if((method.getModifiers() & 0x40) != 0) {
			if(!shouldEmitBridgeMethod(method.getDeclaringClass(), method)) {
				System.out.println("skipping method " + method + ", bridge method");
				skip = true;
				return;
			}
		}
		
		// if this is the getClass() method, we skip it as well, since
		// we emit it in HeaderGenerator.
		if(method.getName().equals("getClass")) {
			skip = true;
			return;
		}
	}
	
	private boolean shouldEmitBridgeMethod(SootClass clazz, SootMethod bridgeMethod) {
		// check if there's a method with the same parameter list but only a different
		// return type. In all cases this seems to be T versus java.lang.Object for
		// generic types.
		boolean found = false;
		for(SootMethod otherMethod: clazz.getMethods()) {
			if(otherMethod.equals(bridgeMethod)) continue;
			if(otherMethod.getName().equals(bridgeMethod.getName())) {				
				if(otherMethod.getParameterCount() != bridgeMethod.getParameterCount()) {
					continue;
				}
				boolean paramsEqual = true;
				for(int i = 0; i < otherMethod.getParameterCount(); i++) {
					if(!otherMethod.getParameterTypes().get(i).equals(bridgeMethod.getParameterType(i))) {
						paramsEqual = false;
						break;
					}
				}
				if(paramsEqual) {
					found = true;
					break;					
				}
			}
		}
		return !found;
	}	
}
