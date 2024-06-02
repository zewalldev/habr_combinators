package examples

import parsers.*


interface Tree {
    data object Leaf : Tree
    data class Node(val left: Tree, val right: Tree) : Tree
}

val `(` = symbol('(')
val `)` = symbol(')')

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

fun parens2(): Parser<Tree> = optional(
    seq(`(`, ref { parens2() }, `)`, ref { parens2() }) { _, left, _, right -> Tree.Node(left, right) },
    Tree.Leaf
)


/**
 * Пример парсеров разбирающих правильную скобочную последовательность:
 * S -> (S)S | &epsi;
 * parens() - без использования вспомогательных функций
 * parens2() - с использования вспомогательных функций, код выглядит проще
 */
fun main() {
    println(parens().parse("(()())")) // output: Success(tail=, result=Node(left=Node(left=Leaf, right=Node(left=Leaf, right=Leaf)), right=Leaf))
    println(parens2().parse("(()())")) // output: Success(tail=, result=Node(left=Node(left=Leaf, right=Node(left=Leaf, right=Leaf)), right=Leaf))
}
