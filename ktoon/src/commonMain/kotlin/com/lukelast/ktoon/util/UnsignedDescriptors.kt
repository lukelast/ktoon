package com.lukelast.ktoon.util

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Descriptors of Kotlin's unsigned types. Their serializers hand the *signed* backing bits to
 * `encodeInt` and friends after an `encodeInline` call, so a format that ignores the inline
 * descriptor writes `UInt.MAX_VALUE` as `-1` (§2: a number must carry its mathematical value).
 */
private val UNSIGNED_DESCRIPTORS: Set<SerialDescriptor> =
    setOf(
        UByte.serializer().descriptor,
        UShort.serializer().descriptor,
        UInt.serializer().descriptor,
        ULong.serializer().descriptor,
    )

/** True when [descriptor] is one of Kotlin's unsigned number types. */
internal fun isUnsignedDescriptor(descriptor: SerialDescriptor): Boolean =
    descriptor in UNSIGNED_DESCRIPTORS
