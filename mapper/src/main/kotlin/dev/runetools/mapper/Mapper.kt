package dev.runetools.mapper

import dev.runetools.asm.tree.*
import dev.runetools.mapper.classifier.*
import dev.runetools.mapper.mapping.NodeMappings
import dev.runetools.mapper.mapping.StaticMethodMapper
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileNotFoundException
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
    }

    private fun run() {
        Logger.info("Starting mapper.")

        val nodeMappings = NodeMappings()

        /*
         * Map static methods.
         */
        StaticMethodMapper().map(fromPool, toPool).also { nodeMappings.merge(it) }
        nodeMappings.reduce()

        val results = nodeMappings.asMap().map { "FROM: ${(it.key as MethodNode).id}, TO: ${(it.value as MethodNode).id}" }
        println()
    }

    private fun save() {

    }

    private fun getRawScore(score: Double, maxScore: Double): Double {
        return sqrt(score) * maxScore
    }
}