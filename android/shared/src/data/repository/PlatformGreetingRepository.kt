package com.example.mygymbroapp.data.repository

import com.example.mygymbroapp.Platform
import com.example.mygymbroapp.domain.repository.GreetingRepository
import com.example.mygymbroapp.getPlatform

/**
 * Implementación local del ejemplo. No realiza llamadas a la API.
 */
class PlatformGreetingRepository(
    private val platform: Platform = getPlatform(),
) : GreetingRepository {
    override fun getGreeting(): String = "Hello, ${platform.name}!"
}
