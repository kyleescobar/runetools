package dev.runetools.asm.tree

import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private val classMap = hashMapOf<String, ClassNode>()

    val classes: Collection<ClassNode> get() = classMap.values.filter { !it.ignored }
    val ignoredClasses: Collection<ClassNode> get() = classMap.values.filter { it.ignored }
    val allClasses: Collection<ClassNode> get() = classMap.values

    fun addClass(node: ClassNode) {
        classMap[node.name] = node
        node.init(this)
    }

    fun addClass(data: ByteArray) {
        val node = ClassNode()
        node.accept(data)
        addClass(node)
    }

    fun removeClass(node: ClassNode) {
        classMap[node.name] = node
    }

    fun replaceClass(old: ClassNode, new: ClassNode) {
        new.ignored = old.ignored
        removeClass(old)
        addClass(new)
        repeat(BUILD_STEPS) { new.build(it) }
    }

    fun addJar(file: File) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(!entry.name.endsWith(".class")) return@forEach
                addClass(jar.getInputStream(entry).readAllBytes())
            }
        }
    }

    fun saveJar(file: File) {
        if(file.exists()) {
            file.deleteRecursively()
        }

        JarOutputStream(FileOutputStream(file)).use { jos ->
            allClasses.forEach { cls ->
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(cls.toByteArray())
                jos.closeEntry()
            }
        }
    }

    fun getClass(name: String) = classMap.filterValues { !it.ignored }[name]
    fun getIgnoredClass(name: String) = classMap.filterValues { it.ignored }[name]
    fun findClass(name: String) = classMap[name]

    fun build() {
        allClasses.forEach { it.reset() }
        repeat(BUILD_STEPS) { step -> allClasses.forEach { it.build(step) } }
    }

    companion object {
        private const val BUILD_STEPS = 3
    }
}