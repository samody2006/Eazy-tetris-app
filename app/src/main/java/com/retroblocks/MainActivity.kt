package com.retroblocks

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { 
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { EazyTetrisApp() } 
    }
}

internal class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val game = TetrisGame()
    private val haptics = HapticManager(application)
    private val sounds = SoundManager(application)
    val music = MusicManager(application)
    private val _state = MutableStateFlow(game.snapshot())
    val state: StateFlow<GameSnapshot> = _state.asStateFlow()
    private var loop: Job? = null

    init {
        GameStateStore.load(application)?.let {
            game.restore(it)
            _state.value = game.snapshot()
        }
    }

    fun hasSavedGame(): Boolean = GameStateStore.load(getApplication()) != null
    
    fun start(initialLevel: Int = 1) { GameStateStore.clear(getApplication()); game.start(initialLevel); publish(); startLoop() }
    fun continueGame() {
        if (game.status == GameStatus.PAUSED) game.resume()
        publish()
        startLoop()
    }

    private fun startLoop() { loop?.cancel(); loop = viewModelScope.launch { while (true) { 
        val s = game.snapshot()
        if (s.status == GameStatus.CLEARING) {
            delay(300)
            game.finishClear()
        } else {
            delay(game.gravityMs())
            game.tick()
        }
        publish()
        if (game.status == GameStatus.GAME_OVER) break 
    } } }
    
    fun action(block: TetrisGame.() -> Unit) { 
        if (game.status == GameStatus.PLAYING) { 
            val oldX = game.snapshot().active?.x
            val oldY = game.snapshot().active?.y
            val oldRot = game.snapshot().active?.rotation
            
            game.block()
            
            val new = game.snapshot().active
            if (new != null) {
                if (new.x != oldX || new.y != oldY) {
                    sounds.play("move")
                } else if (new.rotation != oldRot) {
                    sounds.play("rotate")
                }
            }
            publish() 
        } 
    }
    
    fun pause() { game.togglePause(); publish() }
    
    private fun publish() { 
        val old = _state.value
        val new = game.snapshot()
        
        if (new.status == GameStatus.GAME_OVER && old.status != GameStatus.GAME_OVER) {
            sounds.play("game_over")
        } else if (new.totalLines > old.totalLines) {
            val cleared = new.totalLines - old.totalLines
            if (cleared >= 4) sounds.play("tetris") else sounds.play("line_clear")
            haptics.vibrate(100)
            if (new.level > old.level) sounds.play("level_up")
        } else if (new.active == null && old.active != null && new.status == GameStatus.PLAYING) {
            sounds.play("lock")
            haptics.vibrate(40)
        }
        
        if (new.status == GameStatus.GAME_OVER || new.status == GameStatus.READY) GameStateStore.clear(getApplication())
        else GameStateStore.save(getApplication(), new)
        _state.value = new 
    }
    
    override fun onCleared() {
        super.onCleared()
        sounds.release()
        music.release()
    }
}

private enum class Screen { MENU, GAME, SCORES, SETTINGS, ABOUT }
private val bg = Color(0xFF070B18)
private val panel = Color(0xFF111A31)

