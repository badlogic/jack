class Z {
public:
	virtual void bar();
};

class A : public virtual Z {
public:
	virtual void foo() = 0;
};

class B: public virtual A, public virtual Z {
};

class C: public B, public virtual Z {
	void foo() {
	}
};

void test() {
	C* c = new C();
	B* b = (B*)c;
	b->foo();
}