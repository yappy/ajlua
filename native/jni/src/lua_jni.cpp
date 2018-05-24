#include <io_github_yappy_LuaEngine.h>
#include <lua.hpp>
#include <memory>
#include "jniutil.h"

/* Lua - Java type assert */
static_assert(sizeof(lua_Number) == sizeof(jdouble), "lua_Number");
static_assert(sizeof(lua_Integer) == sizeof(jlong), "lua_Integer");

/* Lua C define - Java constant assert */
static_assert(LUA_MINSTACK == io_github_yappy_LuaEngine_MAX_STACK,
	"MAX_STACK");
static_assert(LUA_MULTRET == io_github_yappy_LuaEngine_LUA_MULTRET,
	"LUA_MULTRET");

namespace {

	const std::array<const char *, 4> VersionStrList = {
		LUA_VERSION,
		LUA_RELEASE,
		LUA_COPYRIGHT,
		LUA_AUTHORS,
	};
	static_assert(
		VersionStrList.size() == io_github_yappy_LuaEngine_VERSION_ARRAY_SIZE,
		"VERSION_ARRAY_SIZE");

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
	static_assert(LoadLibs.size() == io_github_yappy_LuaEngine_LIB_ID_COUNT,
		"LIB_ID_COUNT");

	/*
	 * Lua panic means that longjmp is not caught by pcall.
	 * It is not expected from this module,
	 * and C++, JNI, Java stack might have been vanished.
	 * (destructor, cleanup local ref would not work in that case)
	 * It is dangerous situation and unrecoverable.
	 * So shutdown the JVM with FatalError.
	 */
	JNIEnv *env_for_panic = nullptr;
	int panic_handler(lua_State *L) {
		if (env_for_panic != nullptr) {
			env_for_panic->FatalError("unprotected error in lua");
		}
		return 0;
	}

	class Lua {
	public:
		static const int PROXY_UPVALUE_COUNT = 1;
		static const int PROXY_UPVALUE_IND_ID = 1;

		Lua(JNIEnv *env) :
			m_env(env),
			m_hook(nullptr, jniutil::GlobalRefDeleter(env)),
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
			jboolean ret = lua->m_env->CallBooleanMethod(
				lua->m_hook.get(), method, ar->event, ar->currentline);

			// If false, raise lua error to abort pcall
			if (!ret) {
				luaL_error(L, "execution aborted");
			}
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
			// get from upvalue
			auto id = static_cast<jint>(
				lua_tointeger(L, lua_upvalueindex(PROXY_UPVALUE_IND_ID)));

			// Java interface call
			jmethodID method = jniutil::GetMethodId(
				jniutil::MethodId::FunctionRoot_call);
			int ret = lua->m_env->CallIntMethod(
				lua->m_callback.get(), method, id);
			// exception check
			jthrowable ex = lua->m_env->ExceptionOccurred();
			if (ex == nullptr) {
				// no exception
				return ret;
			}
			else {
				// call Throwable#getMessage()
				jmethodID method = jniutil::GetMethodId(
					jniutil::MethodId::Throwable_getMessage);
				jstring jmsg = static_cast<jstring>(
					lua->m_env->CallObjectMethod(ex, method));

				std::unique_ptr<char[]> msg = nullptr;
				if (jmsg != nullptr) {
					msg = jniutil::JstrToChars(lua->m_env, jmsg);
					if (msg == nullptr) {
						jniutil::ThrowOutOfMemoryError(
							lua->m_env, "Native heap");
						// longjmp
						return lua_error(L);
					}
				}
				const char *cmsg = (msg != nullptr) ? msg.get() : "";

				jclass clsRE = jniutil::FindClass(
					jniutil::ClassId::RuntimeException);
				jclass clsError = jniutil::FindClass(jniutil::ClassId::Error);
				if (lua->m_env->IsInstanceOf(ex, clsRE) ||
					lua->m_env->IsInstanceOf(ex, clsError)) {
					// RuntimeException or Error
					// Do not clear exception status
					// longjmp to pcall point
					return luaL_error(L, "%s", cmsg);
				}
				else {
					// other Exceptions
					// catch exception
					lua->m_env->ExceptionClear();
					// longjmp to pcall point
					return luaL_error(L, "%s", cmsg);
				}
			}
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
		jniutil::GlobalRef m_hook;
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
		return LUA_MINSTACK - lua_gettop(L) >= n;
	}

}

