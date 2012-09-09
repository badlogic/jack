#include "vm/classmanager.h"
#include "classes/classes.h"

ClassManager* ClassManager::instance = 0;

ClassManager* ClassManager::getInstance() {
	if(!instance) {
		instance = new ClassManager();
	}
	return instance;
}

ClassManager::ClassManager() {
	this->classes = new StringMap<java_lang_Class*>(256);
}

java_lang_Class* ClassManager::forArray(java_lang_String* arrayName) {
	int dimensions = 0;
	while(true) {
		if(this->classes->charAt(arrayName, dimensions) == '[') {
			dimensions++;
		} else {
			break;
		}
	}

	java_lang_Class* elementType = 0;
	bool isPrimitive = true;
	Array<j_char>* typeName = 0;
	java_lang_String* typeNameStr = 0;
	switch(this->classes->charAt(arrayName, dimensions)) {
	case 'Z': 
		elementType = java_lang_Boolean::f_TYPE; 
		break;
	case 'B': 
		elementType = java_lang_Byte::f_TYPE; 
		break;
	case 'C': 
		elementType = java_lang_Character::f_TYPE; 
		break;
	case 'D': 
		elementType = java_lang_Double::f_TYPE; 
		break;
	case 'F': 
		elementType = java_lang_Float::f_TYPE; 
		break;
	case 'I': 
		elementType = java_lang_Integer::f_TYPE; 
		break;
	case 'J': 
		elementType = java_lang_Long::f_TYPE; 
		break;
	case 'S': 
		elementType = java_lang_Short::f_TYPE; 
		break;
	case 'L': 
		isPrimitive = false;
		typeName = new Array<j_char>(arrayName->f_length - dimensions - 2, true, 1, &java_lang_Character::f_TYPE);
		for(int i = 0; i < arrayName->f_length - dimensions - 2; i++) {
			j_char c = arrayName->m_charAt(i + dimensions + 1);
			if(c == '/') c = '.';
			typeName->set(i, c);
		}
		typeNameStr = new java_lang_String();
		typeNameStr->m_init();
		typeNameStr->f_length = typeName->length;
		typeNameStr->f_data = typeName;
		elementType = classes->get(typeNameStr);
		if(!elementType) {
			throw new java_lang_ClassNotFoundException();
		}
		break;
	default: throw new java_lang_ClassNotFoundException();
	}

	java_lang_Class* clazz = new java_lang_Class();
	clazz->m_init();
	clazz->f_name = arrayName;
	clazz->f_superClass = java_lang_Object::clazz;
	clazz->f_interfaces = new Array<java_lang_Class*>(0, false, 1, &java_lang_Class::clazz);
	clazz->f_isArray = true;
	clazz->f_componentType = elementType;

	return clazz;
}

java_lang_Class* ClassManager::forName(java_lang_String* name) {
	if(name == 0) throw new java_lang_ClassNotFoundException();
	java_lang_Class* clazz = instance->classes->get(name);
	if(clazz == 0) {
		if(classes->charAt(name, 0) == '[') {
			java_lang_Class* clazz = forArray(name);
			classes->put(name, clazz);
			return clazz;
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

void ClassManager::addClass(java_lang_Class* clazz) {
	this->classes->put(clazz->f_name, clazz);
}