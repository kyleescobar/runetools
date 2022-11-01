package dev.runetools.mapper.mapping

data class FieldMapping(
    val owner: String,
    val name: String,
    val desc: String,
    val obfOwner: String,
    val obfName: String,
    val obfDesc: String,
    val isStatic: Boolean
) {

}