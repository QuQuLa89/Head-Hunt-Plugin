package com.ququla89.headhunt.listener

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TreasureManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class TreasureListenerSetModeTest {
    @Test
    fun `onBlockPlace cancels and warns when placing duplicated registered treasure head`() {
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        val player = mockAdminPlayer()
        val location = mock(Location::class.java)
        val block = mockSkullBlock(location)
        val duplicatedId = UUID.randomUUID()

        `when`(game.isSetModeEnabled(player.uniqueId)).thenReturn(true)
        `when`(treasures.readTreasureId(block)).thenReturn(duplicatedId)
        `when`(treasures.contains(duplicatedId)).thenReturn(true)

        val item = mockItem(Material.PLAYER_HEAD)
        val event =
            BlockPlaceEvent(
                block,
                mock(BlockState::class.java),
                block,
                item,
                player,
                true,
                EquipmentSlot.HAND,
            )

        TreasureListener(treasures, game).onBlockPlace(event)

        assertTrue(event.isCancelled)
        verify(player).sendMessage(anyString())
        verify(treasures, never()).register(location)
    }

    @Test
    fun `onInteract cancels and warns when registering duplicated registered treasure head`() {
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        val player = mockAdminPlayer()
        val location = mock(Location::class.java)
        val block = mockSkullBlock(location)
        val duplicatedId = UUID.randomUUID()

        val emptyHand = mockItem(Material.AIR)
        val inventory = mock(PlayerInventory::class.java)
        `when`(inventory.itemInMainHand).thenReturn(emptyHand)
        `when`(player.inventory).thenReturn(inventory)
        `when`(game.isSetModeEnabled(player.uniqueId)).thenReturn(true)
        `when`(treasures.readTreasureId(block)).thenReturn(duplicatedId)
        `when`(treasures.findAt(block)).thenReturn(null)
        `when`(treasures.contains(duplicatedId)).thenReturn(true)

        val event =
            PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                emptyHand,
                block,
                BlockFace.UP,
                EquipmentSlot.HAND,
            )

        TreasureListener(treasures, game).onInteract(event)

        assertTrue(event.isCancelled)
        verify(player).sendMessage(anyString())
        verify(treasures, never()).register(location)
    }

    private fun mockAdminPlayer(): Player =
        mock(Player::class.java).apply {
            `when`(uniqueId).thenReturn(UUID.randomUUID())
            `when`(hasPermission("headhunt.admin")).thenReturn(true)
        }

    private fun mockSkullBlock(location: Location): Block =
        mock(Block::class.java).apply {
            `when`(state).thenReturn(mock(Skull::class.java))
            `when`(this.location).thenReturn(location)
        }

    private fun mockItem(material: Material): ItemStack =
        mock(ItemStack::class.java).apply {
            `when`(type).thenReturn(material)
        }
}
