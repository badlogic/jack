package jack.tests;

import java.io.Serializable;

public class InstanceOf {
	class A implements IB {
		
	}
	
	class B extends A implements IA {
		
	}
	
	class C implements IC {
		
	}
	
	interface IA {
		
	}
	
	interface IB {
		
	}
	
	interface IC extends IA, IB {
		
	}
	
	public void test() {
		A a = new A();
		B b = new B();
		C c = new C();
		String[] arr = new String[0];
		
		if(A.class != a.getClass()) throw new RuntimeException();
		if(!(a instanceof A)) throw new RuntimeException();
		if(a instanceof B) throw new RuntimeException();
		if(!(arr instanceof Object[]))  throw new RuntimeException();
		if(((Object)arr) instanceof Object[][]) throw new RuntimeException();
		if(!(arr instanceof Serializable)) throw new RuntimeException();
		if(!(arr instanceof Cloneable)) throw new RuntimeException();
	}
}
