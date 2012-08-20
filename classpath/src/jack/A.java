package jack;

public class A {
	int a = 2;
	int b = 3;
	
	public void set(int a, int b) {
		if(a > 0) {
			this.a = b;
			this.b = b;
		} else {
			this.a = a;
			this.b = a;
		}
	}
	
	public int sum() {
		return a + b;
	}
}
