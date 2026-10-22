package com.example.nexusvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// --- CYBERPUNK GAMER PALETTE ---
val CyberBg = Color(0xFF07080C)
val CyberSurface = Color(0xFF12151F)
val CyberNeonCyan = Color(0xFF00F0FF)
val CyberNeonMagenta = Color(0xFFFF0055)
val CyberAcidGreen = Color(0xFF00FF66)
val CyberAmber = Color(0xFFFFB800)
val CyberTextPrimary = Color(0xFFF2F5FA)
val CyberTextSecondary = Color(0xFF8A94A6)

// --- DATA MODELS ---
enum class CaptureCategory(val label: String) {
    ALL("ALL"),
    CLIPS("DVR CLIPS"),
    SCREENSHOTS("4K RAW"),
    ACHIEVEMENTS("TROPHIES"),
    FAVORITES("STARRED")
}

data class GamerMediaItem(
    val id: Int,
    val title: String,
    val gameTitle: String,
    val imageUrl: String,
    val category: CaptureCategory,
    val fps: Int,
    val resolution: String,
    val bitrate: String,
    val duration: String? = null,
    val isHdr: Boolean = true,
    val isFavorite: Boolean = false,
    val aspectRatio: Float = 1f,
    val gpuTemp: String = "64°C"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = CyberBg,
                    surface = CyberSurface,
                    primary = CyberNeonCyan,
                    secondary = CyberNeonMagenta
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = CyberBg) {
                    NexusVaultApp()
                }
            }
        }
    }
}

// --- MAIN APP STATE CONTROLLER ---
enum class AppScreen { ONBOARDING, INITIALIZING, GALLERY }

@Composable
fun NexusVaultApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.ONBOARDING) }
    var mediaItems by remember { mutableStateOf(generateSampleCaptures()) }
    var selectedItem by remember { mutableStateOf<GamerMediaItem?>(null) }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.ONBOARDING -> {
                ProGamerOnboarding(onComplete = { currentScreen = AppScreen.INITIALIZING })
            }
            AppScreen.INITIALIZING -> {
                BootSequenceLoader(onLoaded = { currentScreen = AppScreen.GALLERY })
            }
            AppScreen.GALLERY -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    GamerGalleryMainScreen(
                        items = mediaItems,
                        onSelectItem = { selectedItem = it },
                        onToggleFavorite = { id ->
                            mediaItems = mediaItems.map {
                                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
                            }
                        }
                    )

                    // Fullscreen Interactive Lightbox Overlay
                    AnimatedVisibility(
                        visible = selectedItem != null,
                        enter = fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        ),
                        exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.88f)
                    ) {
                        selectedItem?.let { item ->
                            val liveItem = mediaItems.find { it.id == item.id } ?: item
                            GamerLightboxDetail(
                                item = liveItem,
                                onDismiss = { selectedItem = null },
                                onToggleFavorite = {
                                    mediaItems = mediaItems.map {
                                        if (it.id == liveItem.id) it.copy(isFavorite = !it.isFavorite) else it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 1. PRO ONBOARDING SCREEN (3D Pager Physics + Custom HUD Vector Canvas)
// ============================================================================
data class OnboardingSlide(
    val tag: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProGamerOnboarding(onComplete: () -> Unit) {
    val slides = listOf(
        OnboardingSlide("ENGINE // 01", "120FPS RAW\nCAPTURE VAULT", "Zero-latency indexing for your 4K HDR screenshots, instant replays, and boss-fight highlights.", CyberNeonCyan),
        OnboardingSlide("TELEMETRY // 02", "REAL-TIME\nFRAME INSPECTOR", "Analyze bitrate, frametime graphs, HDR metadata, and GPU thermal logs right inside every capture.", CyberNeonMagenta),
        OnboardingSlide("CLOUD SYNC // 03", "INSTANT SQUAD\nSHARING", "Hardware-accelerated clip trimming and one-tap export to Discord, Steam, and tournament feeds.", CyberAcidGreen)
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "bgGlow")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "glowRot"
    )

    Box(modifier = Modifier.fillMaxSize().background(CyberBg)) {
        // Animated Ambient Cyber Grid Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentAccent = slides[pagerState.currentPage].accent
            rotate(glowRotation) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(currentAccent.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = size.minDimension * 0.75f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(slides[pagerState.currentPage].accent, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "NEXUS // OS v4.2",
                        color = CyberTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )
                }
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComplete()
                }) {
                    Text("SKIP_BOOT >", color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                }
            }

            // 3D Parallax Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val slide = slides[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = 1f - (abs(pageOffset) * 0.18f)
                            scaleX = scale
                            scaleY = scale
                            rotationY = pageOffset * -22f
                            alpha = 1f - (abs(pageOffset) * 0.5f)
                            cameraDistance = 16f * density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Interactive Procedural HUD Graphic
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .graphicsLayer {
                                translationX = pageOffset * -120f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CyberHudIllustration(page = page, accent = slide.accent, rotation = glowRotation)
                    }

                    Spacer(Modifier.height(40.dp))

                    Text(
                        text = slide.tag,
                        color = slide.accent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = slide.title,
                        color = CyberTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.graphicsLayer {
                            translationX = pageOffset * -60f
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = slide.subtitle,
                        color = CyberTextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom Navigation & Spring Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Morphing Page Indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slides.forEachIndexed { index, slide ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 36.dp else 10.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) slide.accent else CyberTextSecondary.copy(alpha = 0.3f))
                        )
                    }
                }

                // Cyber Cut-Corner CTA Button
                val currentAccent = slides[pagerState.currentPage].accent
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (pagerState.currentPage < slides.lastIndex) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                )
                            }
                        } else {
                            onComplete()
                        }
                    },
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccent),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == slides.lastIndex) "INITIALIZE VAULT" else "NEXT MODULE",
                        color = CyberBg,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CyberHudIllustration(page: Int, accent: Color, rotation: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2.2f
        // Outer dashed ring
        rotate(rotation) {
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = radius,
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f))
            )
        }
        // Counter-rotating inner ring
        rotate(-rotation * 1.4f) {
            drawCircle(
                color = accent,
                radius = radius * 0.72f,
                style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(60f, 40f, 10f, 40f), 0f))
            )
        }
        // Central Polygon
        val sides = page + 4 // 4, 5, 6 sides per onboarding screen
        val path = Path()
        val polyRadius = radius * 0.42f
        for (i in 0 until sides) {
            val angle = Math.toRadians((i * 360.0 / sides) + rotation)
            val x = center.x + (polyRadius * cos(angle)).toFloat()
            val y = center.y + (polyRadius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = accent.copy(alpha = 0.2f))
        drawPath(path, color = accent, style = Stroke(width = 2.5.dp.toPx()))
    }
}

