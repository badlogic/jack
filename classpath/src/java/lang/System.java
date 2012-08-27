package java.lang;

public class System {
	public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
		// FIXME vm
	}
	
	public static String getProperty(String key) {
		// FIXME vm
		if(key.equals("line.separator")) return "\n";
		return null;
	}

	public static long currentTimeMillis() {
		// FIXME vm
		return 0;
	}
	
	public static int identityHashCode(Object x) {
		// FIXME vm
		return 0;
	}
}
