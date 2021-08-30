package com.aleksadjordjevic.augmentedtravelguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityMapBinding
import com.aleksadjordjevic.augmentedtravelguide.fragments.AddPlaceFragment
import com.aleksadjordjevic.augmentedtravelguide.fragments.MarkerFragment
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.aleksadjordjevic.augmentedtravelguide.models.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File

private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100

class MapActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener
{

    private lateinit var binding: ActivityMapBinding
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null

    private lateinit var auth:FirebaseAuth

    private lateinit var mMap: GoogleMap
    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    private var placesList = ArrayList<Place>()

     var downloadID:Long = 0L

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        setupNavView()
        setupOnClickListeners()

        auth = FirebaseAuth.getInstance()
        if(auth.currentUser == null)
        {
            binding.fabAddPlace.visibility = View.INVISIBLE
            binding.btnOpenNavView.visibility = View.INVISIBLE
            binding.drawerLayoutMap.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
        else
            setupNavViewHeader()


        var br = object:BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?)
            {
                var id = p1?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)
                if(id==downloadID)
                    Toast.makeText(this@MapActivity,"Download completed",Toast.LENGTH_LONG).show()
            }

        }
        registerReceiver(br, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    }

    @SuppressLint("RtlHardcoded")
    private fun setupOnClickListeners()
    {
        binding.fabARView.setOnClickListener {


//            Firebase.firestore.collection("places").document("9Dbfb6Hbxy2oI21wDwfC").get().addOnCompleteListener { task->
//                if(task.isSuccessful)
//                {
//                    val imageURL = task.result.getString("image_for_scanning")
//
//
//
//                    Glide.with(this)
//                        .asBitmap()
//                        .load(imageURL)
//                        .into(object : CustomTarget<Bitmap>(){
//                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//
//                                compressBitmapToStream("bitmap.png", resource)
//                                val uri = downloadPdf(this@MapActivity,task.result.getString("model_for_ar"),"bla")
//                                val arIntent = Intent(this@MapActivity, ARCameraActivity::class.java)
//                                arIntent.putExtra("USER_IMAGE_BITMAP_FILENAME", "bitmap.png")
//                                arIntent.putExtra("URL_MODEL",uri.toString())
//                                startActivity(arIntent)
//
//
//                            }
//                            override fun onLoadCleared(placeholder: Drawable?) {
//                                // this is called when imageView is cleared on lifecycle call or for
//                                // some other reason.
//                                // if you are referencing the bitmap somewhere else too other than this imageView
//                                // clear it here as you can no longer have the bitmap
//                            }
//                        })
//                }
//            }


            getNearestPlaceToUser()


        }

        binding.fabAddPlace.setOnClickListener {
            addNewPlaceMarker()
        }

        binding.btnOpenNavView.setOnClickListener {
            binding.drawerLayoutMap.openDrawer(Gravity.LEFT)
        }
    }

    private fun getNearestPlaceToUser()
    {

        Firebase.firestore.collection("places").get().addOnCompleteListener { task->
            if(task.isSuccessful)
            {
                var distance = Float.MAX_VALUE
                var nearestPlace:Place? = null

                for(doc in task.result)
                {
                    val place = doc.toObject(Place::class.java)
                    val placeLocation = Location("placeLocation")
                    placeLocation.latitude = place.geoPoint.latitude
                    placeLocation.longitude = place.geoPoint.longitude

                    if(lastKnownLocation!!.distanceTo(placeLocation) < distance)
                    {
                        distance = lastKnownLocation!!.distanceTo(placeLocation)
                        nearestPlace = place
                    }
                }

                if(nearestPlace != null)
                    downloadImageAndModel(nearestPlace)
            }
        }
    }

    private fun downloadImageAndModel(nearestPlace: Place)
    {

        Glide.with(this)
            .asBitmap()
            .load(nearestPlace.image_for_scanning)
            .into(object : CustomTarget<Bitmap>()
            {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?)
                {

                    compressBitmapToStream(nearestPlace.id, resource)
                    val modelUri = downloadModel(nearestPlace.model_for_ar)
                    goToARActivity(nearestPlace.id, modelUri)
                }

                override fun onLoadCleared(placeholder: Drawable?)
                {}
            })
    }

    private fun goToARActivity(imageFilename: String, modelUri: Uri)
    {
        val arIntent = Intent(this, ARCameraActivity::class.java)
        arIntent.putExtra("IMAGE_FOR_SCANNING", imageFilename)
        arIntent.putExtra("MODEL_FOR_AR", modelUri.toString())
        startActivity(arIntent)
    }

    private fun compressBitmapToStream(filename: String, finalBitmap: Bitmap)
    {
        val stream = openFileOutput(filename, Context.MODE_PRIVATE)
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    }

    fun downloadModel(url: String?):Uri
    {

        val file = File(getExternalFilesDir(null),"model.glb")
        val request:DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Model")
            .setDescription("ARTravelGuide")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID = downloadManager.enqueue(request)

        return Uri.fromFile(file)
    }




    private fun setupNavView()
    {
        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayoutMap,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayoutMap.addDrawerListener(actionBarDrawerToggle!!)
        actionBarDrawerToggle!!.syncState()
        binding.navViewGuide.setNavigationItemSelectedListener{ menuItem ->
            userMenuSelector(menuItem)
            false
        }
    }

    private fun setupNavViewHeader()
    {
        Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                Glide.with(this).load(user!!.profile_image)
                    .into(binding.navViewGuide.findViewById(R.id.navViewImage))
                binding.navViewGuide.findViewById<TextView>(R.id.navViewName).text =
                    user!!.organization_name
                binding.navViewGuide.findViewById<TextView>(R.id.navViewPhone).text = user!!.phone

            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return if(actionBarDrawerToggle != null)
        {
            if (actionBarDrawerToggle!!.onOptionsItemSelected(item))
            {
                true
            }
            else super.onOptionsItemSelected(item)
        }
        else super.onOptionsItemSelected(item)
    }

    fun userMenuSelector(item: MenuItem)
    {
        when (item.itemId)
        {
            R.id.nav_places ->
            {
                val placesIntent = Intent(this, GuidePlacesActivity::class.java)
                startActivity(placesIntent)
            }
            R.id.nav_signout ->
            {
                auth.signOut()
                finish()
            }
        }
    }



    private fun addNewPlaceMarker()
    {
        val addPlaceDialogFragment = AddPlaceFragment(auth.currentUser!!.uid)
        addPlaceDialogFragment.show(supportFragmentManager, "AddPlaceFragment")
    }


    override fun onMapReady(googleMap: GoogleMap)
    {
        mMap = googleMap
        updateLocationUI()
        getDeviceLocation()
        getPlacesListFromDB()

        mMap.setOnInfoWindowClickListener { marker ->
           if(marker.snippet.startsWith("Click here to see more"))
           {
               val index = marker.snippet.indexOf("ID:") + 3
               val placeID = marker.snippet.substring(index)
               showPlaceDialog(placeID)
           }
        }

    }

    private fun showPlaceDialog(placeID: String)
    {
        val placeDialogFragment = MarkerFragment(placeID)
        placeDialogFragment.show(supportFragmentManager, "MarkerFragment")
    }


    private fun getLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        )
        {
            locationPermissionGranted = true
        }
        else
        {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray)
    {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false

        when (requestCode)
        {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION ->
            {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    locationPermissionGranted = true

            }
        }
        updateLocationUI()
    }

    private fun updateLocationUI()
    {
        try
        {
            if (locationPermissionGranted)
            {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            }
            else
            {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation()
    {
        try
        {
            if (locationPermissionGranted)
            {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful)
                    {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null)
                        {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), 50f
                                )
                            )
                            val myLocation = LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            )
                            mMap.addMarker(
                                MarkerOptions().position(myLocation).title("My location")
                            )
                        }
                    }
                    else
                    {
                        val nisLocation = LatLng(43.304, 21.9)
                        mMap.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(nisLocation, 50f)
                        )
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onLocationChanged(p0: Location)
    {
        getDeviceLocation()     //PROVERI DA LI RADI LEPO
        updateMapMarkers()
    }

    private fun getPlacesListFromDB()
    {
        Firebase.firestore.collection("places").addSnapshotListener { value, e ->
            if (e != null)
                return@addSnapshotListener

            placesList = ArrayList<Place>()
            for (doc in value!!)
            {
                val place = doc.toObject(Place::class.java)
                placesList.add(place)
            }
            updateMapMarkers()
        }
    }

    private fun addAllPlaceMarkersToMap()
    {
        for(place in placesList)
        {
            val placeLatLng = LatLng(place.geoPoint.latitude,place.geoPoint.longitude)
            val placeMarker = MarkerOptions().position(placeLatLng).title(place.name).snippet("Click here to see more \n ID:${place.id}")
            mMap.addMarker(placeMarker)
        }
    }

    private fun updateMapMarkers()
    {
        mMap.clear()
        getDeviceLocation()
        addAllPlaceMarkersToMap()
    }



}