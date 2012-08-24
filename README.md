jack
====

Java to C transpiler, ignores memory model and other stuff, uses Boehm GC for extra slowness and GC pauses.

TODO (in order, check FIXME Prio! for each step, e.g. 
System.arraycopy() etc)
- clinit
- string literal initialization (add clinit to every class/interface, create strings first)
- Add Boehm GC
- exceptions (set signal handlers, use setjmp), pain
- threads
- class descriptors and reflection, pain
- JNI, based on class descriptors, pain
- add the rest of Avian's classpath + JNI implementations.
- add unit tests, get some from Avian, see if OpenJDK has anything 
useful.
