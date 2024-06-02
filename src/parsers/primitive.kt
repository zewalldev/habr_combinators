package parsers


/**
 * Результат работы синтаксического анализатора.
 */
sealed interface ParseResult<out A> {
    /**
     * Ошибка разбора
     * @param msg - Сообщение об ошибке.
     */
    data class Fail(val msg: String) : ParseResult<Nothing>

    /**
     * Успешный разбор
     * @param tail - остаток строки;
     * @param result - возвращаемое парсером значение.
     */
    data class Success<A>(val tail: String, val result: A) : ParseResult<A>
}

/**
 * Тип парсера.
 */
interface Parser<A> {
    fun parse(str: String): ParseResult<A>
}

fun <T, R> ParseResult<T>.map(block: (ParseResult.Success<T>) -> ParseResult<R>) = when (this) {
    is ParseResult.Fail -> this
    is ParseResult.Success -> block(this)
}

/**
 * Парсер, всегда возвращающий значение a.
 */
class Return<A>(private val a: A) : Parser<A> {
    override fun parse(str: String) = ParseResult.Success(str, a)
}

/**
 * Парсер, проверяющий, удовлетворяет ли первый символ строки str предикату predicate.
 */
class TakeIf(private val description: String, private val predicate: (Char) -> Boolean) : Parser<Char> {
    override fun parse(str: String) = when {
        str.isEmpty() -> ParseResult.Fail("Input string is empty")
        !predicate(str.first()) -> ParseResult.Fail("Symbol '${str.first()}' does not satisfy predicate: $description")
        else -> ParseResult.Success(str.drop(1), str.first())
    }
}

/**
 * Конструирует парсер, распознающий строку str.
 */
fun string(str: String): Parser<String> =
    if (str.isNotEmpty()) seq(symbol(str.first()), string(str.drop(1))) { c, cs -> c + cs } else Return("")

/**
 * Конструирует парсер, распознающий, что первый символ строки является одним из заданных.
 */
fun symbol(c: Char, vararg cs: Char) =
    TakeIf("in [${(cs + c).joinToString { it.toString() }}]") { it in cs + c }

/**
 * Конструирует парсер, распознающий, что первый символ строки не является одним из заданных.
 */
fun notSymbol(c: Char, vararg cs: Char) =
    TakeIf("not in [${(cs + c).joinToString { it.toString() }}]") { it !in cs + c }
