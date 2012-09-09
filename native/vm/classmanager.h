#ifndef jack_classmanager_h
#define jack_classmanager_h

class java_lang_Class;
class java_lang_String;
#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>
#include "vm/stringmap.h"

class ClassManager: public gc {
private:
	static ClassManager* instance;
	StringMap<java_lang_Class*>* classes;
    
	ClassManager();
public:
	static ClassManager* getInstance();
	java_lang_Class* forName(java_lang_String* name);
	java_lang_Class* forArray(int dimensions, java_lang_Class* elementType);	
};

#endif