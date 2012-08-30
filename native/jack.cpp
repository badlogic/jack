#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>

#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include "vm/time.h"
#include "vm/garbagecollection.h"

int main() {
	jack_gc_init();
	jack_init();	

	jack_Primes* primes = new jack_Primes();
	primes->m_init();
	j_long sum = 0;
	j_long start = getCurrentTimeMillis();
	for(int i = 0; i < 50000; i++) {
		sum += primes->m_next();
		if(i % 1000 == 0) printf("%lld\n", sum);
	}
	printf("%lld, %f\n", sum, (getCurrentTimeMillis() - start) / 1000.f);	

	for(int i = 0; i < 10000000; i++) {		
		jack_ArrayTest* at = new jack_ArrayTest();
		at->m_init();
		
		if(i % 10000 == 0) {
			printf("heap size %d\n", jack_gc_heapSize());
		}
	}
}