package jack.tests;

public class Arithmetic {
	public int arithmetic() {
		byte b = 1;
		short s = 1;
		char c = 1;
		int i = 1;
		long l = 1;
		float a = 1;
		double d = 1;
		
		double r = b + c * i / l;
		r %= d;
		r = -r;
		
		int shift = s << b;
		shift = s >> b;
		shift = s >>> b;
		return (int)(r + shift);
	}
}
