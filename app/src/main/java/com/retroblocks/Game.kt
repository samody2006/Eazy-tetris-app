package com.retroblocks

enum class GameStatus { READY, PLAYING, PAUSED, GAME_OVER, CLEARING }
enum class PieceType(val color: Int) {
    I(0xFF00D9FF.toInt()), O(0xFFFFD21F.toInt()), T(0xFFB45CFF.toInt()),
    S(0xFF45D483.toInt()), Z(0xFFFF5263.toInt()), J(0xFF4D7CFF.toInt()), L(0xFFFF9D3D.toInt())
}

data class Cell(val x: Int, val y: Int)
data class ActivePiece(val type: PieceType, val x: Int, val y: Int, val rotation: Int)
data class GameSnapshot(
    val status: GameStatus, val cells: List<PieceType?>, val active: ActivePiece?,
    val ghostY: Int?, val next: PieceType, val hold: PieceType?, val canHold: Boolean,
    val score: Int, val level: Int, val totalLines: Int, val linesInLevel: Int,
    val combo: Int, val clearingRows: List<Int> = emptyList()
)

private val shapes = mapOf(
    PieceType.I to listOf(listOf(Cell(0,1),Cell(1,1),Cell(2,1),Cell(3,1)), listOf(Cell(2,0),Cell(2,1),Cell(2,2),Cell(2,3))),
    PieceType.O to listOf(listOf(Cell(0,0),Cell(1,0),Cell(0,1),Cell(1,1))),
    PieceType.T to listOf(listOf(Cell(1,0),Cell(0,1),Cell(1,1),Cell(2,1)), listOf(Cell(1,0),Cell(1,1),Cell(2,1),Cell(1,2)), listOf(Cell(0,1),Cell(1,1),Cell(2,1),Cell(1,2)), listOf(Cell(1,0),Cell(0,1),Cell(1,1),Cell(1,2))),
    PieceType.S to listOf(listOf(Cell(1,0),Cell(2,0),Cell(0,1),Cell(1,1)), listOf(Cell(1,0),Cell(1,1),Cell(2,1),Cell(2,2))),
    PieceType.Z to listOf(listOf(Cell(0,0),Cell(1,0),Cell(1,1),Cell(2,1)), listOf(Cell(2,0),Cell(2,1),Cell(1,1),Cell(1,2))),
    PieceType.J to listOf(listOf(Cell(0,0),Cell(0,1),Cell(1,1),Cell(2,1)), listOf(Cell(1,0),Cell(2,0),Cell(1,1),Cell(1,2)), listOf(Cell(0,1),Cell(1,1),Cell(2,1),Cell(2,2)), listOf(Cell(1,0),Cell(1,1),Cell(0,2),Cell(1,2))),
    PieceType.L to listOf(listOf(Cell(2,0),Cell(0,1),Cell(1,1),Cell(2,1)), listOf(Cell(1,0),Cell(1,1),Cell(1,2),Cell(2,2)), listOf(Cell(0,1),Cell(1,1),Cell(2,1),Cell(0,2)), listOf(Cell(0,0),Cell(1,0),Cell(1,1),Cell(1,2)))
)

class SevenBag(private val forced: List<PieceType>? = null) {
    private val queue = ArrayDeque<PieceType>()
    private var forcedIndex = 0
    fun next(): PieceType {
        forced?.let { if (forcedIndex < it.size) return it[forcedIndex++] }
        if (queue.isEmpty()) queue.addAll(PieceType.entries.shuffled())
        return queue.removeFirst()
    }
}

class TetrisGame(private val generator: SevenBag = SevenBag()) {
    companion object { const val WIDTH = 10; const val HEIGHT = 20 }
    private val board = Array<PieceType?>(WIDTH * HEIGHT) { null }
    private var piece: ActivePiece? = null
    private var nextPiece = generator.next()
    private var holdPiece: PieceType? = null
    private var canHold = true
    private var clearingRows = emptyList<Int>()
    var status = GameStatus.READY; private set
    var score = 0; private set
    var level = 1; private set
    var totalLines = 0; private set
    var linesInLevel = 0; private set
    var combo = -1; private set

