#ifndef vm_vmarray_h
#define vm_vmarray_h

#include "classes/java_lang_Object.h"
#include <string.h>

template <class T>
class Array: public java_lang_Object {
public:
	Array(int size) {
		this->length = size;
		if(size > 0) {
			this->elements = new T[size];
			memset(this->elements, 0, sizeof(T) * size);
		} else {
			this->elements = 0;
		}
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