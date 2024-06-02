package examples

import parsers.*

/**
 * Парсер для числа.
 */
fun digit0() = seq(symbol('0')) { 0 }
fun digit19(): Parser<Int> = seq(
    symbol('1', '2', '3', '4', '5', '6', '7', '8', '9')
) { it.digitToInt() }

fun digit09(): Parser<Int> = alt(digit0(), digit19())

fun integer(): Parser<Int> =
    alt(
        seq(digit19(), manyOf(alt(digit09()))) { x, xs -> (listOf(x) + xs).fold(0) { a, d -> 10 * a + d } },
        digit0()
    )

fun fractional(): Parser<Double> =
    seq(symbol('.'), someOf(digit09())) { _, xs -> xs.foldRight(0.0) { a, d -> (a + d) / 10.0 } }

fun number() = seq(integer(), optional(fractional(), 0.0)) { i, f -> i + f }

/**
 * Парсеры для арифметических действий.
 */
fun plus(): Parser<(Double, Double) -> Double> = seq(symbol('+')) { _ -> { a, b -> a + b } }
fun minus(): Parser<(Double, Double) -> Double> = seq(symbol('-')) { _ -> { a, b -> a - b } }
fun times(): Parser<(Double, Double) -> Double> = seq(symbol('*')) { _ -> { a, b -> a * b } }
fun div(): Parser<(Double, Double) -> Double> = seq(symbol('/')) { _ -> { a, b -> a / b } }

// op1 -> '+' | '-'
fun op1() = alt(plus(), minus())

// op2 -> '*' | '/'
fun op2() = alt(times(), div())

// exp -> term exp1 | term
fun exp(): Parser<Double> = alt(
    seq(ref { term() }, ref { exp1() }) { a, f -> f(a) },
    ref { term() }
)

// exp1 -> op1 term exp1 | op1 term
fun exp1(): Parser<(Double) -> Double> = alt(
    seq(op1(), term(), ref { exp1() }) { op, b, t -> { a -> t(op(a, b)) } },
    seq(op1(), term()) { op, b -> { a -> op(a, b) } }
)

// term -> factor term1 | factor
fun term(): Parser<Double> = alt(
    seq(factor(), term1()) { a, f -> f(a) },
    factor()
)

// term1 -> op2 factor term1 | op2 factor
fun term1(): Parser<(Double) -> Double> = alt(
    seq(op2(), factor(), ref { term1() }) { op, b, t -> { a -> t(op(a, b)) } },
    seq(op2(), factor()) { op, b -> { a -> op(a, b) } }
)

// factor -> number | ( exp ) | -factor
fun factor(): Parser<Double> = alt(
    number(),
    seq(`(`, exp(), `)`) { _, v, _ -> v },
    seq(minus(), ref { factor() }) { _, f -> -f }
)

/**
 * Пример синтаксического анализатора для арифметических выражений, грамматика:
 * op1 -> '+' | '-'
 * op2 -> '*' | '/'
 * exp -> term exp1 | term
 * exp1 -> op1 term exp1 | op1 term
 * term -> factor term1 | factor
 * term1 -> op2 factor term1 | op2 factor
 * factor -> number | '(' exp ')' | -factor
 */
fun main() {
    println(exp().parse("3+4*(1/(2*3*4)-1/(4*5*6)+1/(6*7*8)-1/(8*9*10)+1/(10*11*12))")) // output: Success(tail=, result=3.1427128427128426)
}
