package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException
import com.lukelast.ktoon.KtoonValidationException
import com.lukelast.ktoon.util.isDigit
import kotlin.math.floor

/**
 * Parser for TOON tokens that builds a logical value structure.
 *
 * Converts the flat token stream from ToonLexer into a nested structure of ToonValue objects that
 * can be consumed by the decoder.
 *
 * Handles:
 * - Object structures (nested key-value pairs)
 * - Arrays in inline, tabular, and list form; keyed tabular objects (§9.5)
 * - Nested field groups (§9.3)
 * - Primitive values under the normative number grammar (§4)
 * - Validation in strict mode
 */
internal class ToonReader(private val tokens: List<Token>, private val config: KtoonConfiguration) {
    private var position = 0

    /**
     * Number of array/keyed header spans currently being read. Blank lines inside a header span are
     * strict-mode errors even when they fall between the fields of a list-item object (§12).
     */
    private var arraySpanDepth = 0

    /** §14.1: a declared count must match the actual count in strict mode (never truncates). */
    private fun validateCount(declared: Int, actual: Int, line: Int) {
        if (config.strictMode && declared != actual) {
            throw KtoonValidationException.arrayLengthMismatch(declared, actual, line)
        }
    }

    /** Reads the root value from the token stream. */
    fun readRoot(): ToonValue {
        if (tokens.isEmpty()) {
            return ToonValue.Object(emptyMap())
        }

        skipBlankLines()

        if (position >= tokens.size) {
            return ToonValue.Object(emptyMap())
        }

        // Determine root type from first token
        val result =
            when (val first = peek()) {
                is Token.ArrayHeader -> {
                    if (first.key.isEmpty()) {
                        // §5: keyless header at root — root array, or keyed tabular root object
                        if (first.keyed) readKeyedObject() else readArray()
                    } else {
                        readObject(baseIndent = 0)
                    }
                }
                is Token.Dash -> {
                    readListItems(itemDepth = 0, declaredLength = null, headerLine = first.line)
                }
                is Token.Key -> {
                    readObject(baseIndent = 0)
                }
                is Token.Value -> {
                    advance()
                    // §5: the literal token [] at root decodes as an empty array
                    if (first.content == "[]") {
                        ToonValue.Array(emptyList())
                    } else {
                        // §5/§14.2: a primitive root requires the document to have exactly one
                        // non-blank line; anything after a root scalar errors in both modes.
                        val primitive = parsePrimitive(first.content, first.line)
                        skipBlankLines()
                        if (position < tokens.size) {
                            throw KtoonParsingException(
                                "Top-level document must start with a key-value or array-header line",
                                peek().line,
                            )
                        }
                        primitive
                    }
                }
                else -> {
                    throw KtoonParsingException("Unexpected token type at root", 1)
                }
            }

        // §5: the root form spans the whole document. In strict mode trailing content MUST error;
        // in non-strict mode it MAY be ignored.
        skipBlankLines()
        if (config.strictMode && position < tokens.size) {
            val token = peek()
            throw KtoonParsingException("Unexpected content after root value", token.line)
        }

        return result
    }

