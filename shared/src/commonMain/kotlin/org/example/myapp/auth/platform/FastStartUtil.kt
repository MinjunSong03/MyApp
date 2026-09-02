package org.example.myapp.auth.platform

expect object FastStartUtil {
    fun process(inputBytes: ByteArray): ByteArray
}