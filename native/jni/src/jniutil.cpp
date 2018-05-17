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
		ClassId classId;
		const char *methodName;
		const char *methodSig;
	};

	ClassCacheEntry s_classCache[] = {
		[ClassId::Boolean]	= { nullptr, "Boolean" },
		[ClassId::Long]		= { nullptr, "Long" },
		[ClassId::Double]	= { nullptr, "Double" },
	};

	MethodCacheEntry s_methodCache[] = {
		[MethodId::Boolean_valueOf]	=
		{ nullptr, ClassId::Boolean,	"valueOf", "(Z)Ljava/lang/Boolean;"},
		[MethodId::Long_valueOf]	=
		{ nullptr, ClassId::Long,		"valueOf", "(J)Ljava/lang/Long;"},
		[MethodId::Double_valueOf]	=
		{ nullptr, ClassId::Double,		"valueOf", "(D)Ljava/lang/Double;"},
	};
}
#include <stdio.h>
namespace jniutil {

	jclass FindClass(JNIEnv *env, ClassId id)
	{
		if (s_classCache[id].classCache == nullptr) {
			s_classCache[id].classCache = env->FindClass(
				s_classCache[id].className);
		}
		return s_classCache[id].classCache;
	}

	jmethodID GetMethodId(JNIEnv *env, MethodId id)
	{
		if (s_methodCache[id].methodCache == nullptr) {
			s_methodCache[id].methodCache = env->GetMethodID(
				FindClass(env, s_methodCache[id].classId),
				s_methodCache[id].methodName,
				s_methodCache[id].methodSig);
		}
		fprintf(stderr, "mid=%d %d %s %s\n", id, s_methodCache[id].classId, s_methodCache[id].methodName, s_methodCache[id].methodSig);
		return s_methodCache[id].methodCache;
	}

}