    /** Reads an object (collection of key-value pairs). */
    private fun readObject(baseIndent: Int): ToonValue.Object {
        val properties = mutableMapOf<String, ToonValue>()
        var readAny = false

        while (position < tokens.size) {
            val token = peek()

            if (token is Token.BlankLine) {
                if (!continuesObjectScope(baseIndent)) {
                    // The blank belongs to an outer scope (e.g. between list items); leave it for
                    // the enclosing reader so §12 span checks can see it.
                    break
                }
                // §12: a blank line inside an array's header span is a strict-mode error, even
                // between a list-item object's fields. Outside any span, blank lines are ignored.
                if (config.strictMode && arraySpanDepth > 0 && readAny) {
                    throw KtoonValidationException(
                        "Blank lines are not allowed within arrays in strict mode",
                        token.line,
                    )
                }
                advance()
                continue
            }

            // Check if we've moved back to parent level
            val indent =
                when (token) {
                    is Token.Key -> token.indent
                    is Token.ArrayHeader -> token.indent
                    else -> -1
                }

            if (indent != -1) {
                if (indent < baseIndent) {
                    break
                }
                if (indent > baseIndent && config.strictMode) {
                    throw KtoonValidationException(
                        "Invalid indentation: expected $baseIndent, got $indent",
                        token.line,
                    )
                }
            }

            when (token) {
                is Token.Key -> {
                    advance()
                    val rawKey = token.name
                    val key = unquote(rawKey, token.line)
                    val value = readValueForKey(token)

                    insertProperty(properties, key, value, token.line)
                    readAny = true
                }
                is Token.ArrayHeader -> {
                    // §14.2: a keyless header cannot occupy an object field position
                    if (token.key.isEmpty()) {
                        throw KtoonParsingException(
                            "Array header without a key in object field position",
                            token.line,
                        )
                    }
                    val arrayValue = if (token.keyed) readKeyedObject() else readArray()
                    val rawKey = token.key
                    val key = unquote(rawKey, token.line)
                    insertProperty(properties, key, arrayValue, token.line)
                    readAny = true
                }
                is Token.Value -> {
                    // §5.2: a scalar line is valid only as a root primitive; anywhere else it is
                    // a structural error, in strict and non-strict mode alike.
                    throw KtoonParsingException(
                        "Misplaced scalar line (missing colon?)",
                        token.line,
                    )
                }
                else -> {
                    // Dash or other token belonging to an enclosing scope
                    break
                }
            }
        }

        return ToonValue.Object(properties)
    }

    /** True when the next non-blank token still belongs to an object scope at [baseIndent]. */
    private fun continuesObjectScope(baseIndent: Int): Boolean {
        var p = position
        while (p < tokens.size && tokens[p] is Token.BlankLine) p++
        if (p >= tokens.size) return false
        return when (val token = tokens[p]) {
            is Token.Key -> token.indent >= baseIndent
            is Token.ArrayHeader -> token.indent >= baseIndent
            else -> false
        }
    }

    private fun insertProperty(
        properties: MutableMap<String, ToonValue>,
        key: String,
        value: ToonValue,
        line: Int,
    ) {
        // §14.3: duplicate sibling keys error in strict mode; last-write-wins in non-strict mode.
        // The surviving entry keeps the key's first document position (plain put on an
        // insertion-ordered map), matching the reference implementation — §2's equality rule
        // makes key order observable, so the position is part of conformance.
        if (config.strictMode && properties.containsKey(key)) {
            throw KtoonValidationException.duplicateKey(key, line)
        }
        properties[key] = value
    }

    /** Reads the value following an object key token. */
    private fun readValueForKey(keyToken: Token.Key): ToonValue {
        // Inline value on the same line?
        if (position < tokens.size) {
            val next = peek()
            if (next is Token.Value && next.line == keyToken.line) {
                advance()
                // §4: the literal token [] in object field position decodes as an empty array
                if (next.content == "[]") return ToonValue.Array(emptyList())
                return parsePrimitive(next.content, next.line)
            }
        }

        skipBlankLines()
        if (position >= tokens.size) {
            // §8: key: alone is an empty object
            return ToonValue.Object(emptyMap())
        }

        return when (val token = peek()) {
            is Token.Key ->
                if (token.indent > keyToken.indent) {
                    readObject(baseIndent = keyToken.indent + config.indentSize)
                } else {
                    ToonValue.Object(emptyMap())
                }
            is Token.ArrayHeader ->
                if (token.indent > keyToken.indent) {
                    readObject(baseIndent = keyToken.indent + config.indentSize)
                } else {
                    ToonValue.Object(emptyMap())
                }
            is Token.Value ->
                // A value on a later line is a misplaced scalar (§5.2)
                throw KtoonParsingException("Misplaced scalar line (missing colon?)", token.line)
            else -> ToonValue.Object(emptyMap())
        }
    }

    /** Reads an array in any form (inline, tabular, or list). */
    private fun readArray(): ToonValue.Array {
        val header = consume<Token.ArrayHeader>()

        return when {
            // Tabular form (has fields)
            header.fields != null -> {
                readTabularArray(header)
            }
            // Inline values on the header line
            position < tokens.size && peek() is Token.InlineArrayValue -> {
                readInlineArray(header)
            }
            // List form (dash markers)
            else -> {
                readListArray(header)
            }
        }
    }

