package com.aleksadjordjevic.augmentedtravelguide.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.aleksadjordjevic.augmentedtravelguide.R
import com.aleksadjordjevic.augmentedtravelguide.databinding.FragmentAddPlaceBinding
import com.bumptech.glide.Glide
import android.text.Editable

import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage


class AddPlaceFragment(private val guideID:String) : DialogFragment()
{
    private var _binding: FragmentAddPlaceBinding? = null
    private val binding get() = _binding!!

    private var photoUriForNewPhoto:Uri? = null
    private var modelUriForAR:Uri? = null
    private var modelType:String = ""

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    private val galleryImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result -> if (result.resultCode == Activity.RESULT_OK) setNewImage(result.data?.data)}

    private val galleryModelResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result -> if (result.resultCode == Activity.RESULT_OK) setNewModel(result.data?.data)}



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        getDeviceLocation()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        _binding = FragmentAddPlaceBinding.inflate(inflater,container,false)
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


        setupOnClickListeners()
        setupTextChangeListeners()
    }


    private fun setupOnClickListeners()
    {
        binding.addPlaceImage.setOnClickListener { openGallery() }
        binding.btnUploadGLB.setOnClickListener { uploadGLB() }
        binding.btnAddPlace.setOnClickListener { createNewPlace() }
        binding.btCloseAddPlace.setOnClickListener { dismiss() }
    }



    private fun uploadGLB()
    {
        val modelIntent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        modelIntent.type = "application/*"
        galleryModelResult.launch(modelIntent)
        modelType = ".glb"
    }

    private fun setNewModel(modelUri: Uri?)
    {
        if(modelUri != null)
        {
            modelUriForAR = modelUri
            binding.addPlaceUploadFileName.text = modelUri.lastPathSegment
            binding.addPlaceNum2.setImageResource(R.drawable.number_check)
        }
        else
            binding.addPlaceNum2.setImageResource(R.drawable.number_1)
    }

    private fun setupTextChangeListeners()
    {

        binding.txtNameAddPlace.addTextChangedListener(object : TextWatcher
        {
            override fun afterTextChanged(s: Editable)
            {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int)
            {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int)
            {
               if(binding.txtNameAddPlace.text.trim().isEmpty())
                   binding.addPlaceNum3.setImageResource(R.drawable.number_3)
                else
                   binding.addPlaceNum3.setImageResource(R.drawable.number_check)
            }
        })

        binding.txtDescriptionAddPlace.addTextChangedListener(object : TextWatcher
        {
            override fun afterTextChanged(s: Editable)
            {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int)
            {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int)
            {
                if(binding.txtDescriptionAddPlace.text.trim().isEmpty())
                    binding.addPlaceNum4.setImageResource(R.drawable.number_4)
                else
                    binding.addPlaceNum4.setImageResource(R.drawable.number_check)
            }
        })



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
            Glide.with(this).load(photoUri).into(binding.addPlaceImage)
            binding.addPlaceNum1.setImageResource(R.drawable.number_check)
        }
        else
            binding.addPlaceNum1.setImageResource(R.drawable.number_1)
    }

    private fun getDeviceLocation()
    {
        try
        {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful)
                    lastKnownLocation = task.result
            }
        }catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun createNewPlace()
    {
        if(checkIfAllIsFilled())
        {
            val markerGeoPoint = if(lastKnownLocation != null)
                GeoPoint(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
            else
                GeoPoint(43.304,21.9)

            val place = hashMapOf("guideID" to guideID,
                                    "name" to binding.txtNameAddPlace.text.toString().trim(),
                                    "description" to binding.txtDescriptionAddPlace.text.toString().trim(),
                                    "geoPoint" to markerGeoPoint)

            val addedDocRef = Firebase.firestore.collection("places").add(place)

            addedDocRef.addOnCompleteListener{task->
                if(task.isSuccessful)
                {
                    val placeID = hashMapOf("id" to addedDocRef.result.id)
                    updatePlaceInDB(addedDocRef.result.id,placeID)

                    val filepathImages = FirebaseStorage.getInstance().reference.child("images_for_scanning")
                        .child("${addedDocRef.result.id}.jpeg")
                    filepathImages.putFile(photoUriForNewPhoto!!).addOnSuccessListener { taskSnapshot ->

                        filepathImages.downloadUrl.addOnSuccessListener { uri ->

                            val placeImage = hashMapOf("image_for_scanning" to uri.toString())
                            updatePlaceInDB(addedDocRef.result.id,placeImage)
                        }
                    }

                    val filepathModels = FirebaseStorage.getInstance().reference.child("models")
                        .child("${addedDocRef.result.id}$modelType")
                    filepathModels.putFile(modelUriForAR!!).addOnSuccessListener { taskSnapshot ->

                        filepathModels.downloadUrl.addOnSuccessListener { uri ->

                            val placeModel = hashMapOf("model_for_ar" to uri.toString())
                            updatePlaceInDB(addedDocRef.result.id,placeModel)
                        }
                    }

                    dismiss()
                }
            }


        }
    }

    private fun checkIfAllIsFilled():Boolean
    {
        return photoUriForNewPhoto != null && modelUriForAR != null && binding.txtNameAddPlace.text.trim().isNotEmpty() && binding.txtDescriptionAddPlace.text.trim().isNotEmpty()
    }

    private fun updatePlaceInDB(placeID:String,field:HashMap<String,String>)
    {
        Firebase.firestore.collection("places").document(placeID).set(field, SetOptions.merge())
    }


}