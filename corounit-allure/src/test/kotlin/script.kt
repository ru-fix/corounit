fun letters(paramsCount: Int) =
        (1..paramsCount).map { 'A' + it - 1 }.joinToString(separator = ", ")


fun rowsPlaceholders(paramsCount: Int) =
    (1..paramsCount).map { 'a' + it - 1 }
            .map { "\${row.$it}" }
            .joinToString(separator = ", ")

fun rows(paramsCount: Int) =
        (1..paramsCount).map { 'a' + it - 1 }
                .map { "row.$it" }
                .joinToString(separator = ", ")


fun template(paramsCount: Int): String = """
suspend fun <${letters(paramsCount)}> parameterized(vararg rows: Row${paramsCount}<${letters(paramsCount)}>, test: suspend (${letters(paramsCount)}) -> Unit) {
    val parentStep = AllureStep.fromCurrentCoroutineContext()
    for (row in rows) {
        parentStep.step("parameterized(${rowsPlaceholders(paramsCount)})") {
            test.invoke(${rows(paramsCount)})
        }
    }
}
"""


fun main() {
    for (params in 1..22) {
        println(template(params))
    }
}