// ============================================================================
// 2. PRO LOADING ANIMATION (Cybernetic Boot Sequence & Shimmer Loader)
// ============================================================================
@Composable
fun BootSequenceLoader(onLoaded: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("MOUNTING NVMe SHADER CACHE...") }

    LaunchedEffect(Unit) {
        delay(350)
        progress = 0.35f
        statusText = "INDEXING 4K HDR CAPTURES..."
        delay(450)
        progress = 0.75f
        statusText = "CALIBRATING 120HZ TELEMETRY HUD..."
        delay(450)
        progress = 1f
        statusText = "VAULT ONLINE."
        delay(250)
        onLoaded()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "bootProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CyberNeonSpinner(size = 96.dp)
        Spacer(Modifier.height(36.dp))
        Text(
            text = statusText,
            color = CyberNeonCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CyberSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(listOf(CyberNeonCyan, CyberNeonMagenta))
                    )
            )
        }
    }
}

@Composable
fun CyberNeonSpinner(size: Dp = 56.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "angle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(size)) {
        val stroke = 3.5.dp.toPx()
        rotate(angle) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, CyberNeonCyan, CyberNeonMagenta)),
                startAngle = 0f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        rotate(-angle * 1.6f) {
            drawArc(
                color = CyberAcidGreen,
                startAngle = 45f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round),
                topLeft = Offset(this.size.width * 0.18f, this.size.height * 0.18f),
                size = this.size * 0.64f
            )
        }
        drawCircle(
            color = CyberNeonCyan.copy(alpha = 0.8f),
            radius = 5.dp.toPx() * pulse
        )
    }
}

