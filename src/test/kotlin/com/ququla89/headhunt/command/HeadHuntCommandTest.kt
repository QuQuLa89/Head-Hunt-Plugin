package com.ququla89.headhunt.command

import com.ququla89.headhunt.manager.GameManager
import com.ququla89.headhunt.manager.TeamManager
import com.ququla89.headhunt.manager.TreasureClearResult
import com.ququla89.headhunt.manager.TreasureManager
import com.ququla89.headhunt.manager.TreasureValidationIssue
import com.ququla89.headhunt.model.GameMode
import com.ququla89.headhunt.model.TeamMode
import com.ququla89.headhunt.model.Treasure
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test

class HeadHuntCommandTest {
    @Test
    fun `setmode on is rejected while game is running`() {
        val treasures = mock(TreasureManager::class.java)
        val game = mock(GameManager::class.java)
        `when`(game.isRunning).thenReturn(true)
        val player = mockAdminPlayer()
        val stack = stackOf(player)

        HeadHuntCommand(treasures, TeamManager(), game).execute(stack, arrayOf("setmode", "on"))

        verify(game, never()).setSetMode(player.uniqueId, true)
    }

    @Test
    fun `deleteall succeeds within 30 seconds and times out after 30 seconds`() {
        var now = 1_000L
        val treasures = mock(TreasureManager::class.java)
        `when`(treasures.clearAll()).thenReturn(TreasureClearResult(2, emptyList()))
        `when`(treasures.save()).thenReturn(true)
        val game = mock(GameManager::class.java)
        `when`(game.isRunning).thenReturn(false)
        val player = mockAdminPlayer()
        val stack = stackOf(player)
        val command = HeadHuntCommand(treasures, TeamManager(), game, clock = { now })

        command.execute(stack, arrayOf("deleteall"))
        now += 10_000L
        command.execute(stack, arrayOf("deleteall", "confirm"))
        verify(treasures).clearAll()
        verify(treasures).save()

        val treasuresTimedOut = mock(TreasureManager::class.java)
        val commandTimedOut = HeadHuntCommand(treasuresTimedOut, TeamManager(), game, clock = { now })
        commandTimedOut.execute(stack, arrayOf("deleteall"))
        now += 31_000L
        commandTimedOut.execute(stack, arrayOf("deleteall", "confirm"))
        verify(treasuresTimedOut, never()).clearAll()
    }

    @Test
    fun `start aborts when validation reports issues`() {
        val treasures = mock(TreasureManager::class.java)
        val issue = TreasureValidationIssue(Treasure(UUID.randomUUID(), "world", 1, 64, 1), "テストエラー")
        `when`(treasures.validateAll()).thenReturn(listOf(issue))
        val game = mock(GameManager::class.java)
        val player = mockAdminPlayer()
        val stack = stackOf(player)

        HeadHuntCommand(treasures, TeamManager(), game).execute(stack, arrayOf("start", "solo"))

        verify(treasures).validateAll()
        verify(player).sendMessage(contains("問題があるため開始できません"))
        verify(game, never()).start(GameMode.SOLO, TeamMode.SHARED)
    }

    private fun mockAdminPlayer(): Player =
        mock(Player::class.java).apply {
            `when`(uniqueId).thenReturn(UUID.randomUUID())
            `when`(hasPermission("headhunt.admin")).thenReturn(true)
            `when`(hasPermission("headhunt.use")).thenReturn(true)
        }

    private fun stackOf(player: Player): CommandSourceStack =
        mock(CommandSourceStack::class.java).apply {
            `when`(sender).thenReturn(player)
        }
}
