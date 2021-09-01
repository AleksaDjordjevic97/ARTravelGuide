package com.aleksadjordjevic.augmentedtravelguide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class RegisterActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnClickListeners()
        auth = Firebase.auth
    }

    private fun setupOnClickListeners()
    {
        binding.btnBackRegister.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.txtSignIn.setOnClickListener {
            sendToLogin()
        }
    }


    private fun registerUser()
    {

        if (hasNoInputErrors())
        {
            val email: String = binding.txtEmailRegister.text.toString().trim()
            val password: String = binding.txtPasswordRegister.text.toString().trim()

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->

                if (task.isSuccessful)
                   onRegistrationSuccessful(email, password)
                else
                    task.exception?.let { onRegistrationFailed(it) }
            }
        }

    }

    private fun hasNoInputErrors(): Boolean
    {
        val email: String = binding.txtEmailRegister.text.toString().trim()
        val password: String = binding.txtPasswordRegister.text.toString().trim()
        val password2: String = binding.txtConfirmPasswordRegister.text.toString().trim()

        if (email.isEmpty())
        {
            binding.txtEmailRegister.error = "Please enter your Email."
            binding.txtEmailRegister.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            binding.txtEmailRegister.error = "Please enter a valid Email address."
            binding.txtEmailRegister.requestFocus()
            return false
        }
        if (password.isEmpty())
        {
            binding.txtPasswordRegister.error = "Please enter your password."
            binding.txtPasswordRegister.requestFocus()
            return false
        }
        if (password.length < 6)
        {
            binding.txtPasswordRegister.error = "Password must contain at least 6 characters."
            binding.txtPasswordRegister.requestFocus()
            return false
        }
        if (password2.isEmpty())
        {
            binding.txtConfirmPasswordRegister.error = "Please enter your password."
            binding.txtConfirmPasswordRegister.requestFocus()
            return false
        }
        if (password != password2)
        {
            binding.txtConfirmPasswordRegister.error = "Passwords must match"
            binding.txtConfirmPasswordRegister.requestFocus()
            return false
        }

        return true
    }

    private fun onRegistrationSuccessful(email: String,password: String)
    {
        Toast.makeText(
            applicationContext,
            "Registration successful!",
            Toast.LENGTH_SHORT
        ).show()

        loginUser(email, password)
    }

    private fun onRegistrationFailed(exception: Exception)
    {
        if (exception is FirebaseAuthUserCollisionException) Toast.makeText(
            applicationContext,
            "User with this Email already exists.",
            Toast.LENGTH_SHORT
        ).show()
        else Toast.makeText(
            applicationContext,
            "There was an error. Try again later.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loginUser(email:String, password:String)
    {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

            if (task.isSuccessful)
                addUserToDatabase()
            else
                Toast.makeText(
                applicationContext,
                "There was an error logging in.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addUserToDatabase()
    {
        if(auth.currentUser != null)
        {
            val user = hashMapOf(
                "userID" to auth.currentUser!!.uid,
                "email" to binding.txtEmailRegister.text.toString().trim()
            )

            Firebase.firestore.collection("users").document(user["userID"]!!).set(user).addOnCompleteListener {
                sendToRegister2()
            }
        }
    }

    private fun sendToLogin()
    {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish()
    }

    private fun sendToRegister2()
    {
        val reg2Intent = Intent(this, Register2Activity::class.java)
        startActivity(reg2Intent)
        finish()
    }

}