// ============================================================================
// 3. MAIN GAMER GALLERY SCREEN (Interactive Grid, Filters, Spring Cards)
// ============================================================================
@Composable
fun GamerGalleryMainScreen(
    items: List<GamerMediaItem>,
    onSelectItem: (GamerMediaItem) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(CaptureCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var gridColumns by remember { mutableIntStateOf(2) }
    val haptic = LocalHapticFeedback.current

    val filteredItems = remember(items, selectedCategory, searchQuery) {
        items.filter { item ->
            val matchesCat = when (selectedCategory) {
                CaptureCategory.ALL -> true
                CaptureCategory.FAVORITES -> item.isFavorite
                else -> item.category == selectedCategory
            }
            val matchesQuery = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.gameTitle.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBg.copy(alpha = 0.92f))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Gamer Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "NEXUS",
                                color = CyberTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                " // VAULT",
                                color = CyberNeonCyan,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${items.size} CAPTURES • NVMe 142GB FREE • 120HZ SYNC",
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Grid Density Switcher Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            gridColumns = if (gridColumns == 2) 3 else 2
                        },
                        modifier = Modifier
                            .clip(CutCornerShape(6.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberNeonCyan.copy(alpha = 0.4f), CutCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = if (gridColumns == 2) Icons.Default.GridView else Icons.Default.ViewModule,
                            contentDescription = "Switch Grid",
                            tint = CyberNeonCyan
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Interactive Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search game, boss clip, or 4K tag...", color = CyberTextSecondary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyberNeonCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CyberTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberNeonCyan,
                        unfocusedBorderColor = CyberSurface,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Animated Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CaptureCategory.entries) { category ->
                        val selected = category == selectedCategory
                        val bgColor by animateColorAsState(
                            if (selected) CyberNeonCyan else CyberSurface,
                            label = "chipBg"
                        )
                        val txtColor by animateColorAsState(
                            if (selected) CyberBg else CyberTextPrimary,
                            label = "chipTxt"
                        )

                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                                .background(bgColor)
                                .border(
                                    1.dp,
                                    if (selected) CyberNeonCyan else CyberTextSecondary.copy(alpha = 0.2f),
                                    CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = category
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.label,
                                color = txtColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = filteredItems,
                key = { _, item -> item.id }
            ) { index, item ->
                GamerMediaCard(
                    item = item,
                    index = index,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectItem(item)
                    },
                    onFavoriteToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFavorite(item.id)
                    }
                )
            }
        }
    }
}

