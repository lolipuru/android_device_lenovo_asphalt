#
# Copyright (C) 2024 The Android Open Source Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_tablet_wifionly.mk)

# Inherit from asphalt device.
$(call inherit-product, device/lenovo/asphalt/device.mk)

## Device identifier
PRODUCT_DEVICE := asphalt
PRODUCT_NAME := lineage_asphalt
PRODUCT_BRAND := lenovo
PRODUCT_MODEL := TB320FC
PRODUCT_MANUFACTURER := lenovo

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc=$(call normalize-path-list, "qti TB320FC TB320FC 15 AQ3A.240812.002 ZUXOS_1.1.350_250418_PRC user release-keys")

BUILD_FINGERPRINT := qti/TB320FC/TB320FC:15/AQ3A.240812.002/ZUXOS_1.1.350_250418_PRC:user/release-keys

# GMS
PRODUCT_GMS_CLIENTID_BASE := android-lenovo