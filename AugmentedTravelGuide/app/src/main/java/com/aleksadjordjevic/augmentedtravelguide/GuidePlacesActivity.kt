package com.aleksadjordjevic.augmentedtravelguide

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.aleksadjordjevic.augmentedtravelguide.adapters.GuidePlaceAdapter
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityGuidePlacesBinding
import com.aleksadjordjevic.augmentedtravelguide.fragments.GuidePlacesFragment
import com.aleksadjordjevic.augmentedtravelguide.fragments.MarkerFragment
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GuidePlacesActivity : AppCompatActivity(),GuidePlaceAdapter.OnGuidePlaceListener
{
    private lateinit var binding:ActivityGuidePlacesBinding

    private lateinit var auth:FirebaseAuth

    private lateinit var guidePlacesAdapter: GuidePlaceAdapter
    private var placesList = ArrayList<Place>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityGuidePlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.btnGuidePlacesBack.setOnClickListener { finish() }

        getAllPlacesForThisGuide()
    }

    private fun getAllPlacesForThisGuide()
    {
        Firebase.firestore.collection("places").whereEqualTo("guideID",auth.currentUser!!.uid).addSnapshotListener { value, e ->
            if (e != null)
                return@addSnapshotListener

            placesList = ArrayList<Place>()
            for (doc in value!!)
            {
                val place = doc.toObject(Place::class.java)
                placesList.add(place)
            }

            populatePlacesList()

        }
    }

    private fun populatePlacesList()
    {
        guidePlacesAdapter = GuidePlaceAdapter(this,this,placesList)
        binding.rcvGuidePlaces.adapter = guidePlacesAdapter
    }

    override fun onEditCellClick(position: Int)
    {
        val place = placesList[position]
        val guidePlaceDialogFragment = GuidePlacesFragment(place)
        guidePlaceDialogFragment.show(supportFragmentManager, "GuidePlacesFragment")
    }

    override fun onRemoveCellClick(position: Int)
    {
        val placeID = placesList[position].id
        Firebase.firestore.collection("places").document(placeID).delete()
    }


}