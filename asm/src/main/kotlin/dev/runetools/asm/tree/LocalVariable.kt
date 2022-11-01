package dev.runetools.asm.tree

import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

data class LocalVariable(
    val method: MethodNode,
    val isArg: Boolean,
    val index: Int,
    val lvIndex: Int,
    val asmIndex: Int,
    val type: Type,
    val startInsn: Int,
    val endInsn: Int,
    val name: String
)