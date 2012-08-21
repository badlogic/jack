package com.badlogic.jack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.ClassSource;
import soot.CoffiClassSource;
import soot.DoubleType;
import soot.FloatType;
import soot.Immediate;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.MethodSource;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.coffi.ClassFile;
import soot.coffi.CoffiMethodSource;
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
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.EqExpr;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Expr;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InterfaceInvokeExpr;
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
import soot.options.Options;
import soot.shimple.toolkits.scalar.SEvaluator.MetaConstant;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.Tag;

import com.badlogic.jack.build.FileDescriptor;

public class Compiler {
	static int ident;
	
	public static void main(String[] args) {
		Options.v().set_keep_line_number(true);
		Options.v().set_process_dir(Arrays.asList("classpath/bin/"));
		Scene.v().setSootClassPath("classpath/bin/;");
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		Scene.v().loadClassAndSupport("jack.Main");
		
		// load the base classes that don't get loaded by loadClassAndSupport
//		loadBaseClasses();
		
		new FileDescriptor("native/classes/").deleteDirectory();
		new FileDescriptor("native/classes/").mkdirs();
		
		StringBuffer buffer = new StringBuffer();
		wl(buffer, "#ifndef jack_all_classes");
		wl(buffer, "#define jack_all_classes");				
		
		for(SootClass c: Scene.v().getClasses()) {
			generateClass(c);		
			wl(buffer, "#include \"classes/" + nor(c.getName()) + ".h\"");
		}
		
		// add array.h for arrays
		wl(buffer, "#include \"vm/array.h\"");
		wl(buffer, "#endif");
		new FileDescriptor("native/classes/classes.h").writeString(buffer.toString(), false);
	}
	
	public static void loadBaseClasses() {
		// load a bunch of exceptions we need...
		Scene.v().loadClassAndSupport("java.lang.RuntimeException");
		Scene.v().loadClassAndSupport("java.lang.ArithmeticException");
		Scene.v().loadClassAndSupport("java.lang.ArrayStoreException");
		Scene.v().loadClassAndSupport("java.lang.ClassCastException");
		Scene.v().loadClassAndSupport("java.lang.IllegalMonitorStateException");
		Scene.v().loadClassAndSupport("java.lang.IndexOutOfBoundsException");
		Scene.v().loadClassAndSupport("java.lang.ArrayIndexOutOfBoundsException");
		Scene.v().loadClassAndSupport("java.lang.NegativeArraySizeException");
		Scene.v().loadClassAndSupport("java.lang.NullPointerException");
		Scene.v().loadClassAndSupport("java.lang.InstantiationError");
		Scene.v().loadClassAndSupport("java.lang.InternalError");
		Scene.v().loadClassAndSupport("java.lang.OutOfMemoryError");
		Scene.v().loadClassAndSupport("java.lang.StackOverflowError");
		Scene.v().loadClassAndSupport("java.lang.UnknownError");
		Scene.v().loadClassAndSupport("java.lang.ThreadDeath");
		Scene.v().loadClassAndSupport("java.lang.ClassCircularityError");
		Scene.v().loadClassAndSupport("java.lang.ClassFormatError");
		Scene.v().loadClassAndSupport("java.lang.IllegalAccessError");
		Scene.v().loadClassAndSupport("java.lang.IncompatibleClassChangeError");
		Scene.v().loadClassAndSupport("java.lang.LinkageError");
		Scene.v().loadClassAndSupport("java.lang.NoClassDefFoundError");
		Scene.v().loadClassAndSupport("java.lang.VerifyError");
		Scene.v().loadClassAndSupport("java.lang.NoSuchFieldError");
		Scene.v().loadClassAndSupport("java.lang.AbstractMethodError");
		Scene.v().loadClassAndSupport("java.lang.NoSuchMethodError");
		Scene.v().loadClassAndSupport("java.lang.UnsatisfiedLinkError");
		
		// base classes that don't get loaded for whatever reason
		Scene.v().loadClassAndSupport("avian.Utf8");
		Scene.v().loadClassAndSupport("avian.Iso88591");
		Scene.v().loadClassAndSupport("java.lang.System");
		Scene.v().loadClassAndSupport("java.lang.Boolean");
		Scene.v().loadClassAndSupport("java.lang.Byte");		
		Scene.v().loadClassAndSupport("java.lang.Character");
		Scene.v().loadClassAndSupport("java.lang.Short");
		Scene.v().loadClassAndSupport("java.lang.Integer");
		Scene.v().loadClassAndSupport("java.lang.Long");
		Scene.v().loadClassAndSupport("java.lang.Float");
		Scene.v().loadClassAndSupport("java.lang.Double");
		Scene.v().loadClassAndSupport("java.lang.Math");
		Scene.v().loadClassAndSupport("java.util.regex.Pattern");
		Scene.v().loadClassAndSupport("java.util.regex.Matcher");
	}
	
