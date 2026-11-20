package com.example.mygymbroapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.mygymbroapp.presentation.home.HomeScreen
import org.jetbrains.compose.reload.DevelopmentEntryPoint

@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    val greeting = remember { Greeting().greet() }

    MaterialTheme {
        HomeScreen(greeting = greeting)
    }
}
