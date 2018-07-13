cmake_minimum_required(VERSION 2.6)

# Linux and Windows build root
enable_testing()

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(MSVC)
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /W4")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /W4")
else()
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
endif()

# for jni.h and jni_md.h
if(ANDROID)
	# use sysroot/usr/include/jni.h
else()
	# use $JAVA_HOME/include
	include_directories("$ENV{JAVA_HOME}/include")
	if(WIN32)
		include_directories("$ENV{JAVA_HOME}/include/win32")
	elseif(UNIX)
		include_directories("$ENV{JAVA_HOME}/include/linux")
	endif()
endif()

add_subdirectory(lua)
add_subdirectory(jni)
