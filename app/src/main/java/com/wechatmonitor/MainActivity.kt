package com.wechatmonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wechatmonitor.ui.MainScreen
import com.wechatmonitor.ui.theme.WeChatMonitorTheme
import com.wechatmonitor.viewmodel.MainViewModel

/**
 * 主 Activity
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy {
        MainViewModel.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeChatMonitorTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkAccessibilityPermission()
    }
}
