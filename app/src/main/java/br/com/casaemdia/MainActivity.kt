package br.com.casaemdia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.casaemdia.ui.navigation.CasaEmDiaNavHost
import br.com.casaemdia.ui.theme.CasaEmDiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CasaEmDiaTheme {
                CasaEmDiaNavHost()
            }
        }
    }
}
