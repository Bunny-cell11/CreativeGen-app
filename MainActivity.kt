package com.example.creativegen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color.TRANSPARENT
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions



import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isEmpty

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.type

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.text

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.substring

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.get
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController



import coil.compose.rememberAsyncImagePainter
import com.example.creativegen.ui.theme.CreativeGenTheme
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.roundToInt


// DATA CLASSES
data class Project(
    val id: String = "",
    val name: String = "",
    val timestamp: Long = 0L,
    // Add 'val' to make it a property that Firestore can deserialize
    val canvasObjects: List<CanvasObject>? = null
)

data class CanvasObject(
    val id: String = UUID.randomUUID().toString(),
    val type: ObjectType,
    var offset: Offset = Offset(100f, 100f),
    var size: Size = Size(200f, 100f),
    var text: String? = null,
    var fontSize: Float = 32f,
    var textColor: Int = Color.Black.toArgb(),
    var fontFamily: String = "Default",
    var iconName: String? = null,
    var imageUri: String? = null,
    @Transient var imageBitmap: Bitmap? = null
)

enum class ObjectType { TEXT, ICON, IMAGE }
data class Size(val width: Float, val height: Float)

data class BrandKit(
    val id: String = "user_brand_kit",
    val brandColors: List<Int> = listOf(Color.Red.toArgb(), Color.Blue.toArgb(), Color.Black.toArgb()),
    val logoUrl: String? = null
)

data class ComplianceRule(val description: String, val predicate: (CanvasObject, Size) -> Boolean)
data class ComplianceResult(val isSuccess: Boolean, val violations: List<String>)
data class Template(val name: String, val objects: List<CanvasObject>)

val templates = listOf(
    Template("Title and Subtitle", listOf(
        CanvasObject(type = ObjectType.TEXT, text = "Main Title", offset = Offset(100f, 200f), fontSize = 48f),
        CanvasObject(type = ObjectType.TEXT, text = "Subtitle here", offset = Offset(100f, 300f), fontSize = 24f)
    )),
    Template("Iconic Statement", listOf(
        CanvasObject(type = ObjectType.ICON, iconName = "Star", offset = Offset(150f, 200f), size = Size(120f, 120f)),
        CanvasObject(type = ObjectType.TEXT, text = "New Arrival", offset = Offset(100f, 350f), fontSize = 32f)
    ))
)

// APP VIEWMODEL
class AppViewModel : androidx.lifecycle.ViewModel() {
    val canvasObjects = mutableStateListOf<CanvasObject>()
    var selectedObjectId by androidx.compose.runtime.mutableStateOf<String?>(null)
    var showTextEditDialog by androidx.compose.runtime.mutableStateOf(false)
    var showTemplateDialog by androidx.compose.runtime.mutableStateOf(false)
    var isLoading by androidx.compose.runtime.mutableStateOf(false)
    var showViolationsDialog by androidx.compose.runtime.mutableStateOf(false)
    var brandKit by androidx.compose.runtime.mutableStateOf<BrandKit?>(null)

    private val _projects = kotlinx.coroutines.flow.MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects
    var currentProject: Project? by androidx.compose.runtime.mutableStateOf(null)
        private set

    // --- Get instances directly for stability ---
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val firestore = com.google.firebase.Firebase.firestore
    private val storage = com.google.firebase.Firebase.storage

    init {
        // We can check the instance directly now
        if (auth.currentUser != null) {
            loadProjects()
            loadBrandKit()
        }
    }

