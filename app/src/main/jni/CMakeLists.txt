project(mtcnn)
cmake_minimum_required(VERSION 3.10)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv-mobile-3.4.15-android/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20210720-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)


set(Common_DIR ${CMAKE_SOURCE_DIR}/face)



#编译为动态库
add_library(mtcnn SHARED mtcnn.cpp mtcnn_jni.cpp)


target_link_libraries(mtcnn VisionEngine ncnn ${OpenCV_LIBS} camera2ndk mediandk )
add_subdirectory(face)