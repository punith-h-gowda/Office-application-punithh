package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Macro
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.data.TerminalLog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusControllerApp(viewModel: MacroViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val bluetoothConnected by viewModel.bluetoothConnected.collectAsStateWithLifecycle()
    val runningMacroId by viewModel.runningMacroId.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    var showManageMacrosModal by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NexusBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header bar
            NexusHeader(
                bluetoothConnected = bluetoothConnected,
                onBluetoothToggle = { viewModel.setBluetoothConnected(!bluetoothConnected) },
                onManageMacrosClick = { showManageMacrosModal = true }
            )

            // Dynamic screen switching
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "Macros" -> {
                            MacrosScreen(
                                macros = macros,
                                runningMacroId = runningMacroId,
                                viewModel = viewModel
                            )
                        }
                        "Keys" -> {
                            KeysScreen(
                                macros = macros,
                                viewModel = viewModel
                            )
                        }
                        "Terminal" -> {
                            TerminalScreen(
                                logs = logs,
                                viewModel = viewModel,
                                onSendCommand = { viewModel.executeTerminalCommand(it) },
                                onClearLogs = { viewModel.clearTerminalLogs() }
                            )
                        }
                    }
                }
            }
        }

        if (showManageMacrosModal) {
            ManageMacrosDialog(
                macros = macros,
                viewModel = viewModel,
                onDismiss = { showManageMacrosModal = false }
            )
        }
    }
}

