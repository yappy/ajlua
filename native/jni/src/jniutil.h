#pragma once

#include <jni.h>
#include <memory>

namespace jniutil {

	enum class ClassId {
		DebugHook,
		FunctionRoot,
		Number,
		Boolean,
		Long,
		Double,
		Throwable,
		RuntimeException,
		Error,
		ClassCacheNum,
	};
	enum class MethodId {
		DebugHook_hook,
		FunctionRoot_call,
		Number_doubleValue,
		Boolean_valueOf,
		Boolean_booleanValue,
		Long_valueOf,
		Double_valueOf,
		Throwable_getMessage,
		MethodCacheNum,
	};

	bool CacheAllClass(JNIEnv *env);
	bool CacheAllMethod(JNIEnv *env);
	void ClearAllCache(JNIEnv *env);
	jclass FindClass(ClassId id);
	jmethodID GetMethodId(MethodId id);


	struct GlobalRefDeleter {
		explicit GlobalRefDeleter(JNIEnv *env) : m_env(env) {}
		void operator()(jobject p)
		{
			m_env->DeleteGlobalRef(p);
		}
	private:
		JNIEnv *m_env;
	};

	using GlobalRef = std::unique_ptr<
		std::remove_pointer<jobject>::type, GlobalRefDeleter>;


	inline jobject BoxingBoolean(JNIEnv *env, jbyte jb)
	{
		jclass cls = FindClass(ClassId::Boolean);
		jmethodID method = GetMethodId(MethodId::Boolean_valueOf);
		env->CallStaticObjectMethod(cls, method, jb);
	}
	inline jobject BoxingLong(JNIEnv *env, jlong jl)
	{
		jclass cls = FindClass(ClassId::Long);
		jmethodID method = GetMethodId(MethodId::Long_valueOf);
		env->CallStaticObjectMethod(cls, method, jl);
	}
	inline jobject BoxingDouble(JNIEnv *env, jdouble jd)
	{
		jclass cls = FindClass(ClassId::Double);
		jmethodID method = GetMethodId(MethodId::Double_valueOf);
		env->CallStaticObjectMethod(cls, method, jd);
	}

	/*
	 * jstring -> const char *
	 * Return nullptr if failed.
	 */
	inline std::unique_ptr<char[]>
	JstrToChars(JNIEnv *env, jstring jstr, jsize *outlen = nullptr)
	{
		if (jstr == nullptr) {
			return std::unique_ptr<char[]>(nullptr);
		}
		jsize len = env->GetStringUTFLength(jstr);
		char *p = new(std::nothrow) char[len + 1];
		if (p == nullptr) {
			return std::unique_ptr<char[]>(nullptr);
		}
		std::unique_ptr<char[]> buf(p);
		env->GetStringUTFRegion(jstr, 0, len, buf.get());
		buf[len] = '\0';

		if (outlen != nullptr) {
			*outlen = len;
		}
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

	inline void ThrowIllegalStateException(JNIEnv *env, const char *msg)
	{
		Throw(env, "java/lang/IllegalStateException", msg);
	}

} // namespace jniutil
