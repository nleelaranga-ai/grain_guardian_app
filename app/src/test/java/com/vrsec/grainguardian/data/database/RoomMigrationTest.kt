package com.vrsec.grainguardian.data.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.UUID

class RoomMigrationTest {

    @Test
    fun testMigrationVersionNumbers() {
        val migration = GrainGuardianDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun testMigration1To2AddsRequiredColumnsAndBackfillsUuids() {
        val executedSql = mutableListOf<String>()
        val updatedRows = mutableListOf<Pair<String, Long>>()

        // Mock cursor simulating 3 pre-existing records in v1 database
        val existingIds = listOf(101L, 102L, 103L)
        var cursorIndex = -1

        val mockCursor = Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "moveToNext" -> {
                    cursorIndex++
                    cursorIndex < existingIds.size
                }
                "getLong" -> {
                    val colIndex = args[0] as Int
                    if (colIndex == 0) existingIds[cursorIndex] else 0L
                }
                "close" -> null
                else -> null
            }
        } as Cursor

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    executedSql.add(sql)
                    if (sql.startsWith("UPDATE inspections SET uuid = ? WHERE id = ?")) {
                        val bindArgs = args[1] as Array<*>
                        val uuid = bindArgs[0] as String
                        val id = bindArgs[1] as Long
                        updatedRows.add(Pair(uuid, id))
                    }
                    null
                }
                "query" -> {
                    val sql = if (args[0] is String) args[0] as String else (args[0] as SupportSQLiteQuery).sql
                    executedSql.add("QUERY: $sql")
                    mockCursor
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        // Execute migration
        GrainGuardianDatabase.MIGRATION_1_2.migrate(mockDb)

        // 1. Verify schema additions
        assertTrue(executedSql.any { it.contains("ADD COLUMN uuid TEXT NOT NULL DEFAULT ''") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN deviceId TEXT DEFAULT NULL") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN sessionId TEXT DEFAULT NULL") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL'") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN source TEXT NOT NULL DEFAULT 'REAL'") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN ruleVersion TEXT DEFAULT NULL") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN statusCode TEXT DEFAULT NULL") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN reasonCode TEXT DEFAULT NULL") })

        // 2. Verify unique index creation
        assertTrue(executedSql.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_inspections_uuid ON inspections(uuid)") })

        // 3. Verify all existing v1 rows were backfilled with valid UUIDs
        assertEquals(3, updatedRows.size)
        assertEquals(listOf(101L, 102L, 103L), updatedRows.map { it.second })

        // Each backfilled row must have received a distinct, valid UUID
        val assignedUuids = updatedRows.map { it.first }.toSet()
        assertEquals(3, assignedUuids.size)
        for (uuidStr in assignedUuids) {
            assertNotNull(UUID.fromString(uuidStr))
        }
    }
}
