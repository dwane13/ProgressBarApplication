package com.example.progressbarapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {


    private var mIsForcedAnimation = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bar = findViewById<CircularProgressIndicator>(R.id.rProgressBar)
        val applyProgressButton = findViewById<Button>(R.id.rApplyProgress)
        val progressText = findViewById<EditText>(R.id.rProgressPercent)
        val forceAnimationSwitch = findViewById<SwitchMaterial>(R.id.rDisableAnimation)

        forceAnimationSwitch.setOnCheckedChangeListener { _, isChecked ->
            mIsForcedAnimation = isChecked
        }

        applyProgressButton.setOnClickListener {
            val progress = progressText.text?.toString()?.toFloat()?.div(100f)

            progress?.let {
                if (progress > 1f || progress < 0f) {
                    Toast.makeText(baseContext, "Не балуйтесь", Toast.LENGTH_SHORT).show()
                } else {
                    bar.setProgress(progress, mIsForcedAnimation)
                }
            }
        }

    }
}