cmake_minimum_required(VERSION 2.6)

project(jlua)

file(GLOB SRCS "src/*.cpp")

add_library(${PROJECT_NAME} SHARED ${SRCS})
add_dependencies(${PROJECT_NAME} "lua")
