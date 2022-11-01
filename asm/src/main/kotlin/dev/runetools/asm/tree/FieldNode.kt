package dev.runetools.asm.tree

import dev.runetools.asm.util.field
import dev.runetools.asm.util.listField
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.util.LinkedList

internal fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}

var FieldNode.owner: ClassNode by field()
val FieldNode.pool: ClassPool get() = owner.pool

var FieldNode.index: Int by field { -1 }
val FieldNode.writeRefs: LinkedList<MethodNode> by listField()
val FieldNode.readRefs: LinkedList<MethodNode> by listField()

val FieldNode.id: String get() = "${owner.id}.$name"

val FieldNode.hierarchy: Set<FieldNode> get() {
    return owner.superClasses.plus(owner.subClasses).mapNotNull { it.getField(name, desc) }.toSet()
}

internal fun FieldNode.reset() {
    writeRefs.clear()
    readRefs.clear()
}

internal fun FieldNode.build(step: Int) {

}