package com.aleksadjordjevic.augmentedtravelguide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupOnClickListeners()
    }

    private fun setupOnClickListeners()
    {
        binding.btnBackLogin.setOnClickListener {
            finish()
        }

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.txtSignUp.setOnClickListener {
            val loginIntent = Intent(this, RegisterActivity::class.java)
            startActivity(loginIntent)
            finish()
        }
    }

    private fun loginUser()
    {
        if(checkInputError())
        {
            val email = binding.txtEmailLogin.text.toString()
            val password = binding.txtPasswordLogin.text.toString()
            auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
                if (task.isSuccessful)
                {
                    Toast.makeText(applicationContext, "Login successful!", Toast.LENGTH_SHORT)
                        .show()

                    sendToMap()
                }
                else
                {

                    if(task.exception is FirebaseAuthInvalidCredentialsException)
                        Toast.makeText(
                            applicationContext,
                            "Wrong password",
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        Toast.makeText(
                            applicationContext,
                            "There was an error logging in. Make sure a user with these credentials exists",
                            Toast.LENGTH_SHORT
                        ).show()

                }
            }
        }
    }

    private fun checkInputError(): Boolean
    {
        if (binding.txtEmailLogin.text.isEmpty())
        {
            binding.txtEmailLogin.error = "Please enter your Email."
            binding.txtEmailLogin.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.txtEmailLogin.text).matches())
        {
            binding.txtEmailLogin.error = "Please enter a valid Email address."
            binding.txtEmailLogin.requestFocus()
            return false
        }
        if (binding.txtPasswordLogin.text.isEmpty())
        {
            binding.txtPasswordLogin.error = "Please enter your password."
            binding.txtPasswordLogin.requestFocus()
            return false
        }
        return true
    }

    private fun sendToMap()
    {
        val mapIntent = Intent(this, MapActivity::class.java)
        startActivity(mapIntent)
        finish()
    }

}