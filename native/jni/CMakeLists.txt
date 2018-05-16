cmake_minimum_required(VERSION 2.6)

project(jlua)

# JNI header (javah) dir and files
set(JNIINC "${CMAKE_CURRENT_BINARY_DIR}/jniinc")
set(JNICLASS "LuaNative")
set(JNIHEADER "${JNIINC}/${JNICLASS}.h")

# source files
file(GLOB SRCS "src/*.cpp")

# dynamic library target
add_library(${PROJECT_NAME} SHARED ${SRCS})
add_dependencies(${PROJECT_NAME} javah)
target_include_directories(${PROJECT_NAME} PRIVATE ${JNIINC})
target_link_libraries(${PROJECT_NAME} "lua")

install(TARGETS ${PROJECT_NAME}
	DESTINATION "${PROJECT_SOURCE_DIR}/../../java/app/build/natives")

# javah target
add_custom_target(javah SOURCES ${JNIHEADER})
add_custom_command(
	OUTPUT "${JNIHEADER}"
	COMMAND "$ENV{JAVA_HOME}/bin/javah"
			"-d" "${JNIINC}"
			"-classpath" "${PROJECT_SOURCE_DIR}/../../java/liblua/build/classes/java/main"
			"${JNICLASS}")
