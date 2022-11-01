package dev.runetools.asm.tree

import dev.runetools.asm.util.field
import dev.runetools.asm.util.listField
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.AbstractInsnNode.VAR_INSN
import java.lang.reflect.Modifier
import java.util.*


internal fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}

var MethodNode.owner: ClassNode by field()
val MethodNode.pool: ClassPool get() = owner.pool

val MethodNode.args: LinkedList<LocalVariable> by listField()
val MethodNode.vars: LinkedList<LocalVariable> by listField()

var MethodNode.index: Int by field { -1 }
val MethodNode.strings: LinkedList<String> by listField()
val MethodNode.numbers: LinkedList<Number> by listField()
val MethodNode.refsIn: LinkedList<MethodNode> by listField()
val MethodNode.refsOut: LinkedList<MethodNode> by listField()
val MethodNode.fieldWriteRefs: LinkedList<FieldNode> by listField()
val MethodNode.fieldReadRefs: LinkedList<FieldNode> by listField()
val MethodNode.classRefs: LinkedList<ClassNode> by listField()

val MethodNode.id: String get() = "${owner.id}.$name$desc"
val MethodNode.type get() = Type.getMethodType(desc)

fun MethodNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun MethodNode.isAbstract() = (access and ACC_ABSTRACT) != 0
fun MethodNode.isStatic() = (access and ACC_STATIC) != 0

val MethodNode.hierarchy: Set<MethodNode> get() {
    return owner.superClasses.plus(owner.subClasses).mapNotNull { it.getMethod(name, desc) }.toSet()
}

internal fun MethodNode.reset() {
    index = -1
    strings.clear()
    refsIn.clear()
    refsOut.clear()
    fieldWriteRefs.clear()
    fieldReadRefs.clear()
    classRefs.clear()
}
internal fun MethodNode.build(step: Int) {
    when(step) {
        0 -> {
            gatherArgs()
            gatherVars()
        }
        1 -> {
            instructions.forEach { insn ->
                when(insn) {
                    is MethodInsnNode -> {
                        val dst = pool.findClass(insn.owner)?.resolveMethod(insn.name, insn.desc) ?: return@forEach
                        dst.refsIn.add(this)
                        refsOut.add(dst)
                        dst.owner.methodTypeRefs.add(this)
                        classRefs.add(dst.owner)
                    }
                    is FieldInsnNode -> {
                        val dst = pool.findClass(insn.owner)?.resolveField(insn.name, insn.desc) ?: return@forEach
                        if(insn.opcode in setOf(GETFIELD, GETSTATIC)) {
                            dst.readRefs.add(this)
                            fieldReadRefs.add(dst)
                        } else {
                            dst.writeRefs.add(this)
                            fieldWriteRefs.add(dst)
                        }
                        dst.owner.methodTypeRefs.add(this)
                        classRefs.add(dst.owner)

                        if(dst.value != null) {
                            val fieldValue = dst.value!!
                            if(fieldValue is String) {
                                strings.add(fieldValue)
                            } else if(fieldValue is Number) {
                                numbers.add(fieldValue)
                            }
                        }
                    }
                    is TypeInsnNode -> {
                        val dst = pool.findClass(insn.desc) ?: return@forEach
                        dst.methodTypeRefs.add(this)
                        classRefs.add(dst)
                    }
                    is LdcInsnNode -> {
                        val cst = insn.cst
                        if(cst is String && cst.isNotBlank()) {
                            strings.add(cst)
                        }
                        else if(cst is Number) {
                            numbers.add(cst)
                        }
                    }
                    is IntInsnNode -> {
                        numbers.add(insn.operand)
                    }
                    is InsnNode -> {
                        when(insn.opcode) {
                            in ICONST_M1..ICONST_5 -> numbers.add(insn.opcode - ICONST_0)
                            in DCONST_0..DCONST_1 -> numbers.add(insn.opcode - DCONST_0)
                            in LCONST_0..LCONST_1 -> numbers.add(insn.opcode - LCONST_0)
                            in FCONST_0..FCONST_2 -> numbers.add(insn.opcode - FCONST_0)
                        }
                    }
                }
            }
        }
    }
}

