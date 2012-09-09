#include "vm/classmanager.h"
#include "classes/classes.h"

ClassManager* ClassManager::instance = 0;

j_int hashCode(java_lang_String* str) {
	if(str == 0) return 0;
	if(str->f_hashCode) return str->f_hashCode;
	if(dynamic_cast<Array<j_char>*>(str->f_data)) {
		Array<j_char>* data = dynamic_cast<Array<j_char>*>(str->f_data);
		j_int hashCode = 0;

		str->f_hashCode = hashCode;
		return hashCode;
	} 
	
	if(dynamic_cast<Array<j_byte>*>(str->f_data)) {
		Array<j_char>* data = dynamic_cast<Array<j_char>*>(str->f_data);
		j_int hashCode = 0;

		str->f_hashCode = hashCode;
		return hashCode;
	}
}

ClassManager* ClassManager::getInstance() {
	if(!instance) {
		instance = new ClassManager();
	}
	return instance;
}

ClassManager::ClassManager() {
	this->classes = new StringMap<java_lang_Class*>(256);
}

java_lang_Class* ClassManager::forName(java_lang_String* name) {
	java_lang_Class* clazz = instance->classes->get(name);
	if(clazz == 0) {
		if(classes->charAt(name, 0) == '[') {
		} else {
			throw new java_lang_ClassNotFoundException();
		}
	} else {
		return clazz;
	}
}

java_lang_Class* ClassManager::forArray(int dimensions, java_lang_Class* elementType) {
	return 0;
}