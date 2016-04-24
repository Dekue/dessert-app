LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := NativeTask
LOCAL_SRC_FILES := nativetask.c

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)