package dev.runetools.mapper.classifier

import dev.runetools.asm.util.linkedListOf
import java.util.LinkedList

abstract class AbstractClassifier<T> {

    val classifiers = hashMapOf<ClassifierLevel, LinkedList<Classifier<T>>>()
    val maxScore = hashMapOf<ClassifierLevel, Double>()

    abstract fun init()

    fun addClassifier(classifier: Classifier<T>, weight: Int, vararg levels: ClassifierLevel = arrayOf(ClassifierLevel.INITIAL)) {
        classifier.weight = weight.toDouble()
        levels.forEach { level ->
            classifiers.computeIfAbsent(level) { linkedListOf() }.add(classifier)
        }
    }

    fun classifier(block: (a: T, b: T) -> Double): Classifier<T> {
        return object : Classifier<T> {
            override var level = ClassifierLevel.INITIAL
            override var weight = 1.0
            override fun calculateScore(a: T, b: T): Double {
                return block(a, b)
            }
        }
    }

    abstract fun rank(level: ClassifierLevel, fromSet: Set<T>, toSet: Set<T>, filter: (from: T, to: T) -> Boolean): List<MatchResult<T>>
}