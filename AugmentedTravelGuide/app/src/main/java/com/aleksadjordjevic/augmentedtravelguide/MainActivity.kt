package com.aleksadjordjevic.augmentedtravelguide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupOnClickListeners()

    }

    private fun setupOnClickListeners()
    {
        binding.btnTouristMain.setOnClickListener {continueAsTourist()}
        binding.btnGuideMain.setOnClickListener {continueAsGuide()}
    }

    private fun continueAsTourist()
    {
        if(isGuideIsLoggedIn())
            auth.signOut()

        val mapIntent = Intent(this, MapActivity::class.java)
        startActivity(mapIntent)
    }

    private fun continueAsGuide()
    {
        val guideIntent:Intent = if(isGuideIsLoggedIn())
            Intent(this, MapActivity::class.java)
        else
            Intent(this, LoginActivity::class.java)

        startActivity(guideIntent)
    }

    private fun isGuideIsLoggedIn():Boolean
    {
        return auth.currentUser != null
    }

}