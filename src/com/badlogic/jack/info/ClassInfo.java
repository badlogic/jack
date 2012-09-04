package com.badlogic.jack.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.jack.utils.Mangling;

import soot.ArrayType;
import soot.Body;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;

/**
 * Stores additional information needed for the translation process. This includes
 * string literals, labels, any synthetic methods that need to be generated and so on.
 * 
 * @author mzechner
 *
 */
public class ClassInfo {
	public final SootClass clazz;
	public String mangledName;
	/** classes that need to be #included in the header file **/
	public final Set<String> includedClasses = new HashSet<String>();
	/** mangled class names that need to be forward declared in the header file **/
	public final Set<String> forwardedClasses = new HashSet<String>();
	/** mangled super class name **/
	public String superClass;
	/** mangled interface names **/
	public final Set<String> interfaces = new HashSet<String>();
	/** all reference types this class depends on or references in methods **/
	public final Set<SootClass> dependencies = new HashSet<SootClass>();
	/** Field information **/
	public final Map<SootField, FieldInfo> fieldInfos = new HashMap<SootField, FieldInfo>();
	/** method information **/
	public final Map<SootMethod, MethodInfo> methodInfos = new HashMap<SootMethod, MethodInfo>();
	/** whether the class has a custom clinit implementation **/
	public boolean hasClinit;
	/** synthetic methods to be generated **/
	public List<SyntheticMethodInfo> syntheticMethods;
	/** string literals **/
	public Map<String, String> literals = new HashMap<String, String>();
	/** next literal id for this class **/
	public int nextLiteralId;
	
	/**
	 * Gathers all information necessary for the translation
	 * process for the given {@link SootClass} and fills
	 * this instance with it.
	 * @param clazz the {@link SootClass} instance to generate the info for
	 */
	public ClassInfo(SootClass clazz) {
		this.clazz = clazz;
		generateClassInfo();
	}
	
	private void generateClassInfo() {
		this.mangledName = Mangling.mangle(clazz);
		generateIncludesAndForwardsInfo();
		generateClassHeaderInfo();
		generateFieldInfo();
		generateMethodInfo();
		gatherDependencies();
	}
	
	private void generateFieldInfo() {
		for(SootField field: clazz.getFields()) {
			fieldInfos.put(field, new FieldInfo(field));
		}
	}
	
	private void generateMethodInfo() {
		// create method infos for all methods. if clinit
		// is found, store that info. it's used in the
		// source generators to decide whether to synthesize it or not.
		for(SootMethod method: clazz.getMethods()) {
			MethodInfo methodInfo = new MethodInfo(method);
			if(methodInfo.skip) continue;
			
			methodInfos.put(method, methodInfo);
			if(method.getName().equals("<clinit>")) {
				hasClinit = true;
			}
		}
		
		// generate any synthetic methods, we pass in the methods that
		// we are going to emit as well.
		syntheticMethods = SyntheticMethodInfo.generateSyntheticMethods(clazz, methodInfos.keySet());
	}

	/**
	 * gathers the classes that need to be forward declared or included
	 */
	private void generateIncludesAndForwardsInfo() {
		// gather super class and implemented interfaces, these
		// need to be included via #include.
		if(clazz.hasSuperclass()) {
			includedClasses.add(Mangling.mangle(clazz.getSuperclass()));
		}
		for(SootClass itf: clazz.getInterfaces()) {
			includedClasses.add(Mangling.mangle(itf));
		}
		
		// figure out covariant return types and output #includes for those
		// C++ needs those to be "complete" (fully defined) when used
		// as covariant return types.
		List<SootMethod> covariantMethods = getCovariantMethods(clazz);
		for(SootMethod covariantMethod: covariantMethods) {
			if(covariantMethod.getReturnType() instanceof RefType) {
				SootClass returnType = ((RefType)covariantMethod.getReturnType()).getSootClass();
				if(returnType == clazz) continue;
				includedClasses.add(Mangling.mangle(returnType));
			}
		}			
		
		// forward declare reference types used in fields
		for(SootField field: clazz.getFields()) {			
			addForwardClass(field.getType());			
		}
		
		// forward declare non-primitive method return types and parameters
		for(SootMethod method: clazz.getMethods()) {
			for(Object param: method.getParameterTypes()) {
				addForwardClass((Type)param);
			}
			
			if(!(method.getReturnType() instanceof PrimType || method.getReturnType() instanceof VoidType)) {
				addForwardClass(method.getReturnType());
			}
		}
		
		// remove this class, the super class and interfaces from forward decls
		forwardedClasses.remove(Mangling.mangle(clazz));
		if(clazz.hasSuperclass()) {
			forwardedClasses.remove(Mangling.mangle(clazz.getSuperclass()));
		}
		for(SootClass itf: clazz.getInterfaces()) {
			forwardedClasses.remove(Mangling.mangle(itf));
		}		
	}
	
	private void addForwardClass(Type type) {
		// ignore primitive and void types
		if(type instanceof PrimType || type instanceof VoidType) return;
		// ignore primitive array base types
		if(type instanceof ArrayType) {
			if(((ArrayType) type).baseType instanceof PrimType) return;
		}
		forwardedClasses.add(Mangling.mangle(type));
	}
	