    /** Reads an inline array: `key[3]: val1,val2,val3` */
    private fun readInlineArray(header: Token.ArrayHeader): ToonValue.Array {
        val valueToken = consume<Token.InlineArrayValue>()

        // Split by delimiter (§12: tokens are trimmed of surrounding spaces, exactly U+0020)
        val values =
            splitRespectingQuotes(valueToken.content, header.delimiter.char)
                .map { it.trimSpaces() }
                .map { parsePrimitive(it, valueToken.line) }

        // Validate array length in strict mode
        validateCount(header.length, values.size, header.line)

        return ToonValue.Array(values)
    }

    /** Computes the depth at which a header's rows/items/entries appear. */
    private fun contentDepth(header: Token.ArrayHeader): Int {
        // When a keyless header sits on a hyphen line (`- [N]: …`), its children are indented
        // relative to the dash, not the synthetic header indent.
        val previousToken = if (position >= 2) tokens[position - 2] else null
        val isOnDashLine = previousToken is Token.Dash && previousToken.line == header.line
        val baseIndent =
            if (isOnDashLine && header.key.isEmpty()) header.indent - config.indentSize
            else header.indent
        return baseIndent + config.indentSize
    }

    /**
     * Handles a blank-line run inside a header span (§12). Returns true when the caller should
     * `continue` the loop (blanks consumed), false when the scope has ended and the blanks belong
     * to an outer scope.
     */
    private fun handleBlankInSpan(
        hasItems: Boolean,
        scopeContinues: () -> Boolean,
        blankLine: Int,
    ): Boolean {
        return if (scopeContinues()) {
            if (config.strictMode && hasItems) {
                throw KtoonValidationException(
                    "Blank lines are not allowed within arrays in strict mode",
                    blankLine,
                )
            }
            skipBlankLines()
            true
        } else {
            false
        }
    }

    /** Reads a tabular array: `key[2]{id,name}:\n 1,Ada\n 2,Bob` */
    private fun readTabularArray(header: Token.ArrayHeader): ToonValue.Array {
        val fields = header.fields ?: throw KtoonParsingException("Missing field list", header.line)
        validateFieldNames(fields, header.line)
        val leafCount = leafFieldCount(fields)

        val elements = mutableListOf<ToonValue>()
        val rowDepth = contentDepth(header)
        val delimiter = header.delimiter.char

        arraySpanDepth++
        try {
            while (position < tokens.size) {
                val token = peek()

                when (token) {
                    is Token.BlankLine -> {
                        if (
                            handleBlankInSpan(
                                hasItems = elements.isNotEmpty(),
                                scopeContinues = { nextNonBlankIsRow(rowDepth, delimiter) },
                                blankLine = token.line,
                            )
                        ) {
                            continue
                        }
                        break
                    }
                    is Token.Value -> {
                        validateRowIndent(token.indent, rowDepth, header.indent, token.line)
                        advance()
                        elements.add(
                            readRowObject(token.content, fields, leafCount, delimiter, token.line)
                        )
                    }
                    is Token.Key -> {
                        // §9.3 disambiguation: at row depth, delimiter before colon → row;
                        // otherwise the rows end at this key-value line. A dedented line is
                        // outside the scope regardless of its shape.
                        val atRowDepth =
                            if (config.strictMode) token.indent == rowDepth
                            else token.indent > header.indent
                        if (atRowDepth && isRowLine(token.rawContent, delimiter)) {
                            advance()
                            // Skip the paired value token from the same line
                            if (position < tokens.size) {
                                val paired = tokens[position]
                                if (paired is Token.Value && paired.line == token.line) advance()
                            }
                            elements.add(
                                readRowObject(
                                    token.rawContent,
                                    fields,
                                    leafCount,
                                    delimiter,
                                    token.line,
                                )
                            )
                        } else {
                            break
                        }
                    }
                    is Token.ArrayHeader -> {
                        // §5.2: within a tabular scope the §9.3 rule is authoritative, so a
                        // header-shaped line whose first unquoted delimiter precedes its first
                        // unquoted colon is still a row (e.g. `1,foo[2]: x`).
                        val atRowDepth =
                            if (config.strictMode) token.indent == rowDepth
                            else token.indent > header.indent
                        if (atRowDepth && isRowLine(token.rawContent, delimiter)) {
                            advance()
                            if (position < tokens.size) {
                                val paired = tokens[position]
                                if (paired is Token.InlineArrayValue && paired.line == token.line) {
                                    advance()
                                }
                            }
                            elements.add(
                                readRowObject(
                                    token.rawContent,
                                    fields,
                                    leafCount,
                                    delimiter,
                                    token.line,
                                )
                            )
                        } else {
                            break
                        }
                    }
                    else -> break
                }
            }
        } finally {
            arraySpanDepth--
        }

        validateCount(header.length, elements.size, header.line)

        return ToonValue.Array(elements)
    }

