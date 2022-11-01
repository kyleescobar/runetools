package dev.runetools.mapper

import dev.runetools.asm.tree.*
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class MethodRefMapper {

    fun map(nodeMappings: NodeMappings) {
        val methodMappings = nodeMappings.asMap().filterKeys { it is MethodNode }

        ConsoleProgressBar.enable("Method References", methodMappings.size)
        methodMappings.forEach { (from, to) ->
            from as MethodNode
            to as MethodNode
            ConsoleProgressBar.step()

            val fromRet = from.type.returnType.let { from.pool.findClass(it.internalName) }
            val toRet = to.type.returnType.let { to.pool.findClass(it.internalName) }
            if(fromRet != null && toRet != null) {
                nodeMappings.map(fromRet, toRet)
            }

            val fromArgs = from.type.argumentTypes.mapNotNull { from.pool.findClass(it.internalName) }
            val toArgs = to.type.argumentTypes.mapNotNull { to.pool.findClass(it.internalName) }
            if(fromArgs.size == toArgs.size) {
                for(i in fromArgs.indices) {
                    val fromArg = fromArgs[i]
                    val toArg = toArgs[i]
                    nodeMappings.map(fromArg, toArg)
                }
            }

            if(!from.isStatic() && !to.isStatic()) {
                nodeMappings.map(from.owner, to.owner)

                val fromHierarchy = from.hierarchy
                val toHierarchy = to.hierarchy
                fromHierarchy.forEach { f ->
                    nodeMappings.mapAll(f, toHierarchy.filterMaybeEqual(f))
                }
            }

            val fromInRefs = from.refsIn
            val toInRefs = to.refsIn
            fromInRefs.forEach { f ->
                nodeMappings.mapAll(f, toInRefs.filterMaybeEqual(f))
            }

            val fromOutRefs = from.refsOut
            val toOutRefs = to.refsOut
            fromOutRefs.forEach { f ->
                nodeMappings.mapAll(f, toOutRefs.filterMaybeEqual(f))
            }

            val fromClassRefs = from.classRefs
            val toClassRefs = to.classRefs
            fromClassRefs.forEach { f ->
                nodeMappings.mapAll(f, toClassRefs.filterMaybeEqual(f))
            }

            val fromFieldReadRefs = from.fieldReadRefs
            val toFieldReadRefs = to.fieldReadRefs
            fromFieldReadRefs.forEach { f ->
                nodeMappings.mapAll(f, toFieldReadRefs.filterMaybeEqual(f))
            }

            val fromFieldWriteRefs = from.fieldWriteRefs
            val toFieldWriteRefs = to.fieldWriteRefs
            fromFieldWriteRefs.forEach { f ->
                nodeMappings.mapAll(f, toFieldWriteRefs.filterMaybeEqual(f))
            }
        }
        ConsoleProgressBar.disable()
    }

    private fun Set<MethodNode>.filterMaybeEqual(method: MethodNode): Set<MethodNode> {
        return this.filter { ClassifierUtil.isMaybeEqual(method, it) }.toSet()
    }

    private fun Set<FieldNode>.filterMaybeEqual(field: FieldNode): Set<FieldNode> {
        return this.filter { ClassifierUtil.isMaybeEqual(field, it) }.toSet()
    }

    private fun Set<ClassNode>.filterMaybeEqual(cls: ClassNode): Set<ClassNode> {
        return this.filter { ClassifierUtil.isMaybeEqual(cls, it) }.toSet()
    }

    private fun NodeMappings.mapAll(from: MethodNode, toSet: Set<MethodNode>) {
        toSet.forEach { to ->
            map(from, to)
        }
    }

    private fun NodeMappings.mapAll(from: FieldNode, toSet: Set<FieldNode>) {
        toSet.forEach { to ->
            map(from, to)
        }
    }

    private fun NodeMappings.mapAll(from: ClassNode, toSet: Set<ClassNode>) {
        toSet.forEach { to ->
            map(from, to)
        }
    }
}