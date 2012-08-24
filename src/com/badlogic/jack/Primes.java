package com.badlogic.jack;

public class Primes {

	private int[] primes;
	private int len;

	public Primes() {
		this.primes = new int[16];
		this.len = 0;
	}

	public int size() {
		return this.len;
	}

	public int get(int i) {
		while (this.size() <= i)
			this.next();
		return this.primes[i];
	}

	public int next() {
		if (this.len == 0) {
			this.primes[this.len++] = 2;
			return 2;
		}

		int last = this.primes[this.len - 1];
		int test = last;

		outer: while (true) {
			test += 1;

			for (int i = 0; i < this.len; i++)
				if (test % this.primes[i] == 0)
					continue outer;

			if (this.primes.length == this.len) {
				int[] newArray = new int[this.primes.length*2];
				for(int i = 0; i < this.primes.length; i++) {
					newArray[i] = this.primes[i];
				}
				this.primes = newArray;
			}

			this.primes[this.len++] = test;
			return test;
		}
	}
	
	public static void main(String[] args) {
		Primes primes = new Primes();
		long start = System.nanoTime();
		long sum = 0;
		for(int i = 0; i < 10000; i++) {
			sum += primes.next();
			if(i % 1000 == 0) System.out.println(i + ": " + (System.nanoTime() - start) / 1000000000.0f + ", " + sum);
		}
		System.out.println(sum);
		System.out.println((System.nanoTime() - start) / 1000000000.0f);
	}
}