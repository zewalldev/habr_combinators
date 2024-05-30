package parsers

sealed interface ParseResult<out A> {
    data class Fail(val msg: String) : ParseResult<Nothing>
    data class Success<A>(val tail: String, val result: A) : ParseResult<A>
}

interface Parser<A> {
    fun parse(str: String): ParseResult<A>
}

fun <T, R> ParseResult<T>.map(block: (ParseResult.Success<T>) -> ParseResult<R>) = when (this) {
    is ParseResult.Fail -> this
    is ParseResult.Success -> block(this)
}

class TakeIf(private val description: String, private val predicate: (Char) -> Boolean) : Parser<Char> {
    override fun parse(str: String) = when {
        str.isEmpty() -> ParseResult.Fail("Input string is empty")
        !predicate(str.first()) -> ParseResult.Fail("Symbol '${str.first()}' does not satisfy predicate: $description")
        else -> ParseResult.Success(str.drop(1), str.first())
    }
}

fun symbol(c: Char) = TakeIf("equals '$c'") { it == c }

class Return<A>(private val a: A) : Parser<A> {
    override fun parse(str: String) = ParseResult.Success(str, a)
}

fun main() {
    println(symbol('a').parse("abc")) // output: Success(tail=bc, pos=1, result=a)
    println(symbol('b').parse("abc")) // output: Fail(msg=Symbol 'a' does not satisfy predicate: equals b)
}