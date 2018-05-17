#include <memory>
#include <LuaEngine.h>
#include <lua.hpp>
#include "jniutil.h"

/* Lua - Java type assert */
static_assert(sizeof(lua_Number) == sizeof(jdouble), "lua_Number");
static_assert(sizeof(lua_Integer) == sizeof(jlong), "lua_Integer");

/* Lua C define - Java constant assert */
static_assert(LUA_MINSTACK == LuaEngine_MIN_STACK, "LUA_MINSTACK");

class Lua {
public:
	Lua() = default;
	~Lua() = default;

	bool Initialize()
	{
		m_lua.reset(luaL_newstate());
		if (m_lua == nullptr) {
			return false;
		}
		return true;
	}
	lua_State *L()
	{
		return m_lua.get();
	}
private:
	struct LuaDeleter {
		void operator()(lua_State *L)
		{
			lua_close(L);
		}
	};
	std::unique_ptr<lua_State, LuaDeleter> m_lua;
};


#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     LuaEngine
 * Method:    getVersionInfo
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_getVersionInfo
  (JNIEnv *env, jclass, jobjectArray info)
{
	const std::array<const char *, 4> strlist = {
		LUA_VERSION,
		LUA_RELEASE,
		LUA_COPYRIGHT,
		LUA_AUTHORS,
	};
	jsize index = 0;
	for (auto str : strlist) {
		jstring jstr = env->NewStringUTF(str);
		env->SetObjectArrayElement(info, index, jstr);
		index++;
	}
	return static_cast<jint>(*lua_version(nullptr));
}

/*
 * Class:     LuaEngine
 * Method:    newPeer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_LuaEngine_newPeer
  (JNIEnv *, jclass)
{
	Lua *lua = new(std::nothrow) Lua();
	if (lua == nullptr) {
		// new failed; out of memory
		return 0;
	}
	if (!lua->Initialize()) {
		// lua_newstate failed; out of memory
		delete lua;
		return 0;
	}
	return reinterpret_cast<jlong>(lua);
}

/*
 * Class:     LuaEngine
 * Method:    deletePeer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_LuaEngine_deletePeer
  (JNIEnv *, jclass, jlong peer)
{
	delete reinterpret_cast<Lua *>(peer);
}

/*
 * Class:     LuaEngine
 * Method:    loadString
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_loadString
  (JNIEnv *env, jclass, jlong peer, jstring buf, jstring chunkName)
{
	if (buf == nullptr || chunkName == nullptr) {
		jniutil::ThrowNullPointerException(env, "buf and chunkName");
		return 0;
	}

	jsize buflen = 0;
	auto cBuf = jniutil::JstrToChars(env, buf, &buflen);
	if (cBuf == nullptr) {
		jniutil::ThrowOutOfMemoryError(env, "Native heap");
		return 0;
	}
	jsize chunklen = 0;
	auto cChunkName = jniutil::JstrToChars(env, chunkName, &chunklen);
	if (cChunkName == nullptr) {
		jniutil::ThrowOutOfMemoryError(env, "Native heap");
		return 0;
	}

	auto L = reinterpret_cast<Lua *>(peer)->L();

	// text only
	return luaL_loadbufferx(L, cBuf.get(), buflen, cChunkName.get(), "t");
}

/*
 * Class:     LuaEngine
 * Method:    getTop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_getTop
  (JNIEnv *, jclass, jlong peer)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	return lua_gettop(L);
}

/*
 * Class:     LuaEngine
 * Method:    setTop
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_LuaEngine_setTop
  (JNIEnv *env, jclass, jlong peer, jint index)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (index >= 0) {
		if (index > LUA_MINSTACK) {
			jniutil::ThrowIllegalArgumentException(env, "new top too large");
			return;
		}
	}
	else {
		if (-lua_gettop(L) > index) {
			jniutil::ThrowIllegalArgumentException(env, "new top too large");
			return;
		}
	}
	lua_settop(L, index);
}

/*
 * Class:     LuaEngine
 * Method:    pushValues
 * Signature: (J[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_pushValues
  (JNIEnv *, jclass, jlong, jobjectArray);

/*
 * Class:     LuaEngine
 * Method:    getValues
 * Signature: (J[B[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_getValues
  (JNIEnv *env, jclass, jlong peer, jbyteArray types, jobjectArray values)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	int num = lua_gettop(L);
	jbyte ctypes[LUA_MINSTACK];
	for (int i = 0; i < num; i++) {
		int lind = i + 1;
		ctypes[i] = lua_type(L, lind);
		switch (ctypes[i]) {
		case LUA_TNIL:
			env->SetObjectArrayElement(values, i, nullptr);
			break;
		case LUA_TBOOLEAN:
			{
				jboolean jb = static_cast<jboolean>(lua_toboolean(L, lind));
				jobject box = jniutil::BoxingBoolean(env, jb);
				env->SetObjectArrayElement(values, i, box);
			}
			break;
		case LUA_TNUMBER:
			{
				jdouble jd = lua_tonumber(L, lind);
				jobject box = jniutil::BoxingDouble(env, jd);
				env->SetObjectArrayElement(values, i, box);
			}
			break;
		case LUA_TSTRING:
			{
				jstring jstr = env->NewStringUTF(lua_tostring(L, lind));
				env->SetObjectArrayElement(values, i, jstr);
			}
			break;
		case LUA_TTABLE:
		case LUA_TFUNCTION:
		case LUA_TLIGHTUSERDATA:
		case LUA_TUSERDATA:
		case LUA_TTHREAD:
			{
				jlong jl = reinterpret_cast<jlong>(lua_topointer(L, lind));
				jobject box = jniutil::BoxingLong(env, jl);
				//env->SetObjectArrayElement(values, i, box);
			}
			break;
		default:
			jniutil::ThrowInternalError(env, "Unknown type");
			return 0;
		}
	}
	env->SetByteArrayRegion(types, 0, num, ctypes);

	return num;
}

/*
 * Class:     LuaEngine
 * Method:    pcall
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_LuaEngine_pcall
  (JNIEnv *, jclass, jlong, jint, jint);

#ifdef __cplusplus
}
#endif
