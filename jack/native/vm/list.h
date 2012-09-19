#ifndef jack_list_h
#define jack_list_h

#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>

template <class T>
class List: public gc {
private:
	T* elements;
	int length;
	int numElements;

public:
	List() {
		elements = (T*)GC_MALLOC(16 * sizeof(T));
		length = 0;
		numElements = 16;
	}

	void add(T el) {
		if(length == numElements) {
			T* newElements = (T*)GC_MALLOC(sizeof(T) * length * 2);
			for(int i = 0; i < length; i++) {
				newElements[i] = elements[i];
			}
			elements = newElements;
			numElements *= 2;
		}
		
		elements[length] = el;
		length++;
	}

	T get(int index) {
		if(index < 0 || index >= length) throw new java_lang_IndexOutOfBoundsException();
		return elements[index];
	}

	void set(int index, T el) {
		if(index < 0 || index >= length) throw new java_lang_IndexOutOfBoundsException();
		elements[index] = el;
	}

	int size() {
		return length;
	}
};

#endif