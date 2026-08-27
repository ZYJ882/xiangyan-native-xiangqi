package com.xiangyan.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xiangyan.nativeapp.ui.XiangqiApp

/** 首帧只挂载 Compose；本地引擎由用户进入人机对弈后按需创建。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { XiangqiApp() }
    }
}