    /**
     * §9.3: a line is a row when its first unquoted delimiter precedes its first unquoted colon.
     */
    private fun isRowLine(rawContent: String, delimiter: Char): Boolean {
        val delimPos = findUnquoted(rawContent, delimiter)
        if (delimPos == -1) return false
        val colonPos = findUnquoted(rawContent, ':')
        return colonPos == -1 || delimPos < colonPos
    }

    /** True when the next non-blank token is another row of this tabular scope. */
    private fun nextNonBlankIsRow(rowDepth: Int, delimiter: Char): Boolean {
        var p = position
        while (p < tokens.size && tokens[p] is Token.BlankLine) p++
        if (p >= tokens.size) return false
        return when (val token = tokens[p]) {
            is Token.Value -> true
            is Token.Key -> token.indent == rowDepth && isRowLine(token.rawContent, delimiter)
            is Token.ArrayHeader ->
                token.indent == rowDepth && isRowLine(token.rawContent, delimiter)
            else -> false
        }
    }

    private fun validateRowIndent(indent: Int, rowDepth: Int, headerIndent: Int, line: Int) {
        if (config.strictMode) {
            if (indent != rowDepth) {
                throw KtoonValidationException(
                    "Invalid row indentation: expected $rowDepth, got $indent",
                    line,
                )
            }
        } else if (indent <= headerIndent) {
            // Non-strict: a row must still be inside the header's scope
            throw KtoonParsingException("Misplaced scalar line (missing colon?)", line)
        }
    }

    /** Decodes one row of cells against the header's field list (§9.3). */
    private fun readRowObject(
        content: String?,
        fields: List<FieldNode>,
        leafCount: Int,
        delimiter: Char,
        line: Int,
    ): ToonValue.Object {
        // §9.5: a bare entry key has zero cells, which is a width error in strict mode.
        val cells =
            if (content == null) emptyList()
            else
                splitRespectingQuotes(content, delimiter)
                    .map { it.trimSpaces() }
                    .map { parsePrimitive(it, line) }

        if (config.strictMode && cells.size != leafCount) {
            throw KtoonValidationException(
                "Row has ${cells.size} cells, expected $leafCount",
                line,
            )
        }

        val cursor = intArrayOf(0)
        return buildRowObject(fields, cells, cursor, line)
    }

    /** Maps cells to leaf fields depth-first, materializing nested field groups (§9.3). */
    private fun buildRowObject(
        fields: List<FieldNode>,
        cells: List<ToonValue>,
        cursor: IntArray,
        line: Int,
    ): ToonValue.Object {
        val properties = mutableMapOf<String, ToonValue>()
        for (field in fields) {
            val name = unquote(field.name, line)
            val value =
                if (field.group == null) {
                    if (cursor[0] < cells.size) cells[cursor[0]++] else break
                } else {
                    buildRowObject(field.group, cells, cursor, line)
                }
            // Duplicate field names apply last-write-wins in non-strict mode (§9.3); strict mode
            // already rejected them from the header line alone. A plain put keeps the first
            // document position, matching the reference implementation.
            properties[name] = value
        }
        return ToonValue.Object(properties)
    }

    /** §9.3/§14.2: repeated field names within one field list are a strict-mode header defect. */
    private fun validateFieldNames(fields: List<FieldNode>, line: Int) {
        val seen = mutableSetOf<String>()
        for (field in fields) {
            val name = unquote(field.name, line)
            if (config.strictMode && !seen.add(name)) {
                throw KtoonValidationException("Duplicate field name: '$name'", line)
            }
            if (field.group != null) validateFieldNames(field.group, line)
        }
    }

