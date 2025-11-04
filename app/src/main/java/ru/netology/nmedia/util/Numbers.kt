package ru.netology.nmedia.util

import kotlin.math.abs

fun formatCount(n: Int): String = shortCount(n.toLong())

fun shortCount(n: Long, useLatinUnits: Boolean = false): String {
    val k = if (useLatinUnits) "K" else "К"
    val m = if (useLatinUnits) "M" else "М"

    val sign = if (n < 0) "-" else ""
    val v = abs(n)

    return when {
        v < 1_000L -> sign + v.toString()
        v < 10_000L -> {
            val thousands = v / 1_000
            val hundreds = (v % 1_000) / 100
            "$sign$thousands.$hundreds$k"
        }
        v < 1_000_000L -> {
            val thousands = v / 1_000
            "$sign${thousands}$k"
        }
        else -> {
            val millions = v / 1_000_000
            val hundredThousands = (v % 1_000_000) / 100_000
            "$sign$millions.$hundredThousands$m"
        }
    }
}