    fun loadProjects() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            firestore.collection("users").document(userId).collection("projects")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val projectList = snapshot.toObjects(Project::class.java)
                        _projects.value = projectList
                    }
                }
        }
    }


    fun createProject(name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            val projectId = com.android.identity.util.UUID.randomUUID().toString()

            val projectData = mapOf(
                "id" to projectId,
                "name" to name,
                "timestamp" to System.currentTimeMillis(),
                "canvasObjects" to emptyList<CanvasObject>()
            )

            firestore.collection("users").document(userId).collection("projects").document(projectId).set(projectData).await()
        }
    }

    fun loadProject(project: Project) {
        currentProject = project
        canvasObjects.clear()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val projectDoc = firestore
                    .collection("users").document(userId)
                    .collection("projects").document(project.id)
                    .get().await()

                val fullProject = projectDoc.toObject<Project>()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    canvasObjects.clear()
                    canvasObjects.addAll(fullProject?.canvasObjects ?: emptyList())
                }
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            firestore.collection("users").document(userId).collection("projects").document(projectId).delete().await()
        }
    }

    fun saveProject(onSaved: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            currentProject?.let {
                val updatedProject = it.copy(canvasObjects = canvasObjects.toList(), timestamp = System.currentTimeMillis())
                firestore.collection("users").document(userId).collection("projects").document(it.id).set(updatedProject).await()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onSaved() }
            }
        }
    }

    fun loadBrandKit() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            val docRef = firestore.collection("users").document(userId).collection("brandKit").document("user_brand_kit")
            val snapshot = docRef.get().await()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                brandKit = snapshot.toObject<BrandKit>() ?: BrandKit()
            }
        }
    }

    fun saveBrandKit(newBrandKit: BrandKit, onFinished: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            firestore.collection("users").document(userId).collection("brandKit").document(newBrandKit.id).set(newBrandKit).await()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                brandKit = newBrandKit
                onFinished()
            }
        }
    }

    fun uploadBrandLogo(uri: Uri, onFinished: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            val storageRef = storage.reference.child("brand_logos/$userId/logo.png")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onFinished(downloadUrl)
            }
        }
    }

    fun processSelectedImageWithML(context: Context, onFinished: () -> Unit) {
        val selectedObject = getSelectedObject() ?: return
        val imageUriString = selectedObject.imageUri ?: return
        if (selectedObject.type != ObjectType.IMAGE) return

        isLoading = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(context, Uri.parse(imageUriString))
                val options = com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.Builder()
                    .setDetectorMode(com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build()
                val segmenter = com.google.mlkit.vision.segmentation.Segmentation.getClient(options)
                val segmentationMask = segmenter.process(inputImage).await()
                val bitmapToProcess = inputImage.bitmapInternal
                if (bitmapToProcess != null) {
                    val processedBitmap = removeBackground(segmentationMask.buffer, inputImage.width, inputImage.height, bitmapToProcess)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val index = canvasObjects.indexOf(selectedObject)
                        if (index != -1) {
                            canvasObjects[index] = selectedObject.copy(imageBitmap = processedBitmap, imageUri = null)
                        }
                    }
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    onFinished()
                }
            }
        }
    }

    private fun removeBackground(maskBuffer: ByteBuffer, imageWidth: Int, imageHeight: Int, originalBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        maskBuffer.rewind()
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val confidence = maskBuffer.float
                if (confidence > 0.6) { // Threshold can be adjusted
                    resultBitmap.setPixel(x, y, originalBitmap.getPixel(x, y))
                } else {
                    resultBitmap.setPixel(x, y, TRANSPARENT)
                }
            }
        }
        return resultBitmap
    }

    private val complianceRules = listOf(
        ComplianceRule("Text should not be in the bottom 15% of the canvas") { obj, canvasSize ->
            !(obj.type == ObjectType.TEXT && obj.offset.y > canvasSize.height * 0.85)
        },
        ComplianceRule("Images must be at least 100x100 pixels") { obj, _ ->
            !(obj.type == ObjectType.IMAGE && (obj.size.width < 100 || obj.size.height < 100))
        }
    )

    fun validateCompliance(): ComplianceResult {
        val violations = mutableListOf<String>()
        val canvasSize = Size(1080f, 1920f)
        canvasObjects.forEach { obj ->
            complianceRules.forEach { rule ->
                if (!rule.predicate(obj, canvasSize)) {
                    violations.add("${obj.type} ('${obj.text?.take(10) ?: obj.id.substring(0,4)}...'): ${rule.description}")
                }
            }
        }
        return ComplianceResult(violations.isEmpty(), violations)
    }

    fun addTextObject(text: String, fontSize: Float) {
        canvasObjects.add(CanvasObject(type = ObjectType.TEXT, text = text, fontSize = fontSize, textColor = brandKit?.brandColors?.firstOrNull() ?: androidx.compose.ui.graphics.Color.Black.toArgb()))
    }

    fun addIconObject(iconName: String) {
        canvasObjects.add(CanvasObject(type = ObjectType.ICON, iconName = iconName, size = Size(100f, 100f)))
    }

    fun addImageObject(uri: Uri?, url: String? = null, size: Size = Size(300f, 300f)) {
        if (uri == null && url == null) return
        val objectUri = uri?.toString() ?: url
        canvasObjects.add(CanvasObject(type = ObjectType.IMAGE, imageUri = objectUri, size = size))
    }

    fun selectObject(id: String?) {
        selectedObjectId = id
    }

    fun getSelectedObject(): CanvasObject? {
        return canvasObjects.find { it.id == selectedObjectId }
    }

    fun applyTemplate(template: Template) {
        canvasObjects.addAll(template.objects)
    }

    fun updateObjectPosition(objId: String, dragAmount: androidx.compose.ui.geometry.Offset) {
        canvasObjects.find { it.id == objId }?.let {
            val index = canvasObjects.indexOf(it)
            canvasObjects[index] = it.copy(offset = it.offset + dragAmount)
        }
    }

    fun updateTextContent(newText: String) {
        getSelectedObject()?.let {
            val index = canvasObjects.indexOf(it)
            if (index != -1) canvasObjects[index] = it.copy(text = newText)
        }
    }

    fun updateTextColor(color: Int) {
        getSelectedObject()?.let {
            val index = canvasObjects.indexOf(it)
            if (index != -1) canvasObjects[index] = it.copy(textColor = color)
        }
    }

    fun updateTextFontFamily(fontFamily: String) {
        getSelectedObject()?.let {
            val index = canvasObjects.indexOf(it)
            if (index != -1) {
                canvasObjects[index] = it.copy(fontFamily = fontFamily)
            }
        }
    }

    fun deleteSelectedObject() {
        selectedObjectId?.let { id ->
            canvasObjects.removeAll { it.id == id }
            selectedObjectId = null
        }
    }

    fun toggleViolationsDialog(show: Boolean) {
        showViolationsDialog = show
    }
}