	public static void generateClass(SootClass clazz) {
		System.out.println("translating " + clazz.getName());
		String header = generateHeader(clazz);
		String cFile = generateCFile(clazz);
		new FileDescriptor("native/classes/" + nor(clazz.getName()) + ".h").writeString(header, false);
		new FileDescriptor("native/classeS/" + nor(clazz.getName()) + ".cpp").writeString(cFile, false);
	}
		
	public static String generateHeader(SootClass clazz) {
		StringBuffer buffer = new StringBuffer();
		String fullName = nor(clazz.getName());
		
		// include guards
		wl(buffer, "#ifndef " + fullName + "_h");
		wl(buffer, "#define " + fullName + "_h");
		wl(buffer, "");
		
		// include common headers
		wl(buffer, "#include \"vm/types.h\"");
		wl(buffer, "");
		
		// include forward declarations of types used
		// as method params and fields
		wl(buffer, "// forward declaration of referenced types");
		wl(buffer, "// interfaces are typedefed to java.lang.Object");
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

		wl(buffer, "");
		wl(buffer, "// methods");
		// add methods
		for(SootMethod method: clazz.getMethods()) {
			generateMethod(buffer, method);
		}
		
		pop();
		pop();
		wl(buffer, "};");
		
		wl(buffer, "");
		wl(buffer, "#endif");
		return buffer.toString();
	}
	
	private static void generateClassHeader(StringBuffer buffer, SootClass clazz) {
		String fullName = nor(clazz.getName());
		// class header, specifying inheritance
		// interfaces do not inherit from java.lang.Object
		if(clazz.hasSuperclass() || clazz.getInterfaceCount() > 0) {
			String classHeader = "class " + fullName;
			if(clazz.hasSuperclass() && !clazz.isInterface()) {
				classHeader += ": public " + nor(clazz.getSuperclass().getName());
			}
			Iterator<SootClass> iter = clazz.getInterfaces().iterator();
			for(int i = 0; i < clazz.getInterfaceCount(); i++) {
				SootClass itf = iter.next();
				if(i == 0) {
					if(!clazz.hasSuperclass()) classHeader +=": public " + nor(itf.getName());
					else classHeader += ", public " + nor(itf.getName());
				} else {
					classHeader +=", public " + nor(itf.getName());
				}
			}
			classHeader += " {";
			wl(buffer, classHeader);
		} else {		
			wl(buffer, "class " + fullName + " {");
		}
	}
	
