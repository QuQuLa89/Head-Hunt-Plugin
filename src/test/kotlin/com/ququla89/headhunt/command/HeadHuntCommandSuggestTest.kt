package com.ququla89.headhunt.command

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TeamManager
import com.ququla89.headhunt.manager.TreasureManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

class HeadHuntCommandSuggestTest {
    @Test
    fun `suggest matches mixed-case team names regardless of prefix case`() {
        val teams =
            TeamManager().apply {
                create("Red")
                create("Blue")
            }
        val command = HeadHuntCommand(mock(TreasureManager::class.java), teams, mock(GameManager::class.java))
        val stack = adminStack()

        assertEquals(listOf("Red"), command.suggest(stack, arrayOf("team", "join", "r")).toList())
        assertEquals(listOf("Red"), command.suggest(stack, arrayOf("team", "join", "R")).toList())
        assertEquals(listOf("Blue"), command.suggest(stack, arrayOf("team", "delete", "b")).toList())
        assertEquals(listOf("Red"), command.suggest(stack, arrayOf("team", "info", "r")).toList())
    }

    @Test
    fun `suggest matches mixed-case online player names regardless of prefix case`() {
        val teams = TeamManager().apply { create("Red") }
        val command = HeadHuntCommand(mock(TreasureManager::class.java), teams, mock(GameManager::class.java))
        val stack = adminStack()
        val player = mock(Player::class.java)
        `when`(player.name).thenReturn("QuQuLa89")

        Mockito.mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(player))

            assertEquals(listOf("QuQuLa89"), command.suggest(stack, arrayOf("team", "leave", "q")).toList())
            assertEquals(listOf("QuQuLa89"), command.suggest(stack, arrayOf("team", "leave", "Q")).toList())
            assertEquals(listOf("QuQuLa89"), command.suggest(stack, arrayOf("team", "join", "Red", "q")).toList())
            assertEquals(listOf("QuQuLa89"), command.suggest(stack, arrayOf("team", "join", "Red", "Q")).toList())
        }
    }

    private fun adminStack(): CommandSourceStack {
        val sender = mock(CommandSender::class.java)
        `when`(sender.hasPermission("headhunt.admin")).thenReturn(true)
        `when`(sender.hasPermission("headhunt.use")).thenReturn(true)
        val stack = mock(CommandSourceStack::class.java)
        `when`(stack.sender).thenReturn(sender)
        return stack
    }
}
