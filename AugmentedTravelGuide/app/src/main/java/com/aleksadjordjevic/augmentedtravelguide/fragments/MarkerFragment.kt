package com.aleksadjordjevic.augmentedtravelguide.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.aleksadjordjevic.augmentedtravelguide.R
import com.aleksadjordjevic.augmentedtravelguide.databinding.FragmentMarkerBinding
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MarkerFragment(private val placeID:String) : DialogFragment()
{
    private var _binding:FragmentMarkerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {

        _binding = FragmentMarkerBinding.inflate(inflater,container,false)
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

        Firebase.firestore.collection("places").document(placeID).get().addOnSuccessListener{ document ->

            val place = document?.toObject(Place::class.java)
            Glide.with(this).load(place!!.image_for_scanning).into(binding.markerFragmentImage)
            binding.markerFragmentName.text = place.name
            binding.markerFragmentDescription.text = place.description
        }

        binding.btnMarkerFragmentClose.setOnClickListener { dismiss() }

    }


}