    /** Reads a keyed tabular object: `key[2:]{host,port}:\n alpha: a,1\n beta: b,2` (§9.5). */
    private fun readKeyedObject(): ToonValue.Object {
        val header = consume<Token.ArrayHeader>()
        val fields =
            header.fields
                ?: throw KtoonParsingException("Keyed header without a field list", header.line)
        validateFieldNames(fields, header.line)
        val leafCount = leafFieldCount(fields)

        val properties = mutableMapOf<String, ToonValue>()
        var entryCount = 0
        val entryDepth = contentDepth(header)
        val delimiter = header.delimiter.char

        arraySpanDepth++
        try {
            while (position < tokens.size) {
                when (val token = peek()) {
                    is Token.BlankLine -> {
                        if (
                            handleBlankInSpan(
                                hasItems = entryCount > 0,
                                scopeContinues = { nextNonBlankIsEntry(entryDepth) },
                                blankLine = token.line,
                            )
                        ) {
                            continue
                        }
                        break
                    }
                    is Token.Key -> {
                        // §9.5: a keyed scope ends only when the depth decreases to the header's
                        // depth or less; every line at entry depth with an unquoted colon is an
                        // entry row.
                        if (token.indent <= header.indent) break
                        if (config.strictMode && token.indent != entryDepth) {
                            throw KtoonValidationException(
                                "Invalid entry row indentation: expected $entryDepth, got ${token.indent}",
                                token.line,
                            )
                        }
                        advance()
                        var cellsContent: String? = null
                        if (position < tokens.size) {
                            val paired = tokens[position]
                            if (paired is Token.Value && paired.line == token.line) {
                                advance()
                                cellsContent = paired.content
                            }
                        }
                        readKeyedEntry(
                            properties,
                            token.name,
                            cellsContent,
                            fields,
                            leafCount,
                            delimiter,
                            token.line,
                        )
                        entryCount++
                    }
                    is Token.ArrayHeader -> {
                        // A header-shaped line at entry depth is still an entry row (§9.5)
                        if (token.indent <= header.indent) break
                        advance()
                        val raw = token.rawContent
                        if (position < tokens.size) {
                            val paired = tokens[position]
                            if (paired is Token.InlineArrayValue && paired.line == token.line) {
                                advance()
                            }
                        }
                        val colon = findUnquoted(raw, ':')
                        val entryKeyRaw = raw.substring(0, colon).trimSpaces()
                        val cells = raw.substring(colon + 1).trimSpaces()
                        readKeyedEntry(
                            properties,
                            entryKeyRaw,
                            cells.ifEmpty { null },
                            fields,
                            leafCount,
                            delimiter,
                            token.line,
                        )
                        entryCount++
                    }
                    is Token.Value -> {
                        // §9.5: a line at entry depth without an unquoted colon errors in strict
                        // mode; non-strict decoders MAY skip it.
                        if (token.indent <= header.indent) break
                        if (config.strictMode) {
                            throw KtoonParsingException(
                                "Entry row without a colon in keyed scope",
                                token.line,
                            )
                        }
                        advance()
                    }
                    else -> break
                }
            }
        } finally {
            arraySpanDepth--
        }

        validateCount(header.length, entryCount, header.line)

        return ToonValue.Object(properties)
    }

    private fun readKeyedEntry(
        properties: MutableMap<String, ToonValue>,
        rawEntryKey: String,
        cellsContent: String?,
        fields: List<FieldNode>,
        leafCount: Int,
        delimiter: Char,
        line: Int,
    ) {
        val entryKey = unquote(rawEntryKey, line)
        val value = readRowObject(cellsContent, fields, leafCount, delimiter, line)
        insertProperty(properties, entryKey, value, line)
    }

    /** True when the next non-blank token is another entry row of this keyed scope. */
    private fun nextNonBlankIsEntry(entryDepth: Int): Boolean {
        var p = position
        while (p < tokens.size && tokens[p] is Token.BlankLine) p++
        if (p >= tokens.size) return false
        return when (val token = tokens[p]) {
            is Token.Key -> token.indent >= entryDepth
            is Token.ArrayHeader -> token.indent >= entryDepth
            is Token.Value -> token.indent >= entryDepth
            else -> false
        }
    }

