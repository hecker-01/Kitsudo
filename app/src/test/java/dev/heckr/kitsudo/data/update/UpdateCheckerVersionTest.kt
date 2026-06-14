package dev.heckr.kitsudo.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerVersionTest {

    @Test
    fun `higher patch is newer`() {
        assertTrue(UpdateChecker.isNewerVersion(current = "1.0.0", latest = "1.0.1"))
    }

    @Test
    fun `higher minor is newer`() {
        assertTrue(UpdateChecker.isNewerVersion(current = "1.0.9", latest = "1.1.0"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(UpdateChecker.isNewerVersion(current = "1.2.3", latest = "1.2.3"))
    }

    @Test
    fun `older latest is not newer`() {
        assertFalse(UpdateChecker.isNewerVersion(current = "2.0.0", latest = "1.9.9"))
    }

    @Test
    fun `dev suffix is ignored`() {
        assertFalse(UpdateChecker.isNewerVersion(current = "1.2.0-dev", latest = "1.2.0"))
        assertTrue(UpdateChecker.isNewerVersion(current = "1.2.0-dev", latest = "1.2.1"))
    }

    @Test
    fun `uneven segment counts compare correctly`() {
        assertTrue(UpdateChecker.isNewerVersion(current = "1.2", latest = "1.2.1"))
        assertFalse(UpdateChecker.isNewerVersion(current = "1.2.0", latest = "1.2"))
    }

    @Test
    fun `non-numeric segments do not crash`() {
        assertFalse(UpdateChecker.isNewerVersion(current = "abc", latest = "x.y.z"))
    }
}