// MAIN ACTIVITY & NAVIGATION
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CreativeGenTheme { AppNavigation() } }
    }
}

object AppRoutes {
    const val AUTH_GATE = "auth_gate"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val PROJECTS_LIST = "projects_list"
    const val BRAND_KIT = "brand_kit"
    const val EDITOR = "editor"
}

@androidx.compose.runtime.Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()
    NavHost(navController = navController, startDestination = AppRoutes.AUTH_GATE) {
        composable(AppRoutes.AUTH_GATE) {
            if (Firebase.auth.currentUser != null) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(AppRoutes.PROJECTS_LIST) { popUpTo(0) }
                }
            } else {
                androidx.compose.runtime.LaunchedEffect(Unit) { navController.navigate(AppRoutes.SIGN_IN) { popUpTo(0) } }
            }
        }
        composable(AppRoutes.SIGN_IN) { AuthScreen(navController = navController, onSignInSuccess = {
            appViewModel.loadProjects()
            appViewModel.loadBrandKit()
            navController.navigate(AppRoutes.PROJECTS_LIST) { popUpTo(AppRoutes.AUTH_GATE) { inclusive = true } }
        }) }
        composable(AppRoutes.SIGN_UP) { AuthScreen(navController = navController, isSignIn = false, onSignInSuccess = {
            appViewModel.loadProjects()
            appViewModel.loadBrandKit()
            navController.navigate(AppRoutes.PROJECTS_LIST) { popUpTo(AppRoutes.AUTH_GATE) { inclusive = true } }
        }) }
        composable(AppRoutes.PROJECTS_LIST) { ProjectsListScreen(navController = navController, appViewModel = appViewModel) }
        composable(AppRoutes.BRAND_KIT) { BrandKitScreen(navController = navController, appViewModel = appViewModel) }
        composable(AppRoutes.EDITOR) { CanvaLikeEditor(navController = navController, appViewModel = appViewModel) }
    }
}

