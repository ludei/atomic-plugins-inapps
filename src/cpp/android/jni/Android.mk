LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	InAppServiceAndroid.cpp
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../../ \
    $(LOCAL_PATH)/safejni/
LOCAL_CPPFLAGS := \
	-frtti \
	-fexceptions \
	-std=c++11 \
	-D__GXX_EXPERIMENTAL_CXX0X__
LOCAL_MODULE := InAppService
include $(BUILD_STATIC_LIBRARY)