    fun start(initialLevel: Int = 1) {
        board.fill(null); score = 0; level = initialLevel; totalLines = 0; linesInLevel = 0; combo = -1
        holdPiece = null; canHold = true; clearingRows = emptyList(); nextPiece = generator.next()
        status = GameStatus.PLAYING; spawn()
    }
    fun pause() { if (status == GameStatus.PLAYING) status = GameStatus.PAUSED }
    fun resume() { if (status == GameStatus.PAUSED) status = GameStatus.PLAYING }
    fun togglePause() { if (status == GameStatus.PAUSED) resume() else pause() }
    fun move(dx: Int, dy: Int = 0): Boolean { val p = piece ?: return false; val candidate = p.copy(x = p.x + dx, y = p.y + dy); if (!collides(candidate)) { piece = candidate; return true }; if (dy > 0) lock(); return false }
    fun rotate() { val p = piece ?: return; val nextRotation = (p.rotation + 1) % shapes.getValue(p.type).size; for (kick in 0..2) { val candidate = p.copy(rotation = nextRotation, x = p.x + if (kick == 0) 0 else if (kick == 1) -kick else kick); if (!collides(candidate)) { piece = candidate; return } } }
    fun tick() { if (status == GameStatus.PLAYING) move(0, 1) }
    fun softDrop(): Boolean { return move(0, 1) }
    fun hardDrop() { while (move(0, 1)); }
    fun hold() {
        if (!canHold || piece == null || status != GameStatus.PLAYING) return
        val current = piece!!.type
        val replacement = holdPiece
        holdPiece = current
        canHold = false
        if (replacement == null) spawn() else {
            piece = ActivePiece(replacement, 3, 0, 0)
            if (collides(piece!!)) { piece = null; status = GameStatus.GAME_OVER }
        }
    }

    fun getGhostY(): Int? {
        val p = piece ?: return null
        var y = p.y
        while (!collides(p.copy(y = y + 1))) {
            y++
        }
        return y
    }

    fun snapshot() = GameSnapshot(status, board.toList(), piece, getGhostY(), nextPiece, holdPiece, canHold, score, level, totalLines, linesInLevel, combo, clearingRows)
    fun cellsFor(p: ActivePiece): List<Cell> = shapes.getValue(p.type)[p.rotation].map { Cell(p.x + it.x, p.y + it.y) }
    fun gravityMs(): Long = when (level) { 1 -> 1200; 2 -> 1100; 3 -> 1000; 4 -> 900; 5 -> 820; 6 -> 750; 7 -> 680; 8 -> 610; 9 -> 550; 10 -> 500; 11 -> 450; 12 -> 400; 13 -> 360; 14 -> 320; 15 -> 280; 16 -> 250; else -> 220 }

    private fun collides(p: ActivePiece): Boolean = cellsFor(p).any { it.x !in 0 until WIDTH || it.y >= HEIGHT || (it.y >= 0 && board[it.y * WIDTH + it.x] != null) }
    private fun spawn() { val candidate = ActivePiece(nextPiece, 3, 0, 0); nextPiece = generator.next(); piece = candidate; if (collides(candidate)) { piece = null; status = GameStatus.GAME_OVER } }
    private fun lock() {
        val p = piece ?: return
        cellsFor(p).forEach { if (it.y >= 0) board[it.y * WIDTH + it.x] = p.type }
        piece = null
        
        val fullRows = (0 until HEIGHT).filter { y -> (0 until WIDTH).all { x -> board[y * WIDTH + x] != null } }
        if (fullRows.isNotEmpty()) {
            combo = if (combo < 0) 0 else combo + 1
            clearingRows = fullRows
            status = GameStatus.CLEARING
        } else {
            combo = -1
            spawn()
        }
    }

    fun finishClear() {
        if (status != GameStatus.CLEARING) return
        val count = clearingRows.size
        val rows = board.toList().chunked(WIDTH)
        val remaining = rows.filterIndexed { y, _ -> y !in clearingRows }
        
        board.fill(null)
        remaining.forEachIndexed { row, cells -> 
            cells.forEachIndexed { x, value -> 
                board[(HEIGHT - remaining.size + row) * WIDTH + x] = value 
            } 
        }
        
        val base = listOf(0, 5, 15, 30, 60)[count]
        score += base * level + (if (combo > 0) combo * 50 * level else 0)
        totalLines += count
        linesInLevel += count
        
        if (linesInLevel >= 20) {
            level++
            linesInLevel = 0
            board.fill(null)
        }
        
        clearingRows = emptyList()
        status = GameStatus.PLAYING
        spawn()
    }

    fun restore(saved: GameSnapshot) {
        board.fill(null)
        saved.cells.take(board.size).forEachIndexed { index, value -> board[index] = value }
        status = saved.status
        piece = saved.active
        nextPiece = saved.next
        holdPiece = saved.hold
        canHold = saved.canHold
        score = saved.score
        level = saved.level
        totalLines = saved.totalLines
        linesInLevel = saved.linesInLevel
        combo = saved.combo
        clearingRows = saved.clearingRows
    }
}
