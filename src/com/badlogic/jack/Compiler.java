package com.badlogic.jack;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.Immediate;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ClassConstant;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.EqExpr;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Expr;
import soot.jimple.FloatConstant;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.jimple.LeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.RemExpr;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SubExpr;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.options.Options;
import soot.shimple.toolkits.scalar.SEvaluator.MetaConstant;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.Tag;

import com.badlogic.jack.build.FileDescriptor;

public class Compiler {
	static boolean INCREMENTAL = false;
	static int ident;
	static JavaSourceProvider provider;
	
	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Usage: Compiler <classpath> <sources> <outputdir>");
			System.exit(0);
		}
		
		String classpath = args[0].endsWith("/")? args[0]: args[0] + "/";
		String sources = args[1].endsWith("/")? args[1]: args[1] + "/";
		String outputDir = args[2].endsWith("/")? args[2]: args[2] + "/";
		
		provider = new JavaSourceProvider();
		provider.load(new FileDescriptor(sources));
		
		Options.v().set_keep_line_number(true);
		Options.v().set_process_dir(Arrays.asList(classpath));
		Scene.v().setSootClassPath(classpath);
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		
		new FileDescriptor(outputDir).mkdirs();
		
		Set<String> generatedFiles = new HashSet<String>();
		
		// generate the classes
		for(SootClass c: Scene.v().getClasses()) {
			if(shouldRecompile(classpath, outputDir, c)) {
				generateClass(outputDir, c);
			} else {
				System.out.println(c.getName() + " is up to date");
			}
			generatedFiles.add(nor(c) + ".h");
			generatedFiles.add(nor(c) + ".cpp");
		}
		
		// delete all files that aren't in the classpath
		for(String f: new File(outputDir).list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.endsWith(".h") || name.endsWith(".cpp");
			}
		})) {
			if(!generatedFiles.contains(f)) new File(outputDir + f).delete();
		}
		
		// generate classes.h and classes.cpp containing
		// <clinit> calls and other startup stuff.
		generateClassesStartup(outputDir);
	}
	
	private static boolean shouldRecompile(String classpath, String outputDir, SootClass clazz) {
		if(!INCREMENTAL) return true;
		String classFile = classpath + clazz.getName().replace(".", "/") + ".class";
		String headerFile = outputDir + nor(clazz) + ".h";
		return new File(classFile).lastModified() > new File(headerFile).lastModified(); 
	}
	
	private static void generateClassesStartup(String outputDir) {
		// generate classes.h
		StringBuffer buffer = new StringBuffer();
		wl(buffer, "#ifndef jack_all_classes");
		wl(buffer, "#define jack_all_classes");
		
		for(SootClass c: Scene.v().getClasses()) {
			wl(buffer, "#include \"classes/" + nor(c) + ".h\"");
		}
		
		// add array.h for arrays
		wl(buffer, "#include \"vm/array.h\"");
		wl(buffer, "void jack_init();");
		wl(buffer, "#endif");
		new FileDescriptor(outputDir + "/classes.h").writeString(buffer.toString(), false);
		
		// generate classes.cpp
		buffer = new StringBuffer();
		wl(buffer, "#include \"classes/classes.h\"");
		wl(buffer, "");
		wl(buffer, "void jack_init() {");
		push();
		
		// generate all the reflection info
		generateReflectionData(buffer);
		
		// call all m_clinit methods, this should cascade
		// FIXME clinit (propagation correct?)
		for(SootClass c: Scene.v().getClasses()) {
			wl(buffer, nor(c) + "::m_clinit();");
		}
		pop();
		wl(buffer, "}");
		new FileDescriptor(outputDir + "/classes.cpp").writeString(buffer.toString(), false);
	}
	
	public static void generateReflectionData(StringBuffer buffer) {
		// initialize the java_lang_Class* for each class/interface
		for(SootClass c: Scene.v().getClasses()) {
			String var = nor(c) + "::clazz";
			wl(buffer, var + "= new java_lang_Class();");
		}
		wl(buffer, "");
		
		// fill out their reflection data
		for(SootClass c: Scene.v().getClasses()) {
			String var = nor(c) + "::clazz";
			wl(buffer, var + "->m_init();");
			// FIXME reflection
//			wl(buffer, var + "->f_isPrimitive = " + )
//			wl(buffer, var + "->f_isArray = " + );
			if(c.hasSuperclass()) {
				wl(buffer, var + "->f_superClass = " + nor(c.getSuperclass()) + "::clazz;");
			}
			
			// create array for interfaces
			wl(buffer, var + "->f_interfaces = new Array<java_lang_Class*>(" + c.getInterfaceCount() + ", false);");
			int i = 0;
			for(SootClass itf: c.getInterfaces()) {
				wl(buffer, "(*" + var + "->f_interfaces)[" + i + "] = " + nor(itf) + "::clazz;");
				i++;
			}
			wl(buffer, "");
		}
		wl(buffer, "");	
	}

	public static void generateClass(String outputDir, SootClass clazz) {
		System.out.println("translating " + clazz.getName());
		String header = generateHeader(clazz);
		String cFile = generateCFile(clazz);
		new FileDescriptor(outputDir + nor(clazz) + ".h").writeString(header, false);
		new FileDescriptor(outputDir + nor(clazz) + ".cpp").writeString(cFile, false);
	}
		
	public static String generateHeader(SootClass clazz) {
		StringBuffer buffer = new StringBuffer();
		String fullName = nor(clazz);
		
		// include guards
		wl(buffer, "#ifndef " + fullName + "_h");
		wl(buffer, "#define " + fullName + "_h");
		wl(buffer, "");			
		
		// include common headers
		wl(buffer, "#include \"vm/types.h\"");
		wl(buffer, "#include \"classes/java_lang_Object.h\"");
		wl(buffer, "");
		
		// include forward declarations of types used
		// as method params and fields
		wl(buffer, "// forward declaration of referenced types");
		generateForwardDeclarations(buffer, clazz);
		generateClassHeader(buffer, clazz);
		
		push();
		wl(buffer, "public:");		
		push();
		
		// add fields
		wl(buffer, "// fields");
		for(SootField field: clazz.getFields()) {
			generateField(buffer, field);
		}

		// add methods
		wl(buffer, "");
		wl(buffer, "// methods");
		List<SootMethod> emittedMethods = new ArrayList<SootMethod>();
		boolean hasClinit = false;
		for(SootMethod method: clazz.getMethods()) {
			if((method.getModifiers() & 0x40) != 0) {
				if(!shouldEmitBridgeMethod(clazz, method)) {
					System.out.println("skipping method " + method + ", bridge method");
					continue;
				}
			}
			emittedMethods.add(method);
			// we implement getClass() ourselves, below.
			if(method.getName().equals("getClass")) {
				continue;
			}
			generateMethod(buffer, method);
			if(method.getName().equals("<clinit>")) {
				hasClinit = true;
			}
		}
		
		// getClass() implementation
		wl(buffer, "virtual java_lang_Class* m_getClass() { return " + nor(clazz) + "::clazz; }");
		
		// if we don't have a <clinit> declaration, create one!
		if(!hasClinit) {
			wl(buffer, "static void m_clinit();");
		}
		
		// if this is an abstract class that implements interfaces, gather all methods
		// from it's base that are not directly implemented. 
		// Generate synthetic methods calling into the base. This resolves any
		// ambiguities with the interfaces.
		if(clazz.isAbstract() && clazz.getInterfaceCount() > 0 && !clazz.getSuperclass().getName().equals("java.lang.Object")) {
			List<SootMethod> missingMethods = gatherMissingMethods(clazz, clazz.getSuperclass());
			if(missingMethods.size() > 0) {
				System.out.println("generating synthetic methods: " + missingMethods);
				for(SootMethod method: missingMethods) {
					generateSyntheticMethod(buffer, method, method.isAbstract());
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
							generateSyntheticMethod(buffer, method, true);
						}
					}
				}
			}
		}
		
		// add in a static field keeping track of whether clinit was called
		// and another static field for the class.
		wl(buffer, "static java_lang_Class* clazz;");
		wl(buffer, "static bool clinit;");
		
		pop();
		pop();
		wl(buffer, "};");
		
		wl(buffer, "");
		wl(buffer, "#endif");
		return buffer.toString();
	}
	
	private static boolean containsSameSignatureMethod(List<SootMethod> methods, SootMethod method) {
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
	
	private static boolean shouldEmitBridgeMethod(SootClass clazz, SootMethod bridgeMethod) {
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
	
	private static void generateClassHeader(StringBuffer buffer, SootClass clazz) {
		String fullName = nor(clazz);
		// class header, specifying inheritance
		// interfaces do not inherit from java.lang.Object
		if(clazz.hasSuperclass() || clazz.getInterfaceCount() > 0) {
			String classHeader = "class " + fullName;
			if(clazz.hasSuperclass() && !clazz.isInterface()) {
				classHeader += ": public virtual " + nor(clazz.getSuperclass());
			}
			if(clazz.isInterface()) {
				classHeader += ": public virtual java_lang_Object";
			}

			Iterator<SootClass> iter = clazz.getInterfaces().iterator();
			int addedInterfaces = 0;
			for(int i = 0; i < clazz.getInterfaceCount(); i++) {
				SootClass itf = iter.next();
				
				// check if the interface is already implemented by a super
				// class and omit it in that case.
				if(isInterfaceImplementedBySuperClass(clazz, itf)) continue;
				
				if(addedInterfaces == 0) {
					if(!clazz.hasSuperclass()) classHeader +=": public virtual " + nor(itf);
					else classHeader += ", public virtual " + nor(itf);
				} else {
					classHeader +=", public virtual " + nor(itf);
				}
				addedInterfaces++;
			}
			classHeader += " {";
			wl(buffer, classHeader);
		} else {			
			// let java.lang.Object derrive from gc so
			// all interfaces and objects become collectables.
			wl(buffer, "#define GC_THREADS");
			wl(buffer, "#define GC_NOT_DLL");
			wl(buffer, "#include <gc_cpp.h>");
			wl(buffer, "class " + fullName + ": public gc {");
		}
	}
	
	private static boolean isInterfaceImplementedBySuperInterface(SootClass clazz, SootClass itf) {
		if(clazz.equals(itf)) return true;
		for(SootClass otherItf: clazz.getInterfaces()) {
			if(isInterfaceImplementedBySuperInterface(otherItf, itf)) return true;
		}
		return false;
	}
	
	private static boolean isInterfaceImplementedBySuperClass(SootClass clazz, SootClass itf) {
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
	
	private static void generateForwardDeclarations(StringBuffer buffer, SootClass clazz) {
		// output superclass and interface forward decls
		if(clazz.hasSuperclass()) {
			wl(buffer, "#include \"classes/" + nor(clazz.getSuperclass()) + ".h\"");
		}
		for(SootClass itf: clazz.getInterfaces()) {
			wl(buffer, "#include \"classes/" + nor(itf) + ".h\"");
		}
		
		// forward declare array template and class
		wl(buffer, "template <class T> class Array;");
		wl(buffer, "class java_lang_Class;");
		
		Set<String> forwardedClasses = new HashSet<String>();
		
		// gather field forward decls.
		for(SootField field: clazz.getFields()) {
			forwardedClasses.add(forwardDeclareType(buffer, field.getType()));
		}
		
		// go through each method signature and
		// gather forward decls. for return types and parameters
		for(SootMethod method: clazz.getMethods()) {
			for(Object type: method.getParameterTypes()) {
				forwardedClasses.add(forwardDeclareType(buffer, (Type)type));
			}			
			forwardedClasses.add(forwardDeclareType(buffer, method.getReturnType()));					
		}
		
		// remove super class and interfaces from forward decls
		if(clazz.hasSuperclass()) {
			forwardedClasses.remove(nor(clazz.getSuperclass()));
		}
		for(SootClass itf: clazz.getInterfaces()) {
			forwardedClasses.remove(nor(itf));
		}
		
		// output the forward decls.
		for(String forwardedClass: forwardedClasses) {
			if(forwardedClass == null) continue;
			if(!forwardedClass.equals(nor(clazz))) {
				wl(buffer, "class " + forwardedClass + ";");
			}
		}
		wl(buffer, "");
	}
	
	private static String forwardDeclareType(StringBuffer buffer, Type type) {
		if(type instanceof RefType) {
			return nor(type);
		}
		if(type instanceof ArrayType) {
			// forward declare base type of arrays if it's not a primitive type
			if(!(((ArrayType) type).baseType instanceof PrimType)) {
				return nor(((ArrayType) type).baseType);
			}
		}
		return null;
	}
	
	public static void generateMethod(StringBuffer buffer, SootMethod method) {
		SootClass clazz = method.getDeclaringClass();
		String methodSig = "";
		
		if(method.isStatic()) {
			methodSig +="static ";
		} else {
			methodSig +="virtual ";
		}
		
		methodSig += toCType(method.getReturnType());
		methodSig += " " + nor(method) + "(";
		
		int i = 0;
		for(Object paramType: method.getParameterTypes()) {
			if(i > 0) methodSig += ", ";
			methodSig += toCType((Type)paramType);
			methodSig += " param" + i;
			i++;
		}
		
		methodSig +=")";
		if(clazz.isInterface() || method.isAbstract()) methodSig += " = 0";
		methodSig += ";";
		wl(buffer, methodSig);
	}
	
	public static void generateSyntheticMethod(StringBuffer buffer, SootMethod method, boolean pure) {
		String methodSig = "";
		
		methodSig +="virtual ";		
		methodSig += toCType(method.getReturnType());
		methodSig += " " + nor(method) + "(";
		
		int i = 0;
		for(Object paramType: method.getParameterTypes()) {
			if(i > 0) methodSig += ", ";
			methodSig += toCType((Type)paramType);
			methodSig += " param" + i;
			i++;
		}
		boolean hasReturnType = !(method.getReturnType() instanceof VoidType);
		if(!pure) {
			methodSig +=") { " + (hasReturnType?"return ":"") + nor(method.getDeclaringClass()) + "::" + nor(method) + "(";
			for(i = 0; i < method.getParameterTypes().size(); i++) {
				if(i > 0) methodSig += ", ";
				methodSig += " param" + i;
			}
			methodSig += "); };";
		} else {
			methodSig +=") = 0;";
		}
		wl(buffer, methodSig);
	}
	
	private static void generateField(StringBuffer buffer, SootField field) {
		// determine type and convert to C type
		String cType = toCType(field.getType());
		wl(buffer, (field.isStatic()?"static ":"") + cType + " " + nor(field) + ";");			
	}
	
	private static String toCType(Type type) {
		if(type instanceof RefType) {
			return nor(type) + "*";
		} else if(type instanceof ArrayType) {
			ArrayType t = (ArrayType)type;			
			String elementType = toCType(t.baseType);
			String array = generateArraySig(elementType, t.numDimensions) + "*";
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
	
	private static boolean isPrimitiveType(Type type) {
		return !(type instanceof RefType || type instanceof ArrayType ||
				 type instanceof NullType || type instanceof VoidType);
	}
	
	private static String toUnsignedCType(Type type) {
		if(type instanceof PrimType) {	
			if(type instanceof ByteType) return "j_ubyte";			
			if(type instanceof ShortType) return "j_ushort";
			if(type instanceof IntType) return "j_uint";
			if(type instanceof LongType) return "j_ulong";
			if(type instanceof CharType) return "j_char";
		}
		throw new RuntimeException("Can't create unsigned primitive type of " + type);			
	}
	
	static final Map<String, String> literals = new HashMap<String, String>();
	static int nextLiteralId = 0;
	static SootClass currClass = null;
	public static String generateCFile(SootClass clazz) {
		// we generate the methods first as we gather some info while
		// walking all the statements, e.g. string literals.
		StringBuffer buffer = new StringBuffer();
		literals.clear();
		nextLiteralId = 0;
		currClass = clazz;
				
		// generate methods, exclude clinit
		SootMethod clinitMethod = null;
		for(SootMethod method: clazz.getMethods()) {
			if((method.getModifiers() & 0x40) != 0) {
				if(!shouldEmitBridgeMethod(clazz, method)) {
					continue;
				}
			}
			
			// if this is getClass() continue, we already implemented that
			// in the header
			if(method.getName().equals("getClass")) continue;
			
			// if this is the clinit method, we fake
			// emission of the method. We need to do
			// this to gather all string literals.
			// hackish but easier.
			if(method.getName().equals("<clinit>")) {
				clinitMethod = method;
				generateMethodImplementation(new StringBuffer(), method);
			} else {
				generateMethodImplementation(buffer, method);
			}
			wl(buffer, "");
		}
		
		// generate the <clinit> method. This includes
		// defining the string literals and java.lang.Class as well
		// as any actual clinit code for that class. We need to do
		// this last so we have all our string literals in place.
		// Note that this will gather all string literals in the
		// <clinit> method itself
		wl(buffer, "void " + nor(clazz) + "::m_clinit() {");
		push();
		wrapClinit(buffer, clazz, clinitMethod);
		pop();
		wl(buffer, "}");
		
		// generate header after the fact, including string literals and
		// static fields. Need to do this as string literals are gathered
		// during method generation.		
		String fullName = nor(clazz);		
		StringBuffer headerBuffer = new StringBuffer();
		// standard includes
		wl(headerBuffer, "#include <math.h>"); 
		wl(headerBuffer, "#include <limits>");
		wl(headerBuffer, "#include \"vm/Array.h\"");
		wl(headerBuffer, "#include \"classes/java_lang_Class.h\"");
		// include the header for this class
		wl(headerBuffer, "#include \"classes/" + fullName + ".h\"");
		// include only the dependencies of this class ala import :)
		Set<SootClass> dependencies = getDependencies(clazz);
		for(SootClass dependency: dependencies) {
			wl(headerBuffer, "#include \"classes/" + nor(dependency) + ".h\"");			
		}
		wl(headerBuffer, "");
		
		// generate static fields
		for(SootField field: clazz.getFields()) {
			if(field.isStatic()) {
				String cType = toCType(field.getType());
				String constantValue = null;
				
				for(Tag tag: field.getTags()) {
					if(tag instanceof FloatConstantValueTag) constantValue = norFloat(Float.toString(((FloatConstantValueTag)tag).getFloatValue()));
					if(tag instanceof DoubleConstantValueTag) constantValue = norDouble(Double.toString(((DoubleConstantValueTag)tag).getDoubleValue()));
					if(tag instanceof IntegerConstantValueTag) constantValue = Integer.toString(((IntegerConstantValueTag)tag).getIntValue());
					if(tag instanceof LongConstantValueTag) constantValue = Long.toString(((LongConstantValueTag)tag).getLongValue());
					if(constantValue != null) break;
				}
				
				if(constantValue == null)
					wl(headerBuffer, cType + " " + fullName + "::" + nor(field) + " = 0;");
				else
					wl(headerBuffer, cType + " " + fullName + "::" + nor(field) + " = " + constantValue + ";");
			}
		}
		
		// generate the clazz static fields
		wl(headerBuffer, "java_lang_Class* " + fullName + "::clazz = 0;");
		wl(headerBuffer, "bool " + fullName + "::clinit = 0;");
		wl(headerBuffer, "");
		
		// output string literal array and java.lang.String delcarations.
		// literal arrays are actually defined via j_short[].
		for(String literal: literals.keySet()) {
			String id = literals.get(literal);
			String literalDef = "";
						
			literalDef += "j_char " + id + "_array[] = {";
			if(literal.length() == 0) {
				literalDef += "0";
			} else {
				for(int i = 0; i < literal.length(); i++) {
					if(i > 0) literalDef += ", ";
					literalDef += Short.toString((short)literal.charAt(i)); // FIXME type conversion/promotion
				}
			}
			literalDef += "};\n";
			literalDef += "java_lang_String* " + id + " = 0;";
			wl(headerBuffer, literalDef);
		}
		wl(headerBuffer, "");
		
		return headerBuffer.toString() + buffer.toString();
	}
	
	private static void wrapClinit(StringBuffer buffer, SootClass clazz, SootMethod method) {
		push();
		wl(buffer, "// would enter monitor for this class' clinit method");
		wl(buffer, "{");
		push();
		
		// check if clinit was already called and bail out in that case
		wl(buffer, "if(" + nor(clazz) + "::clinit) return;");
		
		// set the clinit flag of this class as a guard
		wl(buffer, nor(clazz) + "::clinit = true;");
		
		// generate java.lang.String instances for literals
		for(String literal: literals.keySet()) {
			String id = literals.get(literal);
			wl(buffer, id + " = new java_lang_String();");
			wl(buffer, id + "->m_init(new Array<j_char>(" + id + "_array, " + literal.length() + ", true));");
		}
		
		// emit calls to all classes and interfaces' clinit this class references
		for(SootClass dependency: getDependencies(clazz)) {
			wl(buffer, nor(dependency) + "::m_clinit();");
		}		
		
		// generate the method body
		if(method != null) {
			generateMethodBody(buffer, method);
		}
		pop();
		wl(buffer, "}");
		pop();
	}
		
	private static Set<SootClass> getDependencies(SootClass clazz) {
		Set<SootClass> dependencies = new HashSet<SootClass>();
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
			if(method.hasActiveBody()) {
				Body body = method.getActiveBody();								
				for(ValueBox box: body.getUseAndDefBoxes()) {
					if(box.getValue().getType() instanceof RefType) {
						dependencies.add(((RefType)box.getValue().getType()).getSootClass());
					}
				}
				for(Unit unit: body.getUnits()) {
					Stmt stmt = (Stmt)unit;
					if(stmt instanceof InvokeStmt) {						
						InvokeStmt invStmt = (InvokeStmt)stmt;						
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
		return dependencies;
	}
	
	/** used to generate labels in methods, see {@link #translateStatement(StringBuffer, Stmt, SootMethod) **/
	static int labelNum = 0;
	static Map<Stmt, String> labels = new HashMap<Stmt, String>();
	static int lastEmittedSourceLine = 0;
	private static void generateMethodImplementation(StringBuffer buffer, SootMethod method) {
		labelNum = 0;
		lastEmittedSourceLine = 0;
		labels.clear();
		
		if(!method.isConcrete()) {
			if(method.isNative()) {
				generateNativeMethodImplementation(buffer, method);
			}
			return;
		} else {		
			SootClass clazz = method.getDeclaringClass();
			String methodSig = "";
			
			methodSig += toCType(method.getReturnType());
			methodSig += " " + nor(clazz) + "::" + nor(method) + "(";
			
			int i = 0;
			for(Object paramType: method.getParameterTypes()) {
				if(i > 0) methodSig += ", ";
				methodSig += toCType((Type)paramType);
				methodSig += " param" + i;
				i++;
			}
			
			methodSig +=")";
			if(clazz.isInterface()) methodSig += " = 0";
			methodSig += " {";
			wl(buffer, methodSig);
			push();
			generateMethodBody(buffer, method);
			pop();
			wl(buffer, "}");
		}
	}
	
	private static void generateNativeMethodImplementation(StringBuffer buffer, SootMethod method) {
		SootClass clazz = method.getDeclaringClass();
		String methodSig = "";
		
		methodSig += toCType(method.getReturnType());
		methodSig += " " + nor(clazz) + "::" + nor(method) + "(";
		
		int i = 0;
		for(Object paramType: method.getParameterTypes()) {
			if(i > 0) methodSig += ", ";
			methodSig += toCType((Type)paramType);
			methodSig += " param" + i;
			i++;
		}
		
		methodSig +=")";
		if(clazz.isInterface()) methodSig += " = 0";
		methodSig += " {";
		wl(buffer, methodSig);
		push();
		// FIXME JNI
		
		if(method.getReturnType() instanceof VoidType) {
			wl(buffer, "return;");
		} else {
			wl(buffer, "return 0;");
		}
		pop();
		wl(buffer, "}");
	}
	
	private static void generateMethodBody(StringBuffer buffer, SootMethod method) {
		method.retrieveActiveBody();
		JimpleBody body = (JimpleBody)method.getActiveBody();		
		
		// declare locals
		for(Local local: body.getLocals()) {
			String cType = null;
			// null types need special treatment, we don't output them
			// we also won't generate statements that use a nullType
			// as a target.
			if(local.getType() instanceof NullType) {
				cType = "java_lang_Object*";
			} else {
				cType = toCType(local.getType());
			}
			wl(buffer, cType + " " + local.getName() + " = 0;");
		}
		
		// generate labels for each statement another statement points to
		for(Unit unit: body.getUnits()) {			
			if(unit instanceof IfStmt) {
				makeLabel(((IfStmt)unit).getTarget());
			}
			if(unit instanceof GotoStmt) {
				makeLabel((Stmt)((GotoStmt)unit).getTarget());
			}
			if(unit instanceof TableSwitchStmt) {
				TableSwitchStmt stmt = (TableSwitchStmt)unit;
				for(Object target: stmt.getTargets()) {
					makeLabel((Stmt)target);
				}
			}
			if(unit instanceof LookupSwitchStmt) {
				LookupSwitchStmt stmt = (LookupSwitchStmt)unit;
				for(Object target: stmt.getTargets()) {
					makeLabel((Stmt)target);
				}
			}
		}
		
		// translate statements, but only those that
		// don't have a null type local in them.
		for(Unit unit: body.getUnits()) {
			translateStatement(buffer, (Stmt)unit, method);
		}
	}	

	private static void makeLabel(Stmt stmt) {
		String label = labels.get(stmt);
		if(label == null) {
			label = "label" + labelNum++;
			labels.put(stmt, label);
		}
	}
	private static int getLineNumber(Stmt stmt) {
		for(Tag tag: stmt.getTags()) {
			if(tag instanceof LineNumberTag) {
				return ((LineNumberTag) tag).getLineNumber();
			}
		}
		return -1;
	}
	
	public static void translateStatement(StringBuffer buffer, Stmt stmt, SootMethod method) {
		// emit java source
		int lineNumber = getLineNumber(stmt);
		if(lastEmittedSourceLine != lineNumber) {
			String line = provider.getLine(method.getDeclaringClass().getName(), lineNumber - 1);
			if(line != null) {
				wl(buffer, "// " + line);
			}
			lastEmittedSourceLine = lineNumber;
		}
		
		// emit label if any
		if(labels.containsKey(stmt)) {
			pop();
			wl(buffer, labels.get(stmt) + ":");
			push();
		}
		
		// translate statements and emit as C++
		if(stmt instanceof BreakpointStmt) {
			BreakpointStmt s = (BreakpointStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof AssignStmt) {
			AssignStmt s = (AssignStmt)stmt;
			Value leftOp = s.getLeftOp();
			Value rightOp = s.getRightOp();
			
			// if we try to assign to a NullType, we omit the statement!
			if(leftOp.getType() instanceof NullType) {
				return;
			}
			// if we try to assign to a field of a NullType, we omit the statement!
			if(leftOp instanceof JInstanceFieldRef) {
				JInstanceFieldRef ref = (JInstanceFieldRef)leftOp;
				if(ref.getBase().getType() instanceof NullType) {
					return;
				}
			}
			
			// need to special case for multi array creation
			// as it needs a couple of lines of code to create
			// the nested structure, see generateMultiArray();
			if(rightOp instanceof NewMultiArrayExpr) {
				String target = translateValue(leftOp);
				NewMultiArrayExpr v = (NewMultiArrayExpr)rightOp;
				String elementType = toCType(v.getBaseType().baseType);
				boolean isPrimitive = isPrimitiveType(v.getBaseType().baseType);
				List<String> sizes = new ArrayList<String>();
				for(Object size: v.getSizes()) {
					Value arraySize = (Value)size;
					sizes.add(translateValue(arraySize));
				}
				// FIXME GC
				wl(buffer, generateMultiArray(target, elementType, isPrimitive, sizes));
			} 
			// null type need special treatment too, can't assign void* to class*
			else if(rightOp.getType() instanceof NullType) {
				String l = translateValue(leftOp);
				String r = "0";
				wl(buffer, l + " = " + r + ";");
			} else {
				String l = translateValue(leftOp);
				String r = translateValue(rightOp);
				wl(buffer, l + " = " + r + ";");
			}
		} else if(stmt instanceof IdentityStmt) {
			IdentityStmt s = (IdentityStmt)stmt;
			Value leftOp = s.getLeftOp();
			Value rightOp = s.getRightOp();
			String l = translateValue(leftOp);
			String r = translateValue(rightOp);
			wl(buffer, l + " = " + r + ";");
		} else if(stmt instanceof GotoStmt) {
			GotoStmt s = (GotoStmt)stmt;
			String label = labels.get((Stmt)s.getTarget());
			if(label == null) throw new RuntimeException("No label for goto target!");
			wl(buffer, "goto " + label + ";");
		} else if(stmt instanceof IfStmt) {
			IfStmt s = (IfStmt)stmt;
			String condition = translateValue(s.getCondition());
			String label = labels.get(s.getTarget());
			if(label == null) throw new RuntimeException("No label for if target!");
			wl(buffer, "if(" + condition + ") goto " + label + ";");
		} else if(stmt instanceof InvokeStmt) {
			InvokeStmt s = (InvokeStmt)stmt;			
			wl(buffer, translateValue(s.getInvokeExpr()));
		} else if(stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt s = (LookupSwitchStmt)stmt;
			wl(buffer, "switch(" + translateValue(s.getKey()) + ") {");
			push();
			for(int i = 0; i < s.getLookupValues().size(); i++) {
				String target = labels.get(s.getTargets().get(i));
				wl(buffer, "case " + translateValue((Value)s.getLookupValues().get(i)) + ": goto " + target + ";");
			}
			pop();
			wl(buffer, "}");		
		} else if(stmt instanceof EnterMonitorStmt) {
			EnterMonitorStmt s = (EnterMonitorStmt)stmt;
			// FIXME threading
			wl(buffer, "// enter monitor");
		} else if(stmt instanceof ExitMonitorStmt) {
			ExitMonitorStmt s = (ExitMonitorStmt)stmt;
			// FIXME threading
			wl(buffer, "// exit monitor");			
		} else if(stmt instanceof NopStmt) {
			// nothing do to here
		} else if(stmt instanceof RetStmt) {
			RetStmt s = (RetStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof ReturnStmt) {
			ReturnStmt s = (ReturnStmt)stmt;
			String v = translateValue(s.getOp());
			wl(buffer, "return " + v + ";");
		} else if(stmt instanceof ReturnVoidStmt) {
			wl(buffer, "return;");
		} else if(stmt instanceof TableSwitchStmt) {
			TableSwitchStmt s = (TableSwitchStmt)stmt;
			wl(buffer, "switch(" + translateValue(s.getKey()) + ") {");
			push();
			for(int i = s.getLowIndex(); i <= s.getHighIndex(); i++) {
				String target = labels.get(s.getTargets().get(i - s.getLowIndex()));
				wl(buffer, "case " + i + ": goto " + target + ";");
			}
			pop();
			wl(buffer, "}");
		} else if(stmt instanceof ThrowStmt) {
			ThrowStmt s = (ThrowStmt)stmt;
			// FIXME exceptions
			wl(buffer, "throw \"exception\";");
		} else {
			throw new RuntimeException("Unkown statement " + stmt);
		}
	}
	
	private static String translateValue(Value val) {
		if(val instanceof Expr) {
			return translateExpr((Expr)val);
		} else if(val instanceof Immediate) {
			return translateImmediate((Immediate)val);
		} else if(val instanceof Local) {
			return translateLocal((Local)val);
		} else if(val instanceof Ref) {
			return translateRef((Ref)val);
		} else throw new RuntimeException("Unkown Value " + val);
	} 
	
	private static String translateRef(Ref val) {
		if(val instanceof ArrayRef) {
			ArrayRef v = (ArrayRef)val;
			String target = translateValue(v.getBase());
			String index = translateValue(v.getIndex());
			return "(*" + target + ")[" + index + "]";
		} else if(val instanceof StaticFieldRef) {
			StaticFieldRef v = (StaticFieldRef)val;
			return nor(v.getField().getDeclaringClass()) + "::" + nor(v.getField());
		} else if(val instanceof InstanceFieldRef) {
			InstanceFieldRef v = (InstanceFieldRef)val;
			String target = translateValue(v.getBase());
			return target + "->" + nor(v.getField()); 
		} else if(val instanceof IdentityRef) {
			IdentityRef v = (IdentityRef)val;
			if(v instanceof ThisRef) return "this";
			if(v instanceof CaughtExceptionRef) return "(0)"; // FIXME exceptions
			else return "param" + ((ParameterRef)v).getIndex();
		} else throw new RuntimeException("Unknown Ref Value " + val);
	}

	private static String translateLocal(Local val) {
		Local v = (Local)val;
		return v.getName();
	}

	private static String translateImmediate(Immediate val) {
		if(val instanceof ClassConstant) {
			ClassConstant v = (ClassConstant)val;
			return v.getValue().replace('/', '_') + "::clazz";
		} else if(val instanceof MetaConstant) {
			MetaConstant v = (MetaConstant)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NullConstant) {
			return "0";
		} else if(val instanceof NumericConstant) {
			NumericConstant v = (NumericConstant)val;
			if(v instanceof FloatConstant) return norFloat(v.toString());
			if(v instanceof DoubleConstant) return norDouble(v.toString());
			else return v.toString();
		} else if(val instanceof StringConstant) {
			StringConstant v = (StringConstant)val;
			String literalId = literals.get(v.value);
			if(literalId == null) {
				literalId = nor(currClass) + "_literal" + nextLiteralId++;
				literals.put(v.value, literalId);
			}
			return literalId;			
		} else if(val instanceof Local) {
			Local v = (Local)val;
			return v.getName();
		} else throw new RuntimeException("Unknown Immediate Value " + val);
	}

	private static String translateExpr(Expr val) {
		if(val instanceof AddExpr) {
			AddExpr v = (AddExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " + " + r;
		} else if(val instanceof AndExpr) {
			AndExpr v = (AndExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " & " + r; // FIXME operator			
		} else if(val instanceof CmpExpr) {
			CmpExpr v = (CmpExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME operator
			return "(" + l + " > " + r + ") - (" + l + " < " + r + ")";
		} else if(val instanceof CmplExpr) {
			CmplExpr v = (CmplExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME operator
			return String.format("(%s != %s || %s != %s) ? -1 : (%s > %s) - (%s < %s)", l, l, r, r, l, r, l, r);
		} else if(val instanceof CmpgExpr) {
			CmpgExpr v = (CmpgExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME operator
			return String.format("(%s != %s || %s != %s) ? 1 : (%s > %s) - (%s < %s)", l, l, r, r, l, r, l, r);
		} else if(val instanceof EqExpr) {
			EqExpr v = (EqExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " == " + r;
		} else if(val instanceof GeExpr) {
			GeExpr v = (GeExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " >= " + r;
		} else if(val instanceof GtExpr) {
			GtExpr v = (GtExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " > " + r;
		} else if(val instanceof LeExpr) {
			LeExpr v = (LeExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " <= " + r;
		} else if(val instanceof LtExpr) {
			LtExpr v = (LtExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " < " + r;
		} else if(val instanceof NeExpr) {
			NeExpr v = (NeExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " != " + r;
		} else if(val instanceof DivExpr) {
			DivExpr v = (DivExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " / " + r;
		} else if(val instanceof MulExpr) {
			MulExpr v = (MulExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " * " + r;
		} else if(val instanceof OrExpr) {
			OrExpr v = (OrExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " | " + r; // FIXME operator			
		} else if(val instanceof RemExpr) {
			RemExpr v = (RemExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			if(v.getOp1().getType() instanceof DoubleType ||
			   v.getOp1().getType() instanceof FloatType ||
			   v.getOp2().getType() instanceof DoubleType ||
			   v.getOp2().getType() instanceof FloatType)
				return "fmod(" + l + ", " + r +")";
			else
				return l + " % " + r;
		} else if(val instanceof ShlExpr) {
			ShlExpr v = (ShlExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " << " + r;
		} else if(val instanceof ShrExpr) {
			ShrExpr v = (ShrExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " >> " + r;
		} else if(val instanceof SubExpr) {
			SubExpr v = (SubExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " - " + r;
		} else if(val instanceof UshrExpr) {
			UshrExpr v = (UshrExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME operator
			// this should work in C++, unsigned types will produce unsigned right shift (no 1-padding in the top most bits.)
			return "((" + toUnsignedCType(v.getOp1().getType()) + ")" + l + ") >> " + r;
		} else if(val instanceof XorExpr) {
			XorExpr v = (XorExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " ^ " + r; // FIXME operator
		} else if(val instanceof CastExpr) {
			CastExpr v = (CastExpr)val;
			String type = toCType(v.getCastType());
			String target = translateValue(v.getOp());
			if(v.getCastType() instanceof PrimType) {
				return "static_cast<" + type + ">(" + target + ")";
			} else {
				return "reinterpret_cast<" + type + ">(" + target + ")";
			}
		} else if(val instanceof InstanceOfExpr) {
			InstanceOfExpr v = (InstanceOfExpr)val;
			String type = translateValue(v.getOp());
			String checkType = v.getCheckType().toString().replace('.', '_');
			// FIXME reflection
			return checkType + "::clazz->m_isInstance(" + type + ")";					
		} else if(val instanceof DynamicInvokeExpr) {
			DynamicInvokeExpr v = (DynamicInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr v = (InterfaceInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());
			String method = nor(v.getMethod());
			String invoke = target + "->" + method + "(";			
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr v = (SpecialInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());
			String type = nor(v.getMethodRef().declaringClass());
			String method = nor(v.getMethodRef());
			String invoke = target + "->" + type + "::" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr v = (VirtualInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());			
			String method = nor(v.getMethod());
			String invoke = target + "->" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof StaticInvokeExpr) {
			StaticInvokeExpr v = (StaticInvokeExpr)val;			
			String target = nor(v.getMethod().getDeclaringClass());
			String method = nor(v.getMethod());
			String invoke = target + "::" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof NewArrayExpr) {
			NewArrayExpr v = (NewArrayExpr)val;
			String type = toCType(v.getBaseType());
			boolean isPrimitive = isPrimitiveType(v.getBaseType());
			String size = translateValue(v.getSize());
			// FIXME GC
			return "new Array<" + type + ">(" + size + ", " + isPrimitive + ")";
		} else if(val instanceof NewExpr) {
			NewExpr v = (NewExpr)val;
			// FIXME GC
			return "new " + nor(v.getType()) + "()";
		} else if(val instanceof NewMultiArrayExpr) {
			throw new UnsupportedOperationException("Should never process NewMultiArrayExpr here, implemented in translateStatement()");
		} else if(val instanceof LengthExpr) {
			LengthExpr v = (LengthExpr)val;
			String target = translateValue(v.getOp());
			return target + "->length";
		} else if(val instanceof NegExpr) {
			NegExpr v = (NegExpr)val;
			return "-" + translateValue(v.getOp());
		} else throw new RuntimeException("Unkown Expr Value " + val);
	}
	
	private static String outputArguments(String output, List<Value> args, List types) {
		int i = 0;
		for(Value arg: args) {
			String a = translateValue(arg);
			if(i > 0) output += ", (" + toCType((Type)types.get(i)) + ")" + a;
			else output += "(" + toCType((Type)types.get(i)) + ")" + a;
			i++;
		}
		return output;
	}
	
	private static String generateMultiArray(String target, String elementType, boolean isPrimitive, List<String> sizes) {
		String newMultiArray = target + " = new " + generateArraySig(elementType, sizes.size()) + "(" + sizes.get(0) + ", false);\n";
		String counter = target + "_c0";
		for(int i = 0; i < sizes.size() - 1; i++) {
			newMultiArray += i() + "for(int " + counter + " = 0; " + counter + " < " + sizes.get(i) + "; " + counter + "++) {\n";
			push();
			String subArray = generateArraySig(elementType, sizes.size() - i - 1); 
			newMultiArray += i();
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
				newMultiArray += " = new " + subArray + "(" + sizes.get(i+1) + ", " + isPrimitive + ");\n";
			} else {
				newMultiArray += " = new " + subArray + "(" + sizes.get(i+1) + ", false);\n";
			}
			counter = target + "_c" + (i+1);
		}
		for(int i = 0; i < sizes.size() - 1; i++) {
			pop();
			newMultiArray += i() + "}\n";
		}
		return newMultiArray;
	}
	
	private static String generateArraySig(String elementType, int numDimensions) {
		String array = "";
		for(int i = 0; i < numDimensions; i++) array += "Array<";							
		array += elementType;
		for(int i = 0; i < numDimensions - 1; i++) array += ">*";
		array += ">";
		return array;
	}
	
	private static String nor(SootField field) {
		return "f_" + field.getName().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static String nor(SootMethod method) {
		return "m_" + method.getName().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static String nor(SootMethodRef methodRef) {
		return "m_" + methodRef.name().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static String nor(SootClass clazz) {
		return clazz.getName().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static String nor(Type type) {
		return type.toString().replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static String norFloat(String numeric) {
		if(numeric.equals("Infinity")) return "std::numeric_limits<float>::infinity();";
		if(numeric.equals("-Infinity")) return "-std::numeric_limits<float>::infinity();";
		if(numeric.equals("NaN")) return "std::numeric_limits<float>::signaling_NaN();";
		return numeric;
	}
	
	private static String norDouble(String numeric) {
		if(numeric.equals("Infinity")) return "std::numeric_limits<double>::infinity();";
		if(numeric.equals("-Infinity")) return "-std::numeric_limits<double>::infinity();";
		if(numeric.equals("NaN")) return "std::numeric_limits<float>::signaling_NaN();";
		return numeric;
	}
	
	private static String i() {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < ident; i++) {
			buffer.append("\t");
		}
		return buffer.toString();
	}
	
	private static void wl(StringBuffer buffer, String message) {
		buffer.append(i());
		buffer.append(message);
		buffer.append("\n");
	}
	
	private static void push() {
		ident++;
	}
	
	private static void pop() {
		ident--;
		if(ident < 0) ident = 0;
	}
}
