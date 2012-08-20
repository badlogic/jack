#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>

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
	return 0;
}