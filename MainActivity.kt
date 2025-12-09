package com.example.creativegen

import android.graphics.Bitmap
import android.graphics.Color.TRANSPARENT
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.creativegen.ui.theme.CreativeGenTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer
import kotlin.math.roundToInt

// ---------- NAVIGATION & DATA ----------
object AppRoutes {
    const val AUTH_GATE = "auth_gate"; const val SIGN_IN = "sign_in"; const val SIGN_UP = "sign_up"
    const val PROJECTS_LIST = "projects_list"; const val EDITOR = "editor"
}

enum class ObjectType { ICON, TEXT, IMAGE }

data class CanvasObject(
    val id: String = System.currentTimeMillis().toString(),
    val type: ObjectType,
    var offset: Offset = Offset(50f, 50f),
    var size: Size = Size(250f, 250f),
    // Icon properties
    val iconName: String? = null,
    // Text properties
    var text: String? = null,
    var textColor: Int = Color.Black.hashCode(),
    var fontSize: Float = 24f,
    // Image properties
    val imageUri: String? = null,
    @get:com.google.firebase.firestore.Exclude @set:com.google.firebase.firestore.Exclude
    var imageBitmap: Bitmap? = null
)

data class Project(val id: String = "", val name: String = "Untitled", val userId: String = "", val timestamp: Long = System.currentTimeMillis())
data class Template(val name: String, val objects: List<CanvasObject>)
val templates = listOf(
    Template("Birthday Card", listOf(
        CanvasObject(type = ObjectType.TEXT, text = "Happy Birthday!", fontSize = 48f, offset = Offset(100f, 200f), textColor = Color(0xFFE91E63).hashCode()),
        CanvasObject(type = ObjectType.ICON, iconName = "Favorite", offset = Offset(350f, 350f), size = Size(200f, 200f)),
        CanvasObject(type = ObjectType.TEXT, text = "Have a great day!", fontSize = 24f, offset = Offset(120f, 600f))
    )),
    Template("Business Announcement", listOf(
        CanvasObject(type = ObjectType.ICON, iconName = "Lightbulb", offset = Offset(150f, 100f), size = Size(150f, 150f)),
        CanvasObject(type = ObjectType.TEXT, text = "Big News!", fontSize = 52f, offset = Offset(350f, 150f), textColor = Color(0xFF0D47A1).hashCode()),
        CanvasObject(type = ObjectType.TEXT, text = "We're launching a new product next week.", fontSize = 20f, offset = Offset(150f, 400f))
    ))
)

// ---------- VIEWMODEL WITH FINAL FEATURE LOGIC ----------
class AppViewModel : ViewModel() {
    var currentProject by mutableStateOf<Project?>(null)
    val canvasObjects = mutableStateListOf<CanvasObject>()
    var selectedObjectId by mutableStateOf<String?>(null)
    var showTextEditDialog by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    fun loadProject(project: Project, onComplete: () -> Unit) {
        currentProject = project; selectedObjectId = null; canvasObjects.clear()
        Firebase.firestore.collection("projects").document(project.id).collection("objects").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) { onComplete() } else {
                    val objects = snapshot.toObjects(CanvasObject::class.java); canvasObjects.addAll(objects); onComplete()
                }
            }
    }

    fun saveProject(onComplete: () -> Unit) {
        val project = currentProject ?: return; val db = Firebase.firestore; val batch = db.batch()
        val projectRef = db.collection("projects").document(project.id)
        batch.set(projectRef, project.copy(timestamp = System.currentTimeMillis()))
        canvasObjects.forEach { obj -> batch.set(projectRef.collection("objects").document(obj.id), obj.copy(imageBitmap = null)) }
        batch.commit().addOnSuccessListener { onComplete() }
    }

    fun applyTemplate(template: Template) {
        canvasObjects.clear()
        val newObjects = template.objects.map { it.copy(id = System.currentTimeMillis().toString() + (Math.random()*100).toInt()) }
        canvasObjects.addAll(newObjects); selectedObjectId = null
    }

    fun addIconObject(iconName: String, offset: Offset) { canvasObjects.add(CanvasObject(type = ObjectType.ICON, iconName = iconName, offset = offset, size = Size(150f, 150f))) }
    fun addTextObject(text: String, fontSize: Float) { canvasObjects.add(CanvasObject(type = ObjectType.TEXT, text = text, fontSize = fontSize)) }
    fun addImageObject(uri: Uri) { canvasObjects.add(CanvasObject(type = ObjectType.IMAGE, imageUri = uri.toString())) }

    fun selectObject(objId: String?) { selectedObjectId = objId }
    fun getSelectedObject(): CanvasObject? = canvasObjects.find { it.id == selectedObjectId }

    fun processSelectedImageWithML(context: android.content.Context) {
        val selectedObject = getSelectedObject() ?: return
        if (selectedObject.type != ObjectType.IMAGE || selectedObject.imageUri == null) return
        isLoading = true
        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(context, Uri.parse(selectedObject.imageUri))
                val options = SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build()
                val segmenter = Segmentation.getClient(options)
                val segmentationMask = segmenter.process(inputImage).await()
                val processedBitmap = removeBackground(segmentationMask.buffer, inputImage.width, inputImage.height, inputImage.bitmapInternal!!)
                selectedObject.imageBitmap = processedBitmap
                selectedObject.imageUri = null // No longer points to the original file
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Background removal failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeBackground(maskBuffer: ByteBuffer, imageWidth: Int, imageHeight: Int, originalBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val bgConfidence = 1.0f - maskBuffer.float
                if (bgConfidence > 0.6) { resultBitmap.setPixel(x, y, originalBitmap.getPixel(x, y)) }
                else { resultBitmap.setPixel(x, y, TRANSPARENT) }
            }
        }
        maskBuffer.rewind()
        return resultBitmap
    }

    fun updateObjectPosition(objId: String, dragAmount: Offset) { canvasObjects.find { it.id == objId }?.let { it.offset += dragAmount } }
    fun updateTextContent(newText: String) { getSelectedObject()?.let { it.text = newText } }
    fun deleteSelectedObject() {
        selectedObjectId?.let { id ->
            canvasObjects.removeAll { it.id == id }
            currentProject?.let { project -> Firebase.firestore.collection("projects").document(project.id).collection("objects").document(id).delete() }
            selectedObjectId = null
        }
    }
}

