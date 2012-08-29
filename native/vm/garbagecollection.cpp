#define GC_THREADS
#include <gc_cpp.h>


void* gc_malloc(int size) {
	return GC_MALLOC(size);
}