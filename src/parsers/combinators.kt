package parsers

/**
 * Позволяет преобразовывать результат парсера.
 */
class Mapper<A, B>(private val p: Parser<A>, private val f: (A) -> B) : Parser<B> {
    override fun parse(str: String) = p.parse(str).map { ParseResult.Success(it.tail, f(it.result)) }
}

/**
 * Комбинатор следования.
 */
class Seq<A, B>(private val pa: Parser<A>, private val pb: Parser<B>) : Parser<Pair<A, B>> {
    override fun parse(str: String) = pa.parse(str).map { it1 ->
        pb.parse(it1.tail).map { it2 ->
            ParseResult.Success(it2.tail, it1.result to it2.result)
        }
    }
}

/**
 * Комбинатор альтернативы.
 */
class Alt<A>(private val p1: Parser<A>, private val p2: Parser<A>) : Parser<A> {
    override fun parse(str: String) = when (val r = p1.parse(str)) {
        is ParseResult.Fail -> p2.parse(str)
        is ParseResult.Success -> r
    }
}

/**
 * Комбинатор связывания.
 */
class Binder<A, B>(private val pa: Parser<A>, private val bind: (A) -> Parser<B>) : Parser<B> {
    override fun parse(str: String) = pa.parse(str).map {
        bind(it.result).parse(it.tail)
    }
}
