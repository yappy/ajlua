#pragma once

#include <jni.h>
#include <memory>

namespace jniutil {

	/*
	* jstring -> const char *
	* Return nullptr if failed.
	*/
	inline std::unique_ptr<char[]>
	JstrToChars(JNIEnv *env, jstring jstr, jsize *len)
	{
		if (jstr == nullptr) {
			*len = 0;
			return std::unique_ptr<char[]>(nullptr);
		}
		*len = env->GetStringUTFLength(jstr);
		char *p = new(std::nothrow) char[*len];
		if (p == nullptr) {
			*len = 0;
			return std::unique_ptr<char[]>(nullptr);
		}
		std::unique_ptr<char[]> buf(p);
		env->GetStringUTFRegion(jstr, 0, *len, buf.get());
		return buf;
	}

	inline void Throw(JNIEnv *env, const char *clsName, const char *msg)
	{
		jclass cls = env->FindClass(clsName);
		if (cls == nullptr) {
			env->FatalError("Throwable class not found");
		}
		if (env->ThrowNew(cls, msg) < 0) {
			env->FatalError("ThrowNew failed");
		}
	}

	inline void ThrowOutOfMemoryError(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/OutOfMemoryError", msg);
	}

	inline void ThrowNullPointerException(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/NullPointerException", msg);
	}

} // namespace jniutil
