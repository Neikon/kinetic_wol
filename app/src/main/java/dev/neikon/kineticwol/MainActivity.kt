package dev.neikon.kineticwol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.neikon.kineticwol.ui.KineticWolApp
import dev.neikon.kineticwol.ui.theme.KineticWolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as KineticWolApplication).appContainer

        setContent {
            KineticWolTheme {
                KineticWolApp(appContainer = appContainer)
            }
        }
    }
}
