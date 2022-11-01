package dev.runetools.mapper.mapping

import com.google.common.collect.LinkedHashMultimap
import dev.runetools.asm.tree.ClassPool
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassClassifier
import dev.runetools.mapper.classifier.ClassifierUtil
import org.objectweb.asm.tree.ClassNode

class ClassMapper {

    private val mappings = LinkedHashMultimap.create<ClassNode, ClassNode>()

    private fun ClassPool.filterPotentialMatches(cls: ClassNode) = classes.filter { ClassifierUtil.isMaybeEqual(it, cls) }.toSet()

    fun map(fromPool: ClassPool, toPool: ClassPool): NodeMappings {
        fromPool.classes.forEach { from ->
            mappings.putAll(from, toPool.filterPotentialMatches(from))
        }

        val toMerge = mutableListOf<NodeMappings>()

        ConsoleProgressBar.enable("Classes", mappings.values().size)
        mappings.keySet().forEach { from ->
            val toSet = mappings.get(from)
            val mapping = ClassClassifier.rank(from, toSet) ?: return@forEach
            ConsoleProgressBar.stepBy(toSet.size)
            val nodeMappings = NodeMappings()
            nodeMappings.map(mapping.from, mapping.to).also {
                it.weight = mapping.weight
            }
            toMerge.add(nodeMappings)
        }
        ConsoleProgressBar.disable()

        val mergeMappings = NodeMappings()
        toMerge.forEach { mergeMappings.merge(it) }
        return mergeMappings
    }
}