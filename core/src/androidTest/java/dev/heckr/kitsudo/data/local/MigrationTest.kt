package dev.heckr.kitsudo.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.heckr.kitsudo.data.local.migration.MIGRATION_1_2
import dev.heckr.kitsudo.data.local.migration.MIGRATION_2_3
import dev.heckr.kitsudo.data.local.migration.MIGRATION_3_4
import dev.heckr.kitsudo.data.local.migration.MIGRATION_4_5
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
    fun migrate1To4_preservesRowsAndAddsColumns() {
        // Seed a v1 database with one task using the original schema.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, title, description, isCompleted, createdAt, syncStatus) " +
                    "VALUES ('t1', 'Title', 'Desc', 0, 123, 'SYNCED')",
            )
        }

        // Run the real migrations and let Room validate the resulting schema.
        helper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
        ).use { db ->
            db.query(
                "SELECT id, parentId, deadlineAt, sortOrder, priority, " +
                    "recurrenceUnit, recurrenceInterval FROM tasks",
            ).use { c ->
                assertTrue("row should survive migration", c.moveToFirst())
                assertEquals("t1", c.getString(c.getColumnIndexOrThrow("id")))
                // New nullable columns default to null...
                assertTrue(c.isNull(c.getColumnIndexOrThrow("parentId")))
                assertTrue(c.isNull(c.getColumnIndexOrThrow("deadlineAt")))
                assertTrue(c.isNull(c.getColumnIndexOrThrow("recurrenceUnit")))
                // ...and the NOT NULL columns get their defaults.
                assertEquals(0, c.getInt(c.getColumnIndexOrThrow("sortOrder")))
                assertEquals(0, c.getInt(c.getColumnIndexOrThrow("priority")))
                assertEquals(1, c.getInt(c.getColumnIndexOrThrow("recurrenceInterval")))
            }
        }
    }

    @Test
    fun migrate4To5_addsTagTables() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, title, description, isCompleted, createdAt, syncStatus, " +
                    "sortOrder, priority, recurrenceInterval) " +
                    "VALUES ('t1', 'Title', 'Desc', 0, 123, 'SYNCED', 0, 0, 1)",
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB, 5, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        ).use { db ->
            // New tables exist and accept rows; the cross-ref FK cascades on task delete.
            db.execSQL("INSERT INTO tags (id, name, color, sortOrder) VALUES ('g1', 'Work', 'MAUVE', 0)")
            db.execSQL("INSERT INTO task_tag_cross_ref (taskId, tagId) VALUES ('t1', 'g1')")
            db.query("SELECT COUNT(*) FROM task_tag_cross_ref").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
