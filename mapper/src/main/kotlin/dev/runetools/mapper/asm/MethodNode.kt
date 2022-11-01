package dev.runetools.mapper.asm

import dev.runetools.asm.tree.isStatic
import dev.runetools.asm.tree.owner
import dev.runetools.asm.util.nullField
import org.objectweb.asm.tree.MethodNode

var MethodNode.match: MethodNode? by nullField()
fun MethodNode.hasMatch() = match != null

fun MethodNode.match(other: MethodNode) {
    if(this.match == other) return
    this.match = other
    other.match = this
}