#include <io_github_yappy_lua_LuaEngine.h>
#include <lua.h>
#include <lualib.h>
#include <lauxlib.h>
#include <memory>

namespace {
	struct LuaDeleter {
		void operator()(lua_State *L)
		{
			lua_close(L);
		}
	};
	using LuaPtr = std::unique_ptr<lua_State, LuaDeleter>;
}

/*
 * Export Functions
 */
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    testDestructor
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_testDestructor
  (JNIEnv *, jclass)
{
	LuaPtr lua(luaL_newstate());
	if (lua == nullptr) {
		return -1;
	}
	auto L = lua.get();

	const int AssignValue = 0xabcd;
	class TestClass {
	public:
		explicit TestClass(int *ptr) : m_ptr(ptr) {}
		~TestClass() { *m_ptr = AssignValue; }
	private:
		int *m_ptr;
	};
	lua_CFunction f = [](lua_State *L) -> int
	{
		TestClass testobj(static_cast<int *>(lua_touserdata(L, 1)));
		// If Lua is compiled as C++ properly, throw C++ exception.
		// testobj's destructor should be called.
		lua_error(L);
		return 0;
	};

	int testvar = 0;
	// call f(&testvar)
	lua_pushcfunction(L, f);
	lua_pushlightuserdata(L, &testvar);
	int result = lua_pcall(L, 1, 0, 0);
	if (result != LUA_ERRRUN) {
		return -2;
	}
	// testvar should be changed by destructor
	if (testvar != AssignValue) {
		return -3;
	}

	// OK
	return 0;
}

#ifdef __cplusplus
}
#endif
