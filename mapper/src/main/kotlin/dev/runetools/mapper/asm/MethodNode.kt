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

    val retCls = pool.findClass(this.type.returnType.internalName)
    val otherRetCls = other.pool.findClass(other.type.returnType.internalName)

    if(retCls != null && otherRetCls != null && ClassifierUtil.isMaybeEqual(retCls, otherRetCls)) {
        retCls.match(otherRetCls)
    }

    val argClasses = type.argumentTypes.mapNotNull { pool.findClass(it.internalName) }
    val otherArgClasses = type.argumentTypes.mapNotNull { pool.findClass(it.internalName) }

    if(argClasses.size != otherArgClasses.size) {
        for(i in argClasses.indices) {
            val argCls = argClasses[i]
            val otherArgCls = otherArgClasses[i]
            if(ClassifierUtil.isMaybeEqual(argCls, otherArgCls)) {
                argCls.match(otherArgCls)
            }
        }
    }

    this.match = other
    other.match = this

    if(!this.isStatic() && !other.isStatic()) {
        this.owner.match(other.owner)
    }
}