	/**
	 * Gathers all return-type covariant methods of a class.
	 * @param clazz 
	 * @return
	 */
	private static List<SootMethod> getCovariantMethods(SootClass clazz) {
		// find all bridge methods, they are generated by javac for 
		// return type covariant methods
		ArrayList<SootMethod> bridgeMethods = new ArrayList<SootMethod>();
		for(SootMethod method: clazz.getMethods()) {
			if((method.getModifiers() & 0x40) != 0) bridgeMethods.add(method); 
		}
		
		// find the equivalent, more specialized method
		ArrayList<SootMethod> covariantMethods = new ArrayList<SootMethod>();
		for(SootMethod bridgeMethod: bridgeMethods) {
			for(SootMethod otherMethod: clazz.getMethods()) {
				if(otherMethod == bridgeMethod) continue;
				boolean found = false;
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
						covariantMethods.add(otherMethod);
						found = true;
						break;
					}
				}
				if(found) break;
			}
		}
		
		return covariantMethods;
	}
	
	/**
	 * Generates the information necessary to output the class header,
	 * including the base class and any interfaces that need to be
	 * implemented. 
	 */
	private void generateClassHeaderInfo() {
		// class header, specifying inheritance
		// interfaces do not inherit from java.lang.Object
		if(clazz.hasSuperclass() || clazz.getInterfaceCount() > 0) {			
			if(clazz.hasSuperclass() && !clazz.isInterface()) {
				superClass = Mangling.mangle(clazz.getSuperclass());
			}
			if(clazz.isInterface()) {
				superClass = "java_lang_Object";
			}
			
			for(SootClass itf: clazz.getInterfaces()) {			
				// check if the interface is already implemented by a super
				// class and omit it in that case.
				if(isInterfaceImplementedBySuperClass(clazz, itf)) continue;								
				interfaces.add(Mangling.mangle(itf));											
			}
		} else {
			// if this is java.lang.Object we derrive from Boehm GC's gc
			// class
			superClass = "gc";
		}
	}
	
	/**
	 * Checks if the interface is implemented by the given class or one of its
	 * super interfaces (not super classes!)
	 * @param clazz the class 
	 * @param itf the interface
	 * @return whether the class or one of its super interfaces implements the interface
	 */
	private boolean isInterfaceImplementedBySuperInterface(SootClass clazz, SootClass itf) {
		if(clazz.equals(itf)) return true;
		for(SootClass otherItf: clazz.getInterfaces()) {
			if(isInterfaceImplementedBySuperInterface(otherItf, itf)) return true;
		}
		return false;
	}
	
	/**
	 * Checks whether the class or one of its super classes/interfaces implements the
	 * given interface
	 * @param clazz the class
	 * @param itf the interface
	 * @return whether the class or one of its super classes/interfaces implements the interface
	 */
	private boolean isInterfaceImplementedBySuperClass(SootClass clazz, SootClass itf) {
		// if this class is an interface, it has not super class
		// need to go through all the interfaces it implements, recursively
		if(clazz.isInterface()) {
			for(SootClass otherItf: clazz.getInterfaces()) {
				if(otherItf.equals(itf)) continue; // skip the ocurrance in this class' itf list
				if(isInterfaceImplementedBySuperInterface(otherItf, itf)) return true;
			}
			return false;
		} else {
			if(!clazz.hasSuperclass()) return false;
			SootClass superClass = clazz.getSuperclass();
			while(superClass != null) {
				for(SootClass otherItf: superClass.getInterfaces()) {
					if(otherItf.equals(itf)) {
						System.out.println("Omitting interface " + itf.getName() + " from " + clazz.getName() + " because superclass " + superClass.getName() + " already implements it");
						return true;
					}
				}
				superClass = superClass.hasSuperclass()? superClass.getSuperclass(): null;
			}
			return false;
		}
	}
	
	/**
	 * Finds all the reference types the given class depends on. This
	 * is needed for ordered clinit calls among other things.
	 * @param clazz the {@link SootClass} to find the dependencies for
	 * @return the dependencies
	 */
	private void gatherDependencies() {
		if(clazz.hasSuperclass()) dependencies.add(clazz.getSuperclass());
		if(clazz.hasOuterClass()) dependencies.add(clazz.getOuterClass());
		for(SootClass itf: clazz.getInterfaces()) {
			dependencies.add(itf);
		}
		for(SootField field: clazz.getFields()) {
			if(field.getType() instanceof RefType) {
				RefType type = (RefType)field.getType();
				dependencies.add(type.getSootClass());
			}
		}
		for(SootMethod method: clazz.getMethods()) {
			if(method.getReturnType() instanceof RefType) {
				dependencies.add(((RefType)method.getReturnType()).getSootClass());
			}
			for(Object type: method.getParameterTypes()) {
				if(type instanceof RefType) {
					dependencies.add(((RefType)type).getSootClass());
				}
			}
			if(method.isConcrete()) {
				Body body = method.retrieveActiveBody();								
				for(ValueBox box: body.getUseAndDefBoxes()) {
					if(box.getValue().getType() instanceof RefType) {
						dependencies.add(((RefType)box.getValue().getType()).getSootClass());
					}
				}
				for(Unit unit: body.getUnits()) {
					Stmt stmt = (Stmt)unit;
					if(stmt instanceof InvokeStmt) {						
						dependencies.add(((InvokeStmt)stmt).getInvokeExpr().getMethod().getDeclaringClass());
					}
					if(stmt instanceof AssignStmt) {
						AssignStmt assStmt = (AssignStmt)stmt;
						Value val = assStmt.getRightOp();
						if(val instanceof InvokeExpr) {
							dependencies.add(((InvokeExpr)val).getMethod().getDeclaringClass());							
						}
					}
				}
			}
		}
	}
}
