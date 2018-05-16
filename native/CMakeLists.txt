cmake_minimum_required(VERSION 2.6)

# Linux and Windows build root

# for jni.h and jni_md.h
include_directories("$ENV{JAVA_HOME}/include")
if (WIN32)
	include_directories("$ENV{JAVA_HOME}/include/win32")
endif()
if (UNIX)
	include_directories("$ENV{JAVA_HOME}/include/linux")
endif()

add_subdirectory(lua)
add_subdirectory(jni)
