package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode

object MethodClassifier : AbstractClassifier<MethodNode>() {


    override fun init() {
        addClassifier(methodTypeCheck, 10)
        addClassifier(accessFlags, 4)
        addClassifier(argTypes, 10)
        addClassifier(returnType, 5)
        addClassifier(owner, 8)
        addClassifier(hierarchy, 6)
        addClassifier(strings, 5)
        addClassifier(numbers, 5)
        addClassifier(classRefs, 3)
        addClassifier(inRefs, 6)
        addClassifier(outRefs, 6)
        addClassifier(fieldReadRefs, 5)
        addClassifier(fieldWriteRefs, 5)
        addClassifier(lineNumberRange, 3)
        //addClassifier(code, 12)
    }

    private val methodTypeCheck = classifier { a, b ->
        val mask = Opcodes.ACC_STATIC or Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 3.0
    }

    private val accessFlags = classifier { a, b ->
        val mask =
            (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE) or Opcodes.ACC_FINAL or Opcodes.ACC_SYNCHRONIZED or Opcodes.ACC_BRIDGE or Opcodes.ACC_VARARGS or Opcodes.ACC_STRICT or Opcodes.ACC_SYNTHETIC
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 8.0
    }

    private val argTypes = classifier { a, b ->
        return@classifier ClassifierUtil.compareNodeSets(
            a.type.argumentTypes.toSet(),
            b.type.argumentTypes.toSet(),
            { null },
            ClassifierUtil::isMaybeEqual
        )
    }

    private val returnType = classifier { a, b ->
        return@classifier if (ClassifierUtil.isMaybeEqual(a.type.returnType, b.type.returnType)) 1.0 else 0.0
    }
    
    private val owner = classifier { a, b -> 
        return@classifier if(ClassifierUtil.isMaybeEqual(a.owner, b.owner)) 1.0 else 0.0
    }
    
    private val hierarchy = classifier { a, b -> 
        return@classifier ClassifierUtil.compareMethodSets(a.hierarchy, b.hierarchy)
    }

    private val strings = classifier { a, b ->
        return@classifier ClassifierUtil.compareSets(a.strings, b.strings)
    }

    private val numbers = classifier { a, b ->
        return@classifier ClassifierUtil.compareSets(a.numbers, b.numbers)
    }

    private val classRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.classRefs, b.classRefs)
    }

    private val inRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsIn, b.refsIn)
    }

    private val outRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsOut, b.refsOut)
    }

    private val fieldReadRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldReadRefs, b.fieldReadRefs)
    }

    private val fieldWriteRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldWriteRefs, b.fieldWriteRefs)
    }

    private val lineNumberRange = classifier { a, b ->
        val lineRangeA = a.getLineNumberRange()
        val lineRangeB = b.getLineNumberRange()
        return@classifier ClassifierUtil.compareSets(lineRangeA.toSet(), lineRangeB.toSet())
    }

    private fun MethodNode.getLineNumberRange(): IntRange {
        var min = -1
        var max = -1
        instructions.forEach { insn ->
            if(insn is LineNumberNode) {
                val line = insn.line
                if(min == -1 || line < min) {
                    min = line
                }
                if(max == -1 || line > max) {
                    max = line
                }
            }
        }
        return min..max
    }

    private val code = classifier { a, b ->
        return@classifier ClassifierUtil.compareInsns<AbstractInsnNode>(a, b)
    }
}