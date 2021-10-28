package dev.nmullaney.tesladashboard

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
class FullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION xor View.SYSTEM_UI_FLAG_FULLSCREEN

        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, DashFragment())
            .commit()
    }
}