	private static void generateForwardDeclarations(StringBuffer buffer, SootClass clazz) {
		// output superclass and interface forward decls
		if(clazz.hasSuperclass()) {
			wl(buffer, "#include \"classes/" + nor(clazz.getSuperclass().getName()) + ".h\"");
		}
		for(SootClass itf: clazz.getInterfaces()) {
			wl(buffer, "#include \"classes/" + nor(itf.getName()) + ".h\"");
		}
		
		// output field forward decls.
		for(SootField field: clazz.getFields()) {
			if(field.getType() instanceof RefType) {
				wl(buffer, "class " + nor(field.getType().toString()) + ";");
			}
		}
		
		// go through each method signature and
		// output forward decls. for return types and parameters
		for(SootMethod method: clazz.getMethods()) {
			for(Object type: method.getParameterTypes()) {
				if(type instanceof RefType) {
					wl(buffer, "class " + nor(type.toString()) + ";");
				}
			}
			if(method.getReturnType() instanceof RefType) {
				wl(buffer, "class " + nor(method.getReturnType().toString()) + ";");
			}
		}
		wl(buffer, "");
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
		methodSig += " " + nor(method.getName()) + "(";
		
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
	
	private static void generateField(StringBuffer buffer, SootField field) {
		// determine type and convert to C type
		String cType = toCType(field.getType());
		wl(buffer, (field.isStatic()?"static ":"") + cType + " " + nor(field.getName()) + ";");			
	}
	
	private static String toCType(Type type) {
		if(type instanceof RefType) {
			return nor(type.toString()) + "*";			
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
	public static String generateCFile(SootClass clazz) {		
		// we generate the methods first as we gather some info while
		// walking all the statements, e.g. string literals.
		StringBuffer buffer = new StringBuffer();
		literals.clear();
		nextLiteralId = 0;
				
		// generate methods, including clinit for interfaces and abstract classes etc.	
		for(SootMethod method: clazz.getMethods()) {
			generateMethodImplementation(buffer, method);
			wl(buffer, "");
		}
		
		// generate header after the fact, including string literals and
		// static fields. Need to do this as string literals are gathered
		// during method generation.		
		String fullName = nor(clazz.getName());		
		StringBuffer headerBuffer = new StringBuffer();		
		wl(headerBuffer, "#include \"classes/classes.h\"");
		wl(headerBuffer, "#include <math.h>"); 
		wl(headerBuffer, "");
		
		// generate static fields
		for(SootField field: clazz.getFields()) {
			if(field.isStatic()) {
				String cType = toCType(field.getType());
				String constantValue = null;
				
				for(Tag tag: field.getTags()) {
					if(tag instanceof FloatConstantValueTag) constantValue = Float.toString(((FloatConstantValueTag)tag).getFloatValue());
					if(tag instanceof DoubleConstantValueTag) constantValue = Double.toString(((DoubleConstantValueTag)tag).getDoubleValue());
					if(tag instanceof IntegerConstantValueTag) constantValue = Integer.toString(((IntegerConstantValueTag)tag).getIntValue());
					if(tag instanceof LongConstantValueTag) constantValue = Long.toString(((LongConstantValueTag)tag).getLongValue());
					if(constantValue != null) break;
				}
				
				if(constantValue == null)
					wl(buffer, cType + " " + fullName + "::" + nor(field.getName()) + " = 0;");
				else
					wl(buffer, cType + " " + fullName + "::" + nor(field.getName()) + " = " + constantValue + ";");
			}
		}
		wl(buffer, "");
		
		// output string literals
		
		return headerBuffer.toString() + buffer.toString();
	}
	
	public static void generateStringLiterals(StringBuffer buffer, SootClass clazz) {
		for(SootMethod method: clazz.getMethods()) {
			if(!method.isConcrete()) continue;
			method.retrieveActiveBody();			
		}	
	}
	
	/** used to generate labels in methods, see {@link #translateStatement(StringBuffer, Stmt, SootMethod) **/
	static int labelNum = 0;
	static Map<Stmt, String> labels = new HashMap<Stmt, String>();
	private static void generateMethodImplementation(StringBuffer buffer, SootMethod method) {
		
		if(!method.isConcrete()) return;
		
		labelNum = 0;
		labels.clear();
		SootClass clazz = method.getDeclaringClass();
		String methodSig = "";
		
		methodSig += toCType(method.getReturnType());
		methodSig += " " + nor(clazz.getName()) + "::" + nor(method.getName()) + "(";
		
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
	
	private static void generateMethodBody(StringBuffer buffer, SootMethod method) {
		method.retrieveActiveBody();
		JimpleBody body = (JimpleBody)method.getActiveBody();		
		
		System.out.println(body);
		// declare locals
		for(Local local: body.getLocals()) {
			String cType = toCType(local.getType());
			wl(buffer, cType + " " + local.getName() + " = 0;");
		}
		
		// generate labels for each statement another statement points to
		for(Unit unit: body.getUnits()) {
			Stmt stmt = (Stmt)unit;
			Stmt labeledStmt = null;
			
			if(unit instanceof IfStmt) {
				labeledStmt = ((IfStmt)stmt).getTarget();
			}
			if(unit instanceof GotoStmt) {
				labeledStmt = (Stmt)((GotoStmt)stmt).getTarget();
			}
			
			if(labeledStmt != null) {
				String label = labels.get((Stmt)labeledStmt);
				if(label == null) {
					label = "label" + labelNum++;
					labels.put((Stmt)labeledStmt, label);
				}
			}
		}
		
		// translate statements
		for(Unit unit: body.getUnits()) {
			translateStatement(buffer, (Stmt)unit, method);
		}
	}
	
	public static void translateStatement(StringBuffer buffer, Stmt stmt, SootMethod method) {
		if(labels.containsKey(stmt)) {
			pop();
			wl(buffer, labels.get(stmt) + ":");
			push();
		}
		if(stmt instanceof BreakpointStmt) {
			BreakpointStmt s = (BreakpointStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof AssignStmt) {
			AssignStmt s = (AssignStmt)stmt;
			Value leftOp = s.getLeftOp();
			Value rightOp = s.getRightOp();
			// need to special case for multi array creation
			// as it needs a couple of lines of code to create
			// the nested structure, see generateMultiArray();
			if(rightOp instanceof NewMultiArrayExpr) {
				String target = translateValue(leftOp);
				NewMultiArrayExpr v = (NewMultiArrayExpr)rightOp;
				String elementType = toCType(v.getBaseType().baseType);
				List<String> sizes = new ArrayList<String>();
				for(Object size: v.getSizes()) {
					Value arraySize = (Value)size;
					sizes.add(translateValue(arraySize));
				}
				// FIXME use garbage collector!
				wl(buffer, generateMultiArray(target, elementType, sizes));
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
			throw new UnsupportedOperationException();
		} else if(stmt instanceof EnterMonitorStmt) {
			EnterMonitorStmt s = (EnterMonitorStmt)stmt;
			// FIXME LOWPRIO
			wl(buffer, "// enter monitor");
		} else if(stmt instanceof ExitMonitorStmt) {
			ExitMonitorStmt s = (ExitMonitorStmt)stmt;
			// FIXME LOWPRIO
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
			throw new UnsupportedOperationException();
		} else if(stmt instanceof ThrowStmt) {
			ThrowStmt s = (ThrowStmt)stmt;
			// FIXME LOWPRIO!
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
			return nor(v.getField().getDeclaringClass().getName()) + "::" + v.getField().getName();
		} else if(val instanceof InstanceFieldRef) {
			InstanceFieldRef v = (InstanceFieldRef)val;
			String target = translateValue(v.getBase());
			return target + "->" + nor(v.getField().getName()); 
		} else if(val instanceof IdentityRef) {
			IdentityRef v = (IdentityRef)val;
			if(v instanceof ThisRef) return "this";
			if(v instanceof CaughtExceptionRef) return "(0)"; // FIXME PRIO!
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
			// FIXME PRIO PRIO PRIO!
			return "getClass(" + v.value + ")";
//			throw new UnsupportedOperationException();
		} else if(val instanceof MetaConstant) {
			MetaConstant v = (MetaConstant)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NullConstant) {
			return "0";
		} else if(val instanceof NumericConstant) {
			NumericConstant v = (NumericConstant)val;
			return v.toString();
		} else if(val instanceof StringConstant) {
			StringConstant v = (StringConstant)val;
			String literalId = literals.get(v.value);
			if(literalId == null) {
				literalId = "literal" + nextLiteralId++;
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
			return l + " & " + r; // FIXME PRIO fishy, what about logical or (should be the same in C++, no need for precendence)?			
		} else if(val instanceof CmpExpr) {
			CmpExpr v = (CmpExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME PRIO! fishy fishy fishy from http://jcvm.cvs.sourceforge.net/viewvc/jcvm/jcvm/include/jc_defs.h?view=markup
			return "(" + l + " > " + r + ") - (" + l + " < " + r + ")";
		} else if(val instanceof CmplExpr) {
			CmplExpr v = (CmplExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME PRIO! fishy fishy fishy from http://jcvm.cvs.sourceforge.net/viewvc/jcvm/jcvm/include/jc_defs.h?view=markup
			return String.format("(%s != %s || %s != %s) ? -1 : (%s > %s) - (%s < %s)", l, l, r, r, l, r, l, r);
		} else if(val instanceof CmpgExpr) {
			CmpgExpr v = (CmpgExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME PRIO! fishy fishy fishy from http://jcvm.cvs.sourceforge.net/viewvc/jcvm/jcvm/include/jc_defs.h?view=markup
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
			return l + " | " + r; // FIXME PRIO fishy, what about logical or (should be the same in C++, no need for precendence)?			
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
			// FIXME this should work in C++, unsigned types will produce unsigned right shift
			// (no 1-padding in the top most bits.
			return "((" + toUnsignedCType(v.getOp1().getType()) + ")" + l + ") >> " + r;
		} else if(val instanceof XorExpr) {
			XorExpr v = (XorExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " ^ " + r; // FIXME PRIO! fishy
		} else if(val instanceof CastExpr) {
			CastExpr v = (CastExpr)val;
			String type = toCType(v.getCastType());
			String target = translateValue(v.getOp());
			return "(" + type + ")" + target;
		} else if(val instanceof InstanceOfExpr) {
			InstanceOfExpr v = (InstanceOfExpr)val;
			String type = translateValue(v.getOp());
			String checkType = toCType(v.getCheckType());
			// FIXME PRIO! this is unlikely to actually work, test with interfaces etc.
			return "(dynamic_cast<const " + checkType + ">(" + type + ") != 0)";					
		} else if(val instanceof DynamicInvokeExpr) {
			DynamicInvokeExpr v = (DynamicInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr v = (InterfaceInvokeExpr)val;
			String target = translateValue(v.getBase());
			String method = nor(v.getMethod().getName());
			String invoke = target + "->" + method + "(";
			int i = 0;
			for(Value arg: v.getArgs()) {
				String a = translateValue(arg);
				if(i > 0) invoke += ", " + a;
				else invoke += a;
				i++;
			}
			invoke += ");";
			return invoke;
		} else if(val instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr v = (SpecialInvokeExpr)val;
			String target = translateValue(v.getBase());
			String type = nor(v.getMethodRef().declaringClass().getName());
			String method = nor(v.getMethodRef().name());
			String invoke = target + "->" + type + "::" + method + "(";
			int i = 0;
			for(Value arg: v.getArgs()) {
				String a = translateValue(arg);
				if(i > 0) invoke += ", " + a;
				else invoke += a;
				i++;
			}
			invoke += ");";
			return invoke;
		} else if(val instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr v = (VirtualInvokeExpr)val;
			String target = translateValue(v.getBase());
			String method = nor(v.getMethod().getName());
			String invoke = target + "->" + method + "(";
			int i = 0;
			for(Value arg: v.getArgs()) {
				String a = translateValue(arg);
				if(i > 0) invoke += ", " + a;
				else invoke += a;
				i++;
			}
			invoke += ");";
			return invoke;
		} else if(val instanceof StaticInvokeExpr) {
			StaticInvokeExpr v = (StaticInvokeExpr)val;			
			String target = nor(v.getMethod().getDeclaringClass().getName());
			String method = nor(v.getMethod().getName());
			String invoke = target + "::" + method + "(";
			int i = 0;
			for(Value arg: v.getArgs()) {
				String a = translateValue(arg);
				if(i > 0) invoke += ", " + a;
				else invoke += a;
				i++;
			}
			invoke += ");";
			return invoke;
		} else if(val instanceof NewArrayExpr) {
			NewArrayExpr v = (NewArrayExpr)val;
			String type = toCType(v.getBaseType());
			String size = translateValue(v.getSize());
			// FIXME use garbage collector!
			return "new Array<" + type + ">(" + size + ")";
		} else if(val instanceof NewExpr) {
			NewExpr v = (NewExpr)val;
			// FIXME use garbage collector!
			return "new " + nor(v.getType().toString()) + "()";
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
	
	private static String generateMultiArray(String target, String elementType, List<String> sizes) {
		String newMultiArray = target + " = new " + generateArraySig(elementType, sizes.size()) + "(" + sizes.get(0) + ");\n";
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
			newMultiArray += " = new " + subArray + "(" + sizes.get(i+1) + ");\n";
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
	
	private static String nor(String name) {
		return name.replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
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
