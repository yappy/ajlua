#include <memory>
#include <LuaEngine.h>
#include <lua.hpp>

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

#ifdef __cplusplus
}
#endif