// ---------- MAIN ACTIVITY & NAVIGATION (UNCHANGED) ----------
class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { CreativeGenTheme { AppNavigation() } } } }
@Composable
fun AppNavigation() {
    val navController = rememberNavController(); val appViewModel: AppViewModel = viewModel()
    NavHost(navController = navController, startDestination = AppRoutes.AUTH_GATE) {
        composable(AppRoutes.AUTH_GATE) { if (Firebase.auth.currentUser != null) LaunchedEffect(Unit) { navController.navigate(AppRoutes.PROJECTS_LIST) { popUpTo(0) } } else LaunchedEffect(Unit) { navController.navigate(AppRoutes.SIGN_IN) { popUpTo(0) } } }
        composable(AppRoutes.SIGN_IN) { AuthScreen(navController = navController, isSignIn = true) }
        composable(AppRoutes.SIGN_UP) { AuthScreen(navController = navController, isSignIn = false) }
        composable(AppRoutes.PROJECTS_LIST) { ProjectsListScreen(navController = navController, appViewModel = appViewModel) }
        composable(AppRoutes.EDITOR) { CanvaLikeEditor(navController = navController, appViewModel = appViewModel) }
    }
}

// ---------- AUTH & PROJECT LIST SCREENS (UNCHANGED) ----------
@OptIn(ExperimentalMaterial3Api::class) @Composable fun AuthScreen(navController: NavController, isSignIn: Boolean) { /* ... same as before ... */ }
@OptIn(ExperimentalMaterial3Api::class) @Composable fun ProjectsListScreen(navController: NavController, appViewModel: AppViewModel) { /* ... same as before ... */ }

// ---------- EDITOR WITH FINAL FEATURES ----------
@Composable
fun CanvaLikeEditor(navController: NavController, appViewModel: AppViewModel) {
    var draggedIcon by remember { mutableStateOf<Pair<String, Offset>?>(null) }
    val selectedObject = appViewModel.getSelectedObject()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { EditorTopBar(projectName = appViewModel.currentProject?.name ?: "Untitled", onBack = { navController.popBackStack() }, onSave = { appViewModel.saveProject { navController.popBackStack() } }) },
            bottomBar = {
                EditorBottomBar(
                    viewModel = appViewModel,
                    onIconDragStart = { iconName, offset -> draggedIcon = iconName to offset },
                    onIconDrag = { offsetChange -> draggedIcon = draggedIcon?.let { it.first to it.second + offsetChange } },
                    onIconDragEnd = { draggedIcon?.let { (iconName, finalOffset) -> appViewModel.addIconObject(iconName, finalOffset) }; draggedIcon = null }
                )
            },
            containerColor = Color(0xFF121417)
        ) { innerPadding -> MainCanvas(modifier = Modifier.padding(innerPadding).fillMaxSize(), viewModel = appViewModel) }

        if (appViewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        draggedIcon?.let { (iconName, offset) -> Icon(imageVector = iconMap[iconName]!!, contentDescription = null, tint = Color.White, modifier = Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }.size(50.dp)) }

        if (appViewModel.showTextEditDialog && selectedObject?.type == ObjectType.TEXT) {
            TextEditDialog(initialText = selectedObject.text ?: "", onDismiss = { appViewModel.showTextEditDialog = false }, onConfirm = { newText -> appViewModel.updateTextContent(newText); appViewModel.showTextEditDialog = false })
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class) @Composable fun TextEditDialog(initialText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) { /* ... same as before ... */ }

