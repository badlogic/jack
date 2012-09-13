package jack.tests;

public class Reflection {
	class Inner {
	}
	
	public void test() throws ClassNotFoundException {
		Class c = byte[].class;
		if(!c.getName().equals("[B")) throw new ClassNotFoundException();
		
		c = byte[][][].class;
		if(!c.getName().equals("[[[B")) throw new ClassNotFoundException();
		
		c = Object[][][].class;
		if(!c.getName().equals("[[[Ljava.lang.Object;")) throw new ClassNotFoundException();
		
		c = int.class;
		if(!c.getName().equals("int")) throw new ClassNotFoundException();
		
		c = Object.class;
		if(!c.getName().equals("java.lang.Object")) throw new ClassNotFoundException();
		
		c = Inner[][].class;
		if(!c.getName().equals("[[Ljack.tests.Reflection$Inner;")) throw new ClassNotFoundException();
		
		c = Class.forName("[C");
		if(!c.isArray() || c.getComponentType() != char.class) throw new ClassNotFoundException();
		
		Object[] arr = new Object[0];
		c = arr.getClass();
		if(!c.isArray() || !c.getName().equals("[Ljava.lang.Object;") || c.getComponentType() != Object.class)
			throw new ClassNotFoundException();
		
//		if(Class.forName("[Ljava/lang/Object;") == Object.class) throw new RuntimeException();
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		new Reflection().test();
	}
}
