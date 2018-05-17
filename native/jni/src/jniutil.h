#pragma once

#include <jni.h>
#include <memory>

namespace jniutil {

	enum ClassId {
		Boolean,
		Long,
		Double,
	};
	enum MethodId {
		Boolean_valueOf,
		Long_valueOf,
		Double_valueOf,
	};

	jclass FindClass(JNIEnv *env, ClassId id);
	jmethodID GetMethodId(JNIEnv *env, MethodId id);


	inline jobject BoxingBoolean(JNIEnv *env, jbyte jb)
	{
		jclass cls = FindClass(env, ClassId::Boolean);
		jmethodID method = GetMethodId(env, MethodId::Boolean_valueOf);
		env->CallStaticObjectMethod(cls, method, jb);
	}
	inline jobject BoxingLong(JNIEnv *env, jlong jl)
	{
		jclass cls = FindClass(env, ClassId::Long);
		jmethodID method = GetMethodId(env, MethodId::Long_valueOf);
		env->CallStaticObjectMethod(cls, method, jl);
	}
	inline jobject BoxingDouble(JNIEnv *env, jdouble jd)
	{
		jclass cls = FindClass(env, ClassId::Double);
		jmethodID method = GetMethodId(env, MethodId::Double_valueOf);
		env->CallStaticObjectMethod(cls, method, jd);
	}

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

	inline void ThrowInternalError(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/InternalError", msg);
	}

	inline void ThrowOutOfMemoryError(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/OutOfMemoryError", msg);
	}

	inline void ThrowNullPointerException(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/NullPointerException", msg);
	}

	inline void ThrowIllegalArgumentException(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/IllegalArgumentException", msg);
	}

} // namespace jniutil
