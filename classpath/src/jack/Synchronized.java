package jack;


public class Synchronized {
	public void test() {
		synchronized(this) {
			test2();
		}
	}
	
	public void test2() {
	}
}
