package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/** One entry of a tabular or keyed header's field list; a group is a nested-uniform column. */
internal class FieldNode(val name: String, val group: List<FieldNode>?)

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
            val nameSet = names.toSet()
            if (names.size != nameSet.size) return null
            for (other in objects) {
                if (other.size != names.size) return null
                for ((name, _) in other) if (name !in nameSet) return null
            }

            val nodes = ArrayList<FieldNode>(names.size)
            for (name in names) {
                val column = objects.map { obj -> obj.first { it.first == name }.second }
                when {
                    column.all { it is EncodedElement.Primitive } ->
                        nodes.add(FieldNode(name, null))
                    column.all { it is EncodedElement.Structure && it.entries.isNotEmpty() } -> {
                        val sub =
                            fieldTree(column.map { (it as EncodedElement.Structure).entries })
                                ?: return null
                        nodes.add(FieldNode(name, sub))
                    }
                    else -> return null
                }
            }
            return nodes
        }

        /**
         * Cheap descriptor pre-gate for keyed tabular capture: an object can only be keyed when
         * every field is itself object-shaped (§9.5). Values decide; this just avoids capturing
         * objects that obviously cannot qualify.
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun couldBeKeyed(descriptor: SerialDescriptor): Boolean =
            when (descriptor.kind) {
                StructureKind.MAP ->
                    descriptor.elementsCount >= 2 &&
                        isObjectLike(descriptor.getElementDescriptor(1))
                StructureKind.CLASS,
                StructureKind.OBJECT ->
                    descriptor.elementsCount >= 2 &&
                        (0 until descriptor.elementsCount).all {
                            isObjectLike(descriptor.getElementDescriptor(it))
                        }
                else -> false
            }

        @OptIn(ExperimentalSerializationApi::class)
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
        for (node in tree) {
            val value = entries.first { it.first == node.name }.second
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
