package org.example.myapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform