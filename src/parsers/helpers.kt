package parsers

fun <A> alt(p: Parser<A>, vararg ps: Parser<A>): Parser<A> = ps.fold(p) { x, xs -> Alt(x, xs) }

fun <X1, X2, Y> args2tuple(f: (X1, X2) -> Y): (Pair<X1, X2>) -> Y =
    { (x1, x2) -> f(x1, x2) }

fun <X1, X2, X3, Y> args2tuple(f: (X1, X2, X3) -> Y): (Pair<X1, Pair<X2, X3>>) -> Y =
    args2tuple { x1, (x2, x3) -> f(x1, x2, x3) }

fun <X1, X2, X3, X4, Y> args2tuple(f: (X1, X2, X3, X4) -> Y): (Pair<X1, Pair<X2, Pair<X3, X4>>>) -> Y =
    args2tuple { x1, x2, (x3, x4) -> f(x1, x2, x3, x4) }

fun <X1, X2, X3, X4, X5, Y> args2tuple(f: (X1, X2, X3, X4, X5) -> Y): (Pair<X1, Pair<X2, Pair<X3, Pair<X4, X5>>>>) -> Y =
    args2tuple { x1, x2, x3, (x4, x5) -> f(x1, x2, x3, x4, x5) }

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

fun <A> optional(p: Parser<A>, a: A) = alt(p, Return(a))

fun <A> someOf(p: Parser<A>): Parser<List<A>> = seq(p, ref { manyOf(p) }) { x, xs -> listOf(x) + xs }

fun <A> manyOf(p: Parser<A>): Parser<List<A>> = optional(someOf(p), emptyList())

fun <A> take(p: Parser<A>, count: Int): Parser<List<A>> =
    if (count == 0) Return(listOf()) else seq(p, take(p, count - 1)) { x, xs -> listOf(x) + xs }


fun <A, B> Parser<A>.then(bind: (A) -> Parser<B>): Parser<B> = Binder(this, bind)

/**
 * Парсер, разбирающий грамматику:
 * S -> (S)S | &epsi;
 *
 * @return Дерево разбора
 */
fun parens2(): Parser<Tree> = optional(
    seq(`(`, ref { parens2() }, `)`, ref { parens2() }) { _, left, _, right -> Tree.Node(left, right) },
    Tree.Leaf
)


enum class Spec { STR, NUM }

fun quote() = symbol('"')
fun comma() = symbol(',')

fun strSpec(): Parser<Spec> = seq(symbol('%'), symbol('s')) { _, _ -> Spec.STR }
fun intSpec(): Parser<Spec> = seq(symbol('%'), symbol('f')) { _, _ -> Spec.NUM }

fun spec() = alt(strSpec(), intSpec())

fun frmStr(): Parser<List<Spec>> =
    alt(
        seq(
            manyOf(notSymbol('%')),
            spec(),
            ref { frmStr() }
        ) { _, spec, specs -> listOf(spec) + specs },
        seq(
            manyOf(notSymbol('%', '"'))
        ) { _ -> listOf() }
    )

fun quotedFrmStr(): Parser<List<Spec>> =
    seq(
        quote(),
        frmStr(),
        quote()
    ) { _, specs, _ -> specs }


fun strType() = seq(symbol('"'), manyOf(notSymbol('"')), symbol('"')) { _, _, _ -> }
fun numType() = seq(exp()) { _ -> }

fun printf(): Parser<Unit> = seq(
    string("printf"),
    `(`,
    quotedFrmStr().then {
        it.map { spec ->
            when (spec) {
                Spec.STR -> seq(comma(), strType()) { _, _ -> }
                Spec.NUM -> seq(comma(), numType()) { _, _ -> }
            }
        }.fold(Return(Unit) as Parser<Unit>) { types, type ->
            seq(types, type) { _, _ -> }
        }
    },
    `)`
) { _, _, _, _ -> }


fun main() {
    println(parens2().parse("(()())")) // output: Success(tail=, result=Node(left=Node(left=Leaf, right=Node(left=Leaf, right=Leaf)), right=Leaf))
    println(printf().parse("printf(\"%s = %f\",\"2 + 2\",2+2)")) // output: Success(tail=, result=kotlin.Unit)
    println(printf().parse("printf(\"%f = %s\",\"2 + 2\",5)")) // output:  Fail(...)
}