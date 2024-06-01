package parsers

class Seq<A, B>(private val pa: Parser<A>, private val pb: Parser<B>) : Parser<Pair<A, B>> {
    override fun parse(str: String) = pa.parse(str).map { it1 ->
        pb.parse(it1.tail).map { it2 ->
            ParseResult.Success(it2.tail, it1.result to it2.result)
        }
    }
}

class Alt<A>(private val p1: Parser<A>, private val p2: Parser<A>) : Parser<A> {
    override fun parse(str: String) = when (val r = p1.parse(str)) {
        is ParseResult.Fail -> p2.parse(str)
        is ParseResult.Success -> r
    }
}

fun <A> ref(lazy: () -> Parser<A>) = object : Parser<A> {
    override fun parse(str: String) = lazy().parse(str)
}

class Mapper<A, B>(private val p: Parser<A>, private val f: (A) -> B) : Parser<B> {
    override fun parse(str: String) = p.parse(str).map { ParseResult.Success(it.tail, f(it.result)) }
}

interface Tree {
    data object Leaf : Tree
    data class Node(val left: Tree, val right: Tree) : Tree
}

val `(` = symbol('(')
val `)` = symbol(')')

/**
 * Парсер, разбирающий грамматику:
 * S -> (S)S | &epsi;
 *
 * @return Дерево разбора
 */
fun parens(): Parser<Tree> = Alt(
    Mapper(
        Seq(
            `(`,
            Seq(
                ref { parens() },
                Seq(
                    `)`,
                    ref { parens() }
                )
            )
        )
    ) { (_, a) -> Tree.Node(a.first, a.second.second) },
    Return(Tree.Leaf)
)

fun digit0() = seq(symbol('0')) { 0 }
fun digit19(): Parser<Int> = seq(
    alt(
        symbol('1'), symbol('2'), symbol('3'), symbol('4'), symbol('5'),
        symbol('6'), symbol('7'), symbol('8'), symbol('9')
    )
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

fun main() {
    println(parens().parse("(()())")) // output: Success(tail=, result=Node(left=Node(left=Leaf, right=Node(left=Leaf, right=Leaf)), right=Leaf))
    println(exp().parse("3+4*(1/(2*3*4)-1/(4*5*6)+1/(6*7*8)-1/(8*9*10)+1/(10*11*12))")) // output: Success(tail=, result=3.1427128427128426)
}