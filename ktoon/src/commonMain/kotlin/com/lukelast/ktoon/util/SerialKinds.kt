package com.lukelast.ktoon.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Whether TOON writes this descriptor as an object.
 *
 * A polymorphic value is serialized as a structure holding a `type` discriminator and the concrete
 * `value`, so it needs an object of its own; letting it share the surrounding encoder would put
 * those two fields into the parent's field, row, or map slot.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.isObjectKind(): Boolean =
    when (kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT,
        PolymorphicKind.OPEN,
        PolymorphicKind.SEALED -> true
        else -> false
    }
