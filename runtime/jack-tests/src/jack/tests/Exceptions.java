package jack.tests;

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
			return;
		}
	}
	
	public void controlCatch() {
		try {
			for(int i = 0; i < 10; i++) {
				if(i == 3) {
					return;
				}
			}
		} catch(Throwable t) {
			
		}
	}
	
	public void complexCatch() {
		try {
			try {
				try {
					checked();
				} catch(ClassNotFoundException e) {
					
				} catch(Throwable e) {
					
				}
			} catch(NullPointerException e) {
				
			}
		} catch(Exception e) {
			
		}
	}
}
