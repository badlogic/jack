#include "classes/classes.h"
#include <stdio.h>
#include <string>
#include <math.h>

int main() {	
	jack_init();
	java_lang_Object* obj = new java_lang_Object();
	obj->m_clinit();
	obj->m_init();
}