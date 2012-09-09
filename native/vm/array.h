#ifndef vm_vmarray_h
#define vm_vmarray_h

#include "classes/java_lang_Object.h"
#include "vm/garbagecollection.h"
#include "classes/java_lang_Class.h"

template <class T>
class Array: public java_lang_Object {
public:
	Array(int size, bool isPrimitive, int dimensions, java_lang_Class** elementType) {		
		this->length = size;
		this->isPrimitive = isPrimitive;
		this->dimensions = dimensions;
		this->elementType = elementType;
		this->arrayClass = 0;

		if(size > 0) {
			if(isPrimitive) {
				this->elements = (T*)jack_gc_malloc(sizeof(T) * size);
			} else {
				this->elements = (T*)jack_gc_malloc_atomic(sizeof(T) * size);
			}
		} else {
			this->elements = 0;
		}
	}

	Array(T* elements, int size, bool isPrimitive, int dimensions, java_lang_Class** elementType) {
		this->length = size;
		this->elements = elements;
		this->isPrimitive = isPrimitive;		
		this->dimensions = dimensions;
		this->elementType = elementType;
	}

	java_lang_Class* m_getClass() {
		if(arrayClass == 0) {
			arrayClass = java_lang_Class::m_forArray(dimensions, *elementType);
		}
		return arrayClass;
	}

	inline T get(int idx) {
		// TODO add bounds check		
		return elements[idx];
	}

	inline T set(int idx, T val) {
		// TODO add bounds check		
		elements[idx] = val;
	}

	inline T& operator[] (int idx) {
		return elements[idx];
	}

	int length;
	bool isPrimitive;
private:
	T* elements;
	int dimensions;
	java_lang_Class** elementType;
	java_lang_Class* arrayClass;
};

#endif