package examples

import parsers.symbol

/**
 * Пример применения элементарных парсеров.
 */
fun main() {
    println(symbol('a').parse("abc")) // output: Success(tail=bc, pos=1, result=a)
    println(symbol('b').parse("abc")) // output: Fail(msg=Symbol 'a' does not satisfy predicate: in [b])
}
