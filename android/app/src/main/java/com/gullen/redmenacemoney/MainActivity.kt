package com.gullen.redmenacemoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gullen.redmenacemoney.ui.RedMenaceApp
import com.gullen.redmenacemoney.ui.theme.RedMenaceMoneyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedMenaceMoneyTheme {
                RedMenaceApp(vm = viewModel)
            }
        }
    }
}
