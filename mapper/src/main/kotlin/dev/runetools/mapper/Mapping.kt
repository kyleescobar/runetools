package dev.runetools.mapper

import java.lang.Integer.max

class Mapping(val from: Any, val to: Any) {

    var score = 0

    var weight = 0
        set(value) {
            if(value > field) field = value
        }

    fun merge(other: Mapping) {
        score += other.score
        weight = max(weight, other.weight)
    }
}