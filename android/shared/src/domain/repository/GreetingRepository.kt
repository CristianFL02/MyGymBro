package com.example.mygymbroapp.domain.repository

/**
 * Contrato compartido para el saludo de ejemplo.
 * La lógica que lo utiliza no necesita conocer las APIs de cada plataforma.
 */
fun interface GreetingRepository {
    fun getGreeting(): String
}
