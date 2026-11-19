package com.example.mygymbroapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
