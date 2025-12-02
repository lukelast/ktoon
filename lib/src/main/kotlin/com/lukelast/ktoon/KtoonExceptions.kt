package com.lukelast.ktoon

import kotlinx.serialization.SerializationException

/** Base exception for all TOON format-related errors. */
open class KtoonException(message: String, cause: Throwable? = null) :
    SerializationException(message, cause) {
    companion object {
        internal fun buildMessage(message: String, line: Int, column: Int): String {
            return when {
                line > 0 && column > 0 -> "$message at line $line, column $column"
                line > 0 -> "$message at line $line"
                else -> message
            }
        }
    }
}

/**
 * Exception thrown when TOON validation fails in strict mode.
 *
 * @property line Line number where validation failed (1-based)
 * @property column Column number where validation failed (1-based, optional)
 */
class KtoonValidationException(
    message: String,
    val line: Int = -1,
    val column: Int = -1,
    cause: Throwable? = null,
) : KtoonException(KtoonException.buildMessage(message, line, column), cause) {
    companion object {
        /** Creates a validation exception for array length mismatch. */
        fun arrayLengthMismatch(declared: Int, actual: Int, line: Int) =
            KtoonValidationException(
                "Array length mismatch: declared $declared, found $actual",
                line,
            )

        /** Creates a validation exception for tabular array field count mismatch. */
        fun tabularFieldMismatch(expected: Int, actual: Int, elementIndex: Int, line: Int) =
            KtoonValidationException(
                "Tabular array element $elementIndex has $actual fields, expected $expected",
                line,
            )

        /** Creates a validation exception for invalid indentation. */
        fun invalidIndentation(indent: Int, indentSize: Int, line: Int) =
            KtoonValidationException(
                "Invalid indentation: $indent spaces (expected multiple of $indentSize)",
                line,
            )

        /** Creates a validation exception for duplicate object keys. */
        fun duplicateKey(key: String, line: Int) =
            KtoonValidationException("Duplicate key: '$key'", line)
    }
}

/**
 * Exception thrown when parsing TOON format fails.
 *
 * @property line Line number where parsing failed (1-based)
 * @property column Column number where parsing failed (1-based, optional)
 */
class KtoonParsingException(
    message: String,
    val line: Int = -1,
    val column: Int = -1,
    cause: Throwable? = null,
) : KtoonException(KtoonException.buildMessage(message, line, column), cause) {
    companion object {
        /** Creates a parsing exception for unexpected token. */
        fun unexpectedToken(expected: String, actual: String, line: Int, column: Int = -1) =
            KtoonParsingException("Expected $expected but found '$actual'", line, column)

        /** Creates a parsing exception for invalid array format. */
        fun invalidArrayFormat(reason: String, line: Int) =
            KtoonParsingException("Invalid array format: $reason", line)

        /** Creates a parsing exception for unterminated string. */
        fun unterminatedString(line: Int, column: Int) =
            KtoonParsingException("Unterminated string literal", line, column)

        /** Creates a parsing exception for invalid escape sequence. */
        fun invalidEscapeSequence(sequence: String, line: Int, column: Int) =
            KtoonParsingException(
                "Invalid escape sequence: '$sequence' (only \\\\, \\\", \\n, \\r, \\t are allowed)",
                line,
                column,
            )

        /** Creates a parsing exception for invalid number format. */
        fun invalidNumber(value: String, line: Int, column: Int = -1) =
            KtoonParsingException("Invalid number format: '$value'", line, column)

        /** Creates a parsing exception for unexpected end of input. */
        fun unexpectedEndOfInput(expected: String) =
            KtoonParsingException("Unexpected end of input, expected $expected")
    }
}

/** Exception thrown when TOON encoding fails. */
class KtoonEncodingException(message: String, cause: Throwable? = null) :
    KtoonException(message, cause) {
    companion object {
        /** Creates an encoding exception for unsupported type. */
        fun unsupportedType(typeName: String) =
            KtoonEncodingException("Unsupported type for TOON encoding: $typeName")

        /** Creates an encoding exception for circular reference. */
        fun circularReference(path: String) =
            KtoonEncodingException("Circular reference detected at path: $path")
    }
}

/** Exception thrown when TOON decoding fails. */
class KtoonDecodingException(message: String, cause: Throwable? = null) :
    KtoonException(message, cause) {
    companion object {
        /** Creates a decoding exception for type mismatch. */
        fun typeMismatch(expected: String, actual: String, line: Int = -1) =
            KtoonDecodingException(
                if (line > 0) {
                    "Type mismatch at line $line: expected $expected, found $actual"
                } else {
                    "Type mismatch: expected $expected, found $actual"
                }
            )

        /** Creates a decoding exception for missing required field. */
        fun missingField(fieldName: String, line: Int = -1) =
            KtoonDecodingException(
                if (line > 0) {
                    "Missing required field '$fieldName' at line $line"
                } else {
                    "Missing required field '$fieldName'"
                }
            )
    }
}
