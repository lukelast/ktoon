package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/** §9.5: a keyed tabular object has at least two entries. */
private const val MIN_KEYED_ENTRIES = 2

/** One entry of a tabular or keyed header's field list; a group is a nested-uniform column. */
internal class FieldNode(val name: String, val group: List<FieldNode>?)

/**
 * The value stored under [name], read at [column] first: a header's fields follow the first
 * object's order, which every object of one class repeats, so the positional read almost always
 * hits. Detection has already established that the name is present exactly once.
 */
private fun List<Pair<String, EncodedElement>>.valueOf(name: String, column: Int): EncodedElement {
    val candidate = this[column]
    return if (candidate.first == name) candidate.second else first { it.first == name }.second
}

/**
 * Writes captured [EncodedElement] trees, selecting the form each value MUST take under §9: inline,
 * tabular (with nested field groups), list, or keyed tabular.
 */
@Suppress("TooManyFunctions")
internal class ElementWriter(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
) {

    /** The position a value occupies, which constrains the forms available to it (§9, §10). */
    enum class ArrayPosition {
        ROOT,
        FIELD,

        /** Keyless value directly after a list-item hyphen; tabular and keyed forms don't apply. */
        LIST_ITEM,
    }

    companion object {
        /**
         * §9.3 tabular detection: every element is a non-empty object, all share one key set, and
         * every column is uniform-primitive or nested-uniform. Returns the field tree, or null.
         */
        @Suppress("ReturnCount")
        fun tabularTree(elements: List<EncodedElement>): List<FieldNode>? {
            if (elements.isEmpty()) return null
            val objects = elements.map { (it as? EncodedElement.Structure)?.entries ?: return null }
            return fieldTree(objects)
        }

        /**
         * §9.5 keyed tabular detection: at least two entries whose entries are uniform non-empty
         * objects. Returns the field tree, or null.
         */
        @Suppress("ReturnCount")
        fun keyedTree(entries: List<Pair<String, EncodedElement>>): List<FieldNode>? {
            if (entries.size < 2) return null
            val objects = entries.map { (_, v) ->
                (v as? EncodedElement.Structure)?.entries ?: return null
            }
            return fieldTree(objects)
        }

        @Suppress("ReturnCount")
        private fun fieldTree(objects: List<List<Pair<String, EncodedElement>>>): List<FieldNode>? {
            val first = objects.first()
            if (first.isEmpty()) return null // empty objects are excluded (§9.3)
            val names = first.map { it.first }
            if (names.size != names.toSet().size) return null
            val positions = columnPositions(objects, names) ?: return null

            val nodes = ArrayList<FieldNode>(names.size)
            for (column in names.indices) {
                nodes.add(columnNode(objects, positions, names, column) ?: return null)
            }
            return nodes
        }

        /**
         * The header entry for one column: a plain field when every value is primitive, a group
         * when every value is a non-empty object (§9.3). Null when the column is neither, which the
         * first value ruling both out settles — so a rejected column costs one pass and no
         * allocation.
         */
        @Suppress("ReturnCount")
        private fun columnNode(
            objects: List<List<Pair<String, EncodedElement>>>,
            positions: IntArray,
            names: List<String>,
            column: Int,
        ): FieldNode? {
            val width = names.size
            var allPrimitive = true
            var allNested = true
            for (row in objects.indices) {
                val value = objects[row][positions[row * width + column]].second
                if (value !is EncodedElement.Primitive) allPrimitive = false
                if (value !is EncodedElement.Structure || value.entries.isEmpty()) allNested = false
                if (!allPrimitive && !allNested) return null
            }
            if (allPrimitive) return FieldNode(names[column], null)

            val nested = ArrayList<List<Pair<String, EncodedElement>>>(objects.size)
            for (row in objects.indices) {
                val value = objects[row][positions[row * width + column]].second
                nested.add((value as EncodedElement.Structure).entries)
            }
            return FieldNode(names[column], fieldTree(nested) ?: return null)
        }

        /**
         * §9.3: every object must carry the same *set* of keys. Returns where each of [names] sits
         * within each object, as one row-major `row * names.size + column` table, or null when some
         * object's keys differ.
         *
         * Objects of one class list their fields in descriptor order, so the positional guess below
         * almost always holds and the search never runs; map entries, which keep the host map's
         * order, are the case that can differ. Locating every name within an object of matching
         * size also rejects one that repeats a name and so lacks another, which the column reads
         * assume cannot happen.
         */
        @Suppress("ReturnCount")
        private fun columnPositions(
            objects: List<List<Pair<String, EncodedElement>>>,
            names: List<String>,
        ): IntArray? {
            val width = names.size
            val positions = IntArray(objects.size * width)
            for (row in objects.indices) {
                val entries = objects[row]
                if (entries.size != width) return null
                for (column in 0 until width) {
                    val name = names[column]
                    val at =
                        if (entries[column].first == name) column
                        else entries.indexOfFirst { it.first == name }
                    if (at < 0) return null
                    positions[row * width + column] = at
                }
            }
            return positions
        }

        /**
         * Cheap descriptor pre-gate for keyed tabular capture: a keyed object needs at least two
         * object-shaped entries (§9.5). A field that is not object-shaped may still be omitted
         * during serialization, so it cannot rule the object out — the captured values decide. This
         * only avoids capturing objects that obviously cannot qualify.
         */
        fun couldBeKeyed(descriptor: SerialDescriptor): Boolean =
            when (descriptor.kind) {
                StructureKind.MAP ->
                    descriptor.elementsCount >= 2 &&
                        isObjectLike(descriptor.getElementDescriptor(1))
                StructureKind.CLASS,
                StructureKind.OBJECT ->
                    (0 until descriptor.elementsCount).count {
                        isObjectLike(descriptor.getElementDescriptor(it))
                    } >= MIN_KEYED_ENTRIES
                else -> false
            }

        private fun isObjectLike(descriptor: SerialDescriptor): Boolean =
            when (descriptor.kind) {
                StructureKind.CLASS,
                StructureKind.OBJECT,
                StructureKind.MAP,
                SerialKind.CONTEXTUAL -> true
                else -> false
            }
    }

    private fun quoteKey(value: String) =
        StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)

    // ----- objects -----

    /**
     * Writes a captured root object: keyed tabular when eligible (§9.5), plain fields otherwise.
     */
    fun writeRootObject(entries: List<Pair<String, EncodedElement>>) {
        val tree = keyedTree(entries)
        if (tree != null) {
            writeKeyedTable(null, entries, tree, 0)
            return
        }
        entries.forEachIndexed { i, (name, element) ->
            if (i > 0) writer.writeNewline()
            writer.writeIndent(0)
            writeField(name, element, 0)
        }
    }

    /**
     * Writes a captured object in field position, starting at the key: a keyed table, a bare `key:`
     * for an empty object (§8), or `key:` with nested fields at depth +1.
     */
    fun writeObjectField(
        name: String,
        entries: List<Pair<String, EncodedElement>>,
        indentLevel: Int,
    ) {
        val tree = keyedTree(entries)
        when {
            tree != null -> writeKeyedTable(quoteKey(name), entries, tree, indentLevel)
            entries.isEmpty() -> writer.writeKey(quoteKey(name))
            else -> {
                writer.writeKey(quoteKey(name))
                writeNestedFields(entries, indentLevel, firstInline = false)
            }
        }
    }

    private fun writeField(name: String, element: EncodedElement, indentLevel: Int) {
        when (element) {
            is EncodedElement.Primitive -> writer.writeKeyValue(quoteKey(name), element.value)
            is EncodedElement.NestedArray ->
                writeArray(name, element.elements, indentLevel, ArrayPosition.FIELD)
            is EncodedElement.Structure -> writeObjectField(name, element.entries, indentLevel)
        }
    }

    /**
     * Writes an object's fields at depth [indentLevel] + 1. When [firstInline] is true the first
     * field continues the current line (list-item objects, §10).
     */
    private fun writeNestedFields(
        entries: List<Pair<String, EncodedElement>>,
        indentLevel: Int,
        firstInline: Boolean,
    ) {
        entries.forEachIndexed { i, (name, value) ->
            if (i > 0 || !firstInline) {
                writer.writeNewline()
                writer.writeIndent(indentLevel + 1)
            }
            writeField(name, value, indentLevel + 1)
        }
    }

    // ----- arrays -----

    fun writeArray(
        key: String?,
        elements: List<EncodedElement>,
        indentLevel: Int,
        position: ArrayPosition,
    ) {
        if (elements.isEmpty()) {
            writeEmptyArray(key, position)
            return
        }
        if (elements.all { it is EncodedElement.Primitive }) {
            writeInline(key, elements)
            return
        }
        // §9.4: tabular form is unavailable directly after a list-item hyphen
        val tree = if (position == ArrayPosition.LIST_ITEM) null else tabularTree(elements)
        if (tree != null) {
            writeTabularTable(key, elements, tree, indentLevel)
        } else {
            writeListArray(key, elements, indentLevel)
        }
    }

    private fun writeEmptyArray(key: String?, position: ArrayPosition) {
        when (position) {
            // §9.1: canonical empty-array forms
            ArrayPosition.FIELD -> {
                writer.writeKey(quoteKey(checkNotNull(key) { "FIELD arrays are always keyed" }))
                writer.writeSpace()
                writer.write("[]")
            }
            ArrayPosition.ROOT -> writer.write("[]")
            // §9.2/§9.4: encoders still emit the legacy form in list-item position
            ArrayPosition.LIST_ITEM -> writeArrayHeader(null, 0)
        }
    }

    private fun writeInline(key: String?, elements: List<EncodedElement>) {
        writeArrayHeader(key, elements.size)
        writer.writeSpace()
        elements.forEachIndexed { i, e ->
            writer.write((e as EncodedElement.Primitive).value)
            if (i < elements.lastIndex) writer.writeDelimiter()
        }
    }

    private fun writeListArray(key: String?, elements: List<EncodedElement>, indentLevel: Int) {
        writeArrayHeader(key, elements.size)
        elements.forEach { writeListItem(it, indentLevel + 1) }
    }

    private fun writeListItem(element: EncodedElement, indentLevel: Int) {
        writer.writeNewline()
        writer.writeIndent(indentLevel)
        writer.writeDash()
        when (element) {
            is EncodedElement.Primitive -> {
                writer.writeSpace()
                writer.write(element.value)
            }
            is EncodedElement.Structure -> {
                // §10: an empty-object list item is the bare marker "-"
                if (element.entries.isNotEmpty()) {
                    writer.writeSpace()
                    writeNestedFields(element.entries, indentLevel, firstInline = true)
                }
            }
            is EncodedElement.NestedArray -> {
                writer.writeSpace()
                writeArray(null, element.elements, indentLevel, ArrayPosition.LIST_ITEM)
            }
        }
    }

    private fun writeArrayHeader(key: String?, size: Int) {
        val delim = config.delimiter.char
        if (key != null) {
            writer.writeArrayHeader(quoteKey(key), size, delim)
        } else {
            writer.write('[')
            writer.write(size)
            if (delim != ',') writer.write(delim)
            writer.write("]:")
        }
    }

    // ----- tabular and keyed tables -----

    private fun writeTabularTable(
        key: String?,
        elements: List<EncodedElement>,
        tree: List<FieldNode>,
        indentLevel: Int,
    ) {
        val delim = config.delimiter.char
        if (key != null) writer.write(quoteKey(key))
        writer.write('[')
        writer.write(elements.size)
        if (delim != ',') writer.write(delim)
        writer.write("]{")
        writeFieldTree(tree)
        writer.write("}:")

        elements.forEach { element ->
            writer.writeNewline()
            writer.writeIndent(indentLevel + 1)
            writeRowCells((element as EncodedElement.Structure).entries, tree, first = true)
        }
    }

    private fun writeKeyedTable(
        quotedKey: String?,
        entries: List<Pair<String, EncodedElement>>,
        tree: List<FieldNode>,
        indentLevel: Int,
    ) {
        val delim = config.delimiter.char
        if (quotedKey != null) writer.write(quotedKey)
        writer.write('[')
        writer.write(entries.size)
        writer.write(':')
        if (delim != ',') writer.write(delim)
        writer.write("]{")
        writeFieldTree(tree)
        writer.write("}:")

        entries.forEach { (entryKey, value) ->
            writer.writeNewline()
            writer.writeIndent(indentLevel + 1)
            writer.write(quoteKey(entryKey))
            writer.write(':')
            writer.writeSpace()
            writeRowCells((value as EncodedElement.Structure).entries, tree, first = true)
        }
    }

    private fun writeFieldTree(tree: List<FieldNode>) {
        tree.forEachIndexed { i, node ->
            if (i > 0) writer.writeDelimiter()
            writer.write(quoteKey(node.name))
            if (node.group != null) {
                writer.write('{')
                writeFieldTree(node.group)
                writer.write('}')
            }
        }
    }

    /**
     * Writes one row's cells: leaf entries in a depth-first, pre-order walk of the field tree
     * (§9.3). Returns whether any leaf has been written yet, to place delimiters between leaves
     * across group boundaries.
     */
    private fun writeRowCells(
        entries: List<Pair<String, EncodedElement>>,
        tree: List<FieldNode>,
        first: Boolean,
    ): Boolean {
        var isFirst = first
        for (column in tree.indices) {
            val node = tree[column]
            val value = entries.valueOf(node.name, column)
            if (node.group == null) {
                if (!isFirst) writer.writeDelimiter()
                isFirst = false
                writer.write((value as EncodedElement.Primitive).value)
            } else {
                isFirst =
                    writeRowCells(
                        (value as EncodedElement.Structure).entries,
                        node.group,
                        isFirst,
                    )
            }
        }
        return isFirst
    }
}
