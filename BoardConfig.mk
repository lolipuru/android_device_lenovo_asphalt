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
TARGET_OTA_ASSERT_DEVICE := asphalt_prc,asphalt_nec,TB320FC

# Audio
TARGET_PROVIDES_AUDIO_HAL ?= true

# Display
TARGET_SCREEN_DENSITY := 410

# Kernel
BOARD_VENDOR_KERNEL_MODULES_LOAD += \
    cirrus_wm_adsp_dlkm.ko \
    cirrus_cs35l45_dlkm.ko

BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD += \
	nt36523-spi.ko

BOOT_KERNEL_MODULES += \
	nt36523-spi.ko

# Properties
TARGET_VENDOR_PROP += $(DEVICE_PATH)/configs/properties/vendor.prop

# SEPolicy
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private

# Inherit from the proprietary version
include vendor/lenovo/asphalt/BoardConfigVendor.mk