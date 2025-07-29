#
# Copyright (C) 2023 The Android Open Source Project
#
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/lenovo/asphalt
KERNEL_PATH := $(DEVICE_PATH)-kernel

# Inherit from sm8475-common
include device/lenovo/sm8475-common/BoardConfigCommon.mk

# Assert
TARGET_OTA_ASSERT_DEVICE := asphalt,asphalt_prc,asphalt_nec,TB320FC

# Display
TARGET_SCREEN_DENSITY := 410

# Kernel
BOARD_VENDOR_KERNEL_MODULES_LOAD += \
    cirrus_wm_adsp_dlkm.ko \
    cirrus_cs35l45_dlkm.ko \
	nt36523-spi.ko

BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD += \
	nt36523-spi.ko

BOOT_KERNEL_MODULES := $(strip $(shell cat $(DEVICE_PATH)/configs/modules/modules.load.recovery))
BOOT_KERNEL_MODULES += \
	nt36523-spi.ko \
    adsp_loader_dlkm.ko \
    gpr_dlkm.ko \
    q6_notifier_dlkm.ko \
    q6_pdr_dlkm.ko \
    qti_battery_charger_main.ko \
    snd_event_dlkm.ko \
    spf_core_dlkm.ko

TARGET_KERNEL_SOURCE := kernel/lenovo/sm8475
TARGET_KERNEL_CONFIG := \
    gki_defconfig \
    vendor/waipio_GKI.config \
    vendor/$(PRODUCT_DEVICE)_GKI.config
KERNEL_LTO := none

BOARD_USES_QCOM_MERGE_DTBS_SCRIPT := true
TARGET_NEEDS_DTBOIMAGE := true

# Kernel modules
TARGET_KERNEL_EXT_MODULE_ROOT := kernel/lenovo/sm8475-modules
TARGET_KERNEL_EXT_MODULES := \
	qcom/opensource/mmrm-driver \
	qcom/opensource/audio-kernel \
	qcom/opensource/camera-kernel \
	qcom/opensource/cvp-kernel \
	qcom/opensource/dataipa/drivers/platform/msm \
	qcom/opensource/datarmnet/core \
	qcom/opensource/datarmnet-ext/aps \
	qcom/opensource/datarmnet-ext/offload \
	qcom/opensource/datarmnet-ext/shs \
	qcom/opensource/datarmnet-ext/perf \
	qcom/opensource/datarmnet-ext/perf_tether \
	qcom/opensource/datarmnet-ext/sch \
	qcom/opensource/datarmnet-ext/wlan \
	qcom/opensource/display-drivers/msm \
	qcom/opensource/eva-kernel \
	qcom/opensource/video-driver \
	qcom/opensource/wlan/qcacld-3.0/.qca6490 \
	qcom/opensource/wlan/qcacld-3.0/.qca6750

BOARD_VENDOR_KERNEL_MODULES_LOAD := $(strip $(shell cat $(DEVICE_PATH)/configs/modules/modules.load.vendor_dlkm))
BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat $(DEVICE_PATH)/configs/modules/modules.load.first_stage))
BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD  := $(strip $(shell cat $(DEVICE_PATH)/configs/modules/modules.load.recovery))

# Properties
TARGET_VENDOR_PROP += $(DEVICE_PATH)/configs/properties/vendor.prop

# SEPolicy
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private

# Inherit from the proprietary version
include vendor/lenovo/asphalt/BoardConfigVendor.mk