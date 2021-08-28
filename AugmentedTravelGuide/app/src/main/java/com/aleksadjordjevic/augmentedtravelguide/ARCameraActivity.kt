package com.aleksadjordjevic.augmentedtravelguide

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.android.filament.ColorGrading
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.filamat.MaterialPackage
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.ArFragment.OnViewCreatedListener
import com.google.ar.sceneform.ux.BaseArFragment.OnAugmentedImageUpdateListener
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import java.util.ArrayList
import java.util.concurrent.CompletableFuture


class ARCameraActivity() : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener, OnViewCreatedListener
{
    private var arFragment: ArFragment? = null
    private var matrixDetected = false
    private var rabbitDetected = false
    private var database: AugmentedImageDatabase? = null
    private var plainVideoModel: Renderable? = null
    private var plainVideoMaterial: Material? = null
    private var mediaPlayer: MediaPlayer? = null
    private val futures: MutableList<CompletableFuture<*>> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arcamera)
        //        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
//            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets.getSystemWindowInsetTop();
//            return insets.consumeSystemWindowInsets();
//        });
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

            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            loadMatrixModel()
            loadMatrixMaterial()

        }, 1)


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
        // Disable plane detection
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) config.depthMode =
            Config.DepthMode.AUTOMATIC

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        database = AugmentedImageDatabase(session)
        val matrixImage = BitmapFactory.decodeResource(resources, R.drawable.matrix)
        val rabbitImage = BitmapFactory.decodeResource(resources, R.drawable.rabbit)
        // Every image has to have its own unique String identifier
        database!!.addImage("matrix", matrixImage)
        database!!.addImage("rabbit", rabbitImage)
        config.augmentedImageDatabase = database

        // Check for image detection
        arFragment!!.setOnAugmentedImageUpdateListener({ augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(
                augmentedImage
            )
        })
    }

    override fun onViewCreated(arFragment: ArFragment, arSceneView: ArSceneView)
    {
        // Currently, the tone-mapping should be changed to FILMIC
        // because with other tone-mapping operators except LINEAR
        // the inverseTonemapSRGB function in the materials can produce incorrect results.
        // The LINEAR tone-mapping cannot be used together with the inverseTonemapSRGB function.
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
        if (mediaPlayer != null)
        {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
        }
    }

    private fun loadMatrixModel()
    {
        futures.add(
            ModelRenderable.builder()
                .setSource(this, Uri.parse("models/Video.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept({ model: ModelRenderable ->
                    //removing shadows for this Renderable
                    model.setShadowCaster(false)
                    model.setShadowReceiver(true)
                    plainVideoModel = model
                })
                .exceptionally(
                    { throwable: Throwable? ->
                        Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show()
                        null
                    })
        )
    }

    private fun loadMatrixMaterial()
    {
        val filamentEngine = EngineInstance.getEngine().filamentEngine
        MaterialBuilder.init()
        val materialBuilder = MaterialBuilder()
            .platform(MaterialBuilder.Platform.MOBILE)
            .name("External Video Material")
            .require(MaterialBuilder.VertexAttribute.UV0)
            .shading(MaterialBuilder.Shading.UNLIT)
            .doubleSided(true)
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_EXTERNAL,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.SamplerPrecision.DEFAULT,
                "videoTexture"
            )
            .optimization(MaterialBuilder.Optimization.NONE)
        val plainVideoMaterialPackage = materialBuilder
            .blending(MaterialBuilder.BlendingMode.OPAQUE)
            .material(
                "void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n"
            )
            .build(filamentEngine)
        if (plainVideoMaterialPackage.isValid)
        {
            val buffer = plainVideoMaterialPackage.buffer
            futures.add(
                Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept({ material: Material? ->
                        plainVideoMaterial = material
                    })
                    .exceptionally(
                        { throwable: Throwable? ->
                            Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG)
                                .show()
                            null
                        })
            )
        }
        MaterialBuilder.shutdown()
    }

    fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage)
    {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (matrixDetected && rabbitDetected) return
        if ((augmentedImage.trackingState == TrackingState.TRACKING
                    && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING)
        )
        {

            // Setting anchor to the center of Augmented Image
            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

            // If matrix video haven't been placed yet and detected image has String identifier of "matrix"
            if (!matrixDetected && (augmentedImage.name == "matrix"))
            {
                matrixDetected = true
                Toast.makeText(this, "Matrix tag detected", Toast.LENGTH_LONG).show()

                // AnchorNode placed to the detected tag and set it to the real size of the tag
                // This will cause deformation if your AR tag has different aspect ratio than your video
                anchorNode.worldScale = Vector3(augmentedImage.extentX, 1f, augmentedImage.extentZ)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                val videoNode = TransformableNode(arFragment!!.transformationSystem)
                // For some reason it is shown upside down so this will rotate it correctly
                videoNode.localRotation =
                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f)
                anchorNode.addChild(videoNode)

                // Setting texture
                val externalTexture = ExternalTexture()
                val renderableInstance = videoNode.setRenderable(plainVideoModel)
                renderableInstance.material = plainVideoMaterial

                // Setting MediaPLayer
                renderableInstance.material.setExternalTexture("videoTexture", externalTexture)
                mediaPlayer = MediaPlayer.create(this, R.raw.matrix)
                mediaPlayer!!.setLooping(true)
                mediaPlayer!!.setSurface(externalTexture.surface)
                mediaPlayer!!.start()
            }
            // If rabbit model haven't been placed yet and detected image has String identifier of "rabbit"
            // This is also example of model loading and placing at runtime
            if (!rabbitDetected && (augmentedImage.name == "rabbit"))
            {
                rabbitDetected = true
                Toast.makeText(this, "Rabbit tag detected", Toast.LENGTH_LONG).show()
                anchorNode.worldScale = Vector3(3.5f, 3.5f, 3.5f)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                futures.add(
                    ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/Rabbit.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept({ rabbitModel: ModelRenderable? ->
                            val modelNode: TransformableNode = TransformableNode(
                                arFragment!!.getTransformationSystem()
                            )
                            modelNode.setRenderable(rabbitModel)
                            anchorNode.addChild(modelNode)
                        })
                        .exceptionally(
                            { throwable: Throwable? ->
                                Toast.makeText(
                                    this,
                                    "Unable to load rabbit model",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                null
                            })
                )
            }
        }
        if (matrixDetected && rabbitDetected)
        {
            arFragment!!.instructionsController.setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
            )
        }
    }
}