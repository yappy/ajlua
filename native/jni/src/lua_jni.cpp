#include <io_github_yappy_lua_LuaEngine.h>
#include <lua.h>
#include <lualib.h>
#include <lauxlib.h>
#include <array>
#include <memory>
#include "jniutil.h"

/* Lua - Java type assert */
static_assert(sizeof(lua_Number) == sizeof(jdouble), "lua_Number");
static_assert(sizeof(lua_Integer) == sizeof(jlong), "lua_Integer");

/* Lua C define - Java constant assert */
static_assert(LUA_MULTRET == io_github_yappy_lua_LuaEngine_LUA_MULTRET,
	"LUA_MULTRET");

namespace {

	const std::array<const char *, 4> VersionStrList = {
		LUA_VERSION,
		LUA_RELEASE,
		LUA_COPYRIGHT,
		LUA_AUTHORS,
	};
	static_assert(VersionStrList.size() ==
		io_github_yappy_lua_LuaEngine_VERSION_ARRAY_SIZE, "VERSION_ARRAY_SIZE");

	const std::array<luaL_Reg, 10> LoadLibs = {{
		{"_G", luaopen_base},
		{LUA_LOADLIBNAME, luaopen_package},
		{LUA_COLIBNAME, luaopen_coroutine},
		{LUA_TABLIBNAME, luaopen_table},
		{LUA_IOLIBNAME, luaopen_io},
		{LUA_OSLIBNAME, luaopen_os},
		{LUA_STRLIBNAME, luaopen_string},
		{LUA_MATHLIBNAME, luaopen_math},
		{LUA_UTF8LIBNAME, luaopen_utf8},
		{LUA_DBLIBNAME, luaopen_debug}
	}};
	static_assert(LoadLibs.size() == io_github_yappy_lua_LuaEngine_LIB_ID_COUNT,
		"LIB_ID_COUNT");

	/*
	 * Lua panic means that longjmp/throw destination does not exist.
	 * It is not expected by this module.
	 * So shutdown the JVM with FatalError.
	 */
	JNIEnv *env_for_panic = nullptr;
	int panic_handler(lua_State *) {
		if (env_for_panic != nullptr) {
			// never return
			env_for_panic->FatalError("unprotected error in lua");
		}
		// call abort()
		return 0;
	}

	class Lua {
	public:
		static const int PROXY_UPVALUE_COUNT = 1;
		static const int PROXY_UPVALUE_IND_ID = 1;

		Lua(JNIEnv *env) :
			m_env(env),
			m_hook(nullptr, jniutil::GlobalRefDeleter(env)),
			m_print(nullptr, jniutil::GlobalRefDeleter(env)),
			m_callback(nullptr, jniutil::GlobalRefDeleter(env))
		{}
		~Lua() = default;

		bool Initialize(size_t memoryLimit)
		{
			// custom allocator param
			m_memoryLimit = memoryLimit;
			// initialize with custom allocator
			m_lua.reset(lua_newstate(Alloc, this));
			if (m_lua == nullptr) {
				return false;
			}

			// set panic handler
			env_for_panic = m_env;
			lua_atpanic(m_lua.get(), panic_handler);

			// set this at extraspace
			void *extra = lua_getextraspace(m_lua.get());
			*static_cast<Lua **>(extra) = this;

			return true;
		}

		lua_State *L()
		{
			return m_lua.get();
		}

		void SetDebugHook(jobject hook)
		{
			// Create global ref to hook
			// It will be deleted when overwritten or delete Lua
			jobject global = m_env->NewGlobalRef(hook);
			if (global == nullptr) {
				jniutil::ThrowOutOfMemoryError(m_env, "NewGlobalRef");
				return;
			}
			m_hook.reset(global);
		}

		static void Hook(lua_State *L, lua_Debug *ar)
		{
			Lua *lua = FromExtraSpace(L);

			// Java interface call
			jmethodID method = jniutil::GetMethodId(
				jniutil::MethodId::DebugHook_hook);
			lua->m_env->CallVoidMethod(
				lua->m_hook.get(), method, ar->event, ar->currentline);

			// LuaAbortException, RuntimeException, Error
			if (lua->m_env->ExceptionCheck()) {
				// jump to pcall point
				lua_error(L);
			}
		}

