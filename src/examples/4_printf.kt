package examples

import parsers.*

/**
 * Типы спецификаторов в строке формата.
 */
enum class Spec { STR, NUM }

/**
 * Вспомогательные парсеры.
 */
fun quote() = symbol('"')
fun comma() = symbol(',')

/**
 * Парсеры спецификаторов.
 */
fun strSpec(): Parser<Spec> = seq(symbol('%'), symbol('s')) { _, _ -> Spec.STR }
fun intSpec(): Parser<Spec> = seq(symbol('%'), symbol('f')) { _, _ -> Spec.NUM }

fun spec() = alt(strSpec(), intSpec())

/**
 * Парсер строки формата.
 */
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


/**
 * Парсеры типов аргументов.
 */
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

/**
 * Пример разбора КЗ-грамматики.
 * Анализирует что в выражении вида printf("%s = %f", "2 + 2", 5), типы аргументов соответствуют спецификаторам.
 */
fun main() {
    println(printf().parse("printf(\"%s = %f\",\"2 + 2\",2+2)")) // output: Success(tail=, result=kotlin.Unit)
    println(printf().parse("printf(\"%f = %s\",\"2 + 2\",5)")) // output:  Fail(...)
}
