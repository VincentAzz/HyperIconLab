package com.capybara.hypericonlab.core.utils

// 打印简明可读 toString
fun Any.toPrettyKotlinString(): String {
    val src = this.toString()
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var prev: Char? = null
    var i = 0
    while (i < src.length) {
        val c = src[i]
        when {
            inString -> {
                sb.append(c)
                if (c == '"' && prev != '\\') inString = false
            }

            c == '"' -> {
                inString = true
                sb.append(c)
            }

            c == '(' || c == '[' -> {
                sb.append(c)
                if (c == '[' && i + 1 < src.length && src[i + 1] == ']') {
                    sb.append(']')
                    i += 2
                    prev = ']'
                    continue
                }
                sb.append('\n')
                indent++
                sb.append("    ".repeat(indent))
            }

            c == ')' || c == ']' -> {
                sb.append('\n')
                indent--
                sb.append("    ".repeat(indent))
                sb.append(c)
            }

            c == ',' -> {
                sb.append(",\n")
                sb.append("    ".repeat(indent))
            }
            // 等号两边补空格（仅非字符串内的赋值等号），跳过 == 等比较运算符
            c == '=' && prev != '=' && prev != ' ' && prev != '\n' -> {
                val next = if (i + 1 < src.length) src[i + 1] else null
                if (next != '=' && next != ' ') {
                    sb.append(" = ")
                } else {
                    sb.append(c)
                }
            }

            c == ' ' && (prev == ',' || prev == '(' || prev == '[') -> Unit
            else -> sb.append(c)
        }
        prev = c
        i++
    }
    return sb.toString()
}


