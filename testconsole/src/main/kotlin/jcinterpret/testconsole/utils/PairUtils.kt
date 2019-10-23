package jcinterpret.testconsole.utils

fun Pair<Double, Double>.min(): Double {
    return minOf(first, second)
}

fun Pair<Double, Double>.max(): Double {
    return maxOf(first, second)
}

fun Pair<Double, Double>.avg(): Double {
    return (first + second) / 2.0
}