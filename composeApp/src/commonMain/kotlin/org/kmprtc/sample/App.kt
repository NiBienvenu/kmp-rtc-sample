package org.kmprtc.sample

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kmprtc.sample.ui.screens.AuthScreen
import org.kmprtc.sample.ui.screens.CallScreen
import org.kmprtc.sample.ui.screens.HomeScreen
import org.kmprtc.sample.ui.theme.AppTheme

/**
 * Navigation volontairement simple :
 *   pas connecte -> AuthScreen
 *   connecte     -> HomeScreen
 *   appel actif  -> CallScreen (par-dessus tout, y compris un appel entrant)
 */
@Composable
fun App(vm: AppViewModel = viewModel { AppViewModel() }) {
    val state by vm.state.collectAsStateWithLifecycle()
    val call by vm.engine.call.collectAsStateWithLifecycle()
    val registration by vm.engine.registration.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            val screen = when {
                state.booting -> "boot"
                call != null -> "call"
                state.me == null -> "auth"
                else -> "home"
            }
            AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }) { s ->
                when (s) {
                    "boot" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    "call" -> call?.let { CallScreen(it, vm) }
                    "auth" -> AuthScreen(state, vm)
                    else -> HomeScreen(state, registration, vm, snackbar)
                }
            }
        }
    }
}
