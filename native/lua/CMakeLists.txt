cmake_minimum_required(VERSION 2.6)

project(lua)

set(SRCDIR "${CMAKE_CURRENT_SOURCE_DIR}/lua-5.3.4/src")
file(GLOB SRCS "${SRCDIR}/*.c")
list(REMOVE_ITEM SRCS "${SRCDIR}/lua.c" "${SRCDIR}/luac.c")

add_library(${PROJECT_NAME} STATIC ${SRCS})
target_include_directories(${PROJECT_NAME} PUBLIC "${SRCDIR}")

# Lua compile define
if(UNIX)
	target_compile_definitions(${PROJECT_NAME} PRIVATE "LUA_USE_POSIX")
endif()
