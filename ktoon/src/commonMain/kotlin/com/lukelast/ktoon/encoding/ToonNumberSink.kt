package com.lukelast.ktoon.encoding

/**
 * Accepts a numeric token that has already been validated against the §4 number grammar.
 *
 * Custom serializers holding a number as text — `JsonPrimitive`, for instance — would otherwise
 * have to convert it to a host `Long` or `Double` first, which rounds large integers and turns a
 * literal beyond the floating range into null. Every TOON encoder implements this so a value in any
 * position can be written with its own digits.
 */
internal interface ToonNumberSink {
    fun encodeNumberLiteral(literal: String)
}
