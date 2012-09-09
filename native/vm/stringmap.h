#ifndef jack_stringmap_h
#define jack_stringmap_h

#define GC_THREADS
#define GC_NOT_DLL
#include <gc_cpp.h>
#include "classes/java_lang_String.h"
#include "vm/list.h"

template <class T>
class StringMapCell: public gc {
public:
	java_lang_String* key;
	T value;

	StringMapCell(java_lang_String* key, T value) {
		this->key = key;
		this->value = value;
	}
};

template <class T>
class StringMap: public gc {
private:
	int bucketSize;
	List<List<StringMapCell<T>*>*>* buckets;
public:
	StringMap(int bucketSize) {
		this->bucketSize = bucketSize;
		buckets = new List<List<StringMapCell<T>*>*>();
		for(int i = 0; i < bucketSize; i++) {
			buckets->set(i, new List<StringMapCell<T>*>());
		}
	}

	void put(java_lang_String* key, T value) {
		int hashCode = hash(key) % bucketSize;
		List<StringMapCell<T>*>* bucket = buckets->get(hashCode);
		for(int i = 0; i < bucket->size(); i++) {
			StringMapCell<T>* cell = bucket->get(i);
			if(equals(cell->key, key)) {
				cell->value = value;
				return;
			}
		}
		bucket->add(new StringMapCell<T>(key, value));
	}

	java_lang_Class* get(java_lang_String* key) {
		int hashCode = hash(key) % bucketSize;
		List<StringMapCell<T>*>* bucket = buckets->get(hashCode);
		for(int i = 0; i < bucket->size(); i++) {
			StringMapCell<T>* cell = bucket->get(i);
			if(equals(cell->key, key)) {
				return cell->value;
			}
		}
		return 0;
	}

	unsigned int hash(java_lang_String* str) {
		if(str == 0) return 0;
		if(str->f_hashCode) return str->f_hashCode;

		int hashCode = 0;
		for(int i = 0; i < str->f_length; i++) {
			hashCode = (hashCode * 31) + charAt(str, i);
		}
		str->f_hashCode = hashCode;
		return (unsigned int)hashCode;
	}

	bool equals(java_lang_String* str, java_lang_String* str2) {
		if(!str || !str2) return false;
		if(str->f_length != str2->f_length) return false;
		for(int i = 0; i < str->f_length; i++) {
			if(charAt(str, i) != charAt(str2, i)) return false;
		}
		return true;
	}

	int charAt(java_lang_String* str, int index) {
		if(str == 0) return 0;
		if(dynamic_cast<Array<j_char>*>(str->f_data)) {
			return dynamic_cast<Array<j_char>*>(str->f_data)->get(str->f_offset + index);
		}
		if(dynamic_cast<Array<j_byte>*>(str->f_data)) {
			return dynamic_cast<Array<j_byte>*>(str->f_data)->get(str->f_offset + index);
		}
	}
};

#endif