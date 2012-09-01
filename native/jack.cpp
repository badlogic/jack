#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>

#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include "vm/time.h"
#include "vm/garbagecollection.h"

class Arr: public java_lang_Object {
public:
	Arr(int size) {		
		this->length = size;
		if(size > 0) {
			this->elements = (j_int*)jack_gc_malloc_atomic(sizeof(j_int) * size);
		} else {
			this->elements = 0;
		}
	}

	Arr(j_int* elements, int size) {
		this->length = size;
		this->elements = elements;
	}

	inline j_int get(int idx) {
		// TODO add bounds check		
		return elements[idx];
	}

	inline j_int set(int idx, j_int val) {
		// TODO add bounds check		
		elements[idx] = val;
	}

	int length;
private:
	j_int* elements;	
};

class B {
};

class A: public gc {
public:
	B* b;
};

int main() {
	jack_gc_init();
	// jack_init();	

	//jack_Primes* primes = new jack_Primes();
	//primes->m_init();
	//j_long sum = 0;
	//j_long start = getCurrentTimeMillis();
	//for(int i = 0; i < 50000; i++) {
	//	sum += primes->m_next();
	//	if(i % 1000 == 0) printf("%lld\n", sum);
	//}
	//printf("%lld, %f\n", sum, (getCurrentTimeMillis() - start) / 1000.f);	

	for(int i = 0; i < 10000000; i++) {		
		//jack_ArrayTest* at = new jack_ArrayTest();
		//at->m_init();
		
		A* a = new A();

		if(i % 10000 == 0) {
			printf("heap size %d\n", jack_gc_heapSize());
		}
	}
}