/*
 * Export Functions
 */
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    getVersionInfo
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_getVersionInfo
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
 * Class:     io_github_yappy_LuaEngine
 * Method:    newPeer
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_io_github_yappy_LuaEngine_newPeer
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
 * Class:     io_github_yappy_LuaEngine
 * Method:    deletePeer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_LuaEngine_deletePeer
  (JNIEnv *, jclass, jlong peer)
{
	delete reinterpret_cast<Lua *>(peer);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    setDebugHook
 * Signature: (JLio/github/yappy/DebugHook;)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_LuaEngine_setDebugHook
  (JNIEnv *, jclass, jlong peer, jobject hook)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	lua->SetDebugHook(hook);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    setHookMask
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_LuaEngine_setHookMask
  (JNIEnv *, jclass, jlong peer, jint mask, jint count)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();
	lua_sethook(L, Lua::Hook, mask, count);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    openLibs
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_openLibs
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
		return 0;
	};
	// cfunc
	lua_pushcfunction(L, f);
	// arg1:
	lua_pushinteger(L, bits);
	// longjmp_safe call (args=1, ret=0)
	return lua_pcall(L, 1, 0, 0);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    loadString
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_loadString
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
 * Class:     io_github_yappy_LuaEngine
 * Method:    getTop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_getTop
  (JNIEnv *, jclass, jlong peer)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	return lua_gettop(L);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    setTop
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_LuaEngine_setTop
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
 * Class:     io_github_yappy_LuaEngine
 * Method:    pushValues
 * Signature: (J[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_pushValues
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

		jclass clsBoolean = jniutil::FindClass(jniutil::ClassId::Boolean);
		jclass clsNumber = jniutil::FindClass(jniutil::ClassId::Number);

		for (int i = 0; i < length; i++) {
			jobject jobj = env->GetObjectArrayElement(values, i);

			if (jobj == nullptr) {
				lua_pushnil(L);
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
			else {
				jniutil::ThrowIllegalArgumentException(env, "Invalid type");
			}

			env->DeleteLocalRef(jobj);
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
	// longjmp_safe call (args=3, ret=length)
	return lua_pcall(L, 3, length, 0);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    getValues
 * Signature: (J[B[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_getValues
  (JNIEnv *env, jclass, jlong peer, jbyteArray types, jobjectArray values)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	int num = std::min(lua_gettop(L), LUA_MINSTACK);
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
				env->DeleteLocalRef(box);
			}
			break;
		case LUA_TNUMBER:
			{
				jdouble jd = lua_tonumber(L, lind);
				jobject box = jniutil::BoxingDouble(env, jd);
				env->SetObjectArrayElement(values, i, box);
				env->DeleteLocalRef(box);
			}
			break;
		case LUA_TSTRING:
			{
				jstring jstr = env->NewStringUTF(lua_tostring(L, lind));
				env->SetObjectArrayElement(values, i, jstr);
				env->DeleteLocalRef(jstr);
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
				env->SetObjectArrayElement(values, i, box);
				env->DeleteLocalRef(box);
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
 * Class:     io_github_yappy_LuaEngine
 * Method:    pcall
 * Signature: (JIII)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_pcall
  (JNIEnv *env, jclass, jlong peer, jint nargs, jint nresults, jint msgh)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	return lua_pcall(L, nargs, nresults, 0);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    getGlobal
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_getGlobal
  (JNIEnv *env, jclass, jlong peer, jstring name)
{
	auto L = reinterpret_cast<Lua *>(peer)->L();

	if (!HasFreeStack(L, 2)) {
		jniutil::ThrowIllegalStateException(env, "Stack overflow");
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
	// longjmp_safe call (args=1, ret=1)
	return lua_pcall(L, 1, 1, 0);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    setGlobal
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_setGlobal
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
	// longjmp_safe call (args=2, ret=0)
	return lua_pcall(L, 2, 0, 0);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    setProxyCallback
 * Signature: (JLio/github/yappy/FunctionRoot;)V
 */
JNIEXPORT void JNICALL Java_io_github_yappy_LuaEngine_setProxyCallback
  (JNIEnv *, jclass, jlong peer, jobject callback)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	lua->SetProxyCallback(callback);
}

/*
 * Class:     io_github_yappy_LuaEngine
 * Method:    pushProxyFunction
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_io_github_yappy_LuaEngine_pushProxyFunction
  (JNIEnv *env, jclass, jlong peer, jint id)
{
	auto lua = reinterpret_cast<Lua *>(peer);
	auto L = lua->L();

	// cfunction + upvalue
	if (!HasFreeStack(L, 1 + Lua::PROXY_UPVALUE_COUNT)) {
		jniutil::ThrowIllegalStateException(env, "Stack overflow");
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
	// longjmp_safe call (args=1, ret=1)
	return lua_pcall(L, 1, 1, 0);
}


const jint USE_VNI_VERSION = JNI_VERSION_1_2;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
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

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
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
