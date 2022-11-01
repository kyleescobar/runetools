package dev.runetools.mapper.mapping

data class MethodMapping(
    val owner: String,
    val name: String,
    val desc: String,
    val obfOwner: String,
    val obfName: String,
    val obfDesc: String,
    val isStatic: Boolean
) {
    val args = mutableListOf<MethodArgMapping>()
    val vars = mutableListOf<MethodVarMapping>()
}