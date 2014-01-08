LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libvpx-bindings

LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(LOCAL_LIBVPX_PATH) \
                    $(LOCAL_LIBVPX_PATH)/vpx_ports \
                    $(LOCAL_PATH)

LOCAL_CFLAGS := -DHAVE_LIBYUV

LOCAL_SRC_FILES := libvpx_com_impl.cc \
                   libvpx_dec_impl.cc \
                   libvpx_enc_config_impl.cc \
                   libvpx_enc_impl.cc

LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES := libyuv_static
LOCAL_SHARED_LIBRARIES := libvpx

include $(BUILD_SHARED_LIBRARY)
