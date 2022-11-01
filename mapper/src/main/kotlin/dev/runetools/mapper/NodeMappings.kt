package dev.runetools.mapper

import com.google.common.collect.HashMultimap
import dev.runetools.asm.tree.id
import dev.runetools.asm.util.linkedListOf
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class NodeMappings {

    private var mappings = HashMultimap.create<Any, Mapping>()

    fun getMapping(from: Any, to: Any): Mapping {
        mappings.get(from).forEach { mapping ->
            if(mapping.to == to) return mapping
        }
        val mapping = Mapping(from, to)
        mappings.put(from, mapping)
        return mapping
    }

    fun getBest(from: Any): Any? {
        var best: Mapping? = null
        mappings.get(from).forEach { mapping ->
            if(best == null || mapping.score > best!!.score) {
                best = mapping
            } else if(mapping.score == best!!.score && from.idString > best!!.to.idString) {
                best = mapping
            }
        }
        return best?.to
    }

    fun merge(other: NodeMappings) {
        other.mappings.entries().forEach {
            val from = it.key
            val mapping = getMapping(from, it.value.to)
            mapping.merge(it.value)
        }
    }

    fun map(from: Any, to: Any): Mapping {
        val mapping = getMapping(from, to)
        mapping.score++
        return mapping
    }

    fun reduce() {
        val sorted = linkedListOf<Mapping>(*mappings.values().toTypedArray())
        sorted.sortWith { a, b ->
            a.score.compareTo(b.score).takeIf { it != 0 }?.also {
                return@sortWith it
            }
            if(a.weight != b.weight) {
                return@sortWith a.weight.compareTo(b.weight)
            }
            return@sortWith a.from.idString.compareTo(b.from.idString)
        }
        sorted.reverse()

        val reduced = HashMultimap.create<Any, Mapping>()
        val reversed = hashMapOf<Any, Any>()

        sorted.forEach { mapping ->
            if(reduced.containsKey(mapping.from)) return@forEach
            if(reversed.containsKey(mapping.to)) return@forEach
            reduced.put(mapping.from, mapping)
            reversed[mapping.to] = mapping.from
        }
        mappings = reduced
    }

    operator fun get(from: Any): Any? = getBest(from)

    fun getMappings(from: Any) = mappings.get(from)

    fun asMap(): Map<Any, Any> {
        val ret = hashMapOf<Any, Any>()
        mappings.keySet().forEach { from ->
            ret[from] = getBest(from)!!
        }
        return ret
    }

    private val Any.idString: String get() = when(this) {
        is ClassNode -> this.id
        is MethodNode -> this.id
        is FieldNode -> this.id
        else -> this.toString()
    }
}