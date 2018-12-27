##############################################################################
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MANIFEST_FILE := src/main/AndroidManifest.xml

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/src/main/res

src_dirs := src/main/java/

LOCAL_SRC_FILES := src/main/aidl/com/common/logservice/ILogService.aidl


LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 mqttv3 framework

LOCAL_SRC_FILES += $(call all-java-files-under, $(src_dirs))

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := AutoLogService

LOCAL_CERTIFICATE := platform

LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard-rules.pro

LOCAL_DX_FLAGS := --multi-dex --main-dex-list=$(mainDexList) --minimal-main-dex

LOCAL_JACK_FLAGS += --multi-dex native

include $(BUILD_PACKAGE)

############################################################################
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := mqttv3:libs/org.eclipse.paho.client.mqttv3.jar
include $(BUILD_MULTI_PREBUILT)


