package com.ququla89.headhunt.manager

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.Skull
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
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

class TreasureManagerTest {
    @TempDir
    lateinit var tempDirectory: Path

    private val key = NamespacedKey("headhunt", "treasure_id")
    private val logger: Logger = Logger.getAnonymousLogger()

    @Test
    fun `register, findAt, and unregisterAt manage PDC and memory state`() {
        val world = mockWorld("world")
        val block = mockSkullBlock(world, 1, 64, 2, null)
        val skull = block.state as Skull
        val pdc = skull.persistentDataContainer
        var storedId: String? = null
        `when`(pdc.get(key, PersistentDataType.STRING)).thenAnswer { storedId }
        `when`(skull.update(true, false)).thenReturn(true)
        org.mockito.Mockito
            .doAnswer { inv ->
                storedId = inv.getArgument(2, String::class.java)
                null
            }.`when`(pdc)
            .set(
                org.mockito.ArgumentMatchers.eq(key),
                org.mockito.ArgumentMatchers.eq(PersistentDataType.STRING),
                org.mockito.ArgumentMatchers.anyString(),
            )
        org.mockito.Mockito
            .doAnswer {
                storedId = null
                null
            }.`when`(pdc)
            .remove(key)

        val location = Location(world, 1.0, 64.0, 2.0)
        `when`(location.block.location).thenReturn(location)
        val manager = TreasureManager(tempDirectory.resolve("treasures.yml").toFile(), key, logger, { world })

        val registered = manager.register(location)
        assertNotNull(registered)
        assertEquals(1, manager.size)
        assertEquals(registered, manager.findAt(block))
        assertNull(manager.register(location))

        val otherBlock = mockSkullBlock(world, 9, 64, 9, registered.id.toString())
        assertNull(manager.findAt(otherBlock))

        val removed = manager.unregisterAt(block)
        assertEquals(registered, removed)
        assertEquals(0, manager.size)
        assertNull(storedId)
        verify(pdc).remove(key)
    }

    @Test
    fun `validateAll reports duplicate, unloaded world, out-of-range y, non-skull, and mismatched id`() {
        val dupA = UUID.randomUUID()
        val dupB = UUID.randomUUID()
        val unloadedId = UUID.randomUUID()
        val outOfRangeId = UUID.randomUUID()
        val nonSkullId = UUID.randomUUID()
        val mismatchId = UUID.randomUUID()
        val validId = UUID.randomUUID()

        val dataFile = tempDirectory.resolve("treasures.yml")
        Files.writeString(
            dataFile,
            """
            treasures:
              $dupA:
                world: world
                x: 1
                y: 64
                z: 1
              $dupB:
                world: world
                x: 1
                y: 64
                z: 1
              $unloadedId:
                world: missing_world
                x: 2
                y: 64
                z: 2
              $outOfRangeId:
                world: world
                x: 3
                y: 500
                z: 3
              $nonSkullId:
                world: world
                x: 4
                y: 64
                z: 4
              $mismatchId:
                world: world
                x: 5
                y: 64
                z: 5
              $validId:
                world: world
                x: 6
                y: 64
                z: 6
            """.trimIndent(),
        )

        val world = mockWorld("world")
        mockSkullBlock(world, 1, 64, 1, dupA.toString())
        val nonSkullBlock = mock(Block::class.java)
        `when`(nonSkullBlock.state).thenReturn(mock(BlockState::class.java))
        `when`(world.getBlockAt(4, 64, 4)).thenReturn(nonSkullBlock)
        mockSkullBlock(world, 5, 64, 5, UUID.randomUUID().toString())
        mockSkullBlock(world, 6, 64, 6, validId.toString())

        val manager = TreasureManager(dataFile.toFile(), key, logger, { if (it == "world") world else null })
        manager.load()

        val issues = manager.validateAll()
        assertEquals(5, issues.size)
        assertEquals(setOf(dupB, unloadedId, outOfRangeId, nonSkullId, mismatchId), issues.map { it.treasure.id }.toSet())
    }

    @Test
    fun `clearAll removes valid and invalid blocks while retaining unloaded or failed updates`() {
        val validId = UUID.randomUUID()
        val unloadedId = UUID.randomUUID()
        val outOfRangeId = UUID.randomUUID()
        val nonTileId = UUID.randomUUID()
        val updateFailId = UUID.randomUUID()

        val dataFile = tempDirectory.resolve("treasures.yml")
        Files.writeString(
            dataFile,
            """
            treasures:
              $validId:
                world: world
                x: 1
                y: 64
                z: 1
              $unloadedId:
                world: missing_world
                x: 2
                y: 64
                z: 2
              $outOfRangeId:
                world: world
                x: 3
                y: -100
                z: 3
              $nonTileId:
                world: world
                x: 4
                y: 64
                z: 4
              $updateFailId:
                world: world
                x: 5
                y: 64
                z: 5
            """.trimIndent(),
        )

        val world = mockWorld("world")
        val validBlock = mockSkullBlock(world, 1, 64, 1, validId.toString())
        `when`((validBlock.state as Skull).update(true, false)).thenReturn(true)
        val nonTileBlock = mock(Block::class.java)
        `when`(nonTileBlock.state).thenReturn(mock(BlockState::class.java))
        `when`(world.getBlockAt(4, 64, 4)).thenReturn(nonTileBlock)
        val failBlock = mockSkullBlock(world, 5, 64, 5, updateFailId.toString())
        `when`((failBlock.state as Skull).update(true, false)).thenReturn(false)

        val manager = TreasureManager(dataFile.toFile(), key, logger, { if (it == "world") world else null })
        manager.load()

        val result = manager.clearAll()
        assertEquals(3, result.removed)
        assertEquals(setOf(unloadedId, updateFailId), result.retained.map { it.treasure.id }.toSet())
        assertEquals(2, manager.size)
        assertTrue(manager.contains(unloadedId))
        assertTrue(manager.contains(updateFailId))
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
        `when`(block.world).thenReturn(world)
        `when`(block.x).thenReturn(x)
        `when`(block.y).thenReturn(y)
        `when`(block.z).thenReturn(z)
        `when`(block.state).thenReturn(skull)
        `when`(world.getBlockAt(x, y, z)).thenReturn(block)
        `when`(world.getBlockAt(Location(world, x.toDouble(), y.toDouble(), z.toDouble()))).thenReturn(block)
        return block
    }
}
