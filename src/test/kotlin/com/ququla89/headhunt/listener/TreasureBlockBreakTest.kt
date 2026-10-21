package com.ququla89.headhunt.listener

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TreasureManager
import com.ququla89.headhunt.model.Treasure
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

class TreasureBlockBreakTest {
    @Test
    fun `only idle admins can remove marked treasure including stale markers`() {
        for (registered in listOf(false, true)) {
            for (running in listOf(false, true)) {
                for (admin in listOf(false, true)) {
                    val treasures = mock(TreasureManager::class.java)
                    val game = mock(GameManager::class.java)
                    val block = mock(Block::class.java)
                    val player = mock(Player::class.java)
                    `when`(treasures.isTreasureBlock(block)).thenReturn(true)
                    if (registered) {
                        val treasure = mock(Treasure::class.java)
                        `when`(treasures.findAt(block)).thenReturn(treasure)
                        `when`(treasures.unregisterAt(block)).thenReturn(treasure)
                    }
                    `when`(treasures.save()).thenReturn(true)
                    `when`(game.isRunning).thenReturn(running)
                    `when`(player.hasPermission("headhunt.admin")).thenReturn(admin)
                    val event = BlockBreakEvent(block, player)

                    TreasureListener(treasures, game).onBlockBreak(event)

                    val allowed = !running && admin
                    assertEquals(!allowed, event.isCancelled)
                    if (allowed) {
                        verify(treasures).unregisterAt(block)
                        verify(treasures).save()
                    } else {
                        verify(treasures, never()).unregisterAt(block)
                        verify(treasures, never()).save()
                    }
                }
            }
        }
    }

    @Test
    fun `ordinary blocks remain breakable by nonadmins`() {
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        val block = mock(Block::class.java)
        val player = mock(Player::class.java)
        val event = BlockBreakEvent(block, player)
        TreasureListener(treasures, game).onBlockBreak(event)
        assertEquals(false, event.isCancelled)
        verify(treasures, never()).unregisterAt(block)
        verifyNoInteractions(game, player)
    }
}
