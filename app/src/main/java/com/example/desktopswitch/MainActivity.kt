package com.example.desktopswitch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // opsional: buka setting Accessibility untuk enable service secara manual
    startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
    finish()
  }
}
