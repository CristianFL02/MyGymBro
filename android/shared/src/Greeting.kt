package com.example.mygymbroapp

import com.example.mygymbroapp.data.repository.PlatformGreetingRepository
import com.example.mygymbroapp.domain.repository.GreetingRepository

class Greeting(
    private val repository: GreetingRepository = PlatformGreetingRepository(),
) {
    fun greet(): String = repository.getGreeting()
}
