package dev.runetools.asm.tree

import dev.runetools.asm.util.field
import dev.runetools.asm.util.hashSetField
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
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
val FieldNode.writeRefs: HashSet<MethodNode> by hashSetField()
val FieldNode.readRefs: HashSet<MethodNode> by hashSetField()

val FieldNode.id: String get() = "${owner.id}.$name"
val FieldNode.type get() = Type.getType(desc)

fun FieldNode.isStatic() = (access and ACC_STATIC) != 0
fun FieldNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun FieldNode.isFinal() = (access and ACC_FINAL) != 0

val FieldNode.hierarchy: Set<FieldNode> get() {
    return owner.superClasses.plus(owner.subClasses).mapNotNull { it.getField(name, desc) }.toSet()
}

internal fun FieldNode.reset() {
    writeRefs.clear()
    readRefs.clear()
}

internal fun FieldNode.build(step: Int) {

}