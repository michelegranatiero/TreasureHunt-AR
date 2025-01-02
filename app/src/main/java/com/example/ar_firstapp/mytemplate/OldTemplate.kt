package com.example.ar_firstapp.mytemplate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ar_firstapp.R
import com.example.ar_firstapp.ui.theme.AR_FirstAppTheme
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Future
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberView
import java.util.EnumSet

private const val kModelFile = "models/burger.glb"
private const val kMaxModelInstances = 10



private lateinit var _session: Session
private var future: Future? = null;
private lateinit var _childNodes: SnapshotStateList<Node>;
private var text = ""

private lateinit var engine: Engine
private lateinit var modelLoader: ModelLoader
private lateinit var materialLoader: MaterialLoader
private lateinit var modelInstances: MutableList<ModelInstance>

class OldTemplate : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AR_FirstAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ){
                        val currentModel = remember {
                            mutableStateOf("burger")
                        }
                        ARScreen(currentModel)
                        Menu(modifier = Modifier.align(Alignment.BottomCenter)) {
                            currentModel.value = it
                        }
                        /*Menu(modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = { modelName ->
                                currentModel.value = modelName
                            }
                        )*/
                        Row (Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,

                        ){
                            var text1 by remember { mutableStateOf("") }
                            TextField(
                                modifier = Modifier.width(120.dp),
                                value = text1,
                                onValueChange = {
                                    text1 = it
                                    text = it
                                },
                                label = { Text("Anchor ID") },
                                //modifier = Modifier.align(Alignment.BottomCenter)
                            )
                            Button(
                                onClick = { /*onResolveButtonPressed()*/ },
                            ) {
                                Text("Resolve")
                            }
                            Button(
                                onClick = { /*onClearButtonPressed()*/ },
                            ) {
                                Text("Clear Anchor")
                            }
                        }
                    }



                }
            }
        }
    }
}

@Composable
fun Menu(modifier: Modifier,onClick:(String)->Unit) {
    var currentIndex by remember {
        mutableStateOf(0)
    }

    val itemsList = listOf(
        Food("burger", R.drawable.burger),
        Food("instant", R.drawable.instant),
        Food("momos", R.drawable.momos),
        Food("pizza", R.drawable.pizza),
        Food("ramen", R.drawable.ramen),

        )
    fun updateIndex(offset:Int){
        currentIndex = (currentIndex+offset + itemsList.size) % itemsList.size
        onClick(itemsList[currentIndex].name)
    }
    Row(modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = {
            updateIndex(-1)
        }) {
            Icon(painter = painterResource(id = R.drawable.baseline_arrow_back_ios_24), contentDescription ="previous" )
        }

        CircularImage(imageId = itemsList[currentIndex].imageId)

        IconButton(onClick = {
            updateIndex(1)
        }) {
            Icon(painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24), contentDescription ="next")
        }
    }

}

@Composable
fun CircularImage(
    modifier: Modifier=Modifier,
    imageId: Int
) {
    Box(modifier = modifier
        .size(140.dp)
        .clip(CircleShape)
        .border(width = 3.dp, color = Color.Transparent, shape = CircleShape)
        // .border(width = 3.dp, Translucent, CircleShape)
    ){
        Image(painter = painterResource(id = imageId), contentDescription = null, modifier = Modifier.size(140.dp), contentScale = ContentScale.FillBounds)
    }
}

data class Food(var name:String, var imageId:Int)