@Composable
internal fun EazyTetrisApp(vm: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var screen by remember { mutableStateOf(Screen.MENU) }
    var selectedLevel by remember { mutableStateOf(1) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                vm.pause()
                vm.music.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                vm.music.resume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(screen) {
        if (screen == Screen.GAME) {
            vm.music.play(MusicManager.MusicType.GAME)
        } else {
            vm.music.play(MusicManager.MusicType.MENU)
        }
    }

    val state by vm.state.collectAsState()
    var nameDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(SettingsManager.get(context).playerName) }
    MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = Color(0xFF00D9FF))) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            Box(Modifier.fillMaxSize().background(bg)) {
                when (screen) {
                    Screen.MENU -> MenuScreen(
                        { vm.start(selectedLevel); screen = Screen.GAME },
                        { vm.continueGame(); screen = Screen.GAME },
                        { screen = Screen.SCORES }, 
                        { screen = Screen.SETTINGS }, 
                        { screen = Screen.ABOUT },
                        selectedLevel,
                        vm.hasSavedGame(),
                        { if (it in 1..15) selectedLevel = it }
                    )
                    Screen.SCORES -> ScoresScreen { screen = Screen.MENU }
                    Screen.SETTINGS -> SettingsScreen({ screen = Screen.MENU }, { vm.music.updateSettings() })
                    Screen.ABOUT -> AboutScreen { screen = Screen.MENU }
                    Screen.GAME -> GameScreen(state, vm, { screen = Screen.MENU }, { if (Scores.qualifies(context, state.score)) { name = SettingsManager.get(context).playerName; nameDialog = true } })
                }
                if (nameDialog) AlertDialog(onDismissRequest = { nameDialog = false }, title = { Text("NEW HIGH SCORE") }, text = { OutlinedTextField(name, { name = it.take(15) }, label = { Text("Your name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (name.trim().isNotEmpty()) { Scores.save(context, name.trim(), state); SettingsManager.save(context, SettingsManager.get(context).copy(playerName = name.trim())); nameDialog = false } }) { Text("SAVE") } })
            }
        }
    }
}

@Composable internal fun MenuScreen(play: () -> Unit, continueGame: () -> Unit, scores: () -> Unit, settings: () -> Unit, about: () -> Unit, level: Int, hasSavedGame: Boolean, onLevelChange: (Int) -> Unit) = Column(Modifier.fillMaxSize().padding(28.dp).statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("EAZY", style = MaterialTheme.typography.displayLarge, color = Color(0xFF00D9FF))
            Text("TETRIS", style = MaterialTheme.typography.displayMedium, color = Color(0xFFFFD21F))
        }
    }
    Spacer(Modifier.height(30.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onLevelChange(level - 1) }) { Text("-", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        Text("START LEVEL: $level", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = { onLevelChange(level + 1) }) { Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
    }
    
    Spacer(Modifier.height(10.dp))
    BigButton("PLAY", play)
    if (hasSavedGame) BigButton("CONTINUE", continueGame)
    BigButton("HIGH SCORES", scores)
    BigButton("SETTINGS", settings)
    BigButton("ABOUT", about)
}

@Composable internal fun BigButton(label: String, click: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = click, 
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp).height(54.dp), 
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = panel, contentColor = Color(0xFF00D9FF)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) { 
        Text(label, style = MaterialTheme.typography.titleMedium) 
    } 
}

@Composable internal fun GameScreen(s: GameSnapshot, vm: GameViewModel, quit: () -> Unit, qualify: () -> Unit) {
    var submitted by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val moveThreshold = 40f
    
    BackHandler { if (s.status == GameStatus.PLAYING) vm.pause() else quit() }
    
    LaunchedEffect(s.status) { if (s.status == GameStatus.GAME_OVER && !submitted) { submitted = true; qualify() } }
    
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp).statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Preview("HOLD", s.hold, enabled = s.canHold)
            Stat("SCORE", s.score.toString())
            Stat("LEVEL", s.level.toString())
            Stat("LINES", s.linesInLevel.toString()) 
            Preview("NEXT", s.next)
        }
        
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Board(s, Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { vm.action { rotate() } }
            }.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                        if (kotlin.math.abs(dragX) > moveThreshold) {
                            vm.action { move(if (dragX > 0) 1 else -1) }
                            dragX = 0f
                        }
                        if (dragY > moveThreshold) {
                            vm.action { softDrop() }
                            dragY = 0f
                        }
                    }
                )
            })
            
            if (s.status == GameStatus.GAME_OVER) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GAME OVER", color = Color(0xFFFF5263), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { vm.start(s.level) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = bg)) { Text("RESTART") }
                            Button(onClick = quit, colors = ButtonDefaults.buttonColors(containerColor = panel, contentColor = Color.White)) { Text("MENU") }
                        }
                    }
                }
            } else if (s.status == GameStatus.PAUSED) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Text("PAUSED", color = Color(0xFFFFD21F), style = MaterialTheme.typography.displaySmall)
                }
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
            Control("◀") { vm.action { move(-1) } }
            Control("↻") { vm.action { rotate() } }
            Control("▶") { vm.action { move(1) } }
            Control("▼") { vm.action { softDrop() } } 
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton("HOLD", { vm.action { hold() } }, Modifier.weight(1f))
            BigButton(if (s.status == GameStatus.PAUSED) "START" else "PAUSE", { vm.pause() }, Modifier.weight(1f))
            BigButton("MENU", quit, Modifier.weight(1f))
        }
    }
}

