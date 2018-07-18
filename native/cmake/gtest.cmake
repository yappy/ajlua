cmake_minimum_required(VERSION 3.6)

# Download and unpack googletest at configure time
configure_file(
	"${CMAKE_SOURCE_DIR}/cmake/CMakeLists.txt.in"
	"${CMAKE_BINARY_DIR}/googletest-download/CMakeLists.txt")
execute_process(COMMAND "${CMAKE_COMMAND}" "-G" "${CMAKE_GENERATOR}" "."
	RESULT_VARIABLE result
	WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/googletest-download)
if(result)
	message(FATAL_ERROR "CMake step for googletest failed: ${result}")
endif()
execute_process(COMMAND "${CMAKE_COMMAND}" "--build" "."
	RESULT_VARIABLE result
	WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/googletest-download)
if(result)
	message(FATAL_ERROR "Build step for googletest failed: ${result}")
endif()

# workaround https://github.com/google/googletest/issues/1111
if(WIN32)
	set(CMAKE_CXX_FLAGS
		"${CMAKE_CXX_FLAGS} /D_SILENCE_TR1_NAMESPACE_DEPRECATION_WARNING")
endif()

# Prevent overriding the parent project's compiler/linker
# settings on Windows
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
set(BUILD_GTEST ON CACHE BOOL "" FORCE)

# Add googletest directly to our build.
# This defines the gtest and gtest_main targets.
add_subdirectory(
	${CMAKE_BINARY_DIR}/googletest-src
	${CMAKE_BINARY_DIR}/googletest-build
	EXCLUDE_FROM_ALL)
