package com.badlogic.jack.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.jack.utils.CTypes;

import soot.SootClass;
import soot.SootMethod;

/**
 * Information on methods the transpiler needs to generate itself to 
 * cope with certain C++ semantics. E.g. an abstract class A may extends
 * another abstract class B. B has a method foo() that is abstract. The
 * definition of A does not contain a definition to foo(). In Java this
 * is possible, in C++ we need to generate a synthetic pure virtual method
 * in A for method foo().
 * 
 * Another case is an abstract class A that extends from a base class B.
 * B has a non-abstract method bar() that A does not override. This is
 * valid in Java and will automatically call B#bar() if an A instance
 * is used. In C++ we need to emit a bridge method that explicitely
 * calls B#bar() in A#bar().
 * 
 * @author mzechner
 *
 */
public class SyntheticMethodInfo {
	/** the base method to generate the synthetic method from **/
	public final SootMethod baseMethod;
	/** the mangled C return type **/
	public final String returnType;
	/** whether this method is pure (== Java abstract, no implementation) **/
	public final boolean isPure;
	
	public SyntheticMethodInfo(SootMethod method, boolean isPure) {
		this.baseMethod = method;
		this.returnType = CTypes.toCType(method.getReturnType());
		this.isPure = isPure;
	}
	
	public static List<SyntheticMethodInfo> generateSyntheticMethods(SootClass clazz, Collection<SootMethod> methods) {
		List<SyntheticMethodInfo> synMethods = new ArrayList<SyntheticMethodInfo>();
		List<SootMethod> emittedMethods = new ArrayList<SootMethod>(methods); 
		
		// if this is an abstract class that implements interfaces, gather all methods
		// from it's base that are not directly implemented. 
		// Generate synthetic methods calling into the base. This resolves any
		// ambiguities with the interfaces.
		if(clazz.isAbstract() && clazz.getInterfaceCount() > 0 && !clazz.getSuperclass().getName().equals("java.lang.Object")) {
			List<SootMethod> missingMethods = gatherMissingMethods(clazz, clazz.getSuperclass());
			if(missingMethods.size() > 0) {
				System.out.println("generating synthetic methods: " + missingMethods);
				for(SootMethod method: missingMethods) {
					synMethods.add(new SyntheticMethodInfo(method, method.isAbstract()));
					emittedMethods.add(method);
				}
			}
		}
		
		// if this is an abstract class that implements interfaces, gather all
		// methods from the interfaces that are not directly implemented (including synthetic
		// methods generated above) and emit pure virtual methods.
		if(clazz.isAbstract() && clazz.getInterfaceCount() > 0 && !clazz.getSuperclass().getName().equals("java.lang.Object")) {
			for(SootClass itf: clazz.getInterfaces()) {
				List<SootMethod> missingMethods = gatherMissingMethods(clazz, itf);
				if(missingMethods.size() > 0) {
					System.out.println("generating synthetic pure methods: " + missingMethods);
					for(SootMethod method: missingMethods) {
						if(!containsSameSignatureMethod(emittedMethods, method)) {
							synMethods.add(new SyntheticMethodInfo(method, true));
						}
					}
				}
			}
		}
		
		return synMethods;
	}	
	
	private static boolean containsSameSignatureMethod(Collection<SootMethod> methods, SootMethod method) {
		for(SootMethod otherMethod: methods) {
			if(method.getName().equals(otherMethod.getName())) {
				if(method.getParameterTypes().equals(otherMethod.getParameterTypes())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static List<SootMethod> gatherMissingMethods(SootClass clazz, SootClass otherClass) {
		List<SootMethod> missingMethods = new ArrayList<SootMethod>();
		for(SootMethod superMethod: otherClass.getMethods()) {
			boolean found = false;
			for(SootMethod method: clazz.getMethods()) {
				if(method.getName().equals(superMethod.getName())) {											
					if(superMethod.getParameterTypes().equals(method.getParameterTypes())) {
						found = true;
						break;
					}
				}
			}
			if(!found) missingMethods.add(superMethod);
		}
		return missingMethods;
	}
}
