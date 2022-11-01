package dev.runetools.mapper.classifier

import dev.runetools.asm.util.linkedListOf

abstract class AbstractClassifier<T> {

    val classifiers = linkedListOf<Classifier<T>>()

    abstract fun init()

    fun addClassifier(classifier: Classifier<T>, weight: Int) {
        classifier.weight = weight.toDouble()
        classifiers.add(classifier)
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

    abstract fun rank(fromSet: Set<T>, toSet: Set<T>, filter: (from: T, to: T) -> Boolean): List<MatchResult<T>>
}