// ---------- CANVAS WITH IMAGE DRAWING LOGIC ----------
@OptIn(ExperimentalTextApi::class)
@Composable
fun MainCanvas(modifier: Modifier = Modifier, viewModel: AppViewModel) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    Box(modifier = modifier.padding(16.dp).clip(MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f).background(Color.White).pointerInput(Unit) { detectTapGestures(onTap = { viewModel.selectObject(null) }) }) {
            viewModel.canvasObjects.forEach { obj -> drawObject(obj, isSelected = viewModel.selectedObjectId == obj.id, textMeasurer) }
        }
        viewModel.canvasObjects.forEach { obj ->
            // Load bitmap for drawing if it's an image object from Firestore
            if (obj.type == ObjectType.IMAGE && obj.imageUri != null && obj.imageBitmap == null) {
                LaunchedEffect(obj.imageUri) {
                    val request = ImageRequest.Builder(context).data(obj.imageUri).build()
                    val result = context.imageLoader.execute(request).drawable
                    obj.imageBitmap = (result as android.graphics.drawable.BitmapDrawable).bitmap
                }
            }
            Box(modifier = Modifier
                .offset { IntOffset(obj.offset.x.roundToInt(), obj.offset.y.roundToInt()) }
                .size(width = with(LocalDensity.current) { obj.size.width.toDp() }, height = with(LocalDensity.current) { obj.size.height.toDp() })
                .pointerInput(obj.id) { detectTapGestures(onTap = { viewModel.selectObject(obj.id); if (obj.type == ObjectType.TEXT) viewModel.showTextEditDialog = true }) }
                .pointerInput(obj.id) { detectDragGestures { change, dragAmount -> change.consume(); viewModel.updateObjectPosition(obj.id, dragAmount) } }
            )
        }
        if (viewModel.canvasObjects.isEmpty()) { Text("Canvas is empty. Pick a template!", color = Color.Gray) }
    }
}

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawObject(obj: CanvasObject, isSelected: Boolean, textMeasurer: TextMeasurer) {
    when (obj.type) {
        ObjectType.ICON -> {
            iconMap[obj.iconName]?.let { icon -> val painter = rememberVectorPainter(image = icon); drawIntoCanvas { it.translate(obj.offset.x, obj.offset.y); with(painter) { draw(size = obj.size) }; it.translate(-obj.offset.x, -obj.offset.y) } }
        }
        ObjectType.TEXT -> {
            obj.text?.let { text -> val style = TextStyle(color = Color(obj.textColor), fontSize = obj.fontSize.sp, fontWeight = FontWeight.Bold); val measuredText = textMeasurer.measure(text, style); obj.size = measuredText.size.toSize(); drawText(measuredText, topLeft = obj.offset) }
        }
        ObjectType.IMAGE -> {
            obj.imageBitmap?.let { bmp -> drawImage(bmp.asImageBitmap(), dstOffset = IntOffset(obj.offset.x.roundToInt(), obj.offset.y.roundToInt()), dstSize = IntOffset(obj.size.width.roundToInt(), obj.size.height.roundToInt())) }
        }
    }
    if (isSelected) { drawRect(color = Color.Blue, topLeft = obj.offset, size = obj.size, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())) }
}

fun Size.toSize() = Size(width, height)

