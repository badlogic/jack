#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>
#include <gc.h>

int main() {	
	jack_init();
	java_lang_Object* obj = new java_lang_Object();

	GC_init();
	GC_gcollect();
}