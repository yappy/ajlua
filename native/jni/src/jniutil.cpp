#include "jniutil.h"

#include <jni.h>

namespace {
	using namespace jniutil;

	struct ClassCacheEntry {
		jclass classCache;
		const char *className;
	};

	struct MethodCacheEntry {
		jmethodID methodCache;
		bool isStatic;
		ClassId classId;
		const char *methodName;
		const char *methodSig;
	};

	ClassCacheEntry s_classCache[] = {
		{ nullptr, "java/lang/Boolean" },
		{ nullptr, "java/lang/Long" },
		{ nullptr, "java/lang/Double" },
	};
	static_assert(
		sizeof(s_classCache) / sizeof(s_classCache[0]) ==
			static_cast<size_t>(ClassId::ClassCacheNum),
		"ClassCache num");

	MethodCacheEntry s_methodCache[] = {
		{ nullptr, true,	ClassId::Boolean,
			"valueOf", "(Z)Ljava/lang/Boolean;"	},
		{ nullptr, true,	ClassId::Long,
			"valueOf", "(J)Ljava/lang/Long;"	},
		{ nullptr, true,	ClassId::Double,
			"valueOf", "(D)Ljava/lang/Double;"	},
	};
	static_assert(
		sizeof(s_methodCache) / sizeof(s_methodCache[0]) ==
			static_cast<size_t>(MethodId::MethodCacheNum),
		"MethodCache num");
}
#include <stdio.h>
namespace jniutil {

	bool CacheAllClass(JNIEnv *env)
	{
		for (auto &entry : s_classCache) {
			jobject local = env->FindClass(entry.className);
			if (local == nullptr) {
				return false;
			}
			jobject global = env->NewGlobalRef(local);
			if (global == nullptr) {
				return false;
			}
			env->DeleteLocalRef(local);
			entry.classCache = static_cast<jclass>(global);
		}
		return true;
	}

	bool CacheAllMethod(JNIEnv *env)
	{
		for (auto &entry : s_methodCache) {
			jmethodID method = entry.isStatic ?
				env->GetStaticMethodID(
					FindClass(env, entry.classId),
					entry.methodName, entry.methodSig) :
				env->GetMethodID(
					FindClass(env, entry.classId),
					entry.methodName, entry.methodSig);
			if (method == nullptr) {
				return false;
			}
			entry.methodCache = method;
		}
		return true;
	}

	void ClearAllCache(JNIEnv *env)
	{
		for (auto &entry : s_classCache) {
			env->DeleteGlobalRef(entry.classCache);
		}
	}

	jclass FindClass(JNIEnv *env, ClassId id)
	{
		return s_classCache[static_cast<int>(id)].classCache;
	}

	jmethodID GetMethodId(JNIEnv *env, MethodId id)
	{
		return s_methodCache[static_cast<int>(id)].methodCache;
	}

}