// ---------- BOTTOM BAR WITH UPLOADS & BG REMOVER ----------
val iconMap = mapOf( "Star" to Icons.Outlined.Star, "Favorite" to Icons.Outlined.FavoriteBorder, "Circle" to Icons.Outlined.Circle, "Build" to Icons.Outlined.Build, "Call" to Icons.Outlined.Call, "Mail" to Icons.Outlined.Mail, "Location" to Icons.Outlined.LocationOn, "Cart" to Icons.Outlined.ShoppingCart, "Play" to Icons.Outlined.PlayArrow, "Lock" to Icons.Outlined.Lock, "Face" to Icons.Outlined.Face, "Home" to Icons.Outlined.Home, "Settings" to Icons.Outlined.Settings, "Search" to Icons.Outlined.Search, "Delete" to Icons.Outlined.Delete, "Edit" to Icons.Outlined.Edit, "Done" to Icons.Outlined.Done, "ThumbUp" to Icons.Outlined.ThumbUp, "Lightbulb" to Icons.Outlined.Lightbulb, "Camera" to Icons.Outlined.CameraAlt, "Music" to Icons.Outlined.MusicNote, "Cloud" to Icons.Outlined.Cloud, "Sun" to Icons.Outlined.WbSunny, "Moon" to Icons.Outlined.Brightness2, "Bed" to Icons.Outlined.LocalHotel, "Car" to Icons.Outlined.DirectionsCar )

@Composable
fun EditorBottomBar(viewModel: AppViewModel, onIconDragStart: (String, Offset) -> Unit, onIconDrag: (Offset) -> Unit, onIconDragEnd: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Templates") }
    val context = LocalContext.current
    Column(modifier = Modifier.background(Color(0xFF1F222A))) {
        Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = viewModel.selectedObjectId != null) {
                val selectedObject = viewModel.getSelectedObject()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { viewModel.deleteSelectedObject() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, "Delete"); Spacer(Modifier.width(8.dp)); Text("Delete") }
                    if (selectedObject?.type == ObjectType.TEXT) { Button(onClick = { viewModel.showTextEditDialog = true }) { Icon(Icons.Default.Edit, "Edit Text"); Spacer(Modifier.width(8.dp)); Text("Edit Text") } }
                    if (selectedObject?.type == ObjectType.IMAGE) { Button(onClick = { viewModel.processSelectedImageWithML(context) }) { Icon(Icons.Default.ContentCut, "Remove BG"); Spacer(Modifier.width(8.dp)); Text("Remove BG") } }
                }
            }
            AnimatedVisibility(visible = viewModel.selectedObjectId == null) {
                when (selectedTab) {
                    "Templates" -> TemplateLibrary(onTemplateClick = { template -> viewModel.applyTemplate(template) })
                    "Elements" -> IconLibrary(onIconDragStart, onIconDrag, onIconDragEnd)
                    "Text" -> TextToolActions(onAddText = { text, size -> viewModel.addTextObject(text, size) })
                    "Uploads" -> UploadsTab(onImageSelected = { uri -> viewModel.addImageObject(uri) })
                    else -> Text("$selectedTab content goes here", color = Color.White)
                }
            }
        }
        Divider(color = Color(0xFF33363F))
        EditorNavigationBar(selectedTab) { newTab -> selectedTab = newTab }
    }
}

// ---------- NEW UPLOADS TAB UI ----------
@Composable
fun UploadsTab(onImageSelected: (Uri) -> Unit) {
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let(onImageSelected) }
    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Image")
        Spacer(Modifier.width(8.dp))
        Text("Upload Image")
    }
}

// ---------- OTHER UNCHANGED UI COMPONENTS ----------
@OptIn(ExperimentalTextApi::class) @Composable fun TemplateLibrary(onTemplateClick: (Template) -> Unit) { /* ... same as before ... */ }
@Composable fun TextToolActions(onAddText: (String, Float) -> Unit) { /* ... same as before ... */ }
@Composable fun IconLibrary(onIconDragStart: (String, Offset) -> Unit, onIconDrag: (Offset) -> Unit, onIconDragEnd: () -> Unit) { /* ... same as before ... */ }
@OptIn(ExperimentalMaterial3ai::class) @Composable fun EditorTopBar(projectName: String, onBack: () -> Unit, onSave: () -> Unit) { /* ... same as before ... */ }
@Composable fun EditorNavigationBar(selectedTab: String, onTabSelected: (String) -> Unit) { /* ... same as before ... */ }
fun getIconForTab(tab: String): ImageVector { /* ... same as before ... */ return when (tab) { "Templates" -> Icons.Default.Dashboard; "Elements" -> Icons.Default.Category; "Text" -> Icons.Default.TextFields; "Uploads" -> Icons.Default.CloudUpload; "Draw" -> Icons.Default.Draw; else -> Icons.Default.MoreHoriz } }
