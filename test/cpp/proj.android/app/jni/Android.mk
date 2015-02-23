MY_LOCAL_PATH := $(call my-dir)

LOCAL_PATH := $(MY_LOCAL_PATH)/../../../external/cocos2dx/prebuilt_android
include $(CLEAR_VARS)
LOCAL_MODULE := cocos2dx
LOCAL_SRC_FILES := \
	armeabi/libcocos2dx.a
include $(PREBUILT_STATIC_LIBRARY)


LOCAL_PATH := $(MY_LOCAL_PATH)/../../../../../dist/cpp/android
include $(CLEAR_VARS)
LOCAL_MODULE := safejni
LOCAL_SRC_FILES := \
	$(TARGET_ARCH_ABI)/libsafejni.so
include $(PREBUILT_SHARED_LIBRARY)


LOCAL_PATH := $(MY_LOCAL_PATH)/../../../../../dist/cpp/android
include $(CLEAR_VARS)
LOCAL_MODULE := InAppService
LOCAL_SRC_FILES := \
	$(TARGET_ARCH_ABI)/libInAppService.a
include $(PREBUILT_STATIC_LIBRARY)


LOCAL_PATH := $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	main.cpp \
	../../../src/AppDelegate.cpp \
	../../../src/MainScene.cpp
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../../../external/cocos2dx/include \
    $(LOCAL_PATH)/../../../src \
    $(LOCAL_PATH)/../../../../../src/cpp \

LOCAL_CPPFLAGS := \
	-frtti \
	-fexceptions \
	-std=c++11 \
	-D__GXX_EXPERIMENTAL_CXX0X__ \
	-fsigned-char \
	-DCC_ENABLE_CHIPMUNK_INTEGRATION=0

LOCAL_SHARED_LIBRARIES := \
	safejni

LOCAL_WHOLE_STATIC_LIBRARIES := \
	InAppService \
	cocos2dx

LOCAL_MODULE := game

LOCAL_LDLIBS := \
    -lGLESv1_CM \
	-lGLESv2 \
	-lEGL \
	-llog \
	-lz \
	-lOpenSLES \
	-landroid

include $(BUILD_SHARED_LIBRARY)
