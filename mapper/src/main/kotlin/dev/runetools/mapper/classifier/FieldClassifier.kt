package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode

object FieldClassifier : AbstractClassifier<FieldNode>() {

    override fun init() {
        addClassifier(fieldTypeCheck, 10)
        addClassifier(accessFlags, 4)
        addClassifier(type, 10)
        addClassifier(readRefs, 6)
        addClassifier(writeRefs, 6)
        //addClassifier(initializerIndex, 8)
        addClassifier(initValue, 7)
    }

    private val fieldTypeCheck = classifier { a, b ->
        val mask = ACC_STATIC
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier (1 - Integer.bitCount(resultA xor resultB)).toDouble()
    }

    private val accessFlags = classifier { a, b ->
        val mask = (ACC_PUBLIC or ACC_PROTECTED or ACC_PRIVATE) or ACC_FINAL or ACC_VOLATILE or ACC_TRANSIENT or ACC_SYNTHETIC
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 6.0
    }

    private val type = classifier { a, b ->
        return@classifier if(ClassifierUtil.isMaybeEqual(a.type, b.type)) 1.0 else 0.0
    }

    private val readRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.readRefs, b.readRefs)
    }

    private val writeRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.writeRefs, b.writeRefs)
    }

    private val initializerIndex = classifier { a, b ->
        val fieldIndexsA = a.pool.fieldInitializedOrder()
        val fieldIndexsB = b.pool.fieldInitializedOrder()
        return@classifier ClassifierUtil.compareCounts(fieldIndexsA.indexOf(a), fieldIndexsB.indexOf(b))
    }

    private fun ClassPool.fieldInitializedOrder(): Set<FieldNode> {
        val ret = hashSetOf<FieldNode>()
        classes.flatMap { it.methods }.filter { it.name == "<clinit>" }.forEach { method ->
            method.instructions.forEach insnLoop@ { insn ->
                if(insn.opcode != PUTSTATIC) return@insnLoop
                insn as FieldInsnNode
                val dst = findClass(insn.owner)?.resolveField(insn.name, insn.desc) ?: return@insnLoop
                ret.add(dst)
            }
        }
        return ret
    }

    private val initValue = classifier { a, b ->
        val valueA = a.value
        val valueB = b.value

        if(valueA == null && valueB == null) return@classifier 1.0
        if(valueA == null || valueB == null )return@classifier 0.0

        return@classifier if(valueA == valueB) 1.0 else 0.0
    }
}