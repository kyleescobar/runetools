package dev.runetools.mapper

import com.google.common.collect.LinkedHashMultimap
import dev.runetools.asm.tree.ClassPool
import dev.runetools.asm.tree.isStatic
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassifierUtil
import dev.runetools.mapper.classifier.MethodClassifier
import org.objectweb.asm.tree.MethodNode

class MemberMethodMapper {

    private val mappings = LinkedHashMultimap.create<MethodNode, MethodNode>()

    private fun ClassPool.memberMethods() = classes.flatMap { it.methods }.filter { !it.isStatic() }.toSet()

    private fun ClassPool.filterPotentialMatches(method: MethodNode) = memberMethods()
        .filter { ClassifierUtil.isMaybeEqual(it, method) }
        .toSet()

    fun map(fromPool: ClassPool, toPool: ClassPool): NodeMappings {
        fromPool.memberMethods().forEach { from ->
            mappings.putAll(from, toPool.filterPotentialMatches(from))
        }

        val toMerge = mutableListOf<NodeMappings>()

        ConsoleProgressBar.enable("Member Methods", mappings.values().size)
        mappings.keySet().forEach { from ->
            val toSet = mappings.get(from)
            val mapping = MethodClassifier.rank(from, toSet) ?: return@forEach
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