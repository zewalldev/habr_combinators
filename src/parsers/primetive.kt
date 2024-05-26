package parsers

sealed interface ParseResult<out A> {
    data class Fail(val msg: String) : ParseResult<Nothing>
    data class Success<A>(val tail: String, val result: A) : ParseResult<A>
}

interface Parser<A> {
    fun parse(str: String): ParseResult<A>
    fun <R> accept(visitor: Visitor<R>): R
}

fun <T, R> ParseResult<T>.map(block: (ParseResult.Success<T>) -> ParseResult<R>) = when (this) {
    is ParseResult.Fail -> this
    is ParseResult.Success -> block(this)
}

open class TakeIf(private val predicate: (Char) -> Boolean) : Parser<Char> {
    override fun parse(str: String) = if (str.isNotEmpty() && predicate(str.first())) {
        ParseResult.Success(str.drop(1), str.first())
    } else {
        ParseResult.Fail(str, "The symbol ${str.firstOrNull()} does not satisfy predicate.")
    }

    override fun <R> accept(visitor: Visitor<R>) = visitor.visitParser(this)
}