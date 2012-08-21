#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include <windows.h>

int arithmetic() {
		j_byte b = 1;
		j_short s = 1;
		j_char c = 1;
		j_int i = 1;
		j_long l = 1;
		j_float a = 1;
		j_double d = 1;

		j_double r = b + c * i / l;
		r = fmod(r, d);
		r = -r;

		j_int shift = s << b;
		shift = s >> b;
		shift = ((unsigned short)s) >> b;
		return (int)(r + shift);
}

int main() {
	jack_Main main;

	Array<int>* a = new Array<int>(20);
	(*a)[0] = 123;
	j_int c = (*a)[0] + 1;
	(*a)[0] = c;
	printf("%d\n", (*a)[0]);	

	jack_Arrays* arrays = new jack_Arrays();
	arrays->init();
	arrays->arrays();

	DWORD start = timeGetTime();
	jack_Primes* primes = new jack_Primes();
	primes->init();

	long long sum = 0;
	for(int i = 0; i < 100000; i++) {
		sum += primes->next();
		if(i % 1000 == 0) printf("%d: %f, %lu\n", i, (timeGetTime() - start) / 1000.f, sum);
	}
	float took = (timeGetTime() - start) / 1000.f;
	printf("%lu\n", sum);
	printf("%f\n", took);
	return 0;
}