package dev.runetools.mapper.asm

import dev.runetools.asm.tree.isStatic
import dev.runetools.asm.tree.owner
import dev.runetools.asm.tree.pool
import dev.runetools.asm.tree.type
import dev.runetools.asm.util.nullField
import dev.runetools.mapper.classifier.ClassifierUtil
import org.objectweb.asm.tree.MethodNode

var MethodNode.match: MethodNode? by nullField()
fun MethodNode.hasMatch() = match != null

fun MethodNode.match(other: MethodNode) {
    if(this.match == other) return
    if(other.match == this) return

    this.match = other
    other.match = this

    if(!this.isStatic() && !other.isStatic()) {
        this.owner.match(other.owner)
    }
}