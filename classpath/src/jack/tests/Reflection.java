package jack.tests;

public class Reflection {
	public void test() throws ClassNotFoundException {
		Class c = byte[].class;
		c = byte[][][].class;
		c = Object[][][].class;
		c = int.class;
		c = Object.class;
		c = Class.forName("[C");
		Object[] arr = new Object[0];
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		new Reflection().test();
	}
}
