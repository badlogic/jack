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
	virtual bool isEmpty() = 0;
};

class AbstractList: public virtual AbstractCollection, public virtual List {
public:
	virtual bool isEmpty() {
		return AbstractCollection::isEmpty();
	}
};

void test() {
	Base* obj = new AbstractList();
	List* list = dynamic_cast<List*>(obj);
}
