package com.badlogic.jack.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.ArrayType;
import soot.DoubleType;
import soot.FloatType;
import soot.Immediate;
import soot.Local;
import soot.NullType;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
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
import soot.shimple.toolkits.scalar.SEvaluator.MetaConstant;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;

import com.badlogic.jack.info.ClassInfo;
import com.badlogic.jack.utils.CTypes;
import com.badlogic.jack.utils.JavaSourceProvider;
import com.badlogic.jack.utils.JavaTypes;
import com.badlogic.jack.utils.Mangling;
import com.badlogic.jack.utils.SourceWriter;

/**
 * Generates C++ statements for a {@link SootMethod} body's statements. Also
 * stores string literals in the {@link ClassInfo} instance passed
 * to this method. Those are later used in the {@link ClinitGenerator}
 * to initializes the string literals properly. 
 * @author mzechner
 *
 */
public class StatementGenerator {
	private final SourceWriter writer;
	private final JavaSourceProvider sourceProvider;
	private final ClassInfo info;
	private final JimpleBody body;
	private final Map<Stmt, String> labels = new HashMap<Stmt, String>();
	private int nextLabelId = 0;
	private int lastEmittedSourceLine = 0;
	
	public StatementGenerator(SourceWriter writer, JavaSourceProvider sourceProvider, ClassInfo info, SootMethod method) {
		this.writer = writer;
		this.sourceProvider = sourceProvider;
		this.info = info;
		this.body = (JimpleBody)method.retrieveActiveBody();
	}
	
	public void generate() {
		generateLabels();
		generateStatements();
	}
	
	/**
	 * Generate labels for all statements that are targets of
	 * jumps, e.g. due to if statements or switch statements
	 */
	private void generateLabels() {
		// generate labels for each statement another statement points to
		for(Unit unit: body.getUnits()) {			
			if(unit instanceof IfStmt) {
				newLabel(((IfStmt)unit).getTarget());
			}
			if(unit instanceof GotoStmt) {
				newLabel((Stmt)((GotoStmt)unit).getTarget());
			}
			if(unit instanceof TableSwitchStmt) {
				TableSwitchStmt stmt = (TableSwitchStmt)unit;
				for(Object target: stmt.getTargets()) {
					newLabel((Stmt)target);
				}
			}
			if(unit instanceof LookupSwitchStmt) {
				LookupSwitchStmt stmt = (LookupSwitchStmt)unit;
				for(Object target: stmt.getTargets()) {
					newLabel((Stmt)target);
				}
			}
			// FIXME exceptions labels for handlers
		}		
	}
	
	/**
	 * Generates a label for the given statement and stores it
	 * for later retrieval.
	 * @param stmt
	 */
	private void newLabel(Stmt stmt) {
		String label = labels.get(stmt);
		if(label == null) {
			label = "label" + nextLabelId++;
			labels.put(stmt, label);
		}
	}
	
	/**
	 * Generates C++ statements for statements in the {@link SootMethod}.
	 * Also gathers string literal information and saves it in the {@link ClassInfo}
	 * instance passed to the constructor of this class.
	 */
	private void generateStatements() {
		for(Unit unit: body.getUnits()) {
			generateStatement((Stmt)unit);
		}
	}

