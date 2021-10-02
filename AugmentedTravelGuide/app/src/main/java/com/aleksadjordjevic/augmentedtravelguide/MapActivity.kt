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
import android.util.Log
import android.view.*
import android.widget.ImageView
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
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityMapBinding
import com.aleksadjordjevic.augmentedtravelguide.fragments.AddPlaceFragment
import com.aleksadjordjevic.augmentedtravelguide.fragments.MarkerFilterFragment
import com.aleksadjordjevic.augmentedtravelguide.fragments.MarkerFragment
import com.aleksadjordjevic.augmentedtravelguide.models.Place
import com.aleksadjordjevic.augmentedtravelguide.models.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import com.google.android.gms.maps.model.Marker

import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions


private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100

class MapActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener, MarkerFilterFragment.OnFilterChangeListener
{

    private lateinit var binding: ActivityMapBinding
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null

    private lateinit var auth:FirebaseAuth
    private var navViewHeaderListener:ListenerRegistration? = null

    private lateinit var mMap: GoogleMap
    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    private var placesList = ArrayList<Place>()
    private var markersList= ArrayList<MarkerOptions>()
    private var downloadID:Long = 0L

    private var showHistoric = true
    private var showEducation = true
    private var showCatering = true
    private var showEntertainment = true
    private var showSports = true
    private var filterDistance:Int? = null


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        setupOnClickListeners()
        setupNavView()

        auth = FirebaseAuth.getInstance()
        if(auth.currentUser == null)
            hideNavView()
        else
            setupNavViewHeader()


