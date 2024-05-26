# Парсер комбинаторы на Kotlin

## Введение

Существуют разные способы реализации синтаксических анализоторов для заданной граматики. 
В статье рассматривается подход, называемый **Комбинаторы синтаксического анализа**, так же можно встретить название **Парсер комбинаторы**.
Когда, я в первые познакомился с этим подходом, он произвел на меня сильное впечатление, 
потому что, без использвоания внешних фреймворков можно легко и быстро реализовать свою библиотеку, 
которая позволяет описывать парсеры в деклатротивным стиле. 
**Парсер комбинаторы** можно использовать для прототипирования или написания легко расширяемого синтаксического анализатора.
В статье мы рассмотрим примеры реализации **Парсер комбинаторов** на языке Kotlin. 
Напишим синтаксические анализаторы для арифмитических выражений и функций вида `"%s = %d".format("2 + 2", 5)`, в которых типы аргументов определяются строкой формата.
А также увидим примеры применения комбинаторов не только для написания парсеров.

### Вспомним несколько понятий из теории формальных языков. ### 
**Формальный язык** это множество слов над конечным множеством символов - алфавитом. 
Для того что бы выделить из всего множества слов некоторое его подмножество используют **формальные граммтики**.
Грамматику можно задать набором правил вида: `L -> R`, где `L` - непустая последовательность терминалов и нетерминалов, содержащая хотя бы один нетерминал, 
а `R` - любая последовательность терминалов и нетерминалов. Терминал это объект, состоящие только из симфолов алфавита, а нетерминал - объект обозначающий какую либо сущность языка.
Рассмотрим пример задания правильной скобочной последовательности: 
<pre>
S -> (S)S
S -> &epsi;
</pre>
где `(`, `)` - терминальные символы, `S` - нетерминальный символ, &epsi; - представляет пустую строку.

Для классификации грамматик используют иерархию, предложенную Ноамом Хомским.
По иерархии Хомского грамматики делятся на 4 типа, каждый последующий является более ограниченным подмножеством предыдущего и легче поддается анализу:
- тип 0. неограниченные грамматики — возможны любые правила;
- тип 1. контекстно-зависимые грамматики — левая часть может содержать один нетерминал, окруженный «контекстом» - последовательность символов, в том же виде присутствующая в правой части;
- тип 2. контекстно-свободные грамматики — левая часть состоит из одного нетерминала;
- тип 3. регулярные грамматики.

В статье, мы в качестве примеров, будем рассматривать анализаторы, соответствующие только контекстно-свободным(КС) и контекстно-зависимым(КЗ) грамматикам.
К примеру, языки, порождаемые КС-грамматиками это - правильные скобочные последовательнтсти(пример грамматики был выше) или арифметические выражения.
В качестве языков, порождаемых КЗ-грамматиками можно рассмотреть, например, протоколы передачи данных, 
в которых сообщение сосотоит из заголовка, содержащего длину сообщения, а дальше следует само сообщение. Также интересный пример всречается нам в современных IDE, 
которые умеют подсвечивать проблемы в случае аргументов направильного типа в коде вида:
```kotlin
"%s = %d".format("2 + 2", 5)
```
Для того что бы разобрать КС-грамматику достаточно автомата с магазинной памятью(МП-автомат),
а вот для разбора КЗ-грамматики потребуется линейно-ограниченный автомат, более можщный формализм с точки зрения вычислимости по сравнению с МП-автоматом. 

### Комбинаторы синтаксичсеского анализа ###

Парсер косбинаторы занимают уникальное место в области синтаксичсеского анализа, они позволяют писать выражения, 
повторяющие струкутру грамматических правил, то есть задают удобный DSL для написания парсеров. 

Я познакомился с это концепцией в [статье Еруна Фокера](https://ru.wikibooks.org/wiki/Функциональные_парсеры).
В ней указано, что впервые, такой подход упоминается в книге Вильяма Бурджа "Recursive programming techniques", изданной в 1975 году.  ???
На текущий момент для разных языков программирования существуют удобные библиотеки для работы с парсер комбинаторами.

Итак, комбинаторы парсеров это конструкторы, которые на основе элементарных парсеров создают более сложные парсеры, например комбинатор альтернативы `Alt(p1, p2)` 
применит к строке сначала парсер `p1` и если разбор завершится с ошибкой, то применит парсер `p2`. Или комбинатор следования `Seq(p1, p2)`, прменяет последовательно парсеры `p1` и `p2`.