		void SetPrintFunction(jobject print)
		{
			// Create global ref to callback
			// It will be deleted when overwritten or delete Lua
			jobject global = m_env->NewGlobalRef(print);
			if (global == nullptr) {
				jniutil::ThrowOutOfMemoryError(m_env, "NewGlobalRef");
				return;
			}
			m_print.reset(global);
		}

		static int Print(lua_State *L)
		{
			Lua *lua = FromExtraSpace(L);
			JNIEnv *env = lua->m_env;

			// Java interface methods
			jmethodID idWriteString = jniutil::GetMethodId(
				jniutil::MethodId::LuaPrint_writeString);
			jmethodID idWriteLine = jniutil::GetMethodId(
				jniutil::MethodId::LuaPrint_writeLine);

			auto writeString = [lua, L, env, idWriteString]
					(const char *str, size_t) -> void {
				jstring jstr = env->NewStringUTF(str);
				if (jstr == nullptr) {
					if (env->ExceptionCheck()) {
						// OutOfMemoryError
						// jump to pcall point
						lua_error(L);
					}
					// Invalid UTF-8 string: ignore
					return;
				}
				env->CallVoidMethod(lua->m_print.get(), idWriteString, jstr);
				env->DeleteLocalRef(jstr);
				// RuntimeException or Error
				if (lua->m_env->ExceptionCheck()) {
					// jump to pcall point
					lua_error(L);
				}
			};
			auto writeLine = [lua, L, env, idWriteLine]() -> void {
				env->CallVoidMethod(lua->m_print.get(), idWriteLine);
				// RuntimeException or Error
				if (env->ExceptionCheck()) {
					// jump to pcall point
					lua_error(L);
				}
			};

			// see also: luaB_print() in lbaselib.c
			// number of arguments
			int n = lua_gettop(L);
			lua_getglobal(L, "tostring");
			for (int i = 1; i <= n; i++) {
				const char *s;
				size_t l;
				//  function to be called
				lua_pushvalue(L, -1);
				// value to print
				lua_pushvalue(L, i);
				lua_call(L, 1, 1);
				// get result
				s = lua_tolstring(L, -1, &l);
				if (s == nullptr) {
					return luaL_error(L,
						"'tostring' must return a string to 'print'");
				}
				if (i > 1) {
					writeString("\t", 1);
				}
				writeString(s, l);
				lua_pop(L, 1);  /* pop result */
			}
			writeLine();
			// luaB_print() end

			return 0;
		}

		void SetProxyCallback(jobject callback)
		{
			// Create global ref to callback
			// It will be deleted when overwritten or delete Lua
			jobject global = m_env->NewGlobalRef(callback);
			if (global == nullptr) {
				jniutil::ThrowOutOfMemoryError(m_env, "NewGlobalRef");
				return;
			}
			m_callback.reset(global);
		}

		static int ProxyFunction(lua_State *L)
		{
			// get from extraspace
			Lua *lua = FromExtraSpace(L);
			JNIEnv *env = lua->m_env;
			// get from upvalue
			auto id = static_cast<jint>(
				lua_tointeger(L, lua_upvalueindex(PROXY_UPVALUE_IND_ID)));

			// Java interface call: FunctionRoot#call()
			jmethodID method = jniutil::GetMethodId(
				jniutil::MethodId::FunctionRoot_call);
			int ret = env->CallIntMethod(
				lua->m_callback.get(), method, id);
			// exception check
			jthrowable ex = env->ExceptionOccurred();
			if (ex == nullptr) {
				// no exception
				return ret;
			}

			// exception occured!
			env->ExceptionClear();

			// call Throwable#getMessage()
			jmethodID methodGetMessage = jniutil::GetMethodId(
				jniutil::MethodId::Throwable_getMessage);
			jstring jmsg = static_cast<jstring>(
				env->CallObjectMethod(ex, methodGetMessage));

			std::unique_ptr<char[]> msg = nullptr;
			if (jmsg != nullptr) {
				msg = jniutil::JstrToChars(env, jmsg);
				if (msg == nullptr) {
					jniutil::ThrowOutOfMemoryError(env, "Native heap");
					// jump to pcall point
					return lua_error(L);
				}
			}
			const char *cmsg = (msg != nullptr) ? msg.get() : "";

			jclass clsLRE = jniutil::FindClass(
				jniutil::ClassId::LuaRuntimeException);
			if (env->IsInstanceOf(ex, clsLRE)) {
				// LuaRuntimeException
				// treat as lua error (msg = ex.getMessage())
				// jump to pcall point
				return luaL_error(L, "%s", cmsg);
			}
			else {
				// other Exceptions (including RuntimeException or Error)
				// don't catch here
				// (set exception state again, preserve stack trace)
				// jump to pcall point
				env->Throw(ex);
				return lua_error(L);
			}
		}

