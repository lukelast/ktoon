package com.lukelast.ktoon.validation

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonValidationException

/**
 * Validation engine for TOON format strict mode.
 *
 * Performs validation checks according to TOON specification:
 * - Array length matches declared size
 * - Tabular arrays have consistent field counts
 * - Indentation is correct multiples of indent size
 * - No duplicate object keys
 * - Valid number formats
 * - Valid string escape sequences
 *
 * When strict mode is disabled, validation checks are skipped for better compatibility with
 * malformed input.
 */
internal class ValidationEngine(private val config: KtoonConfiguration) {

    /**
     * Validates that an array's actual length matches its declared length.
     *
     * @param declaredLength Length specified in array header
     * @param actualLength Actual number of elements found
     * @param line Line number for error reporting
     * @throws KtoonValidationException if validation fails in strict mode
     */
    fun validateArrayLength(declaredLength: Int, actualLength: Int, line: Int) {
        if (!config.strictMode) return

        if (declaredLength != actualLength) {
            throw KtoonValidationException.arrayLengthMismatch(
                declared = declaredLength,
                actual = actualLength,
                line = line,
            )
        }
    }

    /**
     * Validates that a tabular array row has the correct number of fields.
     *
     * @param expectedFieldCount Number of fields declared in header
     * @param actualFieldCount Number of fields in this row
     * @param rowIndex Index of the row being validated
     * @param line Line number for error reporting
     * @throws KtoonValidationException if validation fails in strict mode
     */
    fun validateTabularRow(
        expectedFieldCount: Int,
        actualFieldCount: Int,
        rowIndex: Int,
        line: Int,
    ) {
        if (!config.strictMode) return

        if (expectedFieldCount != actualFieldCount) {
            throw KtoonValidationException.tabularFieldMismatch(
                expected = expectedFieldCount,
                actual = actualFieldCount,
                elementIndex = rowIndex,
                line = line,
            )
        }
    }

    /**
     * Validates that indentation is a correct multiple of the configured indent size.
     *
     * @param indent Indentation level in spaces
     * @param line Line number for error reporting
     * @throws KtoonValidationException if validation fails in strict mode
     */
    fun validateIndentation(indent: Int, line: Int) {
        if (!config.strictMode) return

        if (indent % config.indentSize != 0) {
            throw KtoonValidationException.invalidIndentation(
                indent = indent,
                indentSize = config.indentSize,
                line = line,
            )
        }
    }

    /**
     * Validates that a key is not a duplicate within an object. This check is performed by the
     * parser, not here.
     *
     * @param key The key to check
     * @param existingKeys Set of keys already seen
     * @param line Line number for error reporting
     * @throws KtoonValidationException if key is duplicate in strict mode
     */
    fun validateUniqueKey(key: String, existingKeys: Set<String>, line: Int) {
        if (!config.strictMode) return

        if (key in existingKeys) {
            throw KtoonValidationException.duplicateKey(key, line)
        }
    }

    /**
     * Validates that a number string is in valid TOON format.
     *
     * Valid formats:
     * - Integers: 0, 1, -1, 123, -456
     * - Decimals: 1.5, -2.3, 0.5
     * - No scientific notation
     * - No trailing zeros after decimal
     * - No leading zeros (except "0")
     *
     * @param numberStr The number string to validate
     * @param line Line number for error reporting
     * @return true if valid, false otherwise
     */
    fun validateNumberFormat(numberStr: String, line: Int): Boolean {
        if (!config.strictMode) return true

        // Try to parse as a number
        val parsed = numberStr.toDoubleOrNull() ?: return false

        // Check for scientific notation (should not be present in TOON)
        if (numberStr.contains('e', ignoreCase = true)) {
            return false
        }

        // Check for leading zeros (except "0" or "-0")
        if (numberStr.startsWith("0") && numberStr.length > 1 && numberStr[1].isDigit()) {
            return false
        }
        if (numberStr.startsWith("-0") && numberStr.length > 2 && numberStr[2].isDigit()) {
            return false
        }

        // Check for trailing zeros after decimal point
        if (numberStr.contains('.') && numberStr.endsWith('0')) {
            return false
        }

        return true
    }

    /**
     * Validates that an escape sequence is one of the allowed TOON escape sequences.
     *
     * Allowed sequences:
     * - \\ (backslash)
     * - \" (double quote)
     * - \n (newline)
     * - \r (carriage return)
     * - \t (tab)
     *
     * @param escapeChar The character after the backslash
     * @param line Line number for error reporting
     * @param column Column number for error reporting
     * @return true if valid, false otherwise
     */
    fun validateEscapeSequence(escapeChar: Char, line: Int, column: Int): Boolean {
        if (!config.strictMode) return true

        return when (escapeChar) {
            '\\',
            '"',
            'n',
            'r',
            't' -> true
            else -> false
        }
    }

    /**
     * Validates that no blank lines exist within an array in strict mode. This is checked during
     * lexing, not here.
     *
     * @param hasBlankLines Whether blank lines were found
     * @param line Line number for error reporting
     * @throws KtoonValidationException if blank lines found in strict mode
     */
    fun validateNoBlankLinesInArray(hasBlankLines: Boolean, line: Int) {
        if (!config.strictMode) return

        if (hasBlankLines) {
            throw KtoonValidationException(
                "Blank lines are not allowed within arrays in strict mode",
                line,
            )
        }
    }

    /**
     * Validates that a colon is present after a key.
     *
     * @param hasColon Whether a colon was found
     * @param key The key being validated
     * @param line Line number for error reporting
     * @throws KtoonValidationException if colon missing in strict mode
     */
    fun validateColonAfterKey(hasColon: Boolean, key: String, line: Int) {
        if (!config.strictMode) return

        if (!hasColon) {
            throw KtoonValidationException("Missing colon after key '$key'", line)
        }
    }

    /**
     * Validates that tabs are not used for indentation. This is checked during lexing.
     *
     * @param hasTabIndentation Whether tab indentation was found
     * @param line Line number for error reporting
     * @throws KtoonValidationException if tabs found in strict mode
     */
    fun validateNoTabIndentation(hasTabIndentation: Boolean, line: Int) {
        if (!config.strictMode) return

        if (hasTabIndentation) {
            throw KtoonValidationException(
                "Tabs are not allowed for indentation in strict mode (use spaces)",
                line,
            )
        }
    }

    /**
     * Validates that the declared delimiter in an array header is valid.
     *
     * @param delimiter The delimiter character
     * @param line Line number for error reporting
     * @return true if valid, false otherwise
     */
    fun validateDelimiter(delimiter: Char, line: Int): Boolean {
        return when (delimiter) {
            ',',
            '\t',
            '|' -> true
            else -> {
                if (config.strictMode) {
                    throw KtoonValidationException(
                        "Invalid delimiter '$delimiter' (must be comma, tab, or pipe)",
                        line,
                    )
                }
                false
            }
        }
    }

    /**
     * Validates the overall structure integrity. Can be extended for additional structural checks.
     */
    fun validateStructure() {
        // Placeholder for future structural validation
        // e.g., verifying consistent indentation throughout document
    }
}
