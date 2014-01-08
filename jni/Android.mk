
#./configure --sdk-path=/Users/egergo/Downloads/android-ndk-r9b --target=armv7-android-gcc --disable-examples --enable-vp8 --disable-vp9 --enable-realtime-only --enable-error-concealment --disable-neon
#~/Code/sunny-evening/jni/libvpx
#svn checkout http://libyuv.googlecode.com/svn/trunk/ libyuv

WORKING_DIR := $(call my-dir)
LOCAL_PATH := $(WORKING_DIR)

# build libyuv.a
include $(CLEAR_VARS)
LOCAL_PATH := $(WORKING_DIR)
include $(WORKING_DIR)/libyuv/Android.mk


# build libvpx.a
include $(CLEAR_VARS)
LOCAL_PATH := $(WORKING_DIR)
include $(LOCAL_PATH)/libvpx/build/make/Android.mk


# build libvpx-bindings
include $(CLEAR_VARS)
LOCAL_LIBVPX_PATH := $(WORKING_DIR)/libvpx
LOCAL_PATH := $(WORKING_DIR)
include $(LOCAL_PATH)/libvpx-bindings/Android.mk




