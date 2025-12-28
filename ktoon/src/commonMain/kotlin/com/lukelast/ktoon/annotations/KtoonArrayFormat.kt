package com.lukelast.ktoon.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Annotation to manually control the TOON array format for a property.
 *
 * By default, TOON automatically selects the array format based on content:
 * - Inline for primitive arrays
 * - Tabular for uniform objects with primitive fields
 * - Expanded for everything else
 *
 * This annotation allows you to override the automatic selection.
 *
 * Example usage:
 * ```kotlin
 * @Serializable
 * data class User(
 *     val id: Int,
 *     val name: String,
 *     @KtoonArrayFormat(ArrayFormat.INLINE)
 *     val tags: List<String>,  // Force inline format
 *     @KtoonArrayFormat(ArrayFormat.EXPANDED)
 *     val roles: List<Role>  // Force expanded format
 * )
 * ```
 *
 * @property format The desired array format
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class KtoonArrayFormat(val format: ArrayFormat)

/** TOON array format options for the annotation. */
enum class ArrayFormat {
    /**
     * Inline format: all values on one line separated by delimiter.
     *
     * Example: `tags[3]: admin,ops,dev`
     *
     * Best for:
     * - Short primitive arrays
     * - Arrays where all elements are simple values
     * - When you want compact representation
     *
     * Limitations:
     * - Elements cannot contain the delimiter character (unless quoted)
     * - Not suitable for long arrays (exceeds readable line length)
     * - Only works with primitive types
     */
    INLINE,

    /**
     * Tabular format: uniform objects with fields as columns.
     *
     * Example:
     * ```
     * users[2]{id,name}:
     *   1,Alice
     *   2,Bob
     * ```
     *
     * Best for:
     * - Arrays of objects with same structure
     * - When all object fields are primitive types
     * - Database-like tabular data
     * - When you want compact but readable representation
     *
     * Limitations:
     * - All objects must have same structure
     * - All fields must be primitive types
     * - Not suitable for nested structures
     */
    TABULAR,

    /**
     * Expanded format: each element on its own line with dash prefix.
     *
     * Example:
     * ```
     * items[2]:
     *   - value1
     *   - value2
     * ```
     *
     * Best for:
     * - Mixed type arrays
     * - Arrays with nested structures
     * - Arrays with complex objects
     * - When readability is more important than compactness
     *
     * Limitations:
     * - More verbose than inline or tabular
     * - Takes more vertical space
     */
    EXPANDED,
}