    /** Reads an array in list form: `key[2]:\n - val1\n - val2` */
    private fun readListArray(header: Token.ArrayHeader): ToonValue.Array {
        val itemDepth = contentDepth(header)
        return readListItems(itemDepth, header.length, header.line)
    }

    /** Reads list items at [itemDepth]; validates the count when [declaredLength] is present. */
    private fun readListItems(
        itemDepth: Int,
        declaredLength: Int?,
        headerLine: Int,
    ): ToonValue.Array {
        val elements = mutableListOf<ToonValue>()

        arraySpanDepth++
        try {
            while (position < tokens.size) {
                when (val token = peek()) {
                    is Token.BlankLine -> {
                        if (
                            handleBlankInSpan(
                                hasItems = elements.isNotEmpty(),
                                scopeContinues = { nextNonBlankIsListItem(itemDepth) },
                                blankLine = token.line,
                            )
                        ) {
                            continue
                        }
                        break
                    }
                    is Token.Dash -> {
                        if (token.indent != itemDepth) break
                        advance()
                        elements.add(readListItemValue(token))
                    }
                    is Token.Value -> {
                        // §5.2: a bare token line inside an array scope is a structural error in
                        // both modes; a dedented one belongs to an outer scope.
                        if (token.indent >= itemDepth) {
                            throw KtoonParsingException(
                                "Misplaced scalar line (missing colon?)",
                                token.line,
                            )
                        }
                        break
                    }
                    else -> break
                }
            }
        } finally {
            arraySpanDepth--
        }

        if (declaredLength != null) {
            validateCount(declaredLength, elements.size, headerLine)
        }

        return ToonValue.Array(elements)
    }

    /** True when the next non-blank token is another item of this list scope. */
    private fun nextNonBlankIsListItem(itemDepth: Int): Boolean {
        var p = position
        while (p < tokens.size && tokens[p] is Token.BlankLine) p++
        if (p >= tokens.size) return false
        val token = tokens[p]
        return token is Token.Dash && token.indent == itemDepth
    }

    /** Reads the value of one list item, after its dash marker has been consumed. */
    private fun readListItemValue(dash: Token.Dash): ToonValue {
        if (position >= tokens.size) {
            // §10: a bare "-" is an empty-object list item
            return ToonValue.Object(emptyMap())
        }

        val next = peek()
        if (next.line != dash.line) {
            // Nothing on the hyphen line: bare marker → empty object (§10, §12)
            return ToonValue.Object(emptyMap())
        }

        return when (next) {
            is Token.Value -> {
                advance()
                // §9.2: `- []` is an empty inner-array list item
                if (next.content == "[]") ToonValue.Array(emptyList())
                else parsePrimitive(next.content, next.line)
            }
            is Token.ArrayHeader -> {
                if (next.key.isEmpty()) {
                    // §9.4: `- [M]:` opens a nested array; a fields-bearing keyless header is
                    // not valid in list-item position.
                    if (next.fields != null) {
                        throw KtoonParsingException(
                            "Keyless fields-bearing header as list item",
                            next.line,
                        )
                    }
                    readArray()
                } else {
                    // List-item object whose first field is an array or keyed object (§10)
                    readObject(baseIndent = dash.indent + config.indentSize)
                }
            }
            is Token.Key -> {
                // List-item object with its first field on the hyphen line (§10)
                readObject(baseIndent = dash.indent + config.indentSize)
            }
            else -> ToonValue.Object(emptyMap())
        }
    }

    /** Parses a primitive value from a string. */
    private fun parsePrimitive(content: String, line: Int): ToonValue {
        // Check if value is quoted (§7.4 - quoting disambiguates type)
        val isQuoted = content.startsWith('"')

        // Unquote if quoted
        val unquoted = unquote(content, line)

        // If originally quoted, return as string (prevents parsing "42" as number, etc.)
        if (isQuoted) {
            return ToonValue.String(unquoted)
        }

        // Check for null
        if (unquoted == "null") {
            return ToonValue.Null
        }

        // Check for boolean
        if (unquoted == "true") {
            return ToonValue.Boolean(true)
        }
        if (unquoted == "false") {
            return ToonValue.Boolean(false)
        }

        // Try to parse as number under the normative grammar (§4)
        val numberValue = tryParseNumber(unquoted)
        if (numberValue != null) {
            return numberValue
        }

        // Default to string
        return ToonValue.String(unquoted)
    }

