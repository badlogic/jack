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
	
	public void ifCatch() {
		try {
			int i = 2;
			if(i == 3) {
				return;
			}
		} catch(Throwable t) {
			
		}
	}
	
	public void forCatch() {
		try {
			for(int i = 0; i< 10; i++) {
				if(i == 0) return;
			}			
		} catch(Throwable t) {
		}
	}
	
	public void whileCatch() {
		try {
			int i = 0;
			while(true) {
				if(i == 0) return;
			}
		} catch(Throwable t) {
		}
	}
	
	public void doCatch() {
		try {
			int i = 0;
			do {
				if(i == 0) return;
			} while(true);
		} catch(Exception e) {
			
		}
	}
	
	public void complexCatch() {
		try {
			try {
				try {
					checked();
					return;
				} catch(ClassNotFoundException e) {
					
				} catch(Throwable e) {
					
				}
			} catch(NullPointerException e) {
				
			} catch(Throwable e) {
				
			}
		} catch(Exception e) {
			
		}
	}
}
