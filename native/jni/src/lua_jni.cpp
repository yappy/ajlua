#include <memory>
#include <LuaEngine.h>
#include <lua.hpp>
#include "jniutil.h"

/* Lua - Java type assert */
static_assert(sizeof(lua_Number) == sizeof(jdouble), "lua_Number");
static_assert(sizeof(lua_Integer) == sizeof(jlong), "lua_Integer");


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

	auto lua = reinterpret_cast<Lua *>(peer);
	auto L = lua->L();

	// text only
	return luaL_loadbufferx(L, cBuf.get(), buflen, cChunkName.get(), "t");
}

#ifdef __cplusplus
}
#endif
