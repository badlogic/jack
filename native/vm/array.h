#ifndef vm_vmarray_h
#define vm_vmarray_h

template <class T>
class VmArray {
public:
	VmArray(int size) {
		this->size = size;
		this->elements = new T[size];
	}

	T get(int idx) {
		// TODO add bounds check and throw
		// exception via JNIEnv
		return elements[idx];
	}

	T set(int idx, T val) {
		elements[idx] = val;
	}

private:
	T* elements;
	int size;
};

#endif