package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.id
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class MatchResult<T>(val from: T, val to: T, val score: Double) : Comparable<MatchResult<T>> {

    override fun compareTo(other: MatchResult<T>): Int {
        return score.compareTo(other.score)
    }

    override fun toString(): String {
        val names = when(from) {
            is ClassNode -> from.id to (to as ClassNode).id
            is MethodNode -> from.id to (to as MethodNode).id
            is FieldNode -> from.id to (to as FieldNode).id
            else -> from.toString() to to.toString()
        }
        return "[from: ${names.first}, to: ${names.second}, score: $score]"
    }
}