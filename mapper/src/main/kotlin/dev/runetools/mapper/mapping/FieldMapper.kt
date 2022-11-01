package dev.runetools.mapper.mapping

import com.google.common.collect.LinkedHashMultimap
import dev.runetools.asm.tree.ClassPool
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassifierUtil
import dev.runetools.mapper.classifier.FieldClassifier
import org.objectweb.asm.tree.FieldNode

class FieldMapper {

    private val mappings = LinkedHashMultimap.create<FieldNode, FieldNode>()

    private fun ClassPool.fields() = classes.flatMap { it.fields }.toSet()

    private fun ClassPool.filterPotentialMatches(field: FieldNode) = classes.flatMap { it.fields }
        .filter { ClassifierUtil.isMaybeEqual(it, field) }
        .toSet()

    fun map(fromPool: ClassPool, toPool: ClassPool): NodeMappings {
        fromPool.fields().forEach { from ->
            mappings.putAll(from, toPool.filterPotentialMatches(from))
        }

        val toMerge = mutableListOf<NodeMappings>()

        ConsoleProgressBar.enable("Fields", mappings.values().size)
        mappings.keySet().forEach { from ->
            val toSet = mappings.get(from)
            val mapping = FieldClassifier.rank(from, toSet) ?: return@forEach
            val nodeMappings = NodeMappings()
            nodeMappings.map(mapping.from, mapping.to).also {
                it.score = mapping.weight
            }
            toMerge.add(nodeMappings)
        }
        ConsoleProgressBar.disable()

        val mergeMappings = NodeMappings()
        toMerge.forEach { mergeMappings.merge(it) }
        return mergeMappings
    }
}