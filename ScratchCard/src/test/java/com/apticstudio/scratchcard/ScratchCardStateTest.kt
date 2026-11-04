package com.apticstudio.scratchcard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScratchCardStateTest {

    private lateinit var state: ScratchCardState

    @Before
    fun setUp() {
        state = ScratchCardState()
    }

    @Test
    fun initialState_isDefault() {
        assertEquals(0f, state.progress, 0.001f)
        assertFalse(state.isCleared)
    }

    @Test
    fun clear_setsIsClearedAndProgress() {
        state.clear()
        assertTrue(state.isCleared)
        assertEquals(1f, state.progress, 0.001f)
    }

    @Test
    fun reset_resetsStateToInitial() {
        state.clear()
        assertTrue(state.isCleared)

        state.reset()
        assertFalse(state.isCleared)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun updateProgress_calculatesProgressCorrectly() {
        val containerSize = Size(400f, 400f)
        val strokeRadius = 40f

        val initialProgress = state.progress
        assertEquals(0f, initialProgress, 0.001f)

        val newProgress = state.updateProgress(
            point = Offset(200f, 200f),
            strokeRadiusPx = strokeRadius,
            size = containerSize
        )

        assertTrue(newProgress > 0f)
        assertEquals(newProgress, state.progress, 0.001f)
    }
}
