jack
====

Java to C transpiler, ignores memory model and other stuff, uses Boehm GC for extra slowness and GC pauses.

TODO (in order)
- basic String implementation
- custom "JNI" to implement basic printstream
- implement rest of expression translators
- test arithmetic, should be mostly correct (usigned right shift)
- test inheritance
- enums, not sure, probably pain
- clinit (add static field per class/interface, check on object statements), pain
- exceptions (set signal handlers, use setjmp), pain
- classpath, use avian if possible
- class descriptors and reflection, pain
- JNI, based on class descriptors, pain