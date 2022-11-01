package dev.runetools.mapper.mapping

import dev.runetools.asm.tree.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileOutputStream

class MappingWriter(private val mappings: Map<Any, Any>, private val pool: ClassPool) {

    private val classes = hashMapOf<String, ClassMapping>()

    @Suppress("UNCHECKED_CAST")
    private fun init() {
        val classMappings = mappings.filterKeys { it is ClassNode }.toList().associate { it.second to it.first }.toMap() as Map<ClassNode, ClassNode>
        val methodMappings = mappings.filterKeys { it is MethodNode }.toList().associate { it.second to it.first }.toMap() as Map<MethodNode, MethodNode>
        val fieldMappings = mappings.filterKeys { it is FieldNode }.toList().associate { it.second to it.first }.toMap() as Map<FieldNode, FieldNode>

        pool.allClasses.forEach { cls ->
            val name = classMappings[cls]?.name ?: cls.name
            val classMapping = ClassMapping(name, cls.name)

            cls.methods.sortedBy { it.isStatic() }.forEach { method ->
                val owner = methodMappings[method]?.owner?.name ?: method.owner.name
                val methodName = methodMappings[method]?.name ?: method.name
                val methodDesc = methodMappings[method]?.desc ?: method.desc
                val methodMapping = MethodMapping(owner, methodName, methodDesc, method.owner.name, method.name, method.desc, method.isStatic())

                method.args.forEach { arg ->
                    val argName = arg.name
                    val argIndex = arg.index
                    val argMapping = MethodArgMapping(argName, argIndex, argName, argIndex)
                    methodMapping.args.add(argMapping)
                }

                method.vars.forEach { lv ->
                    val lvName = lv.name
                    val lvIndex = lv.index
                    val lvMapping = MethodVarMapping(lvName, lvIndex, 0, 0, lvName, lvIndex, 0, 0)
                    methodMapping.vars.add(lvMapping)
                }

                classMapping.methods.add(methodMapping)
            }

            cls.fields.sortedBy { it.isStatic() }.forEach { field ->
                val owner = fieldMappings[field]?.owner?.name ?: field.owner.name
                val fieldName = fieldMappings[field]?.name ?: field.name
                val fieldDesc = fieldMappings[field]?.desc ?: field.desc
                val fieldMapping = FieldMapping(owner, fieldName, fieldDesc, field.owner.name, field.name, field.desc, field.isStatic())

                classMapping.fields.add(fieldMapping)
            }

            classes[cls.name] = classMapping
        }
    }

    fun writeToDirectory(dir: File) {
        if(!dir.exists()) dir.mkdirs()
        if(dir.exists()) dir.deleteRecursively()

        this.init()

        Logger.info("Writing mappings to directory: ${dir.path}.")

        classes.values.forEach { cls ->
            val name = classes[cls.name]?.name ?: cls.name
            val file = dir.resolve("$name.mapping")
            file.parentFile.mkdirs()
            file.bufferedWriter().use { out ->
                out.write(writeClassMapping(cls))
            }
        }

        Logger.info("Mappings have been written to disk.")
    }

    private fun writeClassMapping(classMapping: ClassMapping): String {
        val buf = StringBuilder()

        buf.append("CLASS ${classMapping.name} ${classMapping.obfName}")
        buf.append("\n")

        classMapping.fields.sortedByDescending { it.isStatic }.forEach { fieldMapping ->
            writeFieldMapping(buf, fieldMapping)
        }

        classMapping.methods.sortedByDescending { it.isStatic }.forEach { methodMapping ->
            writeMethodMapping(buf, methodMapping)
        }

        return buf.toString()
    }

    private fun writeFieldMapping(buf: StringBuilder, fieldMapping: FieldMapping) {
        buf.append("\t")
        val staticToken = if(fieldMapping.isStatic) "STATIC " else ""
        buf.append("FIELD $staticToken${fieldMapping.name} ${fieldMapping.obfName} ${fieldMapping.obfDesc}")
        buf.append("\n")
    }

    private fun writeMethodMapping(buf: StringBuilder, methodMapping: MethodMapping) {
        buf.append("\t")
        val staticToken = if(methodMapping.isStatic) "STATIC " else ""
        buf.append("METHOD $staticToken${methodMapping.name} ${methodMapping.obfName} ${methodMapping.obfDesc}")
        buf.append("\n")

        methodMapping.args.sortedBy { it.index }.forEach { methodArgMapping ->
            writeMethodArgMapping(buf, methodArgMapping)
        }

        methodMapping.vars.sortedBy { it.index }.forEach { methodVarMapping ->
            writeMethodVarMapping(buf, methodVarMapping)
        }
    }

    private fun writeMethodArgMapping(buf: StringBuilder, methodArgMapping: MethodArgMapping) {
        buf.append("\t\t")
        buf.append("ARG ${methodArgMapping.index} ${methodArgMapping.name}")
        buf.append("\n")
    }

    private fun writeMethodVarMapping(buf: StringBuilder, methodVarMapping: MethodVarMapping) {
        buf.append("\t\t")
        buf.append("VAR ${methodVarMapping.index} ${methodVarMapping.name}")
        buf.append("\n")
    }
}