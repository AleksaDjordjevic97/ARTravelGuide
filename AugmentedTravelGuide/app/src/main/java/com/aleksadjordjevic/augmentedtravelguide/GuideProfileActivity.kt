package com.aleksadjordjevic.augmentedtravelguide

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityGuideProfileBinding
import com.aleksadjordjevic.augmentedtravelguide.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class GuideProfileActivity : AppCompatActivity()
{
    private lateinit var binding:ActivityGuideProfileBinding

    private lateinit var auth:FirebaseAuth

    private val galleryImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result -> if (result.resultCode == Activity.RESULT_OK) setProfilePhoto(result.data?.data)}

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityGuideProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnClickListeners()
        auth = FirebaseAuth.getInstance()
        getGuideInfo()
    }

    private fun setupOnClickListeners()
    {
        binding.btnGuideProfileBack.setOnClickListener { finish() }
        binding.btnSaveGuideProfile.setOnClickListener { saveChanges() }
        binding.imgUserGuideProfile.setOnClickListener { openGallery() }
    }

    private fun getGuideInfo()
    {
        Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get().addOnSuccessListener { document ->

            val user = document.toObject(User::class.java)
            if(user!!.profile_image != "")
                Glide.with(this).load(user!!.profile_image).into(binding.imgUserGuideProfile)
            else
                Glide.with(this).load(R.drawable.user).into(binding.imgUserGuideProfile)
            binding.txtOrganizationNameGuideProfile.setText(user.organization_name)
            binding.txtPhoneGuideProfile.setText(user.phone)
        }
    }

    private fun saveChanges()
    {
        if(hasNoInputErrors())
            writeNameAndPhoneToDatabase()

    }

    private fun hasNoInputErrors(): Boolean
    {
        val name = binding.txtOrganizationNameGuideProfile.text.toString().trim()
        val phone = binding.txtPhoneGuideProfile.text.toString().trim()

        if(name.isEmpty())
        {
            binding.txtOrganizationNameGuideProfile.error = "You need to enter an organization name"
            binding.txtOrganizationNameGuideProfile.requestFocus()
            return false
        }

        if(phone.isNotEmpty() && phone[0] != '+')
        {
            binding.txtPhoneGuideProfile.error = "The phone number must start with +"
            binding.txtPhoneGuideProfile.requestFocus()
            return false
        }

        return true
    }

    private fun writeNameAndPhoneToDatabase()
    {
        val name = binding.txtOrganizationNameGuideProfile.text.toString().trim()
        val phone = binding.txtPhoneGuideProfile.text.toString().trim()

        val userMap = hashMapOf("organization_name" to name,
            "phone" to phone)

        FirebaseFirestore.getInstance().collection("users").document(auth.currentUser!!.uid).set(userMap, SetOptions.merge()).addOnCompleteListener { task->
            if(task.isSuccessful)
                Toast.makeText(this,"Changes made successfully",Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this,"There was an error changing your data. Please try again later",Toast.LENGTH_SHORT).show()
        }
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
            Glide.with(this).load(photoUri).into(binding.imgUserGuideProfile)

            val filepath = FirebaseStorage.getInstance().reference.child("profile_images").child("${auth.currentUser?.uid}.jpeg")
            filepath.putFile(photoUri).addOnSuccessListener { taskSnapshot ->

                filepath.downloadUrl.addOnSuccessListener {uri ->

                    val uriLink = uri.toString()
                    val userMap = hashMapOf("profile_image" to uriLink)
                    FirebaseFirestore.getInstance().collection("users").document(auth.currentUser!!.uid).set(userMap, SetOptions.merge()).addOnSuccessListener {
                        Toast.makeText(this,"Successfully changed photo!",Toast.LENGTH_SHORT).show()
                    }

                }

            }
        }
    }
}