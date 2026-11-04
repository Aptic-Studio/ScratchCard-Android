package com.apticstudio.scratchcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.BitSet

/**
 * High-performance, hardware-optimized state object to manage scratch card operations.
 */
@Stable
class ScratchCardState {
    /**
     * Single unified path object containing all recorded scratch strokes.
     * Rendering 1 combined path reduces GPU draw calls from O(N) to O(1).
     */
    internal val scratchPath = Path()

    /**
     * Counter used to trigger recomposition during active drag events.
     */
    internal var pathUpdateSignal by mutableIntStateOf(0)

    /**
     * Total number of resolution grid points used for progress calculation (24x24 = 576 points).
     */
    private val gridCols = 24
    private val gridRows = 24
    private val totalGridPoints = gridCols * gridRows
    private val clearedGridBits = BitSet(totalGridPoints)

    /**
     * Precomputed grid centers for maximum drag performance (avoiding CPU re-allocation).
     */
    private var cachedWidth = 0f
    private var cachedHeight = 0f
    private var gridCenterX = FloatArray(totalGridPoints)
    private var gridCenterY = FloatArray(totalGridPoints)

    /**
     * Current scratch progress from 0.0 (0% cleared) to 1.0 (100% cleared).
     */
    var progress by mutableFloatStateOf(0f)
        internal set

    /**
     * Indicates whether the overlay is fully revealed / cleared.
     */
    var isCleared by mutableStateOf(false)
        internal set

    /**
     * Resets the scratch card to its initial un-scratched state.
     */
    fun reset() {
        scratchPath.reset()
        clearedGridBits.clear()
        progress = 0f
        isCleared = false
        pathUpdateSignal++
    }

    /**
     * Programmatically clears the scratch card overlay, revealing the background.
     */
    fun clear() {
        isCleared = true
        progress = 1f
        pathUpdateSignal++
    }

    /**
     * Pre-computes cell centers when container dimensions change.
     */
    private fun updateSize(size: Size) {
        if (size.width != cachedWidth || size.height != cachedHeight) {
            cachedWidth = size.width
            cachedHeight = size.height
            if (size.width > 0f && size.height > 0f) {
                val cellW = size.width / gridCols
                val cellH = size.height / gridRows
                for (r in 0 until gridRows) {
                    val centerY = (r + 0.5f) * cellH
                    val rowOffset = r * gridCols
                    for (c in 0 until gridCols) {
                        val idx = rowOffset + c
                        gridCenterX[idx] = (c + 0.5f) * cellW
                        gridCenterY[idx] = centerY
                    }
                }
            }
        }
    }

    /**
     * Highly optimized progress resolution using squared distance comparison and BitSet.
     */
    internal fun updateProgress(
        point: Offset,
        strokeRadiusPx: Float,
        size: Size
    ): Float {
        if (size.width <= 0f || size.height <= 0f) return progress

        updateSize(size)

        val radiusSq = strokeRadiusPx * strokeRadiusPx
        val cellW = size.width / gridCols
        val cellH = size.height / gridRows

        val minCol = ((point.x - strokeRadiusPx) / cellW).toInt().coerceIn(0, gridCols - 1)
        val maxCol = ((point.x + strokeRadiusPx) / cellW).toInt().coerceIn(0, gridCols - 1)
        val minRow = ((point.y - strokeRadiusPx) / cellH).toInt().coerceIn(0, gridRows - 1)
        val maxRow = ((point.y + strokeRadiusPx) / cellH).toInt().coerceIn(0, gridRows - 1)

        val px = point.x
        val py = point.y
        var newlyCleared = false

        for (r in minRow..maxRow) {
            val rowOffset = r * gridCols
            for (c in minCol..maxCol) {
                val idx = rowOffset + c
                if (!clearedGridBits.get(idx)) {
                    val dx = gridCenterX[idx] - px
                    val dy = gridCenterY[idx] - py
                    if (dx * dx + dy * dy <= radiusSq) {
                        clearedGridBits.set(idx)
                        newlyCleared = true
                    }
                }
            }
        }

        if (newlyCleared || progress == 0f) {
            val newProgress = (clearedGridBits.cardinality().toFloat() / totalGridPoints).coerceIn(0f, 1f)
            progress = newProgress
        }
        return progress
    }
}

/**
 * Creates and remembers a [ScratchCardState].
 */
@Composable
fun rememberScratchCardState(): ScratchCardState {
    return remember { ScratchCardState() }
}

