#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>

#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include "vm/garbagecollection.h"

template <class T>
class Test: public java_lang_Object {
};

int main() {
	jack_gc_init();
	jack_init();		

	jack_ArrayTest* at = 0;

	for(int i = 0; i < 10000000; i++) {
		java_lang_Object* obj = new java_lang_Object();
		obj->m_init();
		
		at = new jack_ArrayTest();
		at->m_init();
		
		if(i % 10000 == 0) {
			printf("heap size %d\n", jack_gc_heapSize());
		}
	}
}