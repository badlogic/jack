#include "vm/time.h"
#ifdef _MSC_VER
#include <Windows.h>
#else

#endif

j_long getCurrentTimeMillis() {
#ifdef _MSC_VER
	return GetTickCount64();
#else
	return 0;
#endif
}