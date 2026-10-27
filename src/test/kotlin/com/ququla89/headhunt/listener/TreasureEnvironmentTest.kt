package com.ququla89.headhunt.listener

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TeamManager
import com.ququla89.headhunt.manager.TreasureManager
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreasureEnvironmentTest {
    private val treasures = mock(TreasureManager::class.java)
    private val game = GameManager(treasures, TeamManager())
    private val listener = TreasureListener(treasures, game)
    private val treasure = mock(Block::class.java)
    private val ordinary = mock(Block::class.java)

    init {
        `when`(treasures.isTreasureBlock(treasure)).thenReturn(true)
    }

    @Test
    fun `water cannot flow into a treasure but ordinary blocks remain unaffected`() {
        for (target in listOf(treasure, ordinary)) {
            val event = mock(BlockFromToEvent::class.java)
            `when`(event.toBlock).thenReturn(target)
            listener.onBlockFromTo(event)
            if (target === treasure) verify(event).isCancelled = true else verify(event, never()).isCancelled = true
        }
    }

    @Test
    fun `entity explosion removes only treasures from its destruction list`() {
        val event = mock(EntityExplodeEvent::class.java)
        val blocks = mutableListOf(treasure, ordinary)
        `when`(event.blockList()).thenReturn(blocks)
        listener.onEntityExplode(event)
        assertEquals(listOf(ordinary), blocks)
        verify(event, never()).isCancelled = true
    }

    @Test
    fun `block explosion removes only treasures from its destruction list`() {
        val event = mock(BlockExplodeEvent::class.java)
        val blocks = mutableListOf(ordinary, treasure)
        `when`(event.blockList()).thenReturn(blocks)
        listener.onBlockExplode(event)
        assertEquals(listOf(ordinary), blocks)
        verify(event, never()).isCancelled = true
    }

    @Test
    fun `extending piston stops only when its moved blocks contain a treasure`() {
        for (protected in listOf(false, true)) {
            val event = mock(BlockPistonExtendEvent::class.java)
            `when`(event.blocks).thenReturn(if (protected) listOf(ordinary, treasure) else listOf(ordinary))
            listener.onPistonExtend(event)
            if (protected) verify(event).isCancelled = true else verify(event, never()).isCancelled = true
        }
    }

    @Test
    fun `retracting piston stops only when its moved blocks contain a treasure`() {
        for (protected in listOf(false, true)) {
            val event = mock(BlockPistonRetractEvent::class.java)
            `when`(event.blocks).thenReturn(if (protected) listOf(treasure, ordinary) else listOf(ordinary))
            listener.onPistonRetract(event)
            if (protected) verify(event).isCancelled = true else verify(event, never()).isCancelled = true
        }
    }

    @Test
    fun `quit clears only the leaving players set mode`() {
        val player = mock(Player::class.java)
        val id = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val event = mock(PlayerQuitEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(player.uniqueId).thenReturn(id)
        game.setSetMode(id, true)
        game.setSetMode(otherId, true)
        listener.onPlayerQuit(event)
        assertFalse(game.isSetModeEnabled(id))
        assertTrue(game.isSetModeEnabled(otherId))
    }
}
