#ifndef jack_list_h
#define jack_list_h

template <class T>
class List: public gc {
private:
	T* elements;
	int numElements;
	int size;
	
public:
	List() {
		numElements = 16;
		size
	}

	int size() {
		return size;
	}

	void add(T t) {
		if(numElements < size + 1) {
			T* newElements = GC_MALLOC
		}
	}

	T get(int index) {
		return elements[index];
	}

	void set(int index, T element) {
		return elements[index];
	}
};

#endif