/**
 * Interactive ScratchCard component.
 *
 * @param modifier Layout modifier for sizing and positioning.
 * @param state State instance controlling scratch progress and reset actions.
 * @param overlayColor Solid color of the scratchable top layer.
 * @param overlayImage Optional [ImageBitmap] to draw as the scratchable overlay.
 * @param overlayPainter Optional [Painter] to draw as the scratchable overlay.
 * @param overlayText Optional text hint rendered on the overlay (e.g. "Scratch Here!").
 * @param scratchThickness Thickness of the eraser brush.
 * @param enabled Controls whether touch input is accepted.
 * @param clearThreshold Progress fraction (0.0 to 1.0) required to consider scratch completed.
 *                       Defaults to 0.95f (95% to 100% complete).
 * @param autoClearOnThreshold If true, automatically removes the remaining overlay when [clearThreshold] is reached.
 *                             Defaults to false (user must scratch manually).
 * @param onScratchStarted Callback invoked when user initiates scratching.
 * @param onScratchProgress Callback invoked during scratching with current progress (0.0 to 1.0).
 * @param onScratchCompleted Callback invoked when progress reaches or exceeds [clearThreshold].
 * @param content Trailing Composable slot for the hidden background content (image, text, reward layout).
 */
@Composable
fun ScratchCard(
    modifier: Modifier = Modifier,
    state: ScratchCardState = rememberScratchCardState(),
    overlayColor: Color = Color(0xFFC0C0C0),
    overlayImage: ImageBitmap? = null,
    overlayPainter: Painter? = null,
    overlayText: String? = null,
    scratchThickness: Dp = 32.dp,
    enabled: Boolean = true,
    clearThreshold: Float = 0.95f,
    autoClearOnThreshold: Boolean = false,
    onScratchStarted: () -> Unit = {},
    onScratchProgress: (progress: Float) -> Unit = {},
    onScratchCompleted: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var isScratchingStarted by remember { mutableStateOf(false) }
    var hasCompletedTriggered by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { scratchThickness.toPx() }
    val textMeasurer = rememberTextMeasurer()

    // Minimum drag distance squared (3px) to throttle micro touch noise and conserve CPU
    val minTouchDistSq = 9f

    // Reset internal trigger flags when state is reset
    LaunchedEffect(state.progress) {
        if (state.progress == 0f) {
            isScratchingStarted = false
            hasCompletedTriggered = false
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerSize = Size(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Bottom Layer: Hidden Background Content
        content()

        // 2. Top Layer: Scratchable Sheet (Masking Overlay)
        if (!state.isCleared) {
            var previousPoint by remember { mutableStateOf<Offset?>(null) }

            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .pointerInput(enabled, containerSize) {
                        if (!enabled) return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                if (!isScratchingStarted) {
                                    isScratchingStarted = true
                                    onScratchStarted()
                                }

                                previousPoint = offset
                                state.scratchPath.moveTo(offset.x, offset.y)
                                state.scratchPath.lineTo(offset.x + 0.1f, offset.y + 0.1f)
                                state.pathUpdateSignal++

                                val currentProgress = state.updateProgress(offset, strokeWidthPx / 2f, containerSize)
                                onScratchProgress(currentProgress)

                                if (currentProgress >= clearThreshold && !hasCompletedTriggered) {
                                    hasCompletedTriggered = true
                                    onScratchCompleted()
                                    if (autoClearOnThreshold) {
                                        state.clear()
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val position = change.position
                                val prev = previousPoint

                                if (prev != null) {
                                    val dx = position.x - prev.x
                                    val dy = position.y - prev.y

                                    // Touch distance throttling: ignore sub-pixel micro jitter
                                    if (dx * dx + dy * dy >= minTouchDistSq) {
                                        val midX = (prev.x + position.x) / 2f
                                        val midY = (prev.y + position.y) / 2f
                                        state.scratchPath.quadraticTo(prev.x, prev.y, midX, midY)
                                        previousPoint = position
                                        state.pathUpdateSignal++

                                        val currentProgress = state.updateProgress(position, strokeWidthPx / 2f, containerSize)
                                        onScratchProgress(currentProgress)

                                        if (currentProgress >= clearThreshold && !hasCompletedTriggered) {
                                            hasCompletedTriggered = true
                                            onScratchCompleted()
                                            if (autoClearOnThreshold) {
                                                state.clear()
                                            }
                                        }
                                    }
                                } else {
                                    previousPoint = position
                                }
                            },
                            onDragEnd = {
                                previousPoint = null
                            },
                            onDragCancel = {
                                previousPoint = null
                            }
                        )
                    }
            ) {
                @Suppress("UNUSED_VARIABLE")
                val updateTrigger = state.pathUpdateSignal

                // A. Render the solid overlay layer (Color, Bitmap, or Painter)
                if (overlayImage != null) {
                    drawImage(
                        image = overlayImage,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                } else if (overlayPainter != null) {
                    with(overlayPainter) {
                        draw(size = this@Canvas.size)
                    }
                } else {
                    drawRect(color = overlayColor)
                }

                // Render overlay text hint if provided and scratching has not begun
                if (!overlayText.isNullOrEmpty() && state.progress == 0f) {
                    val textLayoutResult = textMeasurer.measure(
                        text = overlayText,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp
                        )
                    )
                    val textX = (size.width - textLayoutResult.size.width) / 2f
                    val textY = (size.height - textLayoutResult.size.height) / 2f
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, textY)
                    )
                }

                // B. Erase top layer along unified path using PorterDuff CLEAR mode (Single GPU Draw Call)
                drawPath(
                    path = state.scratchPath,
                    color = Color.Transparent,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
}
