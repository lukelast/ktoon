package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/** One entry of a tabular or keyed header's field list; a group is a nested-uniform column. */
internal class FieldTreeNode(val name: String, val children: List<FieldTreeNode>?)

/**
 * Writes captured [EncodedElement] trees, selecting the form each value MUST take under §9:
 * inline, tabular (with nested field groups), list, or keyed tabular.
 */
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
        fun tabularTree(elements: List<EncodedElement>): List<FieldTreeNode>? {
            if (elements.isEmpty()) return null
            val objects =
                elements.map { (it as? EncodedElement.Structure)?.values ?: return null }
            return fieldTree(objects)
        }

        /**
         * §9.5 keyed tabular detection: at least two entries whose values are uniform non-empty
         * objects. Returns the field tree, or null.
         */
        fun keyedTree(values: List<Pair<String, EncodedElement>>): List<FieldTreeNode>? {
            if (values.size < 2) return null
            val objects =
                values.map { (_, v) -> (v as? EncodedElement.Structure)?.values ?: return null }
            return fieldTree(objects)
        }

        private fun fieldTree(
            objects: List<List<Pair<String, EncodedElement>>>
        ): List<FieldTreeNode>? {
            val first = objects.first()
            if (first.isEmpty()) return null // empty objects are excluded (§9.3)
            val names = first.map { it.first }
            val nameSet = names.toSet()
            if (names.size != nameSet.size) return null
            for (other in objects) {
                if (other.size != names.size) return null
                for ((name, _) in other) if (name !in nameSet) return null
            }

            val nodes = ArrayList<FieldTreeNode>(names.size)
            for (name in names) {
                val column = objects.map { obj -> obj.first { it.first == name }.second }
                when {
                    column.all { it is EncodedElement.Primitive } ->
                        nodes.add(FieldTreeNode(name, null))
                    column.all {
                        it is EncodedElement.Structure && it.values.isNotEmpty()
                    } -> {
                        val sub =
                            fieldTree(column.map { (it as EncodedElement.Structure).values })
                                ?: return null
                        nodes.add(FieldTreeNode(name, sub))
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
                    descriptor.elementsCount >= 2 && isObjectLike(descriptor.getElementDescriptor(1))
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

    /** Writes a captured root object: keyed tabular when eligible (§9.5), plain fields otherwise. */
    fun writeRootObject(values: List<Pair<String, EncodedElement>>) {
        val tree = keyedTree(values)
        if (tree != null) {
            writeKeyedTable(null, values, tree, 0)
            return
        }
        values.forEachIndexed { i, (name, element) ->
            if (i > 0) writer.writeNewline()
            writer.writeIndent(0)
            writeField(name, element, 0)
        }
    }

    /**
     * Writes a captured object in field position, starting at the key: a keyed table, a bare
     * `key:` for an empty object (§8), or `key:` with nested fields at depth +1.
     */
    fun writeObjectField(name: String, values: List<Pair<String, EncodedElement>>, indent: Int) {
        val tree = keyedTree(values)
        when {
            tree != null -> writeKeyedTable(quoteKey(name), values, tree, indent)
            values.isEmpty() -> writer.writeKey(quoteKey(name))
            else -> {
                writer.writeKey(quoteKey(name))
                writeStructureFields(values, indent, firstInline = false)
            }
        }
    }

    private fun writeField(name: String, element: EncodedElement, indent: Int) {
        when (element) {
            is EncodedElement.Primitive -> writer.writeKeyValue(quoteKey(name), element.value)
            is EncodedElement.NestedArray ->
                writeArray(name, element.elements, indent, ArrayPosition.FIELD)
            is EncodedElement.Structure -> writeObjectField(name, element.values, indent)
        }
    }

    /**
     * Writes an object's fields at depth [indent] + 1. When [firstInline] is true the first field
     * continues the current line (list-item objects, §10).
     */
    private fun writeStructureFields(
        values: List<Pair<String, EncodedElement>>,
        indent: Int,
        firstInline: Boolean,
    ) {
        values.forEachIndexed { i, (name, value) ->
            if (i > 0 || !firstInline) {
                writer.writeNewline()
                writer.writeIndent(indent + 1)
            }
            writeField(name, value, indent + 1)
        }
    }

    // ----- arrays -----

    fun writeArray(
        key: String?,
        elements: List<EncodedElement>,
        indent: Int,
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
            writeTabularTable(key, elements, tree, indent)
        } else {
            writeListArray(key, elements, indent)
        }
    }

    private fun writeEmptyArray(key: String?, position: ArrayPosition) {
        when (position) {
            // §9.1: canonical empty-array forms
            ArrayPosition.FIELD -> {
                writer.writeKey(quoteKey(key!!))
                writer.writeSpace()
                writer.write("[]")
            }
            ArrayPosition.ROOT -> writer.write("[]")
            // §9.2/§9.4: encoders still emit the legacy form in list-item position
            ArrayPosition.LIST_ITEM -> writer.write("[0]:")
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

    private fun writeListArray(key: String?, elements: List<EncodedElement>, indent: Int) {
        writeArrayHeader(key, elements.size)
        elements.forEach { writeListItem(it, indent + 1) }
    }

    private fun writeListItem(element: EncodedElement, indent: Int) {
        writer.writeNewline()
        writer.writeIndent(indent)
        writer.writeDash()
        when (element) {
            is EncodedElement.Primitive -> {
                writer.writeSpace()
                writer.write(element.value)
            }
            is EncodedElement.Structure -> {
                // §10: an empty-object list item is the bare marker "-"
                if (element.values.isNotEmpty()) {
                    writer.writeSpace()
                    writeStructureFields(element.values, indent, firstInline = true)
                }
            }
            is EncodedElement.NestedArray -> {
                writer.writeSpace()
                writeArray(null, element.elements, indent, ArrayPosition.LIST_ITEM)
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
        tree: List<FieldTreeNode>,
        indent: Int,
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
            writer.writeIndent(indent + 1)
            writeRowCells((element as EncodedElement.Structure).values, tree, first = true)
        }
    }

    private fun writeKeyedTable(
        quotedKey: String?,
        entries: List<Pair<String, EncodedElement>>,
        tree: List<FieldTreeNode>,
        indent: Int,
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
            writer.writeIndent(indent + 1)
            writer.write(quoteKey(entryKey))
            writer.write(':')
            writer.writeSpace()
            writeRowCells((value as EncodedElement.Structure).values, tree, first = true)
        }
    }

    private fun writeFieldTree(tree: List<FieldTreeNode>) {
        tree.forEachIndexed { i, node ->
            if (i > 0) writer.writeDelimiter()
            writer.write(quoteKey(node.name))
            if (node.children != null) {
                writer.write('{')
                writeFieldTree(node.children)
                writer.write('}')
            }
        }
    }

    /**
     * Writes one row's cells: leaf values in a depth-first, pre-order walk of the field tree
     * (§9.3). Returns whether any leaf has been written yet, to place delimiters between leaves
     * across group boundaries.
     */
    private fun writeRowCells(
        values: List<Pair<String, EncodedElement>>,
        tree: List<FieldTreeNode>,
        first: Boolean,
    ): Boolean {
        var isFirst = first
        for (node in tree) {
            val value = values.first { it.first == node.name }.second
            if (node.children == null) {
                if (!isFirst) writer.writeDelimiter()
                isFirst = false
                writer.write((value as EncodedElement.Primitive).value)
            } else {
                isFirst =
                    writeRowCells(
                        (value as EncodedElement.Structure).values,
                        node.children,
                        isFirst,
                    )
            }
        }
        return isFirst
    }
}
