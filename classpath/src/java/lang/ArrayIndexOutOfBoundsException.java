package java.lang;

public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
	public ArrayIndexOutOfBoundsException() {
		super();
	}

	public ArrayIndexOutOfBoundsException(java.lang.String message) {
		super(message);
	}
	
	public ArrayIndexOutOfBoundsException(int index) {
		super("index: " + index);
	}
}