@Composable internal fun Stat(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Color.LightGray); Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge) } }
@Composable private fun Preview(label: String, type: PieceType?, enabled: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (enabled) Color.LightGray else Color.Gray, style = MaterialTheme.typography.bodySmall)
        Box(Modifier.size(46.dp).padding(top = 2.dp).background(panel), contentAlignment = Alignment.Center) {
            type?.let { MiniPiece(it, enabled) }
        }
    }
}

@Composable private fun MiniPiece(type: PieceType, enabled: Boolean) {
    Canvas(Modifier.fillMaxSize().padding(5.dp)) {
        val cells = TetrisGame().cellsFor(ActivePiece(type, 0, 0, 0))
        val cell = minOf(size.width, size.height) / 4f
        val minX = cells.minOf { it.x }
        val minY = cells.minOf { it.y }
        cells.forEach { cellData ->
            drawRect(Color(type.color).copy(alpha = if (enabled) 1f else .35f), Offset((cellData.x - minX) * cell, (cellData.y - minY) * cell), androidx.compose.ui.geometry.Size(cell - 1, cell - 1))
        }
    }
}
@Composable internal fun RowScope.Control(label: String, click: () -> Unit) { 
    Button(
        onClick = click, 
        modifier = Modifier.weight(1f).height(56.dp), 
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = panel, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) { 
        Text(label, style = MaterialTheme.typography.headlineSmall) 
    } 
}

@Composable internal fun Board(s: GameSnapshot, modifier: Modifier) { Canvas(modifier.background(Color(0xFF0D1428))) {
    val cellSize = minOf(size.width / TetrisGame.WIDTH, size.height / TetrisGame.HEIGHT); val ox = (size.width - cellSize * TetrisGame.WIDTH) / 2; val oy = (size.height - cellSize * TetrisGame.HEIGHT) / 2
    
    fun drawCell(x: Int, y: Int, color: Color, isClearing: Boolean = false, ghost: Boolean = false) {
        if (y >= 0) { 
            val left = ox + x * cellSize; val top = oy + y * cellSize
            val drawColor = if (isClearing) Color.White else color
            
            if (ghost) {
                drawRect(color, Offset(left + 2, top + 2), androidx.compose.ui.geometry.Size(cellSize - 4, cellSize - 4), style = Stroke(2f))
                return
            }
            // Main block
            drawRect(drawColor, Offset(left + 1, top + 1), androidx.compose.ui.geometry.Size(cellSize - 2, cellSize - 2))
            
            if (!isClearing) {
                // Retro highlights
                // Top highlight
                drawRect(Color.White.copy(alpha = 0.3f), Offset(left + 2, top + 2), androidx.compose.ui.geometry.Size(cellSize - 4, 2f))
                // Left highlight
                drawRect(Color.White.copy(alpha = 0.3f), Offset(left + 2, top + 2), androidx.compose.ui.geometry.Size(2f, cellSize - 4))
                // Bottom shadow
                drawRect(Color.Black.copy(alpha = 0.3f), Offset(left + 2, top + cellSize - 4), androidx.compose.ui.geometry.Size(cellSize - 4, 2f))
                // Right shadow
                drawRect(Color.Black.copy(alpha = 0.3f), Offset(left + cellSize - 4, top + 2), androidx.compose.ui.geometry.Size(2f, cellSize - 4))
                
                drawRect(Color.White.copy(alpha = .1f), Offset(left + 1, top + 1), androidx.compose.ui.geometry.Size(cellSize - 2, cellSize - 2), style = Stroke(1f)) 
            }
        } 
    }
    
    s.cells.forEachIndexed { i, type -> 
        val x = i % TetrisGame.WIDTH
        val y = i / TetrisGame.WIDTH
        type?.let { drawCell(x, y, Color(it.color), isClearing = y in s.clearingRows) } 
    }
    
    s.active?.let { p ->
        s.ghostY?.let { ghostY ->
            TetrisGame().cellsFor(p.copy(y = ghostY)).forEach { drawCell(it.x, it.y, Color(p.type.color).copy(alpha = .55f), ghost = true) }
        }
        TetrisGame().cellsFor(p).forEach { drawCell(it.x, it.y, Color(p.type.color)) }
    }
    
    for (x in 0..TetrisGame.WIDTH) drawLine(Color.White.copy(alpha = .07f), Offset(ox + x * cellSize, oy), Offset(ox + x * cellSize, oy + cellSize * TetrisGame.HEIGHT))
    for (y in 0..TetrisGame.HEIGHT) drawLine(Color.White.copy(alpha = .07f), Offset(ox, oy + y * cellSize), Offset(ox + cellSize * TetrisGame.WIDTH, oy + y * cellSize))
} }

