package dev.runetools.asm.tree

import dev.runetools.asm.util.field
import dev.runetools.asm.util.hashSetField
import dev.runetools.asm.util.nullField
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

internal fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    methods.forEach { it.init(this) }
    fields.forEach { it.init(this) }
}

var ClassNode.pool: ClassPool by field()
var ClassNode.ignored: Boolean by field { false }

val ClassNode.id: String get() = name
val ClassNode.type get() = Type.getObjectType(name)

var ClassNode.parent: ClassNode? by nullField()
val ClassNode.children: HashSet<ClassNode> by hashSetField()
val ClassNode.interfaceClasses: HashSet<ClassNode> by hashSetField()
val ClassNode.implementers: HashSet<ClassNode> by hashSetField()

val ClassNode.methodTypeRefs: HashSet<MethodNode> by hashSetField()
val ClassNode.fieldTypeRefs: HashSet<FieldNode> by hashSetField()

val ClassNode.strings: HashSet<String> get() {
    val results = hashSetOf<String>()
    fields.forEach { field ->
        if(field.value != null && field.value is String) results.add(field.value as String)
    }
    methods.forEach { method ->
        results.addAll(method.strings)
    }
    return results
}

fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getField(name: String, desc: String) = fields.firstOrNull { it.name == name && it.desc == desc }

val ClassNode.superClasses: Set<ClassNode> get() {
    return interfaceClasses
        .plus(parent)
        .filterNotNull()
        .flatMap { it.superClasses.plus(it) }
        .toSet()
}

val ClassNode.subClasses: Set<ClassNode> get() {
    return implementers
        .plus(children)
        .flatMap { it.subClasses.plus(it) }
        .toSet()
}

val ClassNode.hierarchy: Set<ClassNode> get() {
    return superClasses.plus(subClasses).plus(this).distinct().toSet()
}

fun ClassNode.resolveMethod(name: String, desc: String): MethodNode? {
    for(rel in superClasses) {
        return rel.resolveMethod(name, desc) ?: continue
    }
    return getMethod(name, desc)
}

fun ClassNode.resolveField(name: String, desc: String): FieldNode? {
    for(rel in superClasses) {
        return rel.resolveField(name, desc) ?: continue
    }
    return getField(name, desc)
}

fun ClassNode.accept(data: ByteArray) {
    val reader = ClassReader(data)
    reader.accept(this, ClassReader.SKIP_FRAMES)
}

fun ClassNode.toByteArray(): ByteArray {
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
    this.accept(writer)
    return writer.toByteArray()
}

internal fun ClassNode.reset() {
    parent = null
    children.clear()
    interfaceClasses.clear()
    implementers.clear()
    methods.forEach { it.reset() }
    fields.forEach { it.reset() }
}

internal fun ClassNode.build(step: Int) {
    when(step) {
        0 -> {
            parent = pool.findClass(superName)
            if(parent != null) {
                parent!!.children.add(this)
            }

            interfaces.mapNotNull { pool.findClass(it) }.forEach {
                interfaceClasses.add(it)
                it.implementers.add(this)
            }
        }
        1 -> {
            methods.forEachIndexed { index, method ->
                method.index = index
            }
            fields.forEachIndexed { index, field ->
                field.index = index
            }
        }
    }
    methods.forEach { it.build(step) }
    fields.forEach { it.build(step) }
}