fun MethodNode.gatherArgs() {
    val argTypes = type.argumentTypes
    if(argTypes.isEmpty()) return
    if(instructions == null || instructions.size() == 0) return

    val args = hashMapOf<Int, LocalVariable>()
    val locals = localVariables
    val insns = instructions.toArray()
    val firstInsn = insns.first()

    var lvIdx = if(Modifier.isStatic(access)) 0 else 1
    for(i in argTypes.indices) {
        val type = argTypes[i]
        val typeClass = pool.findClass(type.descriptor)
        var asmIndex = -1
        var startInsn = -1
        var endInsn = -1
        var name: String? = null

        if(locals != null) {
            for(j in locals.indices) {
                val lv = locals[j]
                if(lv.index == lvIdx && lv.start == firstInsn) {
                    asmIndex = j
                    startInsn = insns.indexOf(lv.start)
                    endInsn = insns.indexOf(lv.end)
                    name = lv.name
                    break
                }
            }
        }

        if(name == null) {
            name = "arg${i+1}"
        }

        val arg = LocalVariable(this, true, i, lvIdx, asmIndex, type, startInsn, endInsn, name)
        args[i] = arg

        if(typeClass != null) {
            classRefs.add(typeClass)
            typeClass.methodTypeRefs.add(this)
        }

        lvIdx += type.size
    }

    this.args.addAll(args.values)
}

private fun MethodNode.gatherVars() {
    if(instructions == null || instructions.size() == 0) return
    if(localVariables == null || localVariables.isEmpty()) generateVars()

    val insns = instructions.toArray()
    val firstInsn = insns.first()
    val vars = mutableListOf<LocalVariableNode>()

    lvLoop@ for(i in 0 until localVariables.size) {
        val lv = localVariables[i]
        if(lv.start == firstInsn) {
            if(lv.index == 0 && !Modifier.isStatic(access)) continue
            for(arg in args) {
                if(arg.asmIndex == i) {
                    continue@lvLoop
                }
            }
        }
        vars.add(lv)
    }

    if(vars.isEmpty()) return

    val ret = hashMapOf<Int, LocalVariable>()
    for(i in 0 until vars.size) {
        val lv = vars[i]

        val startInsn = insns.indexOf(lv.start)
        val endInsn = insns.indexOf(lv.end)

        var start: AbstractInsnNode? = lv.start
        var startOpIdx = 0
        while (start!!.previous.also { start = it } != null) {
            if (start!!.opcode >= 0) startOpIdx++
        }

        ret[i] = LocalVariable(
            this,
            false,
            i,
            lv.index,
            localVariables.indexOf(lv),
            Type.getObjectType(lv.desc),
            startInsn,
            endInsn,
            "var1"
        )
    }

    this.vars.addAll(ret.values)
}

private fun MethodNode.generateVars() {
    if(instructions != null && instructions.size() > 0) {
        val varInsns = instructions.toArray()
            .filter { it.type == VAR_INSN }
            .map { it as VarInsnNode }
            .toList()
        val loadVars = hashSetOf<Int>()
        if(!Modifier.isStatic(access)) {
            loadVars.add(0)
        }
        varInsns.filter { it.opcode < ISTORE }
            .map { it.`var` }
            .forEach { loadVars.add(it) }
        if(loadVars.size < 1) {
            return
        }
        localVariables = mutableListOf()
        val start = LabelNode()
        val end = LabelNode()
        instructions.insertBefore(instructions.first, start)
        instructions.add(end)
        for(i in 0 until loadVars.size) {
            localVariables.add(LocalVariableNode("var${i + 1}", "java/lang/Object", null, start, end, i))
        }
    }
}

