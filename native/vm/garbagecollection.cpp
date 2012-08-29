#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>
#include "vm/garbagecollection.h"

void jack_gc_init() {
	GC_INIT();
}

int jack_gc_heapSize() {
	return GC_get_heap_size();
}

void* jack_gc_malloc(int size) {
	return GC_MALLOC(size);
}

