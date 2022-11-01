package dev.runetools.mapper

import dev.runetools.asm.tree.*
import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.mapper.classifier.ClassifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class ClassRefMapper {

    fun map(nodeMappings: NodeMappings) {
        val classMappings = nodeMappings.asMap().filterKeys { it is ClassNode }

        ConsoleProgressBar.enable("Class References", classMappings.size)
        classMappings.forEach { (from, to) ->
            from as ClassNode
            to as ClassNode
            ConsoleProgressBar.step()

            val fromParent = from.parent
            val toParent = to.parent
            if(fromParent != null && toParent != null) {
                nodeMappings.map(fromParent, toParent)
            }

            val fromChildren = from.children
            val toChildren = to.children
            fromChildren.forEach { f ->
                nodeMappings.mapAll(f, toChildren.filterMaybeEqual(f))
            }

            val fromInterfaces = from.interfaceClasses
            val toInterfaces = to.interfaceClasses
            fromInterfaces.forEach { f ->
                nodeMappings.mapAll(f, toInterfaces.filterMaybeEqual(f))
            }

            val fromImplementers = from.implementers
            val toImplementers = to.implementers
            fromImplementers.forEach { f ->
                nodeMappings.mapAll(f, toImplementers.filterMaybeEqual(f))
            }

            val fromHierarchy = from.hierarchy
            val toHierarchy = to.hierarchy
            fromHierarchy.forEach { f ->
                nodeMappings.mapAll(f, toHierarchy.filterMaybeEqual(f))
            }

            val fromMethodRefs = from.methodTypeRefs
            val toMethodRefs = to.methodTypeRefs
            fromMethodRefs.forEach { f ->
                nodeMappings.mapAll(f, toMethodRefs.filterMaybeEqual(f))
            }

            val fromFieldRefs = from.fieldTypeRefs
            val toFieldRefs = to.fieldTypeRefs
            fromFieldRefs.forEach { f ->
                nodeMappings.mapAll(f, toFieldRefs.filterMaybeEqual(f))
            }

            val fromFields = from.fields.filter { !it.isStatic() }
            val toFields = to.fields.filter { !it.isStatic() }
            fromFields.forEach { f ->
                nodeMappings.map(f, toFields.filter { ClassifierUtil.isMaybeEqual(f, it) })
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