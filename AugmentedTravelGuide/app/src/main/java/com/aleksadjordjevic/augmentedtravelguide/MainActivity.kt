package com.aleksadjordjevic.augmentedtravelguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private var cameraPermission = false
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnClickListeners()
        requestCameraPermission()

        auth = FirebaseAuth.getInstance()

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

    private fun requestCameraPermission()
    {
        when
        {

            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                cameraPermission = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ->
            {

                Toast.makeText(
                    this,
                    "Be aware that if you choose Deny you won't be able to use all the features of the app!",
                    Toast.LENGTH_LONG
                ).show()

                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        }
    }

}