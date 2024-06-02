package parsers

/**
 * Набор функций, преобразующий функции вида (X1, X2, ...) -> Y в функции вида Pair<X1, Pair<X2, ...>> -> Y.
 */
fun <X1, X2, Y> args2tuple(f: (X1, X2) -> Y): (Pair<X1, X2>) -> Y =
    { (x1, x2) -> f(x1, x2) }

fun <X1, X2, X3, Y> args2tuple(f: (X1, X2, X3) -> Y): (Pair<X1, Pair<X2, X3>>) -> Y =
    args2tuple { x1, (x2, x3) -> f(x1, x2, x3) }

fun <X1, X2, X3, X4, Y> args2tuple(f: (X1, X2, X3, X4) -> Y): (Pair<X1, Pair<X2, Pair<X3, X4>>>) -> Y =
    args2tuple { x1, x2, (x3, x4) -> f(x1, x2, x3, x4) }

fun <X1, X2, X3, X4, X5, Y> args2tuple(f: (X1, X2, X3, X4, X5) -> Y): (Pair<X1, Pair<X2, Pair<X3, Pair<X4, X5>>>>) -> Y =
    args2tuple { x1, x2, x3, (x4, x5) -> f(x1, x2, x3, x4, x5) }

/**
 * Набор вспомогательных функций для конструирования комбинатора следования.
 */
fun <X1, Y> seq(p1: Parser<X1>, f: (X1) -> Y) =
    Mapper(p1, f)

fun <X1, X2, Y> seq(p1: Parser<X1>, p2: Parser<X2>, f: (X1, X2) -> Y) =
    Mapper(Seq(p1, p2), args2tuple(f))

fun <X1, X2, X3, Y> seq(p1: Parser<X1>, p2: Parser<X2>, p3: Parser<X3>, f: (X1, X2, X3) -> Y) =
    Mapper(Seq(p1, Seq(p2, p3)), args2tuple(f))

fun <X1, X2, X3, X4, Y> seq(p1: Parser<X1>, p2: Parser<X2>, p3: Parser<X3>, p4: Parser<X4>, f: (X1, X2, X3, X4) -> Y) =
    Mapper(Seq(p1, Seq(p2, Seq(p3, p4))), args2tuple(f))

fun <X1, X2, X3, X4, X5, Y> seq(
    p1: Parser<X1>,
    p2: Parser<X2>,
    p3: Parser<X3>,
    p4: Parser<X4>,
    p5: Parser<X5>,
    f: (X1, X2, X3, X4, X5) -> Y
) = Mapper(Seq(p1, Seq(p2, Seq(p3, Seq(p4, p5)))), args2tuple(f))

/**
 * Добавляет парсеру "ленивости".
 */
fun <A> ref(lazy: () -> Parser<A>) = object : Parser<A> {
    override fun parse(str: String) = lazy().parse(str)
}

/**
 * Конструирует комбинатор альтернативы.
 */
fun <A> alt(p: Parser<A>, vararg ps: Parser<A>): Parser<A> = ps.fold(p) { x, xs -> Alt(x, xs) }

/**
 * Конструирует парсер, который вернет либо результат работы p либо значение по умолчанию a.
 */
fun <A> optional(p: Parser<A>, a: A) = alt(p, Return(a))

/**
 * Конструирует парсер, который проверяет, что строка удовлетворяет парсеру p хотя бы один раз.
 * Возвращает результат применения p в виде списка.
 */
fun <A> someOf(p: Parser<A>): Parser<List<A>> = seq(p, ref { manyOf(p) }) { x, xs -> listOf(x) + xs }

/**
 * Конструирует парсер, который вернет пустой список, если строка не удовлетворяет парсеру p хотя бы один раз.
 * Возвращает результат применения p в виде списка.
 */
fun <A> manyOf(p: Parser<A>): Parser<List<A>> = optional(someOf(p), emptyList())

/**
 * Конструирует комбинатор связывания.
 */
fun <A, B> Parser<A>.then(bind: (A) -> Parser<B>): Parser<B> = Binder(this, bind)
