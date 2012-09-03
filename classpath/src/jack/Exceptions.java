package jack;

public class Exceptions {
	public void runtime() {
		throw new RuntimeException();
	}
	
	public void checked() throws Exception {
		throw new Exception();
	}
	
	public void simpleCatch() {
		try {
			checked();
		} catch(Exception e) {
			
		}
	}
}
