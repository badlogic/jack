package com.badlogic.jack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.Immediate;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.CastExpr;
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
		
		Scene.v().setSootClassPath("classpath/bin/;");
		Scene.v().loadClassAndSupport("jack.Main");
		
		new FileDescriptor("native/classes/").deleteDirectory();
		new FileDescriptor("native/classes/").mkdirs();
		
		StringBuffer buffer = new StringBuffer();
		wl(buffer, "#ifndef jack_all_classes");
		wl(buffer, "#define jack_all_classes");
		
		for(SootClass c: Scene.v().getClasses()) {
			System.out.println("translating " + c.getName());
			String header = generateHeader(c);
			String cFile = generateCFile(c);
			new FileDescriptor("native/classes/" + nor(c.getName()) + ".h").writeString(header, false);
			new FileDescriptor("native/classeS/" + nor(c.getName()) + ".cpp").writeString(cFile, false);
		
			wl(buffer, "#include \"classes/" + nor(c.getName()) + ".h\"");
		}
		
		wl(buffer, "#endif");
		new FileDescriptor("native/classes/classes.h").writeString(buffer.toString(), false);
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
		if(clazz.isInterface()) methodSig += " = 0";
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
			else throw new RuntimeException("Unknown primitive type " + type);
		}
	}
	
	public static String generateCFile(SootClass clazz) {
		StringBuffer buffer = new StringBuffer();
		
		String fullName = nor(clazz.getName());
		
		// FIXME include all dependencies, using all classes for now :p
//		wl(buffer, "#include \"classes/" + fullName + ".h\"");
		wl(buffer, "#include \"classes/classes.h\"");
		wl(buffer, "");
		
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
		
		// generate methods
		if(!clazz.isInterface()) {
			for(SootMethod method: clazz.getMethods()) {
				generateMethodImplementation(buffer, method);
				wl(buffer, "");
			}
		}
		
		return buffer.toString();
	}
	
	/** used to generate labels in methods, see {@link #translateStatement(StringBuffer, Stmt, SootMethod) **/
	static int labelNum = 0;
	static Map<Stmt, String> labels = new HashMap<Stmt, String>();
	private static void generateMethodImplementation(StringBuffer buffer, SootMethod method) {
		
		if(method.isAbstract()) return;
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
			String l = translateValue(leftOp);
			String r = translateValue(rightOp);
			wl(buffer, l + " = " + r + ";");
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
			if(label == null) {
				label = "label" + labelNum++;
				labels.put((Stmt)s.getTarget(), label);
			}
			wl(buffer, "goto " + label + ";");
		} else if(stmt instanceof IfStmt) {
			IfStmt s = (IfStmt)stmt;
			String condition = translateValue(s.getCondition());
			String label = labels.get(s.getTarget());
			if(label == null) {
				label = "label" + labelNum++;
				labels.put(s.getTarget(), label);
			}
			wl(buffer, "if(" + condition + ") goto " + label + ";");
		} else if(stmt instanceof InvokeStmt) {
			InvokeStmt s = (InvokeStmt)stmt;
			wl(buffer, translateValue(s.getInvokeExpr()));
		} else if(stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt s = (LookupSwitchStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof EnterMonitorStmt) {
			EnterMonitorStmt s = (EnterMonitorStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof ExitMonitorStmt) {
			ExitMonitorStmt s = (ExitMonitorStmt)stmt;
			throw new UnsupportedOperationException();
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
			ReturnVoidStmt s = (ReturnVoidStmt)stmt;
			wl(buffer, "return;");
		} else if(stmt instanceof TableSwitchStmt) {
			TableSwitchStmt s = (TableSwitchStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof ThrowStmt) {
			ThrowStmt s = (ThrowStmt)stmt;
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		} else if(val instanceof MetaConstant) {
			MetaConstant v = (MetaConstant)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NumericConstant) {
			NumericConstant v = (NumericConstant)val;
			return v.toString();
		} else if(val instanceof StringConstant) {
			StringConstant v = (StringConstant)val;
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		} else if(val instanceof CmpExpr) {
			CmpExpr v = (CmpExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof CmplExpr) {
			CmplExpr v = (CmplExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof CmpgExpr) {
			CmpgExpr v = (CmpgExpr)val;
			throw new UnsupportedOperationException();
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
			String r = translateValue(v.getOp1());
			return l + " * " + r;
		} else if(val instanceof OrExpr) {
			OrExpr v = (OrExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof RemExpr) {
			RemExpr v = (RemExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			if(v.getOp1().getType() instanceof DoubleType ||
			   v.getOp1().getType() instanceof FloatType ||
			   v.getOp2().getType() instanceof DoubleType ||
			   v.getOp2().getType() instanceof FloatType)
				return l + " % " + r;
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
			throw new UnsupportedOperationException();
		} else if(val instanceof UshrExpr) {
			UshrExpr v = (UshrExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			// FIXME this is fishy
			return "((unsigned " + v.getOp1().getType() + ")" + l + ") >> " + r;
		} else if(val instanceof XorExpr) {
			XorExpr v = (XorExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof CastExpr) {
			CastExpr v = (CastExpr)val;
			String type = toCType(v.getCastType());
			String target = translateValue(v.getOp());
			return "(" + type + ")" + target;
		} else if(val instanceof InstanceOfExpr) {
			InstanceOfExpr v = (InstanceOfExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof DynamicInvokeExpr) {
			DynamicInvokeExpr v = (DynamicInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr v = (InterfaceInvokeExpr)val;
			String target = translateValue(v.getBase());
			// Type not needed, would require explicit cast which would introduce local 
			// String type = nor(v.getMethod().getDeclaringClass().getName());
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
			// Type not needed, would require explicit cast which would introduce local 
			// String type = nor(v.getMethod().getDeclaringClass().getName());
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
		} else if(val instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr v = (VirtualInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof StaticInvokeExpr) {
			StaticInvokeExpr v = (StaticInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NewArrayExpr) {
			NewArrayExpr v = (NewArrayExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NewExpr) {
			NewExpr v = (NewExpr)val;
			// FIXME use garbage collector!
			return "new " + nor(v.getType().toString()) + "()";
		} else if(val instanceof NewMultiArrayExpr) {
			NewMultiArrayExpr v = (NewMultiArrayExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof LengthExpr) {
			LengthExpr v = (LengthExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NegExpr) {
			NegExpr v = (NegExpr)val;
			throw new UnsupportedOperationException();
		} else throw new RuntimeException("Unkown Expr Value " + val);
	}

	private static String nor(String name) {
		return name.replace('.', '_').replace('<', ' ').replace('>', ' ').trim();
	}
	
	private static void wl(StringBuffer buffer, String message) {
		for(int i = 0; i < ident; i++) {
			buffer.append("\t");
		}
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
