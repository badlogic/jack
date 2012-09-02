#ifndef vm_vmarray_h
#define vm_vmarray_h

#include "classes/java_lang_Object.h"
#include "vm/garbagecollection.h"

template <class T>
class Array: public java_lang_Object {
public:
	Array(int size, bool isPrimitive) {		
		this->length = size;
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

	Array(T* elements, int size) {
		this->length = size;
		this->elements = elements;
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
private:
	T* elements;	
};

#endif