@Composable
fun GamerMediaCard(
    item: GamerMediaItem,
    index: Int,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Staggered Entrance Animation
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index % 8) * 45L)
        appeared = true
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )

    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "entranceScale"
    )

    val entranceAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(350),
        label = "entranceAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(item.aspectRatio)
            .graphicsLayer {
                scaleX = pressScale * entranceScale
                scaleY = pressScale * entranceScale
                alpha = entranceAlpha
            }
            .clip(CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            .background(CyberSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isPressed) listOf(CyberNeonCyan, CyberNeonMagenta)
                    else listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                ),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Image with Built-in Cyber Loading Animation
        SubcomposeAsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(CyberSurface),
                    contentAlignment = Alignment.Center
                ) {
                    CyberNeonSpinner(size = 34.dp)
                }
            }
        )

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, CyberBg.copy(alpha = 0.92f))
                    )
                )
        )

        // Top Telemetry Badges (FPS, HDR, Clip Duration)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberBg.copy(alpha = 0.78f))
                        .border(0.5.dp, CyberAcidGreen.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${item.fps} FPS",
                        color = CyberAcidGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (item.duration != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberNeonMagenta.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "▶ ${item.duration}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Favorite Star Button
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) CyberNeonMagenta else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Game & Capture Title
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = item.gameTitle.uppercase(),
                color = CyberNeonCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = item.title,
                color = CyberTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 4. INTERACTIVE GAMER LIGHTBOX (Pinch-Zoom, Drag-to-Dismiss, Telemetry HUD)
// ============================================================================
@Composable
fun GamerLightboxDetail(
    item: GamerMediaItem,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var showHud by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }

    val animatedDragOffset by animateFloatAsState(
        targetValue = verticalDragOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dragDismiss"
    )

    val backgroundAlpha = (1f - (abs(animatedDragOffset) / 900f)).coerceIn(0.2f, 0.96f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg.copy(alpha = backgroundAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showHud = !showHud },
                    onDoubleTap = {
                        if (zoomScale > 1f) {
                            zoomScale = 1f
                            offset = Offset.Zero
                        } else {
                            zoomScale = 2.4f
                        }
                    }
                )
            }
            .pointerInput(zoomScale) {
                if (zoomScale == 1f) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(verticalDragOffset) > 260f) {
                                onDismiss()
                            } else {
                                verticalDragOffset = 0f
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            verticalDragOffset += dragAmount
                        }
                    )
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                        offset += pan
                    }
                }
            }
    ) {
        // High-Res Capture Viewer
        SubcomposeAsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        // Toggleable Gamer Telemetry Overlay HUD
        AnimatedVisibility(
            visible = showHud && abs(verticalDragOffset) < 60f,
            enter = fadeIn() + slideInVertically { -40 },
            exit = fadeOut() + slideOutVertically { -40 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberSurface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CyberTextPrimary)
                }

                // Live Telemetry Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CyberSurface.copy(alpha = 0.85f))
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(item.resolution, color = CyberNeonCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("•", color = CyberTextSecondary)
                    Text("${item.fps} FPS", color = CyberAcidGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("•", color = CyberTextSecondary)
                    Text("GPU ${item.gpuTemp}", color = CyberAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberSurface.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) CyberNeonMagenta else CyberTextPrimary
                    )
                }
            }
        }

        // Bottom Telemetry & Quick Gamer Actions Panel
        AnimatedVisibility(
            visible = showHud && abs(verticalDragOffset) < 60f,
            enter = fadeIn() + slideInVertically { 80 },
            exit = fadeOut() + slideOutVertically { 80 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clip(CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp))
                    .background(CyberSurface.copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            item.gameTitle.uppercase(),
                            color = CyberNeonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            item.title,
                            color = CyberTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            item.bitrate,
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Quick Gamer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { /* Share to Discord / Squad */ },
                        modifier = Modifier.weight(1f),
                        shape = CutCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CyberBg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SQUAD SHARE", color = CyberBg, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { /* Set as Wallpaper / Export HUD */ },
                        modifier = Modifier.weight(1f),
                        shape = CutCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberNeonMagenta)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberNeonMagenta, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EXPORT LUT", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- SAMPLE GAMER MEDIA DATA ---
fun generateSampleCaptures(): List<GamerMediaItem> = listOf(
    GamerMediaItem(1, "Night City Rain Reflections", "Cyberpunk 2077", "https://picsum.photos/seed/cyber1/900/1200", CaptureCategory.SCREENSHOTS, 120, "3840x2160", "85 Mbps HEVC", null, true, true, 0.75f),
    GamerMediaItem(2, "Malenia No-Hit Phase 2 Parry", "Elden Ring", "https://picsum.photos/seed/elden2/900/700", CaptureCategory.CLIPS, 60, "2560x1440", "64 Mbps AV1", "00:42", true, false, 1.15f),
    GamerMediaItem(3, "Radiant Ace Clutch on Ascent", "Valorant", "https://picsum.photos/seed/val3/900/900", CaptureCategory.CLIPS, 240, "1920x1080", "110 Mbps RAW", "01:15", false, true, 1.0f),
    GamerMediaItem(4, "Platinum Trophy: Legend of the West", "Red Dead II", "https://picsum.photos/seed/rdr4/900/1100", CaptureCategory.ACHIEVEMENTS, 90, "3840x2160", "45 Mbps", null, true, false, 0.8f),
    GamerMediaItem(5, "Orbital Helldive Extraction Chaos", "Helldivers 2", "https://picsum.photos/seed/hell5/900/800", CaptureCategory.CLIPS, 120, "3440x1440", "95 Mbps", "00:28", true, true, 1.1f),
    GamerMediaItem(6, "Raytraced Neon Alleyway", "Alan Wake 2", "https://picsum.photos/seed/alan6/900/1250", CaptureCategory.SCREENSHOTS, 90, "3840x2160", "RAW PNG", null, true, false, 0.72f),
    GamerMediaItem(7, "Nürburgring Wet Lap Record", "Assetto Corsa", "https://picsum.photos/seed/race7/900/900", CaptureCategory.ACHIEVEMENTS, 144, "3840x2160", "120 Mbps", null, true, false, 1.0f),
    GamerMediaItem(8, "Boss Stagger Combo 999x", "Final Fantasy XVI", "https://picsum.photos/seed/ff8/900/1150", CaptureCategory.SCREENSHOTS, 120, "3840x2160", "78 Mbps", null, true, true, 0.78f)
)
