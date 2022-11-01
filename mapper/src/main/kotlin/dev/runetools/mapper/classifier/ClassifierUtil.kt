package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.*
import dev.runetools.mapper.asm.hasMatch
import dev.runetools.mapper.asm.match
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AbstractInsnNode.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ClassifierUtil {

    fun String.isObfuscatedName(): Boolean {
        return (this.length <= 3 && this !in listOf("run", "add")) || (listOf("class", "method", "field", "var").any { this.startsWith(it) })
    }

    fun isNameMaybeEqual(a: String, b: String): Boolean {
        if(!a.isObfuscatedName() && !b.isObfuscatedName()) {
            return a == b
        }
        return true
    }

    fun isMaybeEqual(a: Type, b: Type): Boolean {
        if(a == b) return true
        if(a.sort != b.sort) return false
        if(a.sort == Type.ARRAY && b.sort == Type.ARRAY) {
            if(a.dimensions != b.dimensions) return false
            if(!isMaybeEqual(a.elementType, b.elementType)) return false
        }
        if(a.sort == Type.OBJECT && b.sort == Type.OBJECT) {
            if(!isNameMaybeEqual(a.internalName, b.internalName)) return false
        }
        return true
    }

    fun isMaybeEqual(a: ClassNode, b: ClassNode): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!isMaybeEqual(a.type, b.type)) return false
        if(!isNameMaybeEqual(a.name, b.name)) return false
        return true
    }

    fun isMaybeEqual(a: MethodNode, b: MethodNode): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isStatic() && !b.isStatic()) {
            if(!isMaybeEqual(a.owner, b.owner)) return false
        }
        if(!isNameMaybeEqual(a.name, b.name)) return false
        if(!isMaybeEqual(a.type.returnType, b.type.returnType)) return false
        if(a.type.argumentTypes.size != b.type.argumentTypes.size) return false
        for(i in a.type.argumentTypes.indices) {
            val argA = a.type.argumentTypes[i]
            val argB = b.type.argumentTypes[i]
            if(!isMaybeEqual(argA, argB)) return false
        }
        return true
    }

    fun isMaybeEqual(a: FieldNode, b: FieldNode): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isStatic() && !b.isStatic()) {
            if(!isMaybeEqual(a.owner, b.owner)) return false
        }
        if(!isNameMaybeEqual(a.name, b.name)) return false
        if(!isMaybeEqual(a.type, b.type)) return false
        return true
    }

    fun compareCounts(countA: Int, countB: Int): Double {
        val delta = abs(countA - countB)
        if(delta == 0) return 1.0
        return 1 - delta.toDouble() / max(countA, countB)
    }

    fun <T> compareSets(setA: Set<T>, setB: Set<T>): Double {
        val newSetA = mutableSetOf<T>().also { it.addAll(setA) }
        val newSetB = mutableSetOf<T>().also { it.addAll(setB) }

        val oldSize = newSetB.size
        newSetB.removeAll(newSetA)

        val matched = oldSize - newSetB.size
        val total = newSetA.size - matched + oldSize

        return if(total == 0) 1.0 else matched.toDouble() / total
    }

    fun <T> compareNodeSets(a: Set<T>, b: Set<T>, getMatch: (T) -> T?, predicate: (a: T, b: T) -> Boolean): Double {
        if(a.isEmpty() || b.isEmpty()) {
            return if(a.isEmpty() && b.isEmpty()) 1.0 else 0.0
        }

        val setA = mutableSetOf<T>().also { it.addAll(a) }
        val setB = mutableSetOf<T>().also { it.addAll(b) }

        val total = setA.size + setB.size
        var unmatched = 0

        val itrA1 = setA.iterator()
        while(itrA1.hasNext()) {
            val nodeA = itrA1.next()
            if(setB.remove(nodeA)) {
                itrA1.remove()
            } else if(getMatch(nodeA) != null) {
                if(!setB.remove(getMatch(nodeA))) {
                    unmatched++
                }
                itrA1.remove()
            }
        }

        val itrA = setA.iterator()
        while(itrA.hasNext()) {
            val nodeA = itrA.next()
            var found = false
            val itrB = setB.iterator()
            while(itrB.hasNext()) {
                val nodeB = itrB.next()
                if(predicate(nodeA, nodeB)) {
                    found = true
                    break
                }
            }
            if(!found) {
                unmatched++
                itrA.remove()
            }
        }


        setB.forEach { nodeB ->
            var found = false
            setA.apply {
                forEach { nodeA ->
                    if (predicate(nodeA, nodeB)) {
                        found = true
                        return@apply
                    }
                }
            }
            if(!found) {
                unmatched++
            }
        }

        return ((total - unmatched) / total).toDouble()
    }

    fun compareClassSets(setA: Set<ClassNode>, setB: Set<ClassNode>): Double {
        return compareNodeSets(setA, setB, ClassNode::match, ClassifierUtil::isMaybeEqual)
    }

    fun compareMethodSets(setA: Set<MethodNode>, setB: Set<MethodNode>): Double {
        return compareNodeSets(setA, setB, MethodNode::match, ClassifierUtil::isMaybeEqual)
    }

    fun compareFieldSets(setA: Set<FieldNode>, setB: Set<FieldNode>): Double {
        return compareNodeSets(setA, setB, FieldNode::match, ClassifierUtil::isMaybeEqual)
    }

    fun <T> compareInsns(methodA: MethodNode, methodB: MethodNode): Double {
        return compareLists(methodA.instructions.toList(), methodB.instructions.toList(), List<AbstractInsnNode>::get, List<AbstractInsnNode>::size) { a, b ->
            compareInsns<T>(a, b, methodA.instructions.toList(), methodB.instructions.toList(), methodA, methodB)
        }
    }

    private fun <T> compareInsns(insnA: AbstractInsnNode, insnB: AbstractInsnNode, insnsA: List<AbstractInsnNode> , insnsB: List<AbstractInsnNode>, methodA: MethodNode, methodB: MethodNode): Int {
        if(insnA.opcode != insnB.opcode) return COMPARED_UNIQUE
        when(insnA.type) {
            INT_INSN -> {
                val a = insnA as IntInsnNode
                val b = insnB as IntInsnNode
                return if(a.operand == b.operand) COMPARED_SIMILAR else COMPARED_UNIQUE
            }
            VAR_INSN -> {
                val a = insnA as VarInsnNode
                val b = insnB as VarInsnNode
                return if(a.`var` == b.`var`) COMPARED_SIMILAR else COMPARED_UNIQUE
            }
            TYPE_INSN -> {
                val a = insnA as TypeInsnNode
                val b = insnB as TypeInsnNode
                val clsA = methodA.pool.findClass(a.desc)
                val clsB = methodB.pool.findClass(b.desc)
                if(clsA == null && clsB == null) return COMPARED_SIMILAR
                if(clsA == null || clsB == null) return COMPARED_UNIQUE
                return if(isMaybeEqual(clsA, clsB)) COMPARED_SIMILAR else COMPARED_UNIQUE
            }
            FIELD_INSN -> {
                val a = insnA as FieldInsnNode
                val b = insnB as FieldInsnNode
                val clsA = methodA.pool.findClass(a.owner)
                val clsB = methodB.pool.findClass(b.owner)
                if(clsA == null && clsB == null) return COMPARED_SIMILAR
                if(clsA == null || clsB == null) return COMPARED_UNIQUE
                val fieldA = clsA.resolveField(a.name, a.desc)
                val fieldB = clsB.resolveField(b.name, b.desc)
                if(fieldA == null && fieldB == null) return COMPARED_SIMILAR
                if(fieldA == null || fieldB == null) return COMPARED_UNIQUE
                return if(isMaybeEqual(fieldA, fieldB)) COMPARED_SIMILAR else COMPARED_UNIQUE
            }
        }
        return COMPARED_SIMILAR
    }

    fun <T> comparePositions(a: T, b: T, getMatch: (T) -> T?, getPosition: (T) -> Int, getSiblings: (T) -> Set<T>): Double {
        val posA = getPosition(a)
        val posB = getPosition(b)
        val siblingsA = getSiblings(a)
        val siblingsB = getSiblings(b)

        if(posA == posB && siblingsA.size == siblingsB.size) return 1.0
        if(posA == -1 || posB == -1) return if(posA == posB) 1.0 else 0.0

        var startPosA = 0
        var startPosB = 0
        var endPosA = siblingsA.size
        var endPosB = siblingsB.size

        if(posA > 0) {
            for(i in posA - 1 downTo 0) {
                val c = getSiblings(a).first { getPosition(it) == i }
                val match = getMatch(c)
                if(match != null) {
                    endPosA = i
                    endPosB = getPosition(match)
                    break
                }
            }
        }

        if(startPosB >= endPosB || startPosB > posB || endPosB <= posB) {
            startPosA = 0
            startPosB = 0
            endPosA = siblingsA.size
            endPosB = siblingsB.size
        }

        val relPosA = getRelativePosition(posA - startPosA, endPosA - startPosA)
        val relPosB = getRelativePosition(posB - startPosB, endPosB - startPosB)

        return 1.0 - abs(relPosA - relPosB)
    }

    private fun getRelativePosition(pos: Int, size: Int): Double {
        if(size == 1) return 0.5
        return (pos / (size - 1)).toDouble()
    }

    const val COMPARED_SIMILAR = 0
    const val COMPARED_POSSIBLE = 1
    const val COMPARED_UNIQUE = 2

    fun <T, U> compareLists(listA: T, listB: T, getter: T.(Int) -> U, size: T.() -> Int, compare: (a: U, b: U) -> Int): Double {
        val sizeA = listA.size()
        val sizeB = listB.size()

        if(sizeA == 0 && sizeB == 0) return 1.0
        if(sizeA == 0 || sizeB == 0) return 0.0

        if(sizeA == sizeB) {
            var match = true
            for(i in 0 until sizeA) {
                if(compare(listA.getter(i), listB.getter(i)) != COMPARED_SIMILAR) {
                    match = false
                    break
                }
            }
            if(match) return 1.0
        }

        val v0 = IntArray(sizeB + 1)
        val v1 = IntArray(sizeB + 1)

        for(i in 1 until v0.size) {
            v0[i] = i * COMPARED_UNIQUE
        }

        for(i in 0 until sizeA) {
            v1[0] = (i + 1) * COMPARED_UNIQUE

            for(j in 0 until sizeB) {
                val cost = compare(listA.getter(i), listB.getter(j))
                v1[j + 1] = min(min(v1[j] + COMPARED_UNIQUE, v0[j + 1] + COMPARED_UNIQUE), v0[j] + cost)
            }

            for(j in v0.indices) {
                v0[j] = v1[j]
            }
        }

        val distance = v1[sizeB]
        val upperBound = max(sizeA, sizeB) * COMPARED_UNIQUE
        return 1.0 - (distance / upperBound).toDouble()
    }

    fun <T> runParallel(from: T, toSet: Set<T>, action: (from: T, to: T) -> Unit) {
        toSet.parallelStream().forEach { to ->
            action(from, to)
        }
    }
}