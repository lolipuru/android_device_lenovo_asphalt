#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import extract_utils.tools
from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/lenovo/asphalt',
    'device/lenovo/sm8475-common',
    'hardware/qcom-caf/sm8450',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/lenovo/sm8475-common',
]

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'libar-pal',
        'vendor.qti.hardware.pal@1.0',
        'vendor.qti.hardware.pal@1.0-impl'
    ): lib_fixup_remove,
}

blob_fixups: blob_fixups_user_type = {
    ('vendor/lib64/libcamximageformatutils.so'): blob_fixup()
        .replace_needed(
            'vendor.qti.hardware.display.config-V2-ndk_platform.so',
            'vendor.qti.hardware.display.config-V2-ndk.so',
        ),
    'vendor/bin/hw/vendor.qti.hardware.display.composer-service': blob_fixup()
        .replace_needed('android.hardware.common-V2-ndk_platform.so', 'android.hardware.common-V2-ndk.so')
        .replace_needed('vendor.qti.hardware.display.config-V5-ndk_platform.so', 'vendor.qti.hardware.display.config-V5-ndk.so'),
    (
        'vendor/bin/hw/vendor.qti.hardware.vibrator.service',
        'vendor/lib64/vendor.qti.hardware.vibrator.impl.so',
    ): blob_fixup()
        .replace_needed(
            'android.hardware.vibrator-V2-ndk_platform.so',
            'android.hardware.vibrator-V2-ndk.so',
        ),
    (
        'vendor/bin/hw/dolbycodec2',
        'vendor/bin/hw/vendor.dolby.hardware.dms@2.0-service',
        'vendor/bin/hw/vendor.dolby.media.c2-default-service-dax',
        'vendor/lib64/libdlbdsservice.so',
        'vendor/lib64/libdlbpreg.so',
        'vendor/lib64/soundfx/libdlbvol.so',
        'vendor/lib64/soundfx/libswdap.so',
        'vendor/lib64/soundfx/libswgamedap.so',
    ): blob_fixup()
        .add_needed('libstagefright_foundation-v33.so'),
    (
        'vendor/lib/c2.dolby.client.so',
        'vendor/lib64/c2.dolby.client.so',
    ): blob_fixup()
        .add_needed('dolbycodec_shim.so'),
}

module = ExtractUtilsModule(
    'asphalt',
    'lenovo',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device_with_common(
        module, 'sm8475-common', module.vendor
    )
    utils.run()