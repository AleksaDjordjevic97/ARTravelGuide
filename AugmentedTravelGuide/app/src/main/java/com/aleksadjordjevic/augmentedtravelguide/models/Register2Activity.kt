package com.aleksadjordjevic.augmentedtravelguide.models

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import com.aleksadjordjevic.augmentedtravelguide.MapActivity
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityRegister2Binding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class Register2Activity : AppCompatActivity()
{
    private lateinit var binding: ActivityRegister2Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var profileImagesStorageRef:StorageReference
    private lateinit var userDocumentRef:DocumentReference

    private val galleryImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result -> if (result.resultCode == Activity.RESULT_OK) setProfilePhoto(result.data?.data)}


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        profileImagesStorageRef = FirebaseStorage.getInstance().reference.child("profile_images")
        userDocumentRef = FirebaseFirestore.getInstance().collection("users").document(auth.currentUser!!.uid)
        setupOnClickListeners()
    }

    private fun setupOnClickListeners()
    {
        binding.btnContinueRegister2.setOnClickListener {
            if(checkInputError())
            {
                writeNameAndPhoneToDatabase()
                sendToMap()
            }
        }

        binding.imgUserRegister2.setOnClickListener {
            openGallery()
        }
    }


    private fun checkInputError(): Boolean
    {
        val phone = binding.txtPhoneRegister2.text.toString().trim()

        return if(phone.isNotEmpty() && phone[0] != '+')
        {
            binding.txtPhoneRegister2.error = "The phone number must start with +"
            binding.txtPhoneRegister2.requestFocus()
            false
        }
        else
            true
    }


    private fun openGallery()
    {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageResult.launch(galleryIntent)
    }

    private fun setProfilePhoto(photoUri: Uri?)
    {
        if(photoUri != null)
        {
            Glide.with(this).load(photoUri).into(binding.imgUserRegister2)

            val filepath = profileImagesStorageRef.child("${auth.currentUser?.uid}.jpeg")
            filepath.putFile(photoUri).addOnSuccessListener { taskSnapshot ->

                filepath.downloadUrl.addOnSuccessListener {uri ->

                    val uriLink = uri.toString()
                    val userMap = hashMapOf("profile_image" to uriLink)
                    userDocumentRef.set(userMap, SetOptions.merge())

                }

            }
        }
    }

    private fun writeNameAndPhoneToDatabase()
    {
        val name = binding.txtOrganizationNameRegister2.text.toString().trim()
        val phone = binding.txtPhoneRegister2.text.toString().trim()

        val userMap = hashMapOf("organization_name" to name,
            "phone" to phone)

        userDocumentRef.set(userMap, SetOptions.merge())

    }

    private fun sendToMap()
    {
        val mapIntent = Intent(this, MapActivity::class.java)
        startActivity(mapIntent)
        finish()
    }


}