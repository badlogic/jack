#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include <windows.h>

void foo() {
	throw "test";
}

void bar() {
	foo();
}

int main() {	
	/*jack_Primes* primes = new jack_Primes();
	primes->m_init();
	j_long sum = 0;
	for(int i = 0; i < 10000; i++) {
		sum += primes->m_next();
	}
	printf("%lld\n", sum);
	return 0;*/

	bar();
}