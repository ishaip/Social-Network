cmake_minimum_required(VERSION 3.20)
project(spl_3_c)

set(CMAKE_CXX_STANDARD 14)

add_executable(spl_3_c src/main.cpp include/Client.h src/Client.cpp)
