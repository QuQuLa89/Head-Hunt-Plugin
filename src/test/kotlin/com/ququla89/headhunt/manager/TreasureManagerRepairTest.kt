package com.ququla89.headhunt.manager

import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Skull
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TreasureManagerRepairTest {
    @TempDir
    lateinit var tempDirectory: Path

    private val key = NamespacedKey("headhunt", "treasure_id")
    private val logger: Logger = Logger.getAnonymousLogger()

    @Test
    fun `repairAll keeps entry matching block PDC when duplicate coordinates exist`() {
        val staleId = UUID.randomUUID()
        val matchingId = UUID.randomUUID()
        val dataFile = writeDuplicateYaml(staleId, matchingId)

        val world = mockWorld("world")
        val block = mockSkullBlock(world, 10, 64, 20, matchingId.toString())
        val skull = block.state as Skull
        val manager = TreasureManager(dataFile.toFile(), key, logger, { if (it == "world") world else null })
        manager.load()

        val result = manager.repairAll()

        assertEquals(0, result.repaired)
        assertEquals(1, result.removed)
        assertTrue(result.unresolved.isEmpty())
        assertNull(manager.find(staleId))
        assertNotNull(manager.find(matchingId))
        assertEquals(1, manager.size)
        verify(skull, never()).update(true, false)
    }

    @Test
    fun `repairAll removes all duplicates when skull block is missing`() {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val dataFile = writeDuplicateYaml(firstId, secondId)

        val world = mockWorld("world")
        val block = mock(Block::class.java)
        `when`(world.getBlockAt(10, 64, 20)).thenReturn(block)
        `when`(block.state).thenReturn(mock(org.bukkit.block.BlockState::class.java))
        val manager = TreasureManager(dataFile.toFile(), key, logger, { if (it == "world") world else null })
        manager.load()

        val result = manager.repairAll()

        assertEquals(0, result.repaired)
        assertEquals(2, result.removed)
        assertTrue(result.unresolved.isEmpty())
        assertEquals(0, manager.size)
    }

    @Test
    fun `repairAll keeps first entry and rewrites block PDC when none match`() {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val dataFile = writeDuplicateYaml(firstId, secondId)

        val world = mockWorld("world")
        val block = mockSkullBlock(world, 10, 64, 20, otherId.toString())
        val skull = block.state as Skull
        `when`(skull.update(true, false)).thenReturn(true)
        val manager = TreasureManager(dataFile.toFile(), key, logger, { if (it == "world") world else null })
        manager.load()

        val result = manager.repairAll()

        assertEquals(1, result.repaired)
        assertEquals(1, result.removed)
        assertTrue(result.unresolved.isEmpty())
        assertNotNull(manager.find(firstId))
        assertNull(manager.find(secondId))
        verify(skull.persistentDataContainer).set(key, PersistentDataType.STRING, firstId.toString())
        verify(skull).update(true, false)
    }

    private fun writeDuplicateYaml(
        firstId: UUID,
        secondId: UUID,
    ): Path {
        val dataFile = tempDirectory.resolve("treasures.yml")
        Files.writeString(
            dataFile,
            """
            treasures:
              $firstId:
                world: world
                x: 10
                y: 64
                z: 20
              $secondId:
                world: world
                x: 10
                y: 64
                z: 20
            """.trimIndent(),
        )
        return dataFile
    }

    private fun mockWorld(name: String): World =
        mock(World::class.java).apply {
            `when`(this.name).thenReturn(name)
            `when`(minHeight).thenReturn(-64)
            `when`(maxHeight).thenReturn(320)
        }

    private fun mockSkullBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        markerId: String?,
    ): Block {
        val pdc = mock(PersistentDataContainer::class.java)
        `when`(pdc.get(key, PersistentDataType.STRING)).thenReturn(markerId)
        val skull = mock(Skull::class.java)
        `when`(skull.persistentDataContainer).thenReturn(pdc)
        val block = mock(Block::class.java)
        `when`(block.state).thenReturn(skull)
        `when`(world.getBlockAt(x, y, z)).thenReturn(block)
        return block
    }
}