    /**
     * §4 number grammar: an unquoted token decodes as a number iff it matches
     * `/^-?[0-9]+(?:\.[0-9]+)?(?:e[+-]?[0-9]+)?$/i` (ASCII digits only) without forbidden leading
     * zeros. Anything else — `.5`, `1.`, `+5`, `Infinity`, `NaN`, `0x10`, `1_000` — is a string,
     * and this decision MUST NOT be delegated to a wider host parser.
     */
    private fun matchesNumberGrammar(str: String): Boolean {
        var i = 0
        if (i < str.length && str[i] == '-') i++

        val intStart = i
        while (i < str.length && str[i].isDigit()) i++
        val intLen = i - intStart
        if (intLen == 0) return false
        // Forbidden leading zeros in the integer part (e.g. "05", "-0001")
        if (intLen > 1 && str[intStart] == '0') return false

        if (i < str.length && str[i] == '.') {
            i++
            val fracStart = i
            while (i < str.length && str[i].isDigit()) i++
            if (i == fracStart) return false
        }

        if (i < str.length && (str[i] == 'e' || str[i] == 'E')) {
            i++
            if (i < str.length && (str[i] == '+' || str[i] == '-')) i++
            val expStart = i
            while (i < str.length && str[i].isDigit()) i++
            if (i == expStart) return false
        }

        return i == str.length
    }

    /** Tries to parse a string as a number. Returns null if not a valid number token. */
    private fun tryParseNumber(str: String): ToonValue? {
        if (!matchesNumberGrammar(str)) return null

        // Try integer first (for simple cases without exponents)
        val intValue = str.toIntOrNull()
        if (intValue != null) {
            return ToonValue.Number(intValue)
        }

        // Try long
        val longValue = str.toLongOrNull()
        if (longValue != null) {
            return ToonValue.Number(longValue)
        }

        // Try double (handles fractional and exponent forms)
        val doubleValue = str.toDoubleOrNull()
        if (doubleValue != null && doubleValue.isFinite()) {
            // If the double has no fractional part and fits in Int/Long, store as integer
            if (doubleValue == floor(doubleValue)) {
                if (doubleValue >= Int.MIN_VALUE && doubleValue <= Int.MAX_VALUE) {
                    return ToonValue.Number(doubleValue.toInt())
                }
                if (doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE) {
                    return ToonValue.Number(doubleValue.toLong())
                }
            }
            return ToonValue.Number(doubleValue)
        }

        // Out-of-domain (e.g. overflowing exponent): documented policy is to decode as string
        return null
    }

    /** Peeks at the current token without consuming it. */
    private fun peek(): Token {
        if (position >= tokens.size) {
            throw KtoonParsingException.unexpectedEndOfInput("more tokens")
        }
        return tokens[position]
    }

    /** Advances to the next token and returns the current one. */
    private fun advance(): Token {
        val token = peek()
        position++
        return token
    }

    /** Consumes a token of the expected type. */
    private inline fun <reified T : Token> consume(): T {
        val token = peek()
        if (token !is T) {
            throw KtoonParsingException(
                "Expected ${T::class.simpleName}, got ${token::class.simpleName}",
                -1,
            )
        }
        position++
        return token
    }

    /** Skips blank lines in the token stream. */
    private fun skipBlankLines() {
        while (position < tokens.size && tokens[position] is Token.BlankLine) {
            position++
        }
    }
}

/** Represents a parsed TOON value. */
internal sealed class ToonValue {
    /** Null value */
    object Null : ToonValue()

    /** Boolean value */
    data class Boolean(val value: kotlin.Boolean) : ToonValue()

    /** Numeric value (Int, Long, or Double) */
    data class Number(val value: kotlin.Number) : ToonValue()

    /** String value */
    data class String(val value: kotlin.String) : ToonValue()

    /** Object (map of key-value pairs) */
    data class Object(val properties: Map<kotlin.String, ToonValue>) : ToonValue()

    /** Array (list of values) */
    data class Array(val elements: List<ToonValue>) : ToonValue()
}
