package dev.runetools.mapper.asm

import dev.runetools.asm.tree.interfaceClasses
import dev.runetools.asm.tree.parent
import dev.runetools.asm.util.nullField
import dev.runetools.mapper.classifier.ClassifierUtil
import org.objectweb.asm.tree.ClassNode

var ClassNode.match: ClassNode? by nullField()
fun ClassNode.hasMatch() = match != null

fun ClassNode.match(other: ClassNode) {
    if(this.match == other) return
    if(other.match == this) return

    this.match = other
    other.match = this

    if(parent != null && other.parent != null) {
        if(ClassifierUtil.isMaybeEqual(parent!!, other.parent!!)) {
            parent!!.match(other.parent!!)
        }
    }

    val iterfs = interfaceClasses.toTypedArray()
    val otherIterfs = other.interfaceClasses.toTypedArray()
    if(iterfs.size == otherIterfs.size) {
        for(i in iterfs.indices) {
            val iterf = iterfs[i]
            val otherIterf = otherIterfs[i]
            if(ClassifierUtil.isMaybeEqual(iterf, otherIterf)) {
                iterf.match(otherIterf)
            }
        }
    }

    val clinit = methods.firstOrNull { it.name == "<clinit>" }
    val otherClinit = other.methods.firstOrNull { it.name == "<clinit>" }
    if(clinit != null && otherClinit != null) {
        if(ClassifierUtil.isMaybeEqual(clinit, otherClinit)) {
            clinit.match(otherClinit)
        }
    }

    val init = methods.firstOrNull { it.name == "<init>" }
    val otherInit = other.methods.firstOrNull { it.name == "<init>" }
    if(init != null && otherInit != null) {
        if(ClassifierUtil.isMaybeEqual(init!!, otherInit!!)) {
            init.match(otherInit)
        }
    }
}