@Composable
fun ARScreen(modelState: State<String>) {
    // An Engine instance main function is to keep track of all resources created by the user and manage
    // the rendering thread as well as the hardware renderer.
    // To use filament, an Engine instance must be created first.

    //val engine = rememberEngine()
    engine = rememberEngine()

    // Encompasses all the state needed for rendering a [Scene].
    // [View] instances are heavy objects that internally cache a lot of data needed for
    // rendering. It is not advised for an application to use many View objects.
    // For example, in a game, a [View] could be used for the main scene and another one for the
    // game's user interface. More [View] instances could be used for creating special
    // effects (e.g. a [View] is akin to a rendering pass).
    val view = rememberView(engine)
    // A [Renderer] instance represents an operating system's window.
    // Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
    // commands for the render thread and manages frame latency.
    val renderer = rememberRenderer(engine)
    // Provide your own instance if you want to share [Node]s' scene between multiple views.
        /*val scene = rememberScene(engine)*/
    // Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
    // a bundle of Filament textures, vertex buffers, index buffers, etc.
    // A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.

    //val modelLoader = rememberModelLoader(engine)
    modelLoader = rememberModelLoader(engine)

    // A Filament Material defines the visual appearance of an object.
    // Materials function as a templates from which [MaterialInstance]s can be spawned.

    //val materialLoader = rememberMaterialLoader(engine)
    materialLoader = rememberMaterialLoader(engine)

    val cameraNode = rememberARCameraNode(engine)
    var planeRenderer by remember { mutableStateOf(true) }
    val childNodes = rememberNodes()

    // Utility for decoding an HDR file or consuming KTX1 files and producing Filament textures,
    // IBLs, and sky boxes.
    // KTX is a simple container format that makes it easy to bundle miplevels and cubemap faces
    // into a single file.
        /*val environmentLoader = rememberEnvironmentLoader(engine)*/
    // Physics system to handle collision between nodes, hit testing on a nodes,...
    val collisionSystem = rememberCollisionSystem(view)

    val modelInstances = remember { mutableListOf<ModelInstance>() }
    var frame by remember { mutableStateOf<Frame?>(null) }

    var trackingFailureReason by remember {
        mutableStateOf<TrackingFailureReason?>(null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            // The modifier to be applied to the layout.
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            cameraNode = cameraNode,
            engine = engine,
            view = view,
            renderer = renderer,
            /*scene = scene,*/
            modelLoader = modelLoader,
            /*materialLoader = materialLoader,*/
            /*environmentLoader = environmentLoader,*/
            collisionSystem = collisionSystem,
            // Controls whether the render target (SurfaceView) is opaque or not.
            /*isOpaque = true,*/
            // Always add a direct light source since it is required for shadowing.
            // We highly recommend adding an [IndirectLight] as well.
            /*mainLightNode = rememberMainLightNode(engine) {
                intensity = 100_000.0f
            },*/
            // Load the environement lighting and skybox from an .hdr asset file
            /*environment = rememberEnvironment(environmentLoader) {
                environmentLoader.createHDREnvironment(
                    assetFileLocation = "environments/sky_2k.hdr"
                )!!
            },*/



            // Represents a virtual camera, which determines the perspective through which the scene is
            // viewed.
            // All other functionality in Node is supported. You can access the position and rotation of the
            // camera, assign a collision shape to it, or add children to it.
            /*cameraNode = rememberARCameraNode(engine){
                // Position the camera 4 units away from the object
                position = Position(z = 4.0f)
            },*/
            // Helper that enables camera interaction similar to sketchfab or Google Maps.
            // Needs to be a callable function because it can be reinitialized in case of viewport change
            // or camera node manual position changed.
            // The first onTouch event will make the first manipulator build. So you can change the camera
            // position before any user gesture.
            // Clients notify the camera manipulator of various mouse or touch events, then periodically
            // call its getLookAt() method so that they can adjust their camera(s). Three modes are
            // supported: ORBIT, MAP, and FREE_FLIGHT. To construct a manipulator instance, the desired mode
            // is passed into the create method.
            /*cameraManipulator = rememberCameraManipulator(),*/
            // Scene nodes
            /*childNodes = rememberNodes {
                // Add a glTF model
                add(
                    ModelNode(
                        // Load it from a binary .glb in the asset files
                        modelInstance = modelLoader.createModelInstance(
                            assetFileLocation = "models/damaged_helmet.glb"
                        ),
                        scaleToUnits = 1.0f
                    )
                )
                // Add a Cylinder geometry
                add(
                    CylinderNode(
                    engine = engine,
                    radius = 0.2f,
                    height = 2.0f,
                    // Choose the basic material appearance
                    materialInstance = materialLoader.createColorInstance(
                        color = Color.Blue,
                        metallic = 0.5f,
                        roughness = 0.2f,
                        reflectance = 0.4f
                    )
                ).apply {
                    // Position it on top of the model and rotate it
                    transform(
                        position = Position(y = 1.0f),
                        rotation = Rotation(x = 90.0f)
                    )
                })
                // ...See all available nodes in the nodes packagage
            },*/
            // The listener invoked for all the gesture detector callbacks.
            // Detects various gestures and events.
            // The gesture listener callback will notify users when a particular motion event has occurred.
            // Responds to Android touch events with listeners.
            onGestureListener = rememberOnGestureListener(
                /*onDoubleTapEvent = { event, tapedNode ->
                    // Scale up the tap node (if any) on double tap
                    tapedNode?.let { it.scale *= 2.0f }
                }*/
                onSingleTapConfirmed = { motionEvent, node ->
                    //only 1 node can be tapped at a time (future must be null)
                    if (node == null && future == null) { // check that the tap was not on an existent node
                        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                        hitResults?.firstOrNull {
                            it.isValid(
                                //exclude depth point and points (prefer planes)
                                depthPoint = false,
                                point = false
                            )
                        }?.createAnchorOrNull() // create anchor
                            ?.let { anchor ->

                                planeRenderer = false
                                childNodes += createAnchorNode( // create node on anchor, add it to childNodes
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    modelInstances = modelInstances,
                                    model = modelState.value,
                                    anchor = anchor
                                )

                                /*future = _session.hostCloudAnchorAsync(anchor, 30) { string, state ->
                                    if (onHostComplete(
                                            string,
                                            state
                                        )
                                    ){
                                        planeRenderer = false
                                        childNodes += createAnchorNode( // create node on anchor, add it to childNodes
                                            engine = engine,
                                            modelLoader = modelLoader,
                                            materialLoader = materialLoader,
                                            modelInstances = modelInstances,
                                            model = modelState.value,
                                            anchor = anchor
                                        )
                                    }
                                }*/

                            }
                    }
                }
            ),
            // Receive basics on touch event on the view
            /*onTouchEvent = { event: MotionEvent, hitResult: HitResult? ->
                hitResult?.let { println("World tapped : ${it.worldPosition}") }
                // The touch event is not consumed
                false
            },*/
            // Invoked when an frame is processed.
            // Registers a callback to be invoked when a valid Frame is processing.
            // The callback to be invoked once per frame **immediately before the scene is updated.
            // The callback will only be invoked if the Frame is considered as valid.
            /*onFrame = { frameTimeNanos ->
            },*/
            // Fundamental session features that can be requested.
            /*sessionFeatures = setOf(),*/
            // The camera config to use.
            // The config must be one returned by [Session.getSupportedCameraConfigs].
            // Provides details of a camera configuration such as size of the CPU image and GPU texture.
            /*sessionCameraConfig = null,*/
            sessionCameraConfig = { session ->
                //set front camera
                val filter = CameraConfigFilter(session)
                // filter.setFacingDirection(CameraConfig.FacingDirection.FRONT)
                // filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.REQUIRE_AND_USE)) //hardware depth sensor usage (not working)

                val cameraConfigList = session.getSupportedCameraConfigs(filter)

                // Log.d("CameraConfigSize", cameraConfigList.size.toString())
                /* for ((index, cameraConfig) in cameraConfigList.withIndex()) {
                    Log.d("CameraConfig", cameraConfig.getCameraId())
                    Log.d("CameraConfig", cameraConfig.getDepthSensorUsage().toString())
                    Log.d("CameraConfig", cameraConfig.getFacingDirection().toString())
                    Log.d("CameraConfig", cameraConfig.getFpsRange().toString())
                    Log.d("CameraConfig", cameraConfig.getImageSize().toString())
                    Log.d("CameraConfig", cameraConfig.getTextureSize().toString())
                    Log.d("CameraConfig", index.toString())
                } */

                var cameraIdx = 0

                /* for ((index, cameraConfig) in cameraConfigList.withIndex()) {
                    if (cameraConfig.imageSize.width >= 1920 && cameraConfig.textureSize.width >= 1920) {
                        cameraIdx = index
                        break
                    }
                } */

                // cameraConfigList[0]
                cameraConfigList[cameraIdx]

            },
            // Configures the session and verifies that the enabled features in the specified session config
            // are supported with the currently set camera config.
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL

                Log.d("DepthMode", config.depthMode.toString())


                config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED)
                // config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED

                Log.d("CloudAnchorMode", config.cloudAnchorMode.toString())
                _session = session
                _childNodes = childNodes

            },
            planeRenderer = planeRenderer,
            // The [ARCameraStream] to render the camera texture.
            // Use it to control if the occlusion should be enabled or disabled.
            // cameraStream = rememberARCameraStream(materialLoader),
            cameraStream = rememberARCameraStream(materialLoader).apply {
                // Enable occlusion
                isDepthOcclusionEnabled = true
            },
            // The session is ready to be accessed.
            onSessionCreated = { session ->
            },
            // The session has been resumed.
            onSessionResumed = { session ->
            },
            // The session has been paused
            onSessionPaused = { session ->
            },
            // Updates of the state of the ARCore system.
            // This includes: receiving a new camera frame, updating the location of the device, updating
            // the location of tracking anchors, updating detected planes, etc.
            // This call may update the pose of all created anchors and detected planes. The set of updated
            // objects is accessible through [Frame.getUpdatedTrackables].
            // Invoked once per [Frame] immediately before the Scene is updated.
            onSessionUpdated = { session, updatedFrame ->
                frame = updatedFrame

                // To spawn model on startup
                /*if (childNodes.isEmpty()) {
                    updatedFrame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { it.createAnchorOrNull(it.centerPose) }?.let { anchor ->
                            childNodes += createAnchorNode(
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                modelInstances = modelInstances,
                                model = modelState.value,
                                anchor = anchor
                            )
                        }
                }*/
            },
            // Invoked when an ARCore error occurred.
            // Registers a callback to be invoked when the ARCore Session cannot be initialized because
            // ARCore is not available on the device or the camera permission has been denied.
            onSessionFailed = { exception ->
            },
            // Listen for camera tracking failure.
            // The reason that [Camera.getTrackingState] is [TrackingState.PAUSED] or `null` if it is
            // [TrackingState.TRACKING]
            onTrackingFailureChanged = {
                trackingFailureReason = it
            }
        )
        /*if(placeModelButton.value){
            Button(onClick = {
                modelNode.value?.anchor()
            }, modifier = Modifier.align(Alignment.Center)) {
                Text(text = "Place It")
            }
        }*/
    }

    LaunchedEffect(key1 = modelState){
        // modelFile.value?.loadModelGlbAsync(
        //     glbFileLocation = "models/${model}.glb",
        //     scaleToUnits = 0.8f
        // )
        // Log.e("error loading","ERROR LOADING MODEL")

        // model.value = modelState
        Log.d("MyModel", modelState.value)
    }

}


fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    model: String,
    anchor: Anchor
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
        isPositionEditable = true // true by default
        isRotationEditable = true
        // isScaleEditable = true
        // isSmoothTransformEnabled = true
        // isEditable = true

    }
    val modelNode = ModelNode(
        modelLoader.createInstance(
            modelLoader.createModel(
                assetFileLocation = "models/${model}.glb"
            )
        )!!, // !! asserts that value is not null (even if it is a nullable type)
        scaleToUnits = 0.25f
    ).apply {
        // isEditable = true
        // isScaleEditable = true
        // isPositionEditable = true
        // isRotationEditable = true
        // isSmoothTransformEnabled = true
    }

    /*val modelNode = ModelNode(
        modelInstance = modelInstances.apply {
            if (isEmpty()) {
                // this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
                this += modelLoader.createInstancedModel("models/${model}.glb", kMaxModelInstances)
            }
            Log.d("ModelInstances", modelInstances.size.toString())


        }.removeAt(modelInstances.lastIndex), //.removeLast() or .last() ? -----------------------

        // Scale to fit in a 0.5 meters cube
        scaleToUnits = 0.5f
    ).apply {
        // Model Node needs to be editable for independent rotation from the anchor rotation
        isEditable = true
    }*/

    val boundingBoxNode = CubeNode(
        engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
    ).apply {
        isVisible = false
    }
    modelNode.addChildNode(boundingBoxNode)
    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach { // list just to not repeat the same code
        it.onEditingChanged = { editingTransforms ->
            boundingBoxNode.isVisible = editingTransforms.isNotEmpty() //check if the model is being edited
        }
        // it.onScale = { detector, e, scaleFactor -> /* */ }
    }
    return anchorNode
}


