package java.lang.annotation;

/**
 * Tags classes and/or methods, signaling that the native method implementation(s)
 * are directly done in C++. This allows to circumnavigate JNI for kernel classes
 * like java.lang.Class. Jack will not emit C++ method implementations for
 * any native method tagged with this annotation or any native methods of a class
 * tagged with this annotation. The C++ methods need to be implemented manually
 * to avoid linking errors.
 * 
 * @author mzechner
 *
 */
public @interface DirectNative {

}
