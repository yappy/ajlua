#include <lua.hpp>
#include <LuaNative.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     LuaNative
 * Method:    testMethod
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_LuaNative_testMethod
  (JNIEnv *, jclass)
{
	return 12345;
}

#ifdef __cplusplus
}
#endif
