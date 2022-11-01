package dev.runetools.mapper.classifier

import dev.runetools.asm.util.ConsoleProgressBar
import dev.runetools.asm.util.linkedListOf
import dev.runetools.mapper.mapping.Mapping
import dev.runetools.mapper.mapping.NodeMappings

abstract class AbstractClassifier<T> {

    val classifiers = linkedListOf<Classifier<T>>()

    abstract fun init()

    fun addClassifier(classifier: Classifier<T>, weight: Int) {
        classifier.weight = weight.toDouble()
        classifiers.add(classifier)
    }

    fun classifier(block: (a: T, b: T) -> Double): Classifier<T> {
        return object : Classifier<T> {
            override var weight = 1.0
            override fun calculateScore(a: T, b: T): Double {
                return block(a, b)
            }
        }
    }

    fun rank(from: T, toSet: Set<T>): Mapping? {
        var highest: Mapping? = null
        var multiple = false

        toSet.parallelStream().forEach { to ->
            ConsoleProgressBar.step()
            val mapping = Mapping(from as Any, to as Any)
            var weight = 0.0
            classifiers.forEach { classifier ->
                weight += (classifier.calculateScore(from, to) * classifier.weight)
            }
            mapping.weight= (weight * 100.0).toInt()

            if(highest == null || mapping.weight > highest!!.weight) {
                highest = mapping
                multiple = false
            } else if(mapping.weight == highest!!.weight) {
                multiple = true
            }
        }

        if(multiple) return null
        return highest
    }
}