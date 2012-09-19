#ifndef jack_garbagecollection_h
#define jack_garbagecollection_h

void jack_gc_init();
int jack_gc_heapSize();
void* jack_gc_malloc(int size);
void* jack_gc_malloc_atomic(int size);

#endif