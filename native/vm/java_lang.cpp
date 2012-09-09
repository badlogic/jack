#include "classes/classes.h"
#include "vm/classmanager.h"

java_lang_Class* java_lang_Class::m_forName(java_lang_String* name) {	
	return ClassManager::getInstance()->forName(name);
}

java_lang_Class* java_lang_Class::m_forArray(int dimensions, java_lang_Class* elementType) {	
	return ClassManager::getInstance()->forArray(dimensions, elementType);
}

void java_lang_Class::m_addClass(java_lang_Class* clazz) {
	ClassManager::getInstance()->addClass(clazz);
}

j_int java_lang_Object::m_hashCode() {
	return (j_int)this;
}