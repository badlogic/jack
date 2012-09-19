#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>

#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include "vm/time.h"
#include "vm/garbagecollection.h"
#include "vm/stringmap.h"

void testPrimes() {
	jack_tests_Primes* primes = new jack_tests_Primes();
	primes->m_init();
	j_long sum = 0;
	j_long start = getCurrentTimeMillis();
	for(int i = 0; i < 5000; i++) {
		sum += primes->m_next();
		if(i % 1000 == 0) printf("%lld\n", sum);
	}
	printf("%lld, %f\n", sum, (getCurrentTimeMillis() - start) / 1000.f);	
}

void testAllocation() {
	for(int i = 0; i < 10000000; i++) {		
		jack_tests_ArrayTest* at = new jack_tests_ArrayTest();
		at->m_init();

		if(i % 10000 == 0) {
			printf("%d heap size %d\n", i, jack_gc_heapSize());
		}
	}
}

char* lit1 = "this is a test";
char* lit2 = "this is another test";
char* lit3 = "and another test";

void testUtils() {	
	java_lang_String* str1 = new java_lang_String();
	str1->f_data = new Array<j_byte>(lit1, 14, true, 0, 0);
	str1->f_length = 14;

	java_lang_String* str2 = new java_lang_String();
	str2->f_data = new Array<j_byte>(lit2, 20, true, 0, 0);
	str2->f_length = 20;

	java_lang_String* str3 = new java_lang_String();
	str3->f_data = new Array<j_byte>(lit3, 20, true, 0, 0);
	str3->f_length = 16;

	java_lang_Class* c1 = new java_lang_Class();
	java_lang_Class* c2 = new java_lang_Class();

	StringMap<java_lang_Class*>* map = new StringMap<java_lang_Class*>(256);
	map->put(str1, c1);
	map->put(str2, c2);

	printf("%d\n", c1 == map->get(str1));
	printf("%d\n", c2 == map->get(str2));
	printf("%d\n", 0 == map->get(str2));
}

void testArrays() {
	jack_tests_Arrays* obj = new jack_tests_Arrays();
	obj->m_init();
	obj->m_arrays();

	jack_tests_ArrayTest* obj2 = new jack_tests_ArrayTest();
	obj2->m_init();
	obj->m_arrays();
}

void testReflection() {
	jack_tests_Reflection* obj = new jack_tests_Reflection();
	obj->m_init();
	obj->m_test();
}

void testInstanceOf() {
	jack_tests_InstanceOf* obj = new jack_tests_InstanceOf();
	obj->m_init();
	obj->m_test();
}

int main() {	
	jack_gc_init();	
	jack_init();
	testUtils();
	testArrays();
	testReflection();
	testPrimes();
	testInstanceOf();
}