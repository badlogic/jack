package jack;

public class B extends A {
	int a = 123;
	int b;
	
	public void set(int a, int b) {
		this.a = a;
		this.b = b;
	}
	
	public int sum() {
		return super.sum();
	}
}
