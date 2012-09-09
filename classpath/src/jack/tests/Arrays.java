package jack.tests;

public class Arrays {
	int[] ia = new int[10];
	
	public void arrays() {
		byte[] ba = new byte[120];
		ba[20] = 20;
		int d = ba[20];
		d = ba[d];
		ia[0] = d;
		
		float[][] fa = new float[20][20];
		int a = 13, b = 12, c = 13;
		Object[][][] oa = new Object[a][b][c];
		fa[0][1] = 123.3f;
		oa[0][0][0] = null;
		
		byte[] i = { 0, 1, 2, 3, 4 };
		String[] s = { "hello", "world" };
		String[][] o = {{"hello"}, {"world"}};
	}
}
