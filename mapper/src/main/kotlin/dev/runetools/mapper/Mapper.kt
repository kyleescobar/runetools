package dev.runetools.mapper

import dev.runetools.asm.tree.*
import dev.runetools.mapper.classifier.*
import dev.runetools.mapper.mapping.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileNotFoundException
import java.lang.Integer.max
import kotlin.math.round
import kotlin.math.sqrt

object Mapper {

    private lateinit var fromJar: File
    private lateinit var toJar: File
    private lateinit var outputJar: File

    private val fromPool = ClassPool()
    private val toPool = ClassPool()

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size != 3) throw IllegalArgumentException("Usage: mapper.jar <old-jar> <new-jar> <output-jar>")

        fromJar = File(args[0])
        toJar = File(args[1])
        outputJar = File(args[2])

        if(!fromJar.exists() || !toJar.exists()) {
            throw FileNotFoundException()
        }

        init()
        run()
        save()
    }

    private fun init() {
        Logger.info("Initializing mapper.")

        /*
         * Load classes into pools
         */

        fromPool.addJar(fromJar)
        fromPool.allClasses.forEach {
            if(it.name.contains("bouncycastle") || it.name.contains("json")) {
                it.ignored = true
            }
        }
        fromPool.build()

        toPool.addJar(toJar)
        toPool.allClasses.forEach {
            if(it.name.contains("bouncycastle") || it.name.contains("json")) {
                it.ignored = true
            }
        }
        toPool.build()

        Logger.info("Loaded classes from jar files.")

        /*
         * Initialize classifiers.
         */
        ClassClassifier.init()
        StaticMethodClassifier.init()
        MethodClassifier.init()
        FieldClassifier.init()
    }

    private fun run() {
        Logger.info("Starting mapper.")

        val nodeMappings = NodeMappings()

        /*
         * Map static methods.
         */
        StaticMethodMapper().map(fromPool, toPool).also { nodeMappings.merge(it) }

        /*
         * Map member methods.
         */
        MemberMethodMapper().map(fromPool, toPool).also { nodeMappings.merge(it) }

        /*
         * Reduce matches.
         */
        nodeMappings.reduce()

        /*
         * Map classes
         */
        ClassMapper().map(fromPool, toPool).also { nodeMappings.merge(it) }

        /*
         * Map fields
         */
        FieldMapper().map(fromPool, toPool).also { nodeMappings.merge(it) }

        /*
         * Reduce matches.
         */
        nodeMappings.reduce()

        val totalClasses = max(fromPool.classes.size, toPool.classes.size)
        val totalMethods = max(fromPool.classes.flatMap { it.methods }.size, toPool.classes.flatMap { it.methods }.size)
        val totalFields = max(fromPool.classes.flatMap { it.fields }.size, toPool.classes.flatMap { it.fields }.size)
        val totalLocalVars = max(fromPool.classes.flatMap { it.methods.flatMap { it.localVariables ?: emptyList() } }.size, toPool.classes.flatMap { it.methods.flatMap { it.localVariables ?: emptyList() } }.size)
        val matchedClasses = nodeMappings.asMap().filterKeys { it is ClassNode }.size
        val matchedMethods = nodeMappings.asMap().filterKeys { it is MethodNode }.size
        val matchedFields = nodeMappings.asMap().filterKeys { it is FieldNode }.size
        val matchedLocalVars = nodeMappings.asMap().filterKeys { it is LocalVariableNode }.size

        val percentClasses = (matchedClasses.toDouble() / totalClasses.toDouble()) * 100.0
        val percentMethods = (matchedMethods.toDouble() / totalMethods.toDouble()) * 100.0
        val percentFields = (matchedFields.toDouble() / totalFields.toDouble()) * 100.0
        val percentLocalVars = (matchedLocalVars.toDouble() / totalLocalVars.toDouble()) * 100.0

        Logger.info("Successfully completed mapping. Below are the results.")
        Logger.info("=======================================================")
        Logger.info("Mapped Classes: $matchedClasses / $totalClasses ($percentClasses%)")
        Logger.info("Mapped Methods: $matchedMethods / $totalMethods ($percentMethods%)")
        Logger.info("Mapped Fields: $matchedFields / $totalFields ($percentFields%)")
        Logger.info("Mapped Locals: $matchedLocalVars / $totalLocalVars ($percentLocalVars%)")

        println()
    }

    private fun save() {

    }
}