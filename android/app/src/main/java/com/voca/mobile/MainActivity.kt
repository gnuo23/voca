package com.voca.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voca.mobile.ui.VocaApp
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.theme.VocaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VocaTheme {
                val app: AppViewModel = viewModel(factory = AppViewModel.factory(applicationContext))
                VocaApp(app)
            }
        }
    }
}
