package dev.heckr.kitsudo.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.heckr.kitsudo.data.local.migration.MIGRATION_1_2
import dev.heckr.kitsudo.data.local.migration.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KitsudoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To3_preservesRowsAndAddsColumns() {
        // Seed a v1 database with one task using the original schema.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, title, description, isCompleted, createdAt, syncStatus) " +
                    "VALUES ('t1', 'Title', 'Desc', 0, 123, 'SYNCED')",
            )
        }

        // Run the real migrations and let Room validate the resulting schema.
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            db.query("SELECT id, parentId, deadlineAt, sortOrder, priority FROM tasks").use { c ->
                assertTrue("row should survive migration", c.moveToFirst())
                assertEquals("t1", c.getString(c.getColumnIndexOrThrow("id")))
                // New nullable columns default to null...
                assertTrue(c.isNull(c.getColumnIndexOrThrow("parentId")))
                assertTrue(c.isNull(c.getColumnIndexOrThrow("deadlineAt")))
                // ...and the NOT NULL columns get their defaults.
                assertEquals(0, c.getInt(c.getColumnIndexOrThrow("sortOrder")))
                assertEquals(0, c.getInt(c.getColumnIndexOrThrow("priority")))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
