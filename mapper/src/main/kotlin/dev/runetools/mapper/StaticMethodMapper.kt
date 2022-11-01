package dev.runetools.mapper

import com.google.common.collect.LinkedHashMultimap
import dev.runetools.asm.tree.ClassPool
import dev.runetools.asm.tree.isStatic
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassifierUtil
import dev.runetools.mapper.classifier.StaticMethodClassifier
import org.objectweb.asm.tree.MethodNode

class StaticMethodMapper {

    private val mappings =  LinkedHashMultimap.create<MethodNode, MethodNode>()

    private fun ClassPool.staticMethods() = this.classes.flatMap { it.methods }
        .filter { it.isStatic() }
        .toSet()

    private fun ClassPool.filterPotentialMatches(method: MethodNode) = staticMethods()
        .filter { ClassifierUtil.isMaybeEqual(it, method) }
        .toSet()

    fun map(fromPool: ClassPool, toPool: ClassPool): NodeMappings {
        fromPool.staticMethods().forEach { from ->
            mappings.putAll(from, toPool.filterPotentialMatches(from))
        }

        val toMerge = mutableSetOf<NodeMappings>()

        ConsoleProgressBar.enable("Static Methods", mappings.values().size)
        mappings.keySet().forEach { from ->
            val toSet = mappings.get(from)
            val mapping = StaticMethodClassifier.rank(from, toSet) ?: return@forEach
            val nodeMappings = NodeMappings()
            nodeMappings.map(mapping.from, mapping.to).also {
                it.score = mapping.weight
            }
            toMerge.add(nodeMappings)
        }
        ConsoleProgressBar.disable()

        val mergeMapping = NodeMappings()
        toMerge.forEach { mergeMapping.merge(it) }
        return mergeMapping
    }
}