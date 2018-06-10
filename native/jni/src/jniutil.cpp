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
		{ nullptr, "io/github/yappy/lua/DebugHook"		},
		{ nullptr, "io/github/yappy/lua/LuaPrint"		},
		{ nullptr, "io/github/yappy/lua/FunctionRoot"	},
		{ nullptr, "java/lang/String"					},
		{ nullptr, "java/lang/Number"					},
		{ nullptr, "java/lang/Boolean"					},
		{ nullptr, "java/lang/Long"						},
		{ nullptr, "java/lang/Double"					},
		{ nullptr, "java/lang/Throwable"				},
		{ nullptr, "java/lang/RuntimeException"			},
		{ nullptr, "java/lang/Error"					},
	};
	static_assert(
		sizeof(s_classCache) / sizeof(s_classCache[0]) ==
			static_cast<size_t>(ClassId::ClassCacheNum),
		"ClassCache num");

	MethodCacheEntry s_methodCache[] = {
		{ nullptr, false,	ClassId::DebugHook,
			"hook",			"(II)V"					},
		{ nullptr, false,	ClassId::LuaPrint,
			"writeString",	"(Ljava/lang/String;)V"	},
		{ nullptr, false,	ClassId::LuaPrint,
			"writeLine",	"()V"					},
		{ nullptr, false,	ClassId::FunctionRoot,
			"call",			"(I)I"					},
		{ nullptr, false,	ClassId::Number,
			"doubleValue",	"()D"					},
		{ nullptr, true,	ClassId::Boolean,
			"valueOf",		"(Z)Ljava/lang/Boolean;"},
		{ nullptr, false,	ClassId::Boolean,
			"booleanValue",	"()Z"					},
		{ nullptr, true,	ClassId::Long,
			"valueOf",		"(J)Ljava/lang/Long;"	},
		{ nullptr, true,	ClassId::Double,
			"valueOf",		"(D)Ljava/lang/Double;"	},
		{ nullptr, false,	ClassId::Throwable,
			"getMessage",	"()Ljava/lang/String;"	},
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
					FindClass(entry.classId),
					entry.methodName, entry.methodSig) :
				env->GetMethodID(
					FindClass(entry.classId),
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

	jclass FindClass(ClassId id)
	{
		return s_classCache[static_cast<int>(id)].classCache;
	}

	jmethodID GetMethodId(MethodId id)
	{
		return s_methodCache[static_cast<int>(id)].methodCache;
	}

}