fun onHostComplete(cloudAnchorId: String, cloudState: CloudAnchorState): Boolean {
    if (cloudState == CloudAnchorState.SUCCESS) {
        Log.d("Cloud1", cloudAnchorId)
        return true
    } else {
        Log.d("Cloud2", cloudState.toString());
        return false
    }
}

fun onClearButtonPressed() {
    if (_childNodes.isNotEmpty()) {
        _childNodes.forEach {
            (it as AnchorNode).detachAnchor()
        }
        _childNodes.clear()
        Log.d("Clear", _childNodes.size.toString())
    }

    if (future != null) {
        future?.cancel()
        future = null
    }
}

fun onResolveButtonPressed() {
    if (text.isNotEmpty()) {
        Log.d("Cloud", "resolving, future: " + future.toString())
        Log.d("Cloud", "resolving? " + text)
        future = _session.resolveCloudAnchorAsync(text) { anchor, cloudState ->
            Log.d("Cloud", "still resolving? " + text)
            if (cloudState == CloudAnchorState.SUCCESS) {
                Log.d("Cloud3", "Anchor Resolved")
                _childNodes += createAnchorNode(
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    modelInstances = mutableListOf(),
                    model = "burger",
                    anchor = anchor
                )
            } else {
                Log.d("Cloud4", cloudState.toString())
            }
        }
    }else{
        Log.d("Cloud5", "Empty Anchor ID")
    }
}