		void SetOriginalPcall(lua_CFunction pcall)
		{
			m_pcall = pcall;
		}

		static int Pcall(lua_State *L)
		{
			Lua *lua = FromExtraSpace(L);
			JNIEnv *env = lua->m_env;

			int result = lua->m_pcall(L);

			if (env->ExceptionCheck()) {
				return lua_error(L);
			}

			return result;
		}

		static Lua *FromExtraSpace(lua_State *L)
		{
			return *static_cast<Lua **>(lua_getextraspace(L));
		}

	private:
		struct LuaDeleter {
			void operator()(lua_State *L)
			{
				lua_close(L);
			}
		};
		std::unique_ptr<lua_State, LuaDeleter> m_lua;

		JNIEnv *m_env;
		size_t m_memoryLimit;
		lua_CFunction m_pcall;
		jniutil::GlobalRef m_hook;
		jniutil::GlobalRef m_print;
		jniutil::GlobalRef m_callback;

		static void *Alloc(void *ud, void *ptr, size_t osize, size_t nsize)
		{
			auto lua = static_cast<Lua *>(ud);
			if (nsize == 0) {
				// free oldsize
				lua->m_memoryLimit += osize;
				std::free(ptr);
				return nullptr;
			}
			else {
				if (ptr == nullptr) {
					// malloc newsize
					if (lua->m_memoryLimit < nsize) {
						return nullptr;
					}
					lua->m_memoryLimit -= nsize;
					return std::malloc(nsize);
				}
				else {
					// realloc oldsize -> newsize
					if (nsize > osize) {
						if (lua->m_memoryLimit < nsize - osize) {
							return nullptr;
						}
						lua->m_memoryLimit -= nsize - osize;
					}
					else {
						lua->m_memoryLimit += osize - nsize;
					}
					return std::realloc(ptr, nsize);
				}
			}
		}
	};


	inline bool HasStack(lua_State *L, int n)
	{
		return lua_gettop(L) >= n;
	}

	inline bool HasFreeStack(lua_State *L, int n)
	{
		return lua_checkstack(L, n);
	}

	// might longjmp() or throw C++ exception
	// BUG: lua stack check
	void pushJavaValue(lua_State *L, JNIEnv *env, jobject jobj)
	{
		// for table seti
		if (!HasFreeStack(L, 2)) {
			jniutil::ThrowIllegalStateException(env, "stack overflow");
			return;
		}

		jclass clsArray = jniutil::FindClass(jniutil::ClassId::ObjectArray);
		jclass clsBoolean = jniutil::FindClass(jniutil::ClassId::Boolean);
		jclass clsNumber = jniutil::FindClass(jniutil::ClassId::Number);
		jclass clsString = jniutil::FindClass(jniutil::ClassId::String);

		if (jobj == nullptr) {
			lua_pushnil(L);
		}
		else if (env->IsInstanceOf(jobj, clsArray)) {
			// get array length and push table (pre-allocated [1..length])
			jobjectArray jarray = static_cast<jobjectArray>(jobj);
			int length = env->GetArrayLength(jarray);
			lua_createtable(L, length, 0);
			for (int i = 0; i < length; i++) {
				// push value (Java array dimension <= 255)
				jobject jelem = env->GetObjectArrayElement(jarray, i);
				pushJavaValue(L, env, jelem);
				env->DeleteLocalRef(jelem);
				if (env->ExceptionCheck()) {
					break;
				}
				// table[i + 1] = value (pop value)
				// Lua is 1-origin
				lua_seti(L, -2, i + 1);
			}
		}
		else if (env->IsInstanceOf(jobj, clsBoolean)) {
			jmethodID method = jniutil::GetMethodId(
				jniutil::MethodId::Boolean_booleanValue);
			jboolean b = env->CallBooleanMethod(jobj, method);
			lua_pushboolean(L, b);
		}
		else if (env->IsInstanceOf(jobj, clsNumber)) {
			jmethodID method = jniutil::GetMethodId(
				jniutil::MethodId::Number_doubleValue);
			jdouble d = env->CallDoubleMethod(jobj, method);
			lua_pushnumber(L, d);
		}
		else if (env->IsInstanceOf(jobj, clsString)) {
			auto cstr = jniutil::JstrToChars(env, static_cast<jstring>(jobj));
			if (cstr != nullptr) {
				lua_pushstring(L, cstr.get());
			}
			else {
				lua_pushnil(L);
			}
		}
		else {
			jniutil::ThrowIllegalArgumentException(env, "Invalid type");
		}
	}

}

