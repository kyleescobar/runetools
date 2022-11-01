package dev.runetools.mapper.mapping

data class ClassMapping(val name: String, val obfName: String) {

    val methods = mutableListOf<MethodMapping>()

    val fields = mutableListOf<FieldMapping>()

}