@Composable internal fun ScoresScreen(back: () -> Unit) { 
    BackHandler { back() }
    val context = LocalContext.current
    val scores = remember { Scores.load(context) }

    Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding().navigationBarsPadding()) { 
        Text("HIGH SCORES", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00D9FF))
        Spacer(Modifier.height(12.dp))
        Text("Scores are saved locally on this device.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        if (scores.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO HIGH SCORES YET\nPLAY AND SET A NEW RECORD!",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(45.dp), contentAlignment = Alignment.CenterStart) {
                    Text("RANK", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Text("NAME", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(75.dp), contentAlignment = Alignment.CenterEnd) {
                    Text("SCORE", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.width(35.dp), contentAlignment = Alignment.CenterEnd) {
                    Text("LV", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.width(45.dp), contentAlignment = Alignment.CenterEnd) {
                    Text("LINES", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(1.dp).fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)))
            Spacer(Modifier.height(8.dp))

            // Scores list with scrolling
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                scores.forEachIndexed { index, score ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank
                        Box(modifier = Modifier.width(45.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = "#${index + 1}",
                                color = when (index) {
                                    0 -> Color(0xFFFFD21F) // Gold
                                    1 -> Color(0xFFC0C0C0) // Silver
                                    2 -> Color(0xFFCD7F32) // Bronze
                                    else -> Color.LightGray
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Name
                        Text(
                            text = score.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Score
                        Box(modifier = Modifier.width(75.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = score.score.toString(),
                                color = Color(0xFF00D9FF),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Level
                        Box(modifier = Modifier.width(35.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${score.level}",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Lines
                        Box(modifier = Modifier.width(45.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${score.lines}",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        BigButton("BACK", back) 
    } 
}

@Composable internal fun SettingsScreen(back: () -> Unit, onMusicChange: () -> Unit) {
    val context = LocalContext.current
    BackHandler { back() }
    var settings by remember { mutableStateOf(SettingsManager.get(context)) }
    Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding().navigationBarsPadding()) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00D9FF)); Spacer(Modifier.height(20.dp))
        OutlinedTextField(settings.playerName, { settings = settings.copy(playerName = it.take(15)) }, label = { Text("Player Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        SettingToggle("Sound Effects", settings.sound) { settings = settings.copy(sound = it) }
        SettingToggle("Music", settings.music) { 
            settings = settings.copy(music = it)
            SettingsManager.save(context, settings)
            onMusicChange()
        }
        SettingToggle("Vibration", settings.vibration) { settings = settings.copy(vibration = it) }
        SettingToggle("Hard Drop", settings.hardDrop) { settings = settings.copy(hardDrop = it) }
        Spacer(Modifier.weight(1f))
        BigButton("SAVE & BACK", { SettingsManager.save(context, settings); back() })
    }
}

@Composable private fun SettingToggle(label: String, value: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Switch(value, onCheckedChange)
    }
}

@Composable internal fun AboutScreen(back: () -> Unit) {
    BackHandler { back() }
    Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding().navigationBarsPadding()) {
        Text("ABOUT", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00D9FF)); Spacer(Modifier.height(20.dp))
        Text("EAZY TETRIS", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text("Version 1.0.0", color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text("A classic Nintendo-inspired falling block game for Android. Built with Jetpack Compose.", color = Color.White)
        Spacer(Modifier.weight(1f))
        BigButton("BACK", back)
    }
}

internal data class Settings(val playerName: String, val sound: Boolean, val music: Boolean, val vibration: Boolean, val hardDrop: Boolean)

private object GameStateStore {
    private const val PREFS = "saved_game"
    private const val KEY = "state"

    fun save(context: Context, s: GameSnapshot) {
        val board = s.cells.joinToString("") { it?.ordinal?.let { ordinal -> ('0'.code + ordinal).toChar().toString() } ?: "." }
        val active = s.active?.let { listOf(it.type.ordinal, it.x, it.y, it.rotation).joinToString(",") } ?: "-"
        val hold = s.hold?.ordinal?.toString() ?: "-"
        val rows = s.clearingRows.joinToString(",")
        val value = listOf(board, active, s.next.ordinal, hold, s.canHold, s.score, s.level, s.totalLines, s.linesInLevel, s.combo, s.status.ordinal, rows).joinToString(";")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
    }

    fun load(context: Context): GameSnapshot? {
        val fields = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.split(";") ?: return null
        if (fields.size != 12) return null
        return runCatching {
            val cells = fields[0].map { char -> if (char == '.') null else PieceType.entries[char.code - '0'.code] }
            val activeParts = fields[1].takeUnless { it == "-" }?.split(",")
            val active = activeParts?.let { ActivePiece(PieceType.entries[it[0].toInt()], it[1].toInt(), it[2].toInt(), it[3].toInt()) }
            val hold = fields[3].takeUnless { it == "-" }?.toInt()?.let { PieceType.entries[it] }
            val status = GameStatus.entries[fields[10].toInt()]
            GameSnapshot(status, cells, active, null, PieceType.entries[fields[2].toInt()], hold, fields[4].toBoolean(), fields[5].toInt(), fields[6].toInt(), fields[7].toInt(), fields[8].toInt(), fields[9].toInt(), fields[11].takeIf { it.isNotEmpty() }?.split(",")?.map { it.toInt() } ?: emptyList())
        }.getOrNull()
    }

    fun clear(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply() }
}

internal object SettingsManager {
    fun get(context: Context): Settings {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return Settings(p.getString("name", "PLAYER") ?: "PLAYER", p.getBoolean("sound", true), p.getBoolean("music", false), p.getBoolean("vibration", true), p.getBoolean("hardDrop", false))
    }
    fun save(context: Context, s: Settings) = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        .putString("name", s.playerName).putBoolean("sound", s.sound).putBoolean("music", s.music).putBoolean("vibration", s.vibration).putBoolean("hardDrop", s.hardDrop).apply()
}

internal data class HighScore(val name: String, val score: Int, val level: Int, val lines: Int)

private object Scores {
    fun load(context: Context): List<HighScore> {
        val p = context.getSharedPreferences("scores", Context.MODE_PRIVATE)
        val items = p.getStringSet("items", emptySet()) ?: emptySet()
        return items.mapNotNull {
            val parts = it.split('|')
            if (parts.size == 4) {
                HighScore(
                    name = parts[0],
                    score = parts[1].toIntOrNull() ?: 0,
                    level = parts[2].toIntOrNull() ?: 0,
                    lines = parts[3].toIntOrNull() ?: 0
                )
            } else null
        }.sortedByDescending { it.score }
    }

    fun qualifies(context: Context, score: Int): Boolean {
        val values = context.getSharedPreferences("scores", Context.MODE_PRIVATE).getStringSet("items", emptySet())!!
            .mapNotNull { it.split('|').getOrNull(1)?.toIntOrNull() }.sortedDescending()
        return values.size < 10 || score >= (values.lastOrNull() ?: 0)
    }

    fun save(context: Context, name: String, s: GameSnapshot) { val p = context.getSharedPreferences("scores", Context.MODE_PRIVATE); val old = p.getStringSet("items", emptySet())!!.toMutableSet(); old.add(listOf(name, s.score, s.level, s.totalLines).joinToString("|")); val sorted = old.mapNotNull { it.split('|').takeIf { x -> x.size == 4 } }.sortedByDescending { it[1].toIntOrNull() ?: 0 }.take(10).map { it.joinToString("|") }.toSet(); p.edit().putStringSet("items", sorted).apply() }
}