/*
 * Export Functions
 */
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    getVersionInfo
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_getVersionInfo
  (JNIEnv *env, jclass, jobjectArray info)
{
	jsize index = 0;
	for (auto str : VersionStrList) {
		jstring jstr = env->NewStringUTF(str);
		env->SetObjectArrayElement(info, index, jstr);
		index++;
	}
	return static_cast<jint>(*lua_version(nullptr));
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    newPeer
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_io_github_yappy_lua_LuaEngine_newPeer
  (JNIEnv *env, jclass, jlong nativeMemoryLimit)
{
	Lua *lua = new(std::nothrow) Lua(env);
	if (lua == nullptr) {
		// new failed; out of memory
		return 0;
	}
	if (!lua->Initialize(nativeMemoryLimit)) {
		// lua_newstate failed; out of memory
		delete lua;
		return 0;
	}
	return reinterpret_cast<jlong>(lua);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    deletePeer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_lua_LuaEngine_deletePeer
  (JNIEnv *, jclass, jlong peer)
{
	delete reinterpret_cast<Lua *>(peer);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setDebugHook
 * Signature: (JLio/github/yappy/lua/DebugHook;)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_lua_LuaEngine_setDebugHook
  (JNIEnv *, jclass, jlong peer, jobject hook)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	lua->SetDebugHook(hook);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setHookMask
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_lua_LuaEngine_setHookMask
  (JNIEnv *, jclass, jlong peer, jint mask, jint count)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();
	lua_sethook(L, Lua::Hook, mask, count);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    openLibs
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_openLibs
  (JNIEnv *, jclass, jlong peer, jint libs)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();
	uint32_t bits = static_cast<uint32_t>(libs);

	// arg1: bits(uint32)
	// ret: none
	lua_CFunction f = [](lua_State *L) -> int
	{
		uint32_t bits = lua_tointeger(L, 1);
		for (uint32_t id = 0; id < LoadLibs.size(); id++) {
			if (bits & (1U << id)) {
				luaL_requiref(L, LoadLibs[id].name, LoadLibs[id].func, 1);
				lua_pop(L, 1);  /* remove lib */
			}
		}
		// replace pcall
		lua_getglobal(L, "pcall");
		lua_CFunction org = lua_tocfunction(L, -1);
		lua_pop(L, 1);
		if (org != nullptr) {
			auto lua = Lua::FromExtraSpace(L);
			lua->SetOriginalPcall(org);
			lua_pushcfunction(L, Lua::Pcall);
			lua_setglobal(L, "pcall");
		}
		return 0;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1:
	lua_pushinteger(L, bits);
	// lua error safe call (args=1, ret=0)
	return lua_pcall(L, 1, 0, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    replacePrintFunc
 * Signature: (JLio/github/yappy/lua/LuaPrint;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_replacePrintFunc
  (JNIEnv *env, jclass, jlong peer, jobject print)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	auto L = lua->L();

	if (!HasFreeStack(L, 1)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	lua->SetPrintFunction(print);

	lua_CFunction f = [](lua_State *L) -> int
	{
		// replace global "print" with Lua::Print()
		lua_pushcfunction(L, Lua::Print);
		lua_setglobal(L, "print");
		return 0;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// lua error safe call (args=0, ret=0)
	return lua_pcall(L, 0, 0, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    loadString
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_loadString
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
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    getTop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_getTop
  (JNIEnv *, jclass, jlong peer)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	return lua_gettop(L);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setTop
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_lua_LuaEngine_setTop
  (JNIEnv *env, jclass, jlong peer, jint index)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (index >= 0) {
		if (!lua_checkstack(L, index)) {
			jniutil::ThrowIllegalArgumentException(env, "stack overflow");
			return;
		}
	}
	else {
		if (index < -lua_gettop(L)) {
			jniutil::ThrowIllegalArgumentException(env, "new top too small");
			return;
		}
	}
	lua_settop(L, index);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    pushValues
 * Signature: (J[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_pushValues
  (JNIEnv *env, jclass, jlong peer, jobjectArray values)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (values == nullptr) {
		jniutil::ThrowNullPointerException(env, "values");
		return 0;
	}
	int length = env->GetArrayLength(values);
	if (!HasFreeStack(L, length) || !HasFreeStack(L, 4)) {
		jniutil::ThrowIllegalArgumentException(env, "values length too large");
		return 0;
	}

	// arg1: JNIEnv *env
	// arg2: jobjectArray values
	// arg3: length
	// ret: lua values (multiret)
	auto f = [](lua_State *L) -> int
	{
		auto env = static_cast<JNIEnv *>(lua_touserdata(L, 1));
		auto values = static_cast<jobjectArray>(lua_touserdata(L, 2));
		auto length = static_cast<int>(lua_tonumber(L, 3));
		lua_settop(L, 0);

		for (int i = 0; i < length; i++) {
			jobject jobj = env->GetObjectArrayElement(values, i);
			pushJavaValue(L, env, jobj);
			env->DeleteLocalRef(jobj);
			if (env->ExceptionCheck()) {
				break;
			}
		}

		return length;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1: JNIEnv *env
	lua_pushlightuserdata(L, env);
	// arg2: jobjectArray values
	lua_pushlightuserdata(L, values);
	// arg3: length
	lua_pushinteger(L, length);
	// lua error safe call (args=3, ret=length)
	return lua_pcall(L, 3, length, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    getValues
 * Signature: (J[B[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_getValues
  (JNIEnv *env, jclass, jlong peer, jbyteArray types, jobjectArray values)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (types == nullptr) {
		jniutil::ThrowNullPointerException(env, "types");
		return 0;
	}
	if (values == nullptr) {
		jniutil::ThrowNullPointerException(env, "values");
		return 0;
	}
	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	int num = lua_gettop(L);
	jsize length = env->GetArrayLength(types);
	if (length > num) {
		jniutil::ThrowIllegalArgumentException(env, "larger than stack size");
		return 0;
	}
	std::unique_ptr<jbyte[]> ctypes{new(std::nothrow) jbyte[length]};

	using Params = std::tuple<
		JNIEnv *, jbyteArray, jobjectArray, jbyte *, jsize>;
	Params params = std::make_tuple(env, types, values, ctypes.get(), length);

	auto f = [](lua_State *L) -> int
	{
		// pop param tuple pointer
		const auto &params = *static_cast<Params *>(lua_touserdata(L, -1));
		lua_pop(L, 1);
		JNIEnv *env = std::get<0>(params);
		jbyteArray types = std::get<1>(params);
		jobjectArray values = std::get<2>(params);
		jbyte *ctypes = std::get<3>(params);
		jsize length = std::get<4>(params);

		// set to Java array elements and return them as is
		int num = lua_gettop(L);

		for (int i = 0; i < length; i++) {
			int lind = num - length + i + 1;

			ctypes[i] = lua_type(L, lind);
			switch (ctypes[i]) {
			case LUA_TNIL:
			{
				env->SetObjectArrayElement(values, i, nullptr);
				if (env->ExceptionCheck()) goto EXIT;
				break;
			}
			case LUA_TBOOLEAN:
			{
				jboolean jb = static_cast<jboolean>(lua_toboolean(L, lind));
				jobject box = jniutil::BoxingBoolean(env, jb);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, box);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(box);
				break;
			}
			case LUA_TNUMBER:
			{
				jdouble jd = lua_tonumber(L, lind);
				jobject box = jniutil::BoxingDouble(env, jd);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, box);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(box);
				break;
			}
			case LUA_TSTRING:
			{
				// might cause lua error
				const char *cstr = lua_tostring(L, lind);
				jstring jstr = env->NewStringUTF(cstr);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, jstr);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(jstr);
				break;
			}
			case LUA_TTABLE:
			case LUA_TFUNCTION:
			case LUA_TLIGHTUSERDATA:
			case LUA_TUSERDATA:
			case LUA_TTHREAD:
			{
				jlong jl = reinterpret_cast<jlong>(lua_topointer(L, lind));
				jobject box = jniutil::BoxingLong(env, jl);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, box);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(box);
				break;
			}
			default:
				jniutil::ThrowInternalError(env, "Unknown type");
				goto EXIT;
			}
		}
		env->SetByteArrayRegion(types, 0, length, ctypes);
		if (env->ExceptionCheck()) {
			goto EXIT;
		}
EXIT:
		return num;
	};

	// cfunc into stack bottom
	lua_pushcfunction(L, f);
	lua_insert(L, 1);
	// args: original stack, moreover, params
	lua_pushlightuserdata(L, &params);
	// lua error safe call (args=num+1(params), ret=num)
	return lua_pcall(L, num + 1, num, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    getCheckedValues
 * Signature: (J[I[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_getCheckedValues
  (JNIEnv *env, jclass, jlong peer, jintArray checks, jobjectArray values)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (checks == nullptr) {
		jniutil::ThrowNullPointerException(env, "checks");
		return 0;
	}
	if (values == nullptr) {
		jniutil::ThrowNullPointerException(env, "values");
		return 0;
	}
	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	jsize length = env->GetArrayLength(checks);
	std::unique_ptr<jint[]> cchecks{new(std::nothrow) jint[length]};
	env->GetIntArrayRegion(checks, 0, length, cchecks.get());

	// JNIEnv *env, const jint *cchecks, jobjectArray values, jsize length
	using Params = std::tuple<JNIEnv *, const jint *, jobjectArray, jsize>;
	Params params = std::make_tuple(env, cchecks.get(), values, length);

	// arg[1..x-1]: original stack
	// arg[x]: params tuple
	// ret: original stack (multiret)
	auto f = [](lua_State *L) -> int
	{
		// pop param tuple pointer
		const auto &params = *static_cast<Params *>(lua_touserdata(L, -1));
		lua_pop(L, 1);
		JNIEnv *env = std::get<0>(params);
		const jint *cchecks = std::get<1>(params);
		jobjectArray values = std::get<2>(params);
		jsize length = std::get<3>(params);

		// set to Java array elements and return them as is
		for (jsize i = 0; i < length; i++) {
			int lind = i + 1;
			// treat "not exist" as nil
			bool valid = (lind <= lua_gettop(L));
			if (!valid || lua_isnil(L, lind)) {
				if (cchecks[i] &
					io_github_yappy_lua_LuaEngine_CHECK_OPT_ALLOW_NIL) {
					env->SetObjectArrayElement(values, i, nullptr);
					if (env->ExceptionCheck()) goto EXIT;
					continue;
				}
				else {
					// jump to pcall point
					luaL_error(L, "bad argument #%d (argument needed)", lind);
				}
			}
			// copy and push (the value may be converted)
			lua_pushvalue(L, lind);
			switch (cchecks[i] &
				io_github_yappy_lua_LuaEngine_CHECK_TYPE_MASK) {
			case io_github_yappy_lua_LuaEngine_CHECK_TYPE_BOOLEAN:
			{
				int val = lua_toboolean(L, -1);
				jobject jobj = jniutil::BoxingBoolean(env, val);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, jobj);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(jobj);
				break;
			}
			case io_github_yappy_lua_LuaEngine_CHECK_TYPE_INTEGER:
			{
				int isnum = 0;
				lua_Integer val = lua_tointegerx(L, -1, &isnum);
				if (!isnum) {
					// jump to pcall point
					luaL_error(L, "bad argument #%d (integer needed)", lind);
				}
				jobject jobj = jniutil::BoxingLong(env, val);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, jobj);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(jobj);
				break;
			}
			case io_github_yappy_lua_LuaEngine_CHECK_TYPE_NUMBER:
			{
				int isnum = 0;
				lua_Number val = lua_tonumberx(L, -1, &isnum);
				if (!isnum) {
					// jump to pcall point
					luaL_error(L, "bad argument #%d (number needed)", lind);
				}
				jobject jobj = jniutil::BoxingDouble(env, val);
				if (env->ExceptionCheck()) goto EXIT;
				env->SetObjectArrayElement(values, i, jobj);
				if (env->ExceptionCheck()) goto EXIT;
				env->DeleteLocalRef(jobj);
				break;
			}
			case io_github_yappy_lua_LuaEngine_CHECK_TYPE_STRING:
			{
				const char *cstr = lua_tostring(L, -1);
				if (cstr == nullptr) {
					// jump to pcall point
					luaL_error(L, "bad argument #%d (string needed)", lind);
				}
				jstring jstr = env->NewStringUTF(cstr);
				// jstr might be nullptr if OOM or invalid utf
				env->SetObjectArrayElement(values, i, jstr);
				if (env->ExceptionCheck()) {
					return 0;
				}
				if (jstr != nullptr) {
					env->DeleteLocalRef(jstr);
				}
				break;
			}
			default:
				jniutil::ThrowIllegalArgumentException(env, "checks");
				return 0;
			}
			// pop copy
			lua_pop(L, 1);
		}
EXIT:
		return lua_gettop(L);
	};
	// current values count on the stack
	int orgSize = lua_gettop(L);
	// cfunc into stack bottom
	lua_pushcfunction(L, f);
	lua_insert(L, 1);
	// arg_last: param tuple
	lua_pushlightuserdata(L, &params);
	// lua error safe call (args=orgSize+tuple, ret=orgSize)
	return lua_pcall(L, orgSize + 1, orgSize, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    pushNewTable
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_pushNewTable
  (JNIEnv *env, jclass, jlong peer, jint narr, jint nrec)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();
	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	using Params = std::tuple<jint, jint>;
	Params params = std::make_tuple(narr, nrec);

	auto f = [](lua_State *L) -> int
	{
		// pop param tuple pointer
		const auto &params = *static_cast<Params *>(lua_touserdata(L, -1));
		lua_pop(L, 1);
		jint narr = std::get<0>(params);
		jint nrec = std::get<1>(params);

		// might cause memory error
		lua_createtable(L, narr, nrec);
		return 1;
	};
	lua_pushcfunction(L, f);
	lua_pushlightuserdata(L, &params);
	// lua error safe call (args=1, ret=1)
	return lua_pcall(L, 1, 1, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setTableField
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_setTableField
  (JNIEnv *env, jclass, jlong peer, jstring key)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();
	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}
	auto cKey = jniutil::JstrToChars(env, key);
	if (cKey == nullptr) {
		jniutil::ThrowOutOfMemoryError(env, "Native heap");
		return 0;
	}

	using Params = std::tuple<const char *>;
	Params params = std::make_tuple(cKey.get());

	auto f = [](lua_State *L) -> int
	{
		// params, table, value
		// get param tuple pointer
		const auto &params = *static_cast<Params *>(lua_touserdata(L, 1));
		const char *cKey = std::get<0>(params);

		// table(2)[cKey] = value(3) (pop value)
		lua_setfield(L, 2, cKey);
		// return table
		return 1;
	};
	// (bottom) table, value (top)
	lua_pushcfunction(L, f);
	lua_insert(L, -3);
	// f, table, value
	lua_pushlightuserdata(L, &params);
	lua_insert(L, -3);
	// f, params, table, value
	// lua error safe call (args=3, ret=1(table))
	return lua_pcall(L, 3, 1, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    pcall
 * Signature: (JIII)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_pcall
  (JNIEnv *, jclass, jlong peer, jint nargs, jint nresults, jint msgh)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	/*
	 * (setjmp() or try) and call lua function
	 * pcall could call Lua::ProxyFunc or Lua::Hook.
	 *
	 * They could:
	 * - set Java exception status and (longjmp() or C++ throw) with lua_error()
	 * -- LuaAbortException (checked: declared explicitly in Java code)
	 * -- RuntimeException (unchecked)
	 * -- Error (unchecked)
	 * - longjmp() (with luaL_error() = push string + lua_error())
	 * -- It is caused by LuaRuntimeException but exception status is cleared.
	 *
	 * lua_pcall() will return LUA_ERRRUN in both cases,
	 * but if exception status is set, a Java exception will be thrown and
	 * LUA_ERRRUN return code will be invisible from Java code.
	 *
	 * If exception status is set, almost all JNI functions must never be used.
	 * (Android VM will crash)
	 * I should return to Java code as soon as possible
	 * if a Java exception is active.
	 */
	// return to Java code without calling JNI functions
	// even if (longjmp() or throw-catch) is taken.
	return lua_pcall(L, nargs, nresults, msgh);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    getGlobal
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_getGlobal
  (JNIEnv *env, jclass, jlong peer, jstring name)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	jsize len = 0;
	auto cName = jniutil::JstrToChars(env, name, &len);
	if (cName == nullptr) {
		jniutil::ThrowOutOfMemoryError(env, "Native heap");
		return 0;
	}

	// arg1: const char *name
	// ret: getglobal result
	lua_CFunction f = [](lua_State *L) -> int
	{
		// push ret
		lua_getglobal(L, static_cast<const char *>(lua_touserdata(L, 1)));
		// remove arg1
		lua_remove(L, 1);
		return 1;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1: const char *name
	lua_pushlightuserdata(L, cName.get());
	// lua error safe call (args=1, ret=1)
	return lua_pcall(L, 1, 1, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setGlobal
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_setGlobal
  (JNIEnv *env, jclass, jlong peer, jstring name)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (!HasStack(L, 1)) {
		jniutil::ThrowIllegalStateException(env, "Stack underflow");
		return 0;
	}

	jsize len = 0;
	auto cName = jniutil::JstrToChars(env, name, &len);
	if (cName == nullptr) {
		jniutil::ThrowOutOfMemoryError(env, "Native heap");
		return 0;
	}

	// arg1: const char *name
	// arg2: value
	// ret: none
	lua_CFunction f = [](lua_State *L) -> int
	{
		lua_setglobal(L, static_cast<const char *>(lua_touserdata(L, 1)));
		lua_pop(L, 1);
		return 0;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1: const char *name
	lua_pushlightuserdata(L, cName.get());
	// arg2: value (original top)
	lua_rotate(L, lua_absindex(L, -3), -1);
	// lua error safe call (args=2, ret=0)
	return lua_pcall(L, 2, 0, 0);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    setProxyCallback
 * Signature: (JLio/github/yappy/lua/FunctionRoot;)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_lua_LuaEngine_setProxyCallback
  (JNIEnv *, jclass, jlong peer, jobject callback)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	lua->SetProxyCallback(callback);
}

/*
 * Class:     io_github_yappy_lua_LuaEngine
 * Method:    pushProxyFunction
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_lua_LuaEngine_pushProxyFunction
  (JNIEnv *env, jclass, jlong peer, jint id)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	auto L = lua->L();

	// cfunction + upvalue
	if (!HasFreeStack(L, 1 + Lua::PROXY_UPVALUE_COUNT)) {
		jniutil::ThrowIllegalStateException(env, "stack overflow");
		return 0;
	}

	lua_CFunction f = [](lua_State *L) -> int
	{
		// pop arg1
		// push Lua::proxy with arg1 as upvalue
		lua_pushcclosure(L, Lua::ProxyFunction, Lua::PROXY_UPVALUE_COUNT);
		return 1;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1: id
	lua_pushinteger(L, id);
	// lua error safe call (args=1, ret=1)
	return lua_pcall(L, 1, 1, 0);
}


const jint USE_VNI_VERSION = JNI_VERSION_1_2;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /*reserved*/)
{
	JNIEnv *env;
	if (vm->GetEnv(reinterpret_cast<void **>(&env), USE_VNI_VERSION) < 0) {
		return JNI_ERR;
	}
	if (!jniutil::CacheAllClass(env)) {
		return JNI_ERR;
	}
	if (!jniutil::CacheAllMethod(env)) {
		return JNI_ERR;
	}
	return USE_VNI_VERSION;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void * /*reserved*/)
{	JNIEnv *env;
	if (vm->GetEnv(reinterpret_cast<void **>(&env), USE_VNI_VERSION) < 0) {
		// give up
		return;
	}
	jniutil::ClearAllCache(env);
}

#ifdef __cplusplus
}
#endif
