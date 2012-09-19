package java.lang.reflect;

public class InvocationTargetException extends Exception {
	private Throwable target;
	
	public InvocationTargetException() {
		super();
	}

	public InvocationTargetException(Throwable target) {
		this(target, null);
	}
	
	public InvocationTargetException(Throwable target, String s) {
		super(s);
		this.target = target;
	}
}