@Composable
fun NexusHeader(
    bluetoothConnected: Boolean,
    onBluetoothToggle: () -> Unit,
    onManageMacrosClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pulsating indicator
                val infiniteTransition = rememberInfiniteTransition(label = "PulsatingGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "GlowAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (bluetoothConnected) {
                                NexusPrimary.copy(alpha = glowAlpha)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = glowAlpha)
                            }
                        )
                )

                Text(
                    text = if (bluetoothConnected) "BLUETOOTH HID ACTIVE" else "BLUETOOTH OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bluetoothConnected) NexusPrimary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("bluetooth_status_label")
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Nexus Controller",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings Overlay Toggle
            IconButton(
                onClick = onManageMacrosClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("manage_macros_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Manage macro configurations",
                    tint = NexusPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Connection Action Button with high-fidelity glow
            val btInfiniteTransition = rememberInfiniteTransition(label = "BluetoothButtonGlow")
            val glowRadius by if (bluetoothConnected) {
                btInfiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 6.dp.value,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "GlowRadius"
                )
            } else {
                remember { mutableStateOf(0f) }
            }
            val glowAlpha2 by if (bluetoothConnected) {
                btInfiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.45f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "GlowAlpha2"
                )
            } else {
                remember { mutableStateOf(0f) }
            }

            Box(contentAlignment = Alignment.Center) {
                if (bluetoothConnected) {
                    Box(
                        modifier = Modifier
                            .size(42.dp + (glowRadius * 2).dp)
                            .clip(CircleShape)
                            .background(NexusPrimary.copy(alpha = glowAlpha2))
                    )
                }

                IconButton(
                    onClick = onBluetoothToggle,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (bluetoothConnected) NexusPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (bluetoothConnected) NexusPrimary else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .testTag("bluetooth_toggle_button")
                ) {
                    Icon(
                        imageVector = if (bluetoothConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = "Toggle Bluetooth Connection",
                        tint = if (bluetoothConnected) NexusPrimary else NexusSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MacrosScreen(
    macros: List<Macro>,
    runningMacroId: Int?,
    viewModel: MacroViewModel
) {
    val currentIndex by viewModel.currentMacroIndex.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (macros.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Empty Preset",
                    tint = NexusSecondary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Macros Stored",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Navigate to the 'Keys' tab to create or initialize preset macro routines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Safety index validation
    val activeIndex = if (currentIndex < macros.size) currentIndex else 0

    // Initialize pager state
    val pagerState = rememberPagerState(
        initialPage = activeIndex,
        pageCount = { macros.size }
    )

    // Sync external activeIndex changes with pager
    LaunchedEffect(activeIndex) {
        if (pagerState.currentPage != activeIndex) {
            pagerState.animateScrollToPage(activeIndex)
        }
    }

    // Sync pager swipe changes back to ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setMacroIndex(pagerState.currentPage)
    }

    // We use a Column layout to structure the pager and the virtual keyboard beneath it
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Carousel Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("macros_pager")
            ) { pageIndex ->
                val macro = macros[pageIndex]

                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                val absOffset = pageOffset.absoluteValue
                
                // Credit card style visual transformation
                val scale = 1f - (absOffset * 0.12f).coerceIn(0f, 0.12f)
                val alpha = 1f - (absOffset * 0.25f).coerceIn(0f, 0.25f)
                val rotationY = pageOffset * -15f
                val translationX = pageOffset * 24.dp.value * LocalDensity.current.density

                // Main controller active card
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.rotationY = rotationY
                            this.translationX = translationX
                            cameraDistance = 8 * density
                        }
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(40.dp))
                        .padding(28.dp)
                        .testTag("macro_card_${macro.id}"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Massive background number overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        val formattedNum = String.format("%02d", pageIndex + 1)
                        Text(
                            text = formattedNum,
                            fontSize = 110.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 110.sp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header category details
                        Column {
                            Text(
                                text = macro.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = NexusPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Title with custom line breaks for long titles
                            val spacedTitle = macro.name.replace(" ", "\n")
                            Text(
                                text = spacedTitle,
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Middle area displaying credential payloads and parameters
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            val showAccessKey = rememberStateFlowOf(macro.id, false)

                            // Account
                            Column {
                                Text(
                                    text = "TARGET ACCOUNT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexusSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = macro.targetAccount,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Key sequence mask
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ACCESS HASH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NexusSecondary
                                    )
                                    Icon(
                                        imageVector = if (showAccessKey.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle credentials security mask",
                                        tint = NexusSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { showAccessKey.value = !showAccessKey.value }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (showAccessKey.value) macro.accessKey else "•••••••••••••",
                                    style = if (showAccessKey.value) {
                                        MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontSize = 16.sp, letterSpacing = 0.5.sp)
                                    } else {
                                        MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontSize = 18.sp, letterSpacing = 2.sp)
                                    },
                                    color = if (showAccessKey.value) MaterialTheme.colorScheme.onSurface else NexusSecondary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Interactive Run Macro Button and Carousel Nav Controls inside the card
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Swipe navigation controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Arrow controller switches
                                IconButton(
                                    onClick = {
                                        val next = if (pageIndex == 0) macros.size - 1 else pageIndex - 1
                                        scope.launch {
                                            pagerState.animateScrollToPage(next)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .testTag("prev_macro_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous macro card",
                                        tint = NexusPrimary
                                    )
                                }

                                // Pagination indicator dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    macros.forEachIndexed { i, _ ->
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .width(if (i == pageIndex) 20.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(if (i == pageIndex) NexusPrimary else MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val next = if (pageIndex == macros.size - 1) 0 else pageIndex + 1
                                        scope.launch {
                                            pagerState.animateScrollToPage(next)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .testTag("next_macro_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next macro card",
                                        tint = NexusPrimary
                                    )
                                }
                            }

                            // Prominent Execute Button inside the swipable card
                            val isRunning = runningMacroId == macro.id
                            Button(
                                onClick = { viewModel.runMacro(macro) },
                                enabled = !isRunning,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexusPrimary,
                                    contentColor = NexusOnPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = NexusSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .testTag("run_macro_button"),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isRunning) {
                                        CircularProgressIndicator(
                                            color = NexusPrimary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "EXECUTING MACRO...",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 2.sp
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = "RUN MACRO",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 2.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(NexusOnPrimary)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 1.dp)

        // Compact Virtual Alphanumeric Keyboard Section beneath the carousel
        VirtualKeyboardLayout(
            onKeyClick = { key ->
                viewModel.sendKeystroke(key)
            }
        )
    }
}

@Composable
fun KeysScreen(
    macros: List<Macro>,
    viewModel: MacroViewModel
) {
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED MACROS",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexusSecondary
                )

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = NexusPrimary
                    ),
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("open_add_macro_dialog"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "NEW MACRO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("macros_list")
            ) {
                items(macros, key = { it.id }) { macro ->
                    MacroItemRow(
                        macro = macro,
                        onEdit = { editingMacro = macro },
                        onDelete = { viewModel.deleteMacro(macro) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddEditMacroDialog(
                onDismiss = { showAddDialog = false },
                onSave = { _, name, category, target, key, steps, icon ->
                    viewModel.insertMacro(name, category, target, key, steps, icon)
                    showAddDialog = false
                }
            )
        }

        if (editingMacro != null) {
            AddEditMacroDialog(
                macro = editingMacro,
                onDismiss = { editingMacro = null },
                onSave = { id, name, category, target, key, steps, icon ->
                    viewModel.updateMacro(
                        Macro(
                            id = id,
                            name = name,
                            category = category,
                            targetAccount = target,
                            accessKey = key,
                            steps = steps,
                            iconType = icon,
                            isSystemPreset = editingMacro?.isSystemPreset ?: false
                        )
                    )
                    editingMacro = null
                }
            )
        }
    }
}

@Composable
fun MacroItemRow(
    macro: Macro,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("macro_row_${macro.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon identifier
            val iconVec = when (macro.iconType.lowercase()) {
                "outlook" -> Icons.Default.Email
                "github" -> Icons.Default.Code
                "jira" -> Icons.Default.Task
                "terminal" -> Icons.Default.Terminal
                else -> Icons.Default.Layers
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVec,
                    contentDescription = null,
                    tint = NexusPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusPrimary
                )
                Text(
                    text = macro.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${macro.stepsList.size} instruction steps • ${macro.targetAccount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_macro_${macro.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit macro configuration",
                    tint = NexusPrimary
                )
            }

            if (!macro.isSystemPreset) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_macro_${macro.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete custom macro",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRESET",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditMacroDialog(
    macro: Macro? = null,
    onDismiss: () -> Unit,
    onSave: (id: Int, name: String, category: String, targetAccount: String, accessKey: String, steps: String, iconType: String) -> Unit
) {
    var name by remember { mutableStateOf(macro?.name ?: "") }
    var category by remember { mutableStateOf(macro?.category ?: "") }
    var targetAccount by remember { mutableStateOf(macro?.targetAccount ?: "") }
    var accessKey by remember { mutableStateOf(macro?.accessKey ?: "") }
    var steps by remember { mutableStateOf(macro?.steps ?: "") }
    var iconType by remember { mutableStateOf(macro?.iconType?.uppercase() ?: "CUSTOM") }

    val iconOptions = listOf("OUTLOOK", "GITHUB", "JIRA", "TERMINAL", "CUSTOM")
    var expandedIconMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_edit_macro_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (macro == null) "STORE NEW MACRO" else "EDIT MACRO PROFILE",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )

                // Macro Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Macro Name (e.g. GITHUB SYNC)") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_macro_name")
                )

                // Macro Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. UTILITY MACRO 02)") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_macro_category")
                )

                // Target Account
                OutlinedTextField(
                    value = targetAccount,
                    onValueChange = { targetAccount = it },
                    label = { Text("Target Account Login") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_target_account")
                )

                // Access Key
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    label = { Text("Access Key Hash") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_access_key")
                )

                // Steps
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("Steps (semicolon separated; e.g. step_one;step_two)") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("input_macro_steps")
                )

                // Icon selection dropdown simulation
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedIconMenu = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusPrimary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_icon_button")
                    ) {
                        Text(text = "Icon Type: $iconType")
                    }
                    DropdownMenu(
                        expanded = expandedIconMenu,
                        onDismissRequest = { expandedIconMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        iconOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    iconType = opt
                                    expandedIconMenu = false
                                }
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_save_macro")
                    ) {
                        Text("CANCEL", color = NexusSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && steps.isNotBlank()) {
                                val cat = if (category.isBlank()) "CUSTOM MACRO" else category
                                onSave(macro?.id ?: 0, name, cat, targetAccount, accessKey, steps, iconType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary, contentColor = NexusOnPrimary),
                        modifier = Modifier.testTag("confirm_save_macro")
                    ) {
                        Text("SAVE CONFIG", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageMacrosDialog(
    macros: List<Macro>,
    viewModel: MacroViewModel,
    onDismiss: () -> Unit
) {
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
                .padding(16.dp)
                .testTag("manage_macros_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANAGE MACROS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )

                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .testTag("manage_add_macro_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Macro", tint = NexusPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("manage_macros_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(macros) { macro ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = macro.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = macro.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexusPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { editingMacro = macro },
                                    modifier = Modifier.testTag("edit_macro_button_${macro.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Macro", tint = NexusPrimary)
                                }

                                if (!macro.isSystemPreset) {
                                    IconButton(
                                        onClick = { viewModel.deleteMacro(macro) },
                                        modifier = Modifier.testTag("delete_macro_button_${macro.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Macro",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_manage_macros")
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold, color = NexusSecondary)
                }
            }
        }
    }

    if (editingMacro != null) {
        AddEditMacroDialog(
            macro = editingMacro,
            onDismiss = { editingMacro = null },
            onSave = { id, name, category, targetAccount, accessKey, steps, iconType ->
                viewModel.updateMacro(
                    Macro(
                        id = id,
                        name = name,
                        category = category,
                        targetAccount = targetAccount,
                        accessKey = accessKey,
                        steps = steps,
                        iconType = iconType,
                        isSystemPreset = editingMacro?.isSystemPreset ?: false
                    )
                )
                editingMacro = null
            }
        )
    }

    if (showCreateDialog) {
        AddEditMacroDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { _, name, category, targetAccount, accessKey, steps, iconType ->
                viewModel.insertMacro(name, category, targetAccount, accessKey, steps, iconType)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun TerminalScreen(
    logs: List<TerminalLog>,
    viewModel: MacroViewModel,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var commandInput by remember { mutableStateOf("") }
    var rawInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Auto-scroll to top (newest is first in our logs query or reversed layout)
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active shell window
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(16.dp)
                .testTag("terminal_window")
        ) {
            // Console toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                    Text(
                        text = "NEXUS SHELL CONSOLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Console",
                        tint = NexusSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Monospace Log feed
            LazyColumn(
                state = listState,
                reverseLayout = true, // Shows newest at bottom
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("terminal_logs_feed")
            ) {
                items(logs) { log ->
                    val logColor = when (log.logType) {
                        "SUCCESS" -> Color.Green
                        "ERROR" -> MaterialTheme.colorScheme.error
                        "INPUT" -> NexusInputLog
                        else -> NexusTerminalLog
                    }
                    val prefix = when (log.logType) {
                        "INPUT" -> "nexus> "
                        "ERROR" -> "[ERR] "
                        "SUCCESS" -> "[OK] "
                        else -> "[INF] "
                    }

                    Text(
                        text = "$prefix${log.message}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = logColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "nexus> ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = NexusInputLog,
                    fontWeight = FontWeight.Bold
                )

                BasicTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(Color.Green),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank()) {
                                onSendCommand(commandInput)
                                commandInput = ""
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_command_input")
                )

                if (commandInput.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSendCommand(commandInput)
                            commandInput = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send terminal command",
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Dedicated Terminal Mode (Raw Keystrokes testing) section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("terminal_mode_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "RAW HID TRANSMITTER (TERMINAL MODE)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = NexusPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = it },
                        placeholder = { Text("Enter raw keyboard sequence...", fontSize = 13.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("raw_keystrokes_input")
                    )

                    Button(
                        onClick = {
                            viewModel.sendRawKeystrokeSequence(rawInput)
                            rawInput = ""
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusPrimary,
                            contentColor = NexusOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("send_keystrokes_button")
                    ) {
                        Text(text = "SEND KEYSTROKES", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun NexusBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    // Custom bottom row mimicking the Design HTML's elegant border and active state indicators
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding() // Safely padding system navigation gesture bar/pill
    ) {
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusNavItem(
                label = "Macros",
                icon = Icons.Outlined.Layers,
                activeIcon = Icons.Default.Layers,
                isSelected = currentTab == "Macros",
                onClick = { onTabSelected("Macros") }
            )
            NexusNavItem(
                label = "Keys",
                icon = Icons.Outlined.Keyboard,
                activeIcon = Icons.Default.Keyboard,
                isSelected = currentTab == "Keys",
                onClick = { onTabSelected("Keys") }
            )
            NexusNavItem(
                label = "Terminal",
                icon = Icons.Outlined.Terminal,
                activeIcon = Icons.Default.Terminal,
                isSelected = currentTab == "Terminal",
                onClick = { onTabSelected("Terminal") }
            )
        }
    }
}

@Composable
fun NexusNavItem(
    label: String,
    icon: ImageVector,
    activeIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Custom ripple or simple click
                onClick = onClick
            )
            .testTag("tab_button_$label"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val backgroundAnim by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            animationSpec = tween(250),
            label = "BgAnim"
        )

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(backgroundAnim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else icon,
                contentDescription = label,
                tint = if (isSelected) NexusPrimary else NexusSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NexusPrimary else NexusSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun VirtualKeyboardLayout(onKeyClick: (String) -> Unit) {
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("virtual_keyboard"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RAPID DIRECT INPUT INTERFACE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            color = NexusSecondary,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            row1.forEach { char ->
                KeyButton(char, weight = 1f, onClick = { onKeyClick(char) })
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            row2.forEach { char ->
                KeyButton(char, weight = 1f, onClick = { onKeyClick(char) })
            }
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            row3.forEach { char ->
                KeyButton(char, weight = 1f, onClick = { onKeyClick(char) })
            }
        }

        // Row 4 (Space, Backspace, Enter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            KeyButton("SPACE", weight = 2f, onClick = { onKeyClick(" ") })
            KeyButton("BACKSPACE", weight = 1.5f, onClick = { onKeyClick("BACKSPACE") })
            KeyButton("ENTER", weight = 1.5f, onClick = { onKeyClick("ENTER") })
        }
    }
}

@Composable
fun RowScope.KeyButton(
    label: String,
    weight: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("key_button_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (label.length > 1) 10.sp else 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helper to handle multiple toggles or state flows per id without sharing
@Composable
fun rememberStateFlowOf(id: Any, initialValue: Boolean): MutableState<Boolean> {
    return remember(id) { mutableStateOf(initialValue) }
}