	/**
	 * Generates a single statement and adds Java source code lines
	 * via a {@link JavaSourceProvider}.
	 * 
	 * Statements are often made up of operands. These need to be 
	 * translated as well.
	 * 
	 * @param unit
	 */
	private void generateStatement(Stmt stmt) {
		generateSourceComment(stmt);
		
		// emit label if any
		if(labels.containsKey(stmt)) {
			writer.pop();
			writer.wl(labels.get(stmt) + ":");
			writer.push();
		}
		
		// translate statements and emit as C++
		if(stmt instanceof BreakpointStmt) {
//			BreakpointStmt s = (BreakpointStmt)stmt;
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
				String elementType = CTypes.toCType(v.getBaseType().baseType);
				boolean isPrimitive = v.getBaseType().baseType instanceof PrimType;
				List<String> sizes = new ArrayList<String>();
				for(Object size: v.getSizes()) {
					Value arraySize = (Value)size;
					sizes.add(translateValue(arraySize));
				}
				info.dependencies.add(JavaTypes.getClassFromType(v.getBaseType().baseType));
				writer.wl(CTypes.generateMultiArray(target, elementType, isPrimitive, sizes));
			} 
			// null type need special treatment too, can't assign void* to class*
			else if(rightOp.getType() instanceof NullType) {
				String l = translateValue(leftOp);
				String r = "0";
				writer.wl(l + " = " + r + ";");
			} else {
				String l = translateValue(leftOp);
				String r = translateValue(rightOp);
				writer.wl(l + " = " + r + ";");
			}
		} else if(stmt instanceof IdentityStmt) {
			IdentityStmt s = (IdentityStmt)stmt;
			Value leftOp = s.getLeftOp();
			Value rightOp = s.getRightOp();
			String l = translateValue(leftOp);
			String r = translateValue(rightOp);
			writer.wl(l + " = " + r + ";");
		} else if(stmt instanceof GotoStmt) {
			GotoStmt s = (GotoStmt)stmt;
			String label = labels.get((Stmt)s.getTarget());
			if(label == null) throw new RuntimeException("No label for goto target!");
			writer.wl("goto " + label + ";");
		} else if(stmt instanceof IfStmt) {
			IfStmt s = (IfStmt)stmt;
			String condition = translateValue(s.getCondition());
			String label = labels.get(s.getTarget());
			if(label == null) throw new RuntimeException("No label for if target!");
			writer.wl("if(" + condition + ") goto " + label + ";");
		} else if(stmt instanceof InvokeStmt) {
			InvokeStmt s = (InvokeStmt)stmt;			
			writer.wl(translateValue(s.getInvokeExpr()));
		} else if(stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt s = (LookupSwitchStmt)stmt;
			writer.wl("switch(" + translateValue(s.getKey()) + ") {");
			writer.push();
			for(int i = 0; i < s.getLookupValues().size(); i++) {
				String target = labels.get(s.getTargets().get(i));
				writer.wl("case " + translateValue((Value)s.getLookupValues().get(i)) + ": goto " + target + ";");
			}
			writer.pop();
			writer.wl( "}");		
		} else if(stmt instanceof EnterMonitorStmt) {
//			EnterMonitorStmt s = (EnterMonitorStmt)stmt;
			// FIXME threading
			writer.wl("// enter monitor");
		} else if(stmt instanceof ExitMonitorStmt) {
//			ExitMonitorStmt s = (ExitMonitorStmt)stmt;
			// FIXME threading
			writer.wl("// exit monitor");			
		} else if(stmt instanceof NopStmt) {
			// nothing do to here
		} else if(stmt instanceof RetStmt) {
//			RetStmt s = (RetStmt)stmt;
			throw new UnsupportedOperationException();
		} else if(stmt instanceof ReturnStmt) {
			ReturnStmt s = (ReturnStmt)stmt;
			String v = translateValue(s.getOp());
			writer.wl("return " + v + ";");
		} else if(stmt instanceof ReturnVoidStmt) {
			writer.wl("return;");
		} else if(stmt instanceof TableSwitchStmt) {
			TableSwitchStmt s = (TableSwitchStmt)stmt;
			writer.wl("switch(" + translateValue(s.getKey()) + ") {");
			writer.push();
			for(int i = s.getLowIndex(); i <= s.getHighIndex(); i++) {
				String target = labels.get(s.getTargets().get(i - s.getLowIndex()));
				writer.wl("case " + i + ": goto " + target + ";");
			}
			writer.pop();
			writer.wl("}");
		} else if(stmt instanceof ThrowStmt) {
			ThrowStmt s = (ThrowStmt)stmt;		
			writer.wl("throw " + translateValue(s.getOp()) + ";");
		} else {
			throw new RuntimeException("Unkown statement " + stmt);
		}
	}
	
	private void generateSourceComment(Stmt stmt) {
		int lineNumber = getLineNumber(stmt);
		if(lastEmittedSourceLine != lineNumber) {
			String line = sourceProvider.getLine(info.clazz.getName(), lineNumber - 1);
			if(line != null) {
				writer.wl("// " + line);
			}
			lastEmittedSourceLine = lineNumber;
		}		
	}

	/**
	 * Translates an operand value used in a statement to C++. Values
	 * can be expressions, which have operand values themselves, immediate
	 * values (literals), local variable values and reference values (fields, etc.)
	 * @param val
	 * @return
	 */
	private String translateValue(Value val) {
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
	
	/**
	 * Translates an immediate or literal value.
	 * @param val
	 * @return
	 */
	private String translateImmediate(Immediate val) {
		if(val instanceof ClassConstant) {
			ClassConstant v = (ClassConstant)val;
			return translateClassConstant(v);
		} else if(val instanceof MetaConstant) {
//			MetaConstant v = (MetaConstant)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof NullConstant) {
			return "0";
		} else if(val instanceof NumericConstant) {
			NumericConstant v = (NumericConstant)val;
			if(v instanceof FloatConstant) return Mangling.mangleFloat(v.toString());
			if(v instanceof DoubleConstant) return Mangling.mangleDouble(v.toString());
			else return v.toString();
		} else if(val instanceof StringConstant) {
			StringConstant v = (StringConstant)val;
			return info.literals.addLiteral(v.value);			
		} else if(val instanceof Local) {
			Local v = (Local)val;
			return v.getName();
		} else throw new RuntimeException("Unknown Immediate Value " + val);
	}
	
	private String translateClassConstant(ClassConstant constant) {
		if(constant.value.contains("[")) {
			String literal = info.literals.addLiteral(constant.value.replace('/', '.'));
			return "java_lang_Class::m_forName(" + literal + ");";
		} else {
			addDependency(constant.value);
			return constant.getValue().replace('/', '_') + "::clazz";
		}
	}

	/**
	 * Translates access to a local variable
	 * @param val the local variable
	 * @return the C++ translation of the local variable
	 */
	private String translateLocal(Local val) {
		Local v = (Local)val;
		return v.getName();
	}
	
	/**
	 * Translates a reference value, e.g. a field or array access, and
	 * identity references (this, or the current exception).
	 * @param val the reference value
	 * @return the C++ translation of the reference value
	 */
	private String translateRef(Ref val) {
		if(val instanceof ArrayRef) {
			ArrayRef v = (ArrayRef)val;
			String target = translateValue(v.getBase());
			String index = translateValue(v.getIndex());
			return "(*" + target + ")[" + index + "]";
		} else if(val instanceof StaticFieldRef) {
			StaticFieldRef v = (StaticFieldRef)val;
			addDependency(v.getField().getDeclaringClass());
			return Mangling.mangle(v.getField().getDeclaringClass()) + "::" + Mangling.mangle(v.getField());
		} else if(val instanceof InstanceFieldRef) {
			InstanceFieldRef v = (InstanceFieldRef)val;
			String target = translateValue(v.getBase());
			return target + "->" + Mangling.mangle(v.getField()); 
		} else if(val instanceof IdentityRef) {
			IdentityRef v = (IdentityRef)val;
			if(v instanceof ThisRef) return "this";
			if(v instanceof CaughtExceptionRef) return "(0)"; // FIXME exceptions
			else return "param" + ((ParameterRef)v).getIndex();
		} else throw new RuntimeException("Unknown Ref Value " + val);
	}
	
	/**
	 * Translates an expression to C++. An expression is a basic
	 * operations such as arithemtic operators, method invocation
	 * and so on.
	 * @param val
	 * @return
	 */
	private String translateExpr(Expr val) {
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
			return "((" + CTypes.toUnsignedCType(v.getOp1().getType()) + ")" + l + ") >> " + r;
		} else if(val instanceof XorExpr) {
			XorExpr v = (XorExpr)val;
			String l = translateValue(v.getOp1());
			String r = translateValue(v.getOp2());
			return l + " ^ " + r; // FIXME operator
		} else if(val instanceof CastExpr) {
			CastExpr v = (CastExpr)val;
			String type = CTypes.toCType(v.getCastType());
			String target = translateValue(v.getOp());
			if(v.getCastType() instanceof PrimType) {
				return "static_cast<" + type + ">(" + target + ")";
			} else {
				// for assignments like Object[] v = null;
				if(target.equals("0")) {
					return "0";
				} else {
					return "dynamic_cast<" + type + ">(" + target + ")";
				}
			}
		} else if(val instanceof InstanceOfExpr) {
			InstanceOfExpr v = (InstanceOfExpr)val;
			String type = translateValue(v.getOp());			
			Type checkType = v.getCheckType();	
			// need to special case for arrays and primitive types
			// we resolve those via Class#forName(String). This methods
			// semantics have been extended so #<primitivetype> returns
			// the corresponding Class, e.g. #int -> int.class, #byte -> byte.class.
			if(checkType instanceof ArrayType) {
				ArrayType arrayType = (ArrayType)checkType;
				String className = JavaTypes.toClassName(arrayType);
				return "java_lang_Class::m_forName(" + info.literals.addLiteral(className) + ")";				
			} else if(checkType instanceof PrimType) {
				throw new RuntimeException("This should not happen, primitive types are referenced via Type#f_TYPE");
			} else {
				String checkTypeStr = checkType.toString().replace('.', '_');
				addDependency(checkType.toString());
				return checkTypeStr + "::clazz->m_isInstance(" + type + ")";
			}
		} else if(val instanceof DynamicInvokeExpr) {
//			DynamicInvokeExpr v = (DynamicInvokeExpr)val;
			throw new UnsupportedOperationException();
		} else if(val instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr v = (InterfaceInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());
			String method = Mangling.mangle(v.getMethod());
			String invoke = target + "->" + method + "(";			
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr v = (SpecialInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());
			String type = Mangling.mangle(v.getMethodRef().declaringClass());
			String method = Mangling.mangle(v.getMethodRef());
			String invoke = target + "->" + type + "::" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr v = (VirtualInvokeExpr)val;
			if(v.getBase().getType() instanceof NullType) return ""; // If we invoke on a NullType, we omit the expression.
			String target = translateValue(v.getBase());			
			String method = Mangling.mangle(v.getMethod());
			String invoke = target + "->" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof StaticInvokeExpr) {
			StaticInvokeExpr v = (StaticInvokeExpr)val;
			addDependency(v.getMethod().getDeclaringClass());
			String target = Mangling.mangle(v.getMethod().getDeclaringClass());
			String method = Mangling.mangle(v.getMethod());
			String invoke = target + "::" + method + "(";
			invoke = outputArguments(invoke, v.getArgs(), v.getMethod().getParameterTypes());
			invoke += ");";
			return invoke;
		} else if(val instanceof NewArrayExpr) {
			NewArrayExpr v = (NewArrayExpr)val;
			String type = CTypes.toCType(v.getBaseType());
			boolean isPrimitive = v.getBaseType() instanceof PrimType;
			String size = translateValue(v.getSize());
			// FIXME GC
			info.dependencies.add(JavaTypes.getClassFromType(v.getBaseType()));
			return CTypes.generateArray(size, type, isPrimitive);
		} else if(val instanceof NewExpr) {
			NewExpr v = (NewExpr)val;
			// FIXME GC
			return "new " + Mangling.mangle(v.getType()) + "()";
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

	private String outputArguments(String output, List<Value> args, @SuppressWarnings("rawtypes") List types) {
		int i = 0;
		for(Value arg: args) {
			String a = translateValue(arg);
			if(i > 0) output += ", (" + CTypes.toCType((Type)types.get(i)) + ")" + a;
			else output += "(" + CTypes.toCType((Type)types.get(i)) + ")" + a;
			i++;
		}
		return output;
	}
	
	/**
	 * Returns the line number for a statement in the method.
	 * @param stmt
	 * @return
	 */
	private int getLineNumber(Stmt stmt) {
		for(Tag tag: stmt.getTags()) {
			if(tag instanceof LineNumberTag) {
				return ((LineNumberTag) tag).getLineNumber();
			}
		}
		return -1;
	}
	
	/**
	 * Adds a dependency to the class given by the name. Assumes
	 * names are encoded like "java/lang/Object".
	 * @param className
	 */
	private void addDependency(String className) {
		info.dependencies.add(Scene.v().loadClassAndSupport(className.replace('/', '.')));
	}
	
	/**
	 * Adds a dependency to the class given.
	 * @param declaringClass
	 */
	private void addDependency(SootClass declaringClass) {
		info.dependencies.add(declaringClass);
	}
}
