package dev.runetools.asm.util

import java.util.LinkedList

fun <T> linkedListOf(vararg elements: T) = LinkedList(elements.toMutableList())

fun <T, R> listField(init: (R) -> LinkedList<T> = { linkedListOf() }) = ExtensionField(init)