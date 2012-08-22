/*
Test for interfaces that specify methods already defined in super interfaces.
Need to make sure any abstract class has a method that invokes the base class
method so c++ doesn't get confused...

Let list declare isEmpty() = 0 to see it fail.

class Base {
	virtual void init() {
	}
};

class Collection: public virtual Base {
public:
	virtual bool isEmpty() = 0;
};

class AbstractCollection: public virtual Base, public virtual Collection {
public:
	virtual bool isEmpty() {
		return false;
	}
};

class List: public virtual Base, public virtual Collection {
public:
	
};

class AbstractList: public virtual AbstractCollection, public virtual List {
public:
};

void test() {
	Base* obj = new AbstractList();
	List* list = dynamic_cast<List*>(obj);
}*/

/*class List {
public:
	virtual void add(void* ptr, int a) = 0;
};

class AbstractCollection: public virtual List {
public:
	virtual void add(void* ptr, int a) = 0;
	virtual void add(void* ptr) {
		add(ptr, 0);
	}
};*/

#include "vm/array.h"


class B {
	virtual ~B() { };
};

class D: public virtual B {
};

void test() {
	Array<D*>* a = new Array<D*>(10);
	Array<B*>* b = (Array<B*>*)a;
	b->set(0, new D());
	b->set(1, new D());
}