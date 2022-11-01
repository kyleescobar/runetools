package dev.runetools.mapper.classifier

interface Classifier<T> {
    var weight: Double
    fun calculateScore(a: T, b: T): Double
}