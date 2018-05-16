cmake_minimum_required(VERSION 2.6)

project(jlua)

# JNI header (javah) dir and files
set(JNIINC "${CMAKE_CURRENT_BINARY_DIR}/jniinc")
set(JNICLASS "LuaNative")
set(JNIHEADER "${JNIINC}/${JNICLASS}.h")

# source files
file(GLOB SRCS "src/*.cpp")

# library target
add_library(${PROJECT_NAME} SHARED ${SRCS})
add_dependencies(${PROJECT_NAME} javah)
include_directories(${JNIINC})
target_link_libraries(${PROJECT_NAME} "lua")

# javah target
add_custom_target(javah DEPENDS ${JNIHEADER})
add_custom_command(
	OUTPUT "${JNIHEADER}"
	COMMAND "$ENV{JAVA_HOME}/bin/javah"
			"-d" "${JNIINC}"
			"-classpath" "${PROJECT_SOURCE_DIR}/../../java/liblua/build/classes/java/main"
			"${JNICLASS}")
