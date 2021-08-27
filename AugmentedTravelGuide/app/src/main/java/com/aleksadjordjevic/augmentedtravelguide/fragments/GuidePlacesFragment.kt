package com.aleksadjordjevic.augmentedtravelguide.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.aleksadjordjevic.augmentedtravelguide.R
import com.aleksadjordjevic.augmentedtravelguide.adapters.GuidePlaceAdapter
import com.aleksadjordjevic.augmentedtravelguide.databinding.FragmentGuidePlacesBinding
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.bumptech.glide.Glide
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage


class GuidePlacesFragment(private val place:Place) : DialogFragment()
{

    private var _binding: FragmentGuidePlacesBinding? = null
    private val binding get() = _binding!!


    private lateinit var guidePlacesAdapter:GuidePlaceAdapter
    private var placesList = ArrayList<Place>()
    private var photoUriForNewPhoto:Uri? = null

    private val galleryImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result -> if (result.resultCode == Activity.RESULT_OK) setNewImage(result.data?.data)}

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        _binding = FragmentGuidePlacesBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart()
    {
        super.onStart()
        val dialog: Dialog? = dialog

        if (dialog != null)
        {
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels*0.85).toInt()
            val height = (displayMetrics.heightPixels*0.85).toInt()
            dialog.window?.setLayout(width,height)
            dialog.window?.setBackgroundDrawableResource(R.drawable.white_bg)
            dialog.window?.setDimAmount(0.5f)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(this).load(place.image_for_scanning).into(binding.guidePlacesFragmentImage)
        binding.guidePlacesFragmentName.setText(place.name)
        binding.guidePlacesFragmentDescription.setText(place.description)

        binding.btnGuidePlacesFragmentClose.setOnClickListener { dismiss() }
        binding.btnGuidePlacesFragmentSave.setOnClickListener { saveChanges() }
        binding.guidePlacesFragmentImage.setOnClickListener { openGallery() }
    }

    private fun openGallery()
    {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageResult.launch(galleryIntent)
    }

    private fun setNewImage(photoUri: Uri?)
    {
        if(photoUri != null)
        {
            photoUriForNewPhoto = photoUri
            Glide.with(this).load(photoUri).into(binding.guidePlacesFragmentImage)
        }
    }



    private fun saveChanges()
    {

        if(photoUriForNewPhoto != null)
        {
            val filepath = FirebaseStorage.getInstance().reference.child("images_for_scanning")
                .child("${place.guideID}.jpeg")
            filepath.putFile(photoUriForNewPhoto!!).addOnSuccessListener { taskSnapshot ->

                filepath.downloadUrl.addOnSuccessListener { uri ->

                    place.image_for_scanning =  uri.toString()
                    place.name =  binding.guidePlacesFragmentName.text.toString()
                    place.description = binding.guidePlacesFragmentDescription.text.toString()

                    updatePlaceInDB()
                }
            }
        }
        else
        {
            place.name =  binding.guidePlacesFragmentName.text.toString()
            place.description = binding.guidePlacesFragmentDescription.text.toString()

            updatePlaceInDB()
        }

    }

    private fun updatePlaceInDB()
    {
        Firebase.firestore.collection("places").document(place.id).set(place, SetOptions.merge()).addOnCompleteListener { task->

            if(task.isSuccessful)
                Toast.makeText(requireContext(),"Changes made successfully!",Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(requireContext(),"Changes failed. Try again later",Toast.LENGTH_SHORT).show()

            dismiss()
        }
    }


}