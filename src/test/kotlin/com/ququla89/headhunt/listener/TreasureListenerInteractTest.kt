package com.ququla89.headhunt.listener

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TeamManager
import com.ququla89.headhunt.manager.TreasureCatalog
import com.ququla89.headhunt.manager.TreasureManager
import com.ququla89.headhunt.model.GameMode
import com.ququla89.headhunt.model.TeamMode
import com.ququla89.headhunt.model.Treasure
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreasureListenerInteractTest {
    @Test
    fun `onInteract cancels and warns when running player clicks unregistered marked skull`() {
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        val player = mockPlayer("QuQuLa89", admin = false)
        val block = mockSkullBlock()
        `when`(game.isRunning).thenReturn(true)
        `when`(treasures.readTreasureId(block)).thenReturn(UUID.randomUUID())
        `when`(treasures.findAt(block)).thenReturn(null)

        val event = interactEvent(player, block)
        TreasureListener(treasures, game).onInteract(event)

        assertTrue(event.isCancelled)
        verify(player).sendMessage("§6[HeadHunt] §r§cこの宝HEADは登録情報と一致しません。管理者に連絡してください。")
    }

    @Test
    fun `onInteract completes solo game and broadcasts completion`() {
        val treasureId = UUID.randomUUID()
        val treasure = Treasure(treasureId, "world", 1, 64, 1)
        val treasures = mock(TreasureManager::class.java)
        val block = mockSkullBlock()
        `when`(treasures.readTreasureId(block)).thenReturn(treasureId)
        `when`(treasures.findAt(block)).thenReturn(treasure)

        val catalog =
            object : TreasureCatalog {
                override val size: Int = 1

                override fun contains(id: UUID): Boolean = id == treasureId
            }
        val game = GameManager(catalog, TeamManager())
        game.start(GameMode.SOLO, TeamMode.SHARED)

        val player = mockPlayer("QuQuLa89", admin = false)
        val console = mock(ConsoleCommandSender::class.java)
        val event = interactEvent(player, block)

        Mockito.mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(player))
            bukkit.`when`<ConsoleCommandSender> { Bukkit.getConsoleSender() }.thenReturn(console)

            TreasureListener(treasures, game).onInteract(event)
        }

        assertTrue(event.isCancelled)
        assertFalse(game.isRunning)
        verify(player).sendMessage(org.mockito.ArgumentMatchers.any(Component::class.java))
        verify(console).sendMessage(org.mockito.ArgumentMatchers.any(Component::class.java))
    }

    @Test
    fun `onInteract unregisters registered skull when setmode admin sneak right-clicks`() {
        val treasure = Treasure(UUID.randomUUID(), "world", 2, 64, 2)
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        val player = mockPlayer("AdminUser", admin = true)
        val block = mockSkullBlock()

        `when`(player.isSneaking).thenReturn(true)
        `when`(game.isSetModeEnabled(player.uniqueId)).thenReturn(true)
        `when`(treasures.readTreasureId(block)).thenReturn(treasure.id)
        `when`(treasures.findAt(block)).thenReturn(treasure)
        `when`(treasures.unregisterAt(block)).thenReturn(treasure)
        `when`(treasures.save()).thenReturn(true)

        val event = interactEvent(player, block)
        TreasureListener(treasures, game).onInteract(event)

        assertTrue(event.isCancelled)
        verify(treasures).unregisterAt(block)
        verify(treasures).save()
        verify(player).sendMessage("§6[HeadHunt] §r§a宝HEADの登録を解除しました。")
    }

    private fun mockPlayer(
        name: String,
        admin: Boolean,
    ): Player =
        mock(Player::class.java).apply {
            `when`(this.name).thenReturn(name)
            `when`(uniqueId).thenReturn(UUID.randomUUID())
            `when`(hasPermission("headhunt.admin")).thenReturn(admin)
        }

    private fun mockSkullBlock(): Block =
        mock(Block::class.java).apply {
            `when`(state).thenReturn(mock(Skull::class.java))
        }

    private fun interactEvent(
        player: Player,
        block: Block,
    ): PlayerInteractEvent {
        val item = mock(ItemStack::class.java)
        `when`(item.type).thenReturn(Material.AIR)
        return PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, block, BlockFace.UP, EquipmentSlot.HAND)
    }
}
