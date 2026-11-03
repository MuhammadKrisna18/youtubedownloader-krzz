package com.personal.ytmusicdownloader.data

import android.util.Base64
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

object DefaultCookieProvider {

    private val KEY = byteArrayOf(
        0x59, 0x74, 0x4d, 0x75, 0x73, 0x69, 0x63, 0x53,
        0x61, 0x66, 0x65, 0x53, 0x65, 0x63, 0x32, 0x36
    )

    private const val PAYLOAD = "Rv9FdXLWijljmaAGrM2R/kspHiqSP4iGr5FXXEi+NxLadX66rUnvYsJnVkumtIVZrNbpgteyyb2kLeBHN0nA8pC8deTMhpc/e7XBnBNk5QMu1KafQbLqNvIbni4yL2EJp4Wz670qkAVFqYKI/d494a1JsGRl5h7F33O2/hymiHNgmhPjn5vPnucCr33eiDnQ7mmSmKkHuTb2FB+ok45f278dcxIcABTQPb1285LcLnRoTIxlQ+Mi7Y2uyzKi0RwsEQjZCHLTYzDFss5wz74dd8HCocZT41aSBB+zNkTzGbyUvc/hwNCurWaFOqVgQT+lQTp+eX6s0sDcbPS+XGyatFJ1iOfNU+sBUSulZ2/6hBYbI6d2soiMgJen6zaXpRdbBwO4gliY4pHOk5gZG4OtOKYAcgP1xazBiKW11oqqDkMk5+FqoFkEDIdCDU9DC9QRALgpuCRAG7sN9oMZe8p3rN4JU5qsMKZlOp9J9IUd2UfM8zJgi3pWLCo4OSqO1YOkEGJEDKJQAdk06lSQU3qjshRcIDN18BQhFK8z1jlojHtiSm5AEXOJ1u4zq4L8pIKGdbxu4MYu5OYyIdFdN+tY08hHgidPfVkJCBXJJMeMMlYuSqb8h8O9kEixv6OLgUDzbsScZqoNcmMt9xosfGW0uN9QpkrJZQGWuishlKDV1mpTeDl5/t3wm0CNyaGBfBa1m/ke3Z7OIZcmkx2Y5p5uDWvkYPXvo+SvBUGeOohijLMOc2Dy0Bx97wSFM8+v0PiIcxx7TJ929TQAPMCM1zXjghNFsC9nlSU80lYnYMQs/FjT6ZzLAivtH+S4KPnj6+JRKR22vIYFuKYUqCCWrBB9Vwlj2JqZ6+xHLntI+dHaLmUBLNAlf8vl1Zwg5nDp+OB7uBj6BP/qLiuUw9XugwfvSnLd1N4EwrT/LNEN6D0lUumLOZMZzDhQwjK7g2djlhbOWMd32+3NDmJFvazlwPAsYtzhR/9jkEFKo2yT9kMyMBMRbs710lLRsGtc7XMzggts+LmLfAd+g+7eFmKida/mBnycMDi4n60xJqHabY9KjD78NXLblDLP3ZBfElXhUMT0Y/bny2xbznb1uAgUBdA20cZiQia/Klfc+YQS+qlclREuylKN76198/u0QGZlIbFze495CVHao+QpfzSpeTPAX6LGIgXrLQNJnk9ntzZnnDXUkShvhMmR69vfMaL/7AJtjIgPpGawLw6X1lHZ81pt/EUIg/26ta3FJn0U7vq2T0Cw3pbU21jaVIygcZs3096ZqLSz6k/BnLFgZqH5p3lcl/arFaXWiij5QNUdhn9rWom7Id2FZP1i5LTDltWGLGugmw5p6UJEuw892462R9GVUbhUhuLQ8OV6i1WabJtMzCG7q7JiR2VAe+2aSRnB3DQTwR9Dxe0aOFfv0CugDH2ipENtmso8Dtjg+ivMBe4y24t8BX194LA69TP8+uLc6cMVQzuxuSwA3yfrU2oNeFis1pxTShypzpMtwmP6p1yb3n7W4pMvqPp+zWeOoMMgKJ/xL1S+nnvNOGEq4QB1I4PiZHpreNlMGZLTpiB4F+ll1TD8cANkiqNaigtxQMPm3iCq2cDJ/D+V2h8usw8gS4AN4uSmt8fCbtr7tHjijgp0RT+WGYF4gKpkwgjfwGR7dpIDE62h2rxbNTqjIvohaZ9gn3bG2zuteh1eHMhK8fmX/I9pEMwrPFACpMZNKSnvm6buo6PnGGiuwLLY1w77520zW2KSgcgTr5rCEBq2YAi+mJKTSmYR6VnIHdL2OK2LNlMeK3R5DVUYEgHXPNE8GmFX81AXdXxzaQ=="

    fun getDecryptedCookie(): String? = runCatching {
        val decoded = Base64.decode(PAYLOAD, Base64.DEFAULT)
        val decrypted = ByteArray(decoded.size) { i ->
            (decoded[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        GZIPInputStream(ByteArrayInputStream(decrypted)).bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }.getOrNull()
}
