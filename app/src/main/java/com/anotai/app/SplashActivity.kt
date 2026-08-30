package com.anotai.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class SplashActivity :
    ComponentActivity() {

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private val openMain =
        Runnable {
            if (
                !isFinishing &&
                !isDestroyed
            ) {
                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

                finish()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContent {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.White
                        )
                        .padding(
                            28.dp
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Image(
                    painter =
                        painterResource(
                            R.drawable.anotai_art
                        ),
                    contentDescription =
                        "Anotaí",
                    modifier =
                        Modifier.fillMaxWidth(
                            0.78f
                        ),
                    contentScale =
                        ContentScale.Fit
                )
            }
        }

        handler.postDelayed(
            openMain,
            SPLASH_DURATION_MILLIS
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(
            openMain
        )

        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DURATION_MILLIS =
            850L
    }
}
