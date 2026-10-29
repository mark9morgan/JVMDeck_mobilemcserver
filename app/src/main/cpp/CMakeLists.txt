cmake_minimum_required(VERSION 3.22.1)
project(notbi C)
add_library(notbi SHARED notbi.c)
target_link_options(notbi PRIVATE -Wl,-z,max-page-size=16384)
