#include "classes/classes.h"
#include <stdio.h>

struct test$_ {
	int a;
};

class Test {
public: 
	Test() {
	}

	void run() {
	}
private:
	int a;
};

int main(int argc, char** argv) {
	jack_B obj;
	obj.a = 123;
	printf("%d\n", sizeof(jack_A));
	printf("%d\n", sizeof(Test));
	return 0;
}