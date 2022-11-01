package dev.runetools.mapper.classifier

interface Classifier<T> {
    var level: ClassifierLevel
    var weight: Double
    fun calculateScore(a: T, b: T): Double
}