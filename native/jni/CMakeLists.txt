cmake_minimum_required(VERSION 2.6)

project(jlua)

# JNI header (javah) dir and files
set(JNICLASSDIR "${PROJECT_SOURCE_DIR}/../../java/liblua/build/classes/java/main")
set(JNIINC "${CMAKE_CURRENT_SOURCE_DIR}/jniinc")
# class name to javah
set(JNICLASS "io.github.yappy.lua.LuaEngine")
# class file path for depandency
string (REPLACE "." "/" JNICLASS_SLASH "${JNICLASS}")
set(JNICLASSFILE "${JNICLASSDIR}/${JNICLASS_SLASH}.class")
# javah output file name
string(REPLACE "." "_" JNICLASS_BAR "${JNICLASS}")
set(JNIHEADER "${JNIINC}/${JNICLASS_BAR}.h")

# source files
file(GLOB SRCS "src/*.cpp")

# dynamic library target
add_library(${PROJECT_NAME} SHARED ${SRCS})
target_include_directories(${PROJECT_NAME} PRIVATE ${JNIINC})
target_link_libraries(${PROJECT_NAME} "lua")

# install to /java gradle build dir
install(TARGETS ${PROJECT_NAME}
	DESTINATION "${PROJECT_SOURCE_DIR}/../../java/app/build/natives")
install(TARGETS ${PROJECT_NAME}
	DESTINATION "${PROJECT_SOURCE_DIR}/../../java/liblua/build/natives")

# javah target
# `make javah` to update JNI header files
add_custom_target(javah
	SOURCES "${JNIHEADER}"
	COMMAND "$ENV{JAVA_HOME}/bin/javah"
			"-d" "${JNIINC}"
			"-classpath" "${JNICLASSDIR}"
			"${JNICLASS}"
	DEPENDS "${JNICLASSFILE}")
