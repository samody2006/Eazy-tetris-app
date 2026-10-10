package com.retroblocks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    @Test fun sevenBagContainsEachPieceExactlyOnce() {
        val bag = SevenBag()
        val first = (1..7).map { bag.next() }
        assertEquals(PieceType.entries.toSet(), first.toSet())
        assertEquals(7, first.size)
    }

    @Test fun levelSpeedGetsFaster() {
        val game = TetrisGame()
        assertTrue(game.gravityMs() > 220)
        assertEquals(1200L, game.gravityMs())
    }

    @Test fun forcedSequenceIsDeterministic() {
        val bag = SevenBag(listOf(PieceType.I, PieceType.T, PieceType.O))
        assertEquals(PieceType.I, bag.next())
        assertEquals(PieceType.T, bag.next())
        assertEquals(PieceType.O, bag.next())
    }
}
