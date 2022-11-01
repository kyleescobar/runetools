package dev.runetools.mapper.asm

import dev.runetools.asm.util.nullField
import org.objectweb.asm.tree.FieldNode

var FieldNode.match: FieldNode? by nullField()
fun FieldNode.hasMatch() = match != null

