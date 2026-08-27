cmake_minimum_required(VERSION 3.22.1)
project(xiangqi_engine)

add_library(xiangqi_engine SHARED xiangqi_engine.cpp)
target_compile_features(xiangqi_engine PRIVATE cxx_std_20)
target_compile_options(xiangqi_engine PRIVATE -O3 -ffast-math -fvisibility=hidden)
