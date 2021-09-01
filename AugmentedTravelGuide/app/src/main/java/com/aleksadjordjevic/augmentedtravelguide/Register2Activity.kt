package com.aleksadjordjevic.augmentedtravelguide

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
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

    private val galleryImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
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
            if(hasNoInputErrors())
            {
                writeNameAndPhoneToDatabase()
                sendToMap()
            }
        }

        binding.imgUserRegister2.setOnClickListener {
            openGallery()
        }
    }


    private fun hasNoInputErrors(): Boolean
    {
        val name = binding.txtOrganizationNameRegister2.text.toString().trim()
        val phone = binding.txtPhoneRegister2.text.toString().trim()

        if(name.isEmpty())
        {
            binding.txtOrganizationNameRegister2.error = "You need to enter an organization name"
            binding.txtOrganizationNameRegister2.requestFocus()
            return false
        }

        if(phone.isNotEmpty() && phone[0] != '+')
        {
            binding.txtPhoneRegister2.error = "The phone number must start with +"
            binding.txtPhoneRegister2.requestFocus()
            return false
        }

        return true
    }


    private fun openGallery()
    {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageLauncher.launch(galleryIntent)
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