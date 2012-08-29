#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#define GC_THREADS
#include <gc.h>
#include <gc_cpp.h>

int main() {
	GC_INIT();
	jack_init();		

	jack_ArrayTest* at = 0;

	for(int i = 0; i < 100000000; i++) {
		java_lang_Object* obj = new java_lang_Object();
		obj->m_init();
		
		at = new jack_ArrayTest();
		at->m_init();
		
		if(i % 10000 == 0) {
			printf("heap size %d\n", GC_get_heap_size());
		}
	}
}