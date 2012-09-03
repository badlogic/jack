package jack;

public class InstanceOf {
	class A {
		
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
		
		if(A.class != a.getClass()) throw new RuntimeException();
		if(!(a instanceof A)) throw new RuntimeException();
		if(a instanceof B) throw new RuntimeException();
	}
}
