package com.aleksadjordjevic.augmentedtravelguide

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.aleksadjordjevic.augmentedtravelguide.databinding.ActivityArcameraBinding
import com.google.android.filament.ColorGrading
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.ArFragment.OnViewCreatedListener
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import java.util.ArrayList
import java.util.concurrent.CompletableFuture


class ARCameraActivity() : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener, OnViewCreatedListener
{
    private lateinit var binding:ActivityArcameraBinding
    private var arFragment: ArFragment? = null
    private var database: AugmentedImageDatabase? = null
    private val futures: MutableList<CompletableFuture<*>> = ArrayList()
    private lateinit var imageForScanning:Bitmap
    private lateinit var modelURL:String
    private var imageDetected = false


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityArcameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadImageForScanning()
        getModelURL()

        Handler(Looper.getMainLooper()).postDelayed({

            supportFragmentManager.addFragmentOnAttachListener(this)
            if (savedInstanceState == null)
            {
                if (Sceneform.isSupported(this))
                {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.arFragment, ArFragment::class.java, null)
                        .commit()
                }
            }
        }, 1)
    }

    private fun loadImageForScanning()
    {
        val imageForScanningFilename = intent.getStringExtra("IMAGE_FOR_SCANNING")
        try
        {
            val inputStream = this.openFileInput(imageForScanningFilename)
            imageForScanning = BitmapFactory.decodeStream(inputStream)
            binding.imgForScaningAR.setImageBitmap(imageForScanning)
            inputStream.close()

        } catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun getModelURL()
    {
        modelURL = intent.getStringExtra("MODEL_FOR_AR")!!
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment)
    {
        if (fragment.id == R.id.arFragment)
        {
            arFragment = fragment as ArFragment
            arFragment!!.setOnSessionConfigurationListener(this)
            arFragment!!.setOnViewCreatedListener(this)
        }
    }

    override fun onSessionConfiguration(session: Session, config: Config)
    {
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) config.depthMode =
            Config.DepthMode.AUTOMATIC

        database = AugmentedImageDatabase(session)
        database!!.addImage("imageForScanning", imageForScanning)
        config.augmentedImageDatabase = database

        arFragment!!.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(
                augmentedImage
            )
        }
    }

    override fun onViewCreated(arFragment: ArFragment, arSceneView: ArSceneView)
    {
        val renderer = arSceneView.renderer
        if (renderer != null)
        {
            renderer.filamentView.colorGrading = ColorGrading.Builder()
                .toneMapping(ColorGrading.ToneMapping.FILMIC)
                .build(EngineInstance.getEngine().filamentEngine)
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        for (future: CompletableFuture<*> in futures)
        {
            if (!future.isDone)
            {
                future.cancel(true)
            }
        }
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage)
    {
        if ((augmentedImage.trackingState == TrackingState.TRACKING && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING))
        {

            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

            if (!imageDetected && (augmentedImage.name == "imageForScanning"))
            {
                imageDetected = true
                Toast.makeText(this, "Image detected", Toast.LENGTH_LONG).show()
                anchorNode.worldScale = Vector3(1.5f, 1.5f, 1.5f)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                futures.add(
                    ModelRenderable.builder()
                        .setSource(this, Uri.parse(modelURL))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept { rabbitModel: ModelRenderable? ->
                            val modelNode = TransformableNode(arFragment!!.transformationSystem)
                            modelNode.renderable = rabbitModel
                            anchorNode.addChild(modelNode)
                        }
                        .exceptionally { throwable: Throwable? ->
                            Toast.makeText(
                                this,
                                "Unable to load model",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            null
                        }
                )
            }
        }

        if (imageDetected)
            arFragment!!.instructionsController.setEnabled(InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false)

    }
}