// AUTH, PROJECT LIST, BRAND KIT SCREENS
@androidx.compose.runtime.Composable
fun AuthScreen(navController: NavController, isSignIn: Boolean = true, onSignInSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var password by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(if (isSignIn) "Sign In" else "Sign Up", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
            androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))
            androidx.compose.material3.OutlinedTextField(value = email, onValueChange = { email = it }, label = { androidx.compose.material3.Text("Email") })
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { androidx.compose.material3.Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))
            androidx.compose.material3.Button(onClick = {
                val action = if (isSignIn) auth.signInWithEmailAndPassword(email, password) else auth.createUserWithEmailAndPassword(email, password)
                action.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSignInSuccess()
                    } else {
                        Toast.makeText(context, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                androidx.compose.material3.Text(if (isSignIn) "Sign In" else "Create Account")
            }
            androidx.compose.material3.TextButton(onClick = {
                val route = if (isSignIn) AppRoutes.SIGN_UP else AppRoutes.SIGN_IN
                navController.navigate(route) { popUpTo(AppRoutes.AUTH_GATE) }
            }) {
                androidx.compose.material3.Text(if (isSignIn) "Don't have an account? Sign Up" else "Already have an account? Sign In")
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@androidx.compose.runtime.Composable
fun ProjectsListScreen(navController: NavController, appViewModel: AppViewModel) {
    val projects by appViewModel.projects.collectAsState()
    var showCreateDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newProjectName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("My Projects") },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { navController.navigate(AppRoutes.BRAND_KIT) }) {
                        androidx.compose.material3.Icon(Icons.Default.Palette, contentDescription = "Brand Kit")
                    }
                    androidx.compose.material3.IconButton(onClick = { Firebase.auth.signOut(); navController.navigate(AppRoutes.AUTH_GATE) { popUpTo(0) } }) {
                        androidx.compose.material3.Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                }
            ) },
        floatingActionButton = { androidx.compose.material3.FloatingActionButton(onClick = { showCreateDialog = true }) { androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "New Project") } }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(projects.sortedByDescending { it.timestamp }) { project ->
                androidx.compose.material3.ListItem(
                    headlineContent = { androidx.compose.material3.Text(project.name) },
                    supportingContent = { androidx.compose.material3.Text("Last edited: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(project.timestamp))}") },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            appViewModel.loadProject(project)
                            navController.navigate(AppRoutes.EDITOR)
                        },
                        onLongClick = { appViewModel.deleteProject(project.id) }
                    )
                )
            }
        }
    }

    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { androidx.compose.material3.Text("New Project") },
            text = { androidx.compose.material3.OutlinedTextField(value = newProjectName, onValueChange = { newProjectName = it }, label = { androidx.compose.material3.Text("Project Name") }) },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    if (newProjectName.isNotBlank()) {
                        appViewModel.createProject(newProjectName)
                        showCreateDialog = false
                        newProjectName = ""
                    }
                }) { androidx.compose.material3.Text("Create") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showCreateDialog = false }) { androidx.compose.material3.Text("Cancel") } }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun BrandKitScreen(navController: NavController, appViewModel: AppViewModel) {
    val context = LocalContext.current
    val brandKitInitial = appViewModel.brandKit ?: BrandKit()
    var colors by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(brandKitInitial.brandColors) }
    var logoUrl by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(brandKitInitial.logoUrl) }
    val colorPickerController = rememberColorPickerController()
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            appViewModel.isLoading = true
            appViewModel.uploadBrandLogo(it) { url ->
                logoUrl = url
                appViewModel.isLoading = false
            }
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("Brand Kit") },
                navigationIcon = { androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) { androidx.compose.material3.Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )},
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                val newKit = brandKitInitial.copy(brandColors = colors, logoUrl = logoUrl)
                appViewModel.saveBrandKit(newKit) {
                    Toast.makeText(context, "Brand Kit Saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            }) { androidx.compose.material3.Icon(Icons.Default.Save, "Save Brand Kit") }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            androidx.compose.material3.Text("Brand Colors", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(colors) { color ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(40.dp).background(Color(color), CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                }
                item {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.LightGray, CircleShape)
                            .clickable {
                                val newColor = colorPickerController.selectedColor.value.toArgb()
                                if (!colors.contains(newColor)) {
                                    colors = colors + newColor
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Color"
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                controller = colorPickerController,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Text("Brand Logo", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoUrl != null) {
                    Image(painter = rememberAsyncImagePainter(logoUrl), contentDescription = "Brand Logo", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    androidx.compose.material3.Text("No logo uploaded.")
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                androidx.compose.material3.Button(onClick = { imagePickerLauncher.launch("image/*") }) { androidx.compose.material3.Text("Upload Logo") }
            }
            if (appViewModel.isLoading) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}

// EDITOR COMPOSABLES
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun CanvaLikeEditor(navController: NavController, appViewModel: AppViewModel) {
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { appViewModel.addImageObject(it) } }
    val context = LocalContext.current

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(appViewModel.currentProject?.name ?: "New Project") },
                navigationIcon = { androidx.compose.material3.IconButton(onClick = { navController.navigate(AppRoutes.PROJECTS_LIST) { popUpTo(AppRoutes.EDITOR){ inclusive = true } } }) { androidx.compose.material3.Icon(Icons.Default.ArrowBack, contentDescription = "Projects") } },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { appViewModel.saveProject { Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show() } }) {
                        androidx.compose.material3.Icon(Icons.Default.Save, contentDescription = "Save Project")
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomBar(
                appViewModel = appViewModel,
                onAddIcon = { appViewModel.addIconObject(it) },
                onAddText = { appViewModel.showTextEditDialog = true },
                onAddImage = { imagePickerLauncher.launch("image/*") },
                onApplyTemplate = { appViewModel.showTemplateDialog = true }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.LightGray)) {
            DrawingCanvas(appViewModel = appViewModel)
            if (appViewModel.isLoading) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            appViewModel.getSelectedObject()?.let {
                ObjectToolbar(appViewModel = appViewModel, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            }
            ComplianceStatus(appViewModel = appViewModel, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
    }

    if (appViewModel.showTextEditDialog) {
        var textValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(appViewModel.getSelectedObject()?.text ?: "New Text") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { appViewModel.showTextEditDialog = false },
            title = { androidx.compose.material3.Text("Edit Text") },
            text = { androidx.compose.material3.OutlinedTextField(value = textValue, onValueChange = { textValue = it }) },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    if (appViewModel.getSelectedObject() != null) {
                        appViewModel.updateTextContent(textValue)
                    } else {
                        appViewModel.addTextObject(textValue, 32f)
                    }
                    appViewModel.showTextEditDialog = false
                }) { androidx.compose.material3.Text("OK") }
            }
        )
    }

    if (appViewModel.showTemplateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { appViewModel.showTemplateDialog = false },
            title = { androidx.compose.material3.Text("Apply a Template") },
            text = {
                LazyColumn {
                    items(templates) { template ->
                        androidx.compose.material3.Text(
                            template.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appViewModel.applyTemplate(template)
                                    appViewModel.showTemplateDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { appViewModel.showTemplateDialog = false }) { androidx.compose.material3.Text("Cancel") }
            }
        )
    }

    if (appViewModel.showViolationsDialog) {
        val violations = appViewModel.validateCompliance().violations
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { appViewModel.toggleViolationsDialog(false) },
            title = { androidx.compose.material3.Text("Compliance Violations") },
            text = {
                LazyColumn {
                    items(violations) { violation ->
                        androidx.compose.material3.Text(violation, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { appViewModel.toggleViolationsDialog(false) }) { androidx.compose.material3.Text("Close") }
            }
        )
    }
}

@androidx.compose.runtime.Composable
fun DrawingCanvas(appViewModel: AppViewModel) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { appViewModel.selectObject(null) })
            }
    ) {
        appViewModel.canvasObjects.forEach { canvasObject ->
            CanvasObjectRenderer(
                canvasObject = canvasObject,
                appViewModel = appViewModel,
                modifier = Modifier
                    .offset { IntOffset(canvasObject.offset.x.roundToInt(), canvasObject.offset.y.roundToInt()) }
                    .size(width = canvasObject.size.width.dp, height = canvasObject.size.height.dp)
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun CanvasObjectRenderer(canvasObject: CanvasObject, appViewModel: AppViewModel, modifier: Modifier) {
    val interactionModifier = Modifier.pointerInput(canvasObject.id) {
        detectDragGestures(
            onDragStart = { appViewModel.selectObject(canvasObject.id) },
            onDrag = { _, dragAmount -> appViewModel.updateObjectPosition(canvasObject.id, dragAmount) }
        )
    }
    val borderModifier = if (appViewModel.selectedObjectId == canvasObject.id) Modifier.border(1.dp, Color.Blue) else Modifier

    androidx.compose.foundation.layout.Box(modifier = modifier.then(interactionModifier).then(borderModifier)) {
        when (canvasObject.type) {
            ObjectType.TEXT -> {
                val fontFamily = when(canvasObject.fontFamily) {
                    "Cursive" -> FontFamily.Cursive
                    "Serif" -> FontFamily.Serif
                    "Monospace" -> FontFamily.Monospace
                    else -> FontFamily.Default
                }
                androidx.compose.material3.Text(
                    text = canvasObject.text ?: "",
                    fontSize = canvasObject.fontSize.sp,
                    color = Color(canvasObject.textColor),
                    fontFamily = fontFamily
                )
            }
            ObjectType.ICON -> {
                androidx.compose.material3.Icon(getIconByName(canvasObject.iconName), contentDescription = canvasObject.iconName, modifier = Modifier.fillMaxSize())
            }
            ObjectType.IMAGE -> {
                val painter = if (canvasObject.imageBitmap != null) {
                    androidx.compose.runtime.remember(canvasObject.imageBitmap) { androidx.compose.ui.graphics.painter.BitmapPainter(canvasObject.imageBitmap!!.asImageBitmap()) }
                } else {
                    rememberAsyncImagePainter(model = canvasObject.imageUri)
                }
                Image(painter = painter, contentDescription = "User Image", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun ObjectToolbar(appViewModel: AppViewModel, modifier: Modifier = Modifier) {
    val selectedObject = appViewModel.getSelectedObject() ?: return
    var showColorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showFontPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = LocalContext.current

    if (showColorPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { androidx.compose.material3.Text("Select Color") },
            text = {
                androidx.compose.foundation.layout.Column {
                    val controller = rememberColorPickerController()
                    LazyRow(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        items(appViewModel.brandKit?.brandColors ?: emptyList()) { color ->
                            androidx.compose.foundation.layout.Box(modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable {
                                    appViewModel.updateTextColor(color)
                                    showColorPicker = false
                                })
                        }
                    }
                    HsvColorPicker(modifier = Modifier.fillMaxWidth().height(300.dp), controller = controller)
                    androidx.compose.material3.Button(onClick = {
                        appViewModel.updateTextColor(controller.selectedColor.value.toArgb())
                        showColorPicker = false
                    }, modifier = Modifier.align(Alignment.End)) {
                        androidx.compose.material3.Text("Select")
                    }
                }
            },
            confirmButton = {}
        )
    }

    androidx.compose.material3.Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectedObject.type == ObjectType.TEXT) {
                androidx.compose.material3.IconButton(onClick = { appViewModel.showTextEditDialog = true }) { androidx.compose.material3.Icon(Icons.Default.Edit, "Edit Text") }
                androidx.compose.material3.IconButton(onClick = { showColorPicker = true }) { androidx.compose.material3.Icon(Icons.Default.Colorize, "Change Color") }
                androidx.compose.foundation.layout.Box {
                    androidx.compose.material3.IconButton(onClick = { showFontPicker = true }) { androidx.compose.material3.Icon(Icons.Default.FontDownload, "Change Font") }
                    androidx.compose.material3.DropdownMenu(expanded = showFontPicker, onDismissRequest = { showFontPicker = false }) {
                        listOf("Default", "Cursive", "Serif", "Monospace").forEach { fontName ->
                            androidx.compose.material3.DropdownMenuItem(text = { androidx.compose.material3.Text(fontName) }, onClick = {
                                appViewModel.updateTextFontFamily(fontName)
                                showFontPicker = false
                            })
                        }
                    }
                }
            }
            if (selectedObject.type == ObjectType.IMAGE && selectedObject.imageUri != null) {
                androidx.compose.material3.Button(onClick = {
                    appViewModel.processSelectedImageWithML(context) {
                        Toast.makeText(context, "Background Removed!", Toast.LENGTH_SHORT).show()
                    }
                }, enabled = !appViewModel.isLoading) { androidx.compose.material3.Text("Remove BG") }
            }
            androidx.compose.material3.IconButton(onClick = { appViewModel.deleteSelectedObject() }) { androidx.compose.material3.Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@androidx.compose.runtime.Composable
fun EditorBottomBar(appViewModel: AppViewModel, onAddIcon: (String) -> Unit, onAddText: () -> Unit, onAddImage: () -> Unit, onApplyTemplate: () -> Unit) {
    var showIconPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.BottomAppBar {
        androidx.compose.material3.IconButton(onClick = onAddText) { androidx.compose.material3.Icon(Icons.Default.TextFields, "Add Text") }
        androidx.compose.material3.IconButton(onClick = onAddImage) { androidx.compose.material3.Icon(Icons.Default.Image, "Add Image") }
        androidx.compose.material3.IconButton(onClick = { showIconPicker = true }) { androidx.compose.material3.Icon(Icons.Default.Star, "Add Icon") }
        androidx.compose.material3.IconButton(onClick = onApplyTemplate) { androidx.compose.material3.Icon(Icons.Default.AutoAwesome, "Apply Template") }

        appViewModel.brandKit?.logoUrl?.let { logoUrl ->
            androidx.compose.material3.IconButton(onClick = { appViewModel.addImageObject(null, logoUrl, size = Size(150f, 150f)) }) {
                androidx.compose.material3.Icon(Icons.Default.Business, "Add Brand Logo")
            }
        }

        if (showIconPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showIconPicker = false },
                title = { androidx.compose.material3.Text("Select an Icon") },
                text = {
                    val iconList = listOf("Star", "Favorite", "Settings", "Build", "Info", "ThumbUp", "Home", "Face", "ShoppingCart")
                    LazyColumn {
                        items(iconList) { iconName ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { androidx.compose.material3.Text(iconName) },
                                leadingContent = { androidx.compose.material3.Icon(getIconByName(iconName), null) },
                                modifier = Modifier.clickable {
                                    onAddIcon(iconName)
                                    showIconPicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { showIconPicker = false }) { androidx.compose.material3.Text("Cancel") } }
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun ComplianceStatus(appViewModel: AppViewModel, modifier: Modifier = Modifier) {
    val complianceResult = appViewModel.validateCompliance()
    val statusColor = if (complianceResult.isSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val statusText = if (complianceResult.isSuccess) "Compliance: Passed" else "Compliance: Failed (${complianceResult.violations.size})"

    androidx.compose.material3.Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable(enabled = !complianceResult.isSuccess) { appViewModel.toggleViolationsDialog(true) },
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = statusColor)
    ) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(if (complianceResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = "Status", tint = Color.White)
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Text(statusText, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

fun getIconByName(name: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Star" -> Icons.Default.Star
        "Favorite" -> Icons.Default.Favorite
        "Settings" -> Icons.Default.Settings
        "Build" -> Icons.Default.Build
        "Info" -> Icons.Default.Info
        "ThumbUp" -> Icons.Default.ThumbUp
        "Home" -> Icons.Default.Home
        "Face" -> Icons.Default.Face
        "ShoppingCart" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Help
    }
}