        registerReceiverForModelDownloads()

    }

    override fun onDestroy()
    {
        super.onDestroy()
        navViewHeaderListener?.remove()
    }

    @SuppressLint("RtlHardcoded")
    private fun setupOnClickListeners()
    {
        binding.fabARView.setOnClickListener {
            deleteOldModels()
            getNearestPlaceToUser()
        }

        binding.fabFilters.setOnClickListener {
            openFilterDialog()
        }

        binding.btnOpenNavView.setOnClickListener {
            binding.drawerLayoutMap.openDrawer(Gravity.LEFT)
        }
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
       Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get().addOnCompleteListener {task ->

           if(task.isSuccessful)
           {
               val user = task.result!!.toObject(User::class.java)
               val navViewImage = binding.navViewGuide.findViewById<ImageView>(R.id.navViewImage)
               if (user!!.profile_image != "")
                   Glide.with(this).load(user!!.profile_image)
                       .into(navViewImage)
               else
                   Glide.with(this).load(R.drawable.user)
                       .into(navViewImage)
               binding.navViewGuide.findViewById<TextView>(R.id.navViewName).text =
                   user.organization_name
               binding.navViewGuide.findViewById<TextView>(R.id.navViewPhone).text = user.phone


               navViewHeaderListener = Firebase.firestore.collection("users").document(auth.currentUser!!.uid).addSnapshotListener { value, e ->
                   if (e != null)
                       return@addSnapshotListener

                   val user = value!!.toObject(User::class.java)
                   val navViewImage = binding.navViewGuide.findViewById<ImageView>(R.id.navViewImage)
                   if(user!!.profile_image != "")
                       Glide.with(this).load(user!!.profile_image)
                           .into(navViewImage)
                   else
                       Glide.with(this).load(R.drawable.user)
                           .into(navViewImage)
                   binding.navViewGuide.findViewById<TextView>(R.id.navViewName).text =
                       user.organization_name
                   binding.navViewGuide.findViewById<TextView>(R.id.navViewPhone).text = user.phone
               }
           }
        }

//        navViewHeaderListener = Firebase.firestore.collection("users").document(auth.currentUser!!.uid).addSnapshotListener { value, e ->
//            if (e != null)
//                return@addSnapshotListener
//
//            val user = value!!.toObject(User::class.java)
//            //val navViewImage = binding.navViewGuide.findViewById<ImageView>(R.id.navViewImage)
//            if(user!!.profile_image != "")
//                Glide.with(this).load(user!!.profile_image)
//                    .into(navViewImage)
//            else
//                Glide.with(this).load(R.drawable.user)
//                    .into(navViewImage)
//            binding.navViewGuide.findViewById<TextView>(R.id.navViewName).text =
//                user.organization_name
//            binding.navViewGuide.findViewById<TextView>(R.id.navViewPhone).text = user.phone
//        }
    }

    private fun hideNavView()
    {
        binding.btnOpenNavView.visibility = View.INVISIBLE
        binding.drawerLayoutMap.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
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
            R.id.nav_profile ->
            {
                val profileIntent = Intent(this, GuideProfileActivity::class.java)
                startActivity(profileIntent)
            }
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
            R.id.nav_add_places ->
            {
                binding.drawerLayoutMap.closeDrawer(Gravity.LEFT)
                addNewPlaceMarker()
            }
        }
    }

    private fun registerReceiverForModelDownloads()
    {
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

    override fun onMapReady(googleMap: GoogleMap)
    {
        mMap = googleMap
        getLocationPermission()
        showMyLocationOnMap()
        getDeviceLocation()
        getPlacesListFromDB()

        mMap.setOnInfoWindowClickListener { marker ->
            if(marker.snippet != null)
            {
                if (marker.snippet!!.startsWith("Click here to see more"))
                {
                    val index = marker.snippet!!.indexOf("ID:") + 3
                    val placeID = marker.snippet!!.substring(index)
                    showPlaceDialog(placeID)
                }
            }
        }

        mMap.setOnMarkerDragListener(object : OnMarkerDragListener
        {
            override fun onMarkerDragStart(marker: Marker)
            {}

            override fun onMarkerDragEnd(marker: Marker)
            {
                if(marker.snippet != null)
                {
                    if (marker.snippet!!.startsWith("Click here to see more"))
                    {
                        val index = marker.snippet!!.indexOf("ID:") + 3
                        val placeID = marker.snippet!!.substring(index)
                        val newGeoPoint =
                            GeoPoint(marker.position.latitude, marker.position.longitude)
                        val placeGeoPoint = hashMapOf("geoPoint" to newGeoPoint)
                        Firebase.firestore.collection("places").document(placeID)
                            .set(placeGeoPoint, SetOptions.merge())
                    }
                }
            }

            override fun onMarkerDrag(marker: Marker)
            {}
        })


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
        showMyLocationOnMap()
    }

    private fun showMyLocationOnMap()
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


    private fun animateCameraToLocation(location: Location)
    {
        val bottomBoundary = location.latitude - .1
        val leftBoundary = location.longitude - .1
        val topBoundary = location.latitude + .1
        val rightBoundary = location.longitude + .1
        val mMapBoundary = LatLngBounds(LatLng(bottomBoundary, leftBoundary), LatLng(topBoundary, rightBoundary))
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, 0))
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
                            animateCameraToLocation(lastKnownLocation!!)

                    }
                    else
                    {
                        val nisLocation = Location("Nis")
                        nisLocation.latitude = 43.304
                        nisLocation.longitude = 21.9
                        animateCameraToLocation(nisLocation)
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
        getDeviceLocation()
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
        markersList.clear()

        for(place in placesList)
        {
            val placeMarker = createOthersMarker(place)
            checkIfMyMarker(place, placeMarker)
            checkIfIsFilteredMarker(place,placeMarker)
        }

        for(marker in markersList)
            mMap.addMarker(marker)
    }


    private fun createOthersMarker(place: Place):MarkerOptions
    {
        val placeLatLng = LatLng(place.geoPoint.latitude,place.geoPoint.longitude)
        val markerIconRes= when(place.type)
        {
            "Historic Monument" -> R.drawable.ar_marker_historic
            "Education" -> R.drawable.ar_marker_education
            "Catering" -> R.drawable.ar_marker_catering
            "Entertainment" -> R.drawable.ar_marker_entertainment
            "Sports" -> R.drawable.ar_marker_sports
            else -> R.drawable.ar_marker
        }

        return MarkerOptions().position(placeLatLng).title(place.name).snippet("Click here to see more \n ID:${place.id}").icon(
            BitmapDescriptorFactory.fromResource(markerIconRes))
    }

    private fun checkIfMyMarker(place: Place, placeMarker: MarkerOptions)
    {
        val markerIconRes= when(place.type)
        {
            "Historic Monument" -> R.drawable.ar_marker_historic_2
            "Education" -> R.drawable.ar_marker_education_2
            "Catering" -> R.drawable.ar_marker_catering_2
            "Entertainment" -> R.drawable.ar_marker_entertainment_2
            "Sports" -> R.drawable.ar_marker_sports_2
            else -> R.drawable.ar_marker3
        }


        if(auth.currentUser != null)
            if(place.guideID == auth.currentUser!!.uid)
                placeMarker.draggable(true).icon(
                    BitmapDescriptorFactory.fromResource(markerIconRes))
    }

    private fun checkIfIsFilteredMarker(place: Place, placeMarker: MarkerOptions)
    {
        val showMarker = when(place.type)
        {
            "Historic Monument" -> showHistoric
            "Education" -> showEducation
            "Catering" -> showCatering
            "Entertainment" -> showEntertainment
            "Sports" -> showSports
            else -> false
        }

        if(showMarker && isWithinDistance(place))
            markersList.add(placeMarker)
    }

    private fun isWithinDistance(place: Place): Boolean
    {
        return if(filterDistance != null)
        {
            val placeLocation = Location("placeLocation")
            placeLocation.latitude = place.geoPoint.latitude
            placeLocation.longitude = place.geoPoint.longitude

            val distance = lastKnownLocation!!.distanceTo(placeLocation)

            distance <= filterDistance!!
        }
        else
            true

    }

    private fun openFilterDialog()
    {
        val filterFragment = MarkerFilterFragment(showHistoric,showEducation,showCatering,showEntertainment,showSports,filterDistance,this)
        filterFragment.show(supportFragmentManager, "MarkerFilterFragment")
    }

    override fun onApplyFilter(showHistoric: Boolean,
                               showEducation: Boolean,
                               showCatering: Boolean,
                               showEntertainment: Boolean,
                               showSports: Boolean,
                               distance: Int?)
    {
        this.showHistoric = showHistoric
        this.showEducation = showEducation
        this.showCatering = showCatering
        this.showEntertainment = showEntertainment
        this.showSports = showSports
        filterDistance = distance

        updateMapMarkers()
    }

    private fun updateMapMarkers()
    {
        mMap.clear()
        getDeviceLocation()
        addAllPlaceMarkersToMap()
    }

    private fun addNewPlaceMarker()
    {
        val addPlaceDialogFragment = AddPlaceFragment(auth.currentUser!!.uid)
        addPlaceDialogFragment.show(supportFragmentManager, "AddPlaceFragment")
    }

    private fun showPlaceDialog(placeID: String)
    {
        val placeDialogFragment = MarkerFragment(placeID)
        placeDialogFragment.show(supportFragmentManager, "MarkerFragment")
    }

    private fun deleteOldModels()
    {
        try
        {
            val file = File(getExternalFilesDir(null)!!.path,"model.glb")
            if(file.exists())
            {
                file.delete()
            }
        } catch (e: java.lang.Exception)
        {
            e.printStackTrace()
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




}