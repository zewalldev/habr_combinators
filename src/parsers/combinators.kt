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

fun main() {
    println(parens().parse("(()())")) // output: Success(tail=, result=Node(left=Node(left=Leaf, right=Node(left=Leaf, right=Leaf)), right=Leaf))
}