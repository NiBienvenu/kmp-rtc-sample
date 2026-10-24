package org.kmprtc.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KMP RTC Sample", state = rememberWindowState(width = 420.dp, height = 820.dp)) {
        App()
    }
}
