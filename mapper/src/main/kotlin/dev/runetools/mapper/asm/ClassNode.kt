package dev.runetools.mapper.asm

import dev.runetools.asm.util.nullField
import org.objectweb.asm.tree.ClassNode

var ClassNode.match: ClassNode? by nullField()
fun ClassNode.hasMatch() = match != null

