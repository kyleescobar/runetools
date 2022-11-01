package dev.runetools.mapper

import dev.runetools.asm.tree.*
import dev.runetools.mapper.asm.hasMatch
import dev.runetools.mapper.asm.match
import dev.runetools.mapper.classifier.ClassClassifier
import dev.runetools.mapper.classifier.ClassifierLevel
import dev.runetools.mapper.classifier.ClassifierUtil
import dev.runetools.mapper.classifier.StaticMethodClassifier
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
    }

    private fun run() {
        Logger.info("Starting mapper.")

        autoMatchAll(ClassifierLevel.INITIAL)
        autoMatchAll(ClassifierLevel.SECONDARY)

        val staticMethodMatches = fromPool.classes.flatMap { it.methods }.filter { it.hasMatch() }.associate { it.id to it.match!!.id }
        val classMatches = fromPool.classes.filter { it.hasMatch() }.associate { it.id to it.match!!.id }
        println()
    }

    private fun autoMatchAll(level: ClassifierLevel) {
        Logger.info("Matching level: ${level.name}.")

        var matchedAny: Boolean
        do {
            matchedAny = autoMatchStaticMethods(level)
            matchedAny = matchedAny or autoMatchClasses(level)
        } while(matchedAny)
    }

    private fun autoMatchClasses(level: ClassifierLevel): Boolean {
        Logger.info("Matching classes.")

        val fromSet = fromPool.classes.filter { !it.hasMatch() }.toSet()
        val toSet = toPool.classes.filter { !it.hasMatch() }.toSet()

        val matches = ClassClassifier.rank(level, fromSet, toSet, ClassifierUtil::isMaybeEqual)
        var matched = 0

        matches.forEach { match ->
            if(match.from.match != match.to) {
                match.from.match(match.to)
                matched++
            }
        }

        Logger.info("Matched $matched classes.")

        return matched > 0
    }

    private fun autoMatchStaticMethods(level: ClassifierLevel): Boolean {
        Logger.info("Matching static methods.")

        val fromSet = fromPool.classes.flatMap { it.methods }
            .filter { it.isStatic() && !it.hasMatch() }
            .toSet()

        val toSet = toPool.classes.flatMap { it.methods }
            .filter { it.isStatic() && !it.hasMatch() }
            .toSet()

        val matches = StaticMethodClassifier.rank(level, fromSet, toSet, ClassifierUtil::isMaybeEqual)
        var matched = 0
        matches.forEach { match ->
            match.from.match(match.to)
            matched++
        }

        Logger.info("Matched $matched static methods.")
        return matched > 0
    }

    private fun save() {

    }

    private fun getRawScore(score: Double, maxScore: Double): Double {
        return sqrt(score) * maxScore
    }
}