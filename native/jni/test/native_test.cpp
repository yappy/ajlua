#include <gtest/gtest.h>
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

	inline LuaPtr make_lua()
	{
		return LuaPtr(luaL_newstate());
	}
}

/*
 * Lua error + C++ destructor test
 */
TEST(NativeTest, LuaErrorAndCppDestructor)
{
	LuaPtr lua = make_lua();
	ASSERT_NE(nullptr, lua);
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
	ASSERT_EQ(LUA_ERRRUN, result);

	// testvar should be changed by the destructor
	ASSERT_EQ(AssignValue, testvar);
}
