package de.kamilkirstein.repocast.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * The RepoCast logo of a style, drawn on a 64×64 grid.
 * [progress] runs the intro animation from 0 (nothing) to 1 (final logo).
 */
@Composable
fun Logo(style: AppStyle, modifier: Modifier = Modifier, progress: Float = 1f) {
    Canvas(modifier) {
        val unit = size.minDimension / 64f
        translate((size.width - 64f * unit) / 2f, (size.height - 64f * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                when (style) {
                    AppStyle.NACHTRADIO -> nachtradio(progress)
                    AppStyle.LEINEN -> leinen(progress)
                    AppStyle.DAEMMERUNG -> daemmerung(progress)
                }
            }
        }
    }
}

/** 0..1 position of [p] inside the window [start]..[end]. */
private fun phase(p: Float, start: Float, end: Float) = ((p - start) / (end - start)).coerceIn(0f, 1f)

private fun easeOut(t: Float) = FastOutSlowInEasing.transform(t)

/** Ease-out with a small overshoot, for things that pop into place. */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val x = t - 1f
    return 1f + (c1 + 1f) * x * x * x + c1 * x * x
}

private fun DrawScope.line(color: Color, from: Offset, to: Offset, width: Float, alpha: Float = 1f) =
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round, alpha = alpha)

/** Vertical bar centered on [cx], [cy] with full length [len], scaled by [s]. */
private fun DrawScope.bar(color: Color, cx: Float, cy: Float, len: Float, s: Float, width: Float) {
    val half = len / 2f * s.coerceAtLeast(0.08f)
    line(color, Offset(cx, cy - half), Offset(cx, cy + half), width)
}

// Code brackets slide in, then three sound bars pop up between them.
private fun DrawScope.nachtradio(p: Float) {
    val amber = Color(0xFFF2B544)
    val slide = easeOut(phase(p, 0f, 0.45f))
    val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    translate(left = -22f * (1f - slide)) {
        drawPath(Path().apply { moveTo(20f, 16f); lineTo(8f, 32f); lineTo(20f, 48f) }, amber, alpha = slide, style = stroke)
    }
    translate(left = 22f * (1f - slide)) {
        drawPath(Path().apply { moveTo(44f, 16f); lineTo(56f, 32f); lineTo(44f, 48f) }, amber, alpha = slide, style = stroke)
    }
    listOf(26f to 12f, 32f to 24f, 38f to 12f).forEachIndexed { i, (x, len) ->
        val s = easeOutBack(phase(p, 0.4f + i * 0.08f, 0.75f + i * 0.08f))
        if (s > 0f) bar(amber, x, 32f, len, s, 5f)
    }
}

// A record spins in, then a git branch draws itself on the label.
private fun DrawScope.leinen(p: Float) {
    val ink = Color(0xFF18221D)
    val groove = Color(0xFF34423B)
    val sage = Color(0xFF2F6B4F)
    val paper = Color(0xFFF3F4F0)
    val center = Offset(32f, 32f)
    val spin = easeOut(phase(p, 0f, 0.55f))
    scale(0.4f + 0.6f * spin, pivot = center) {
        rotate(-140f * (1f - spin), pivot = center) {
            drawCircle(ink, 28f, center, alpha = spin)
            drawCircle(groove, 21f, center, style = Stroke(1f), alpha = spin)
            drawCircle(groove, 15f, center, style = Stroke(1f), alpha = spin)
            // A light glint so the rotation is visible.
            drawArc(groove, 200f, 50f, false, Offset(8f, 8f), androidx.compose.ui.geometry.Size(48f, 48f), style = Stroke(2f, cap = StrokeCap.Round), alpha = spin)
            drawCircle(sage, 10f, center, alpha = spin)
        }
    }
    val trunk = Path().apply { moveTo(28f, 26f); lineTo(28f, 38f) }
    val fork = Path().apply { moveTo(28f, 30f); cubicTo(28f, 33f, 36f, 31f, 36f, 35f) }
    val draw = easeOut(phase(p, 0.45f, 0.8f))
    if (draw > 0f) {
        listOf(trunk, fork).forEach { path ->
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * draw, partial, true)
            drawPath(partial, paper, style = Stroke(2.2f, cap = StrokeCap.Round))
        }
    }
    val dot = easeOutBack(phase(p, 0.7f, 1f))
    if (dot > 0f) {
        drawCircle(paper, 2.2f * dot, Offset(28f, 26f))
        drawCircle(paper, 2.2f * dot, Offset(36f, 36f))
    }
}

// A timeline draws across, then the bars grow into a sound wave.
private fun DrawScope.daemmerung(p: Float) {
    val lavender = Color(0xFFB9A8F0)
    val mauve = Color(0xFFD4A0D0)
    val coral = Color(0xFFFF8C73)
    val baseline = Color(0xFF4A3F63)
    val draw = easeOut(phase(p, 0f, 0.35f))
    if (draw > 0f) line(baseline, Offset(4f, 32f), Offset(4f + 56f * draw, 32f), 2f)
    val bars = listOf(
        Triple(10f, 12f, lavender), Triple(21f, 28f, mauve), Triple(32f, 40f, coral),
        Triple(43f, 24f, mauve), Triple(54f, 10f, lavender),
    )
    bars.forEachIndexed { i, (x, len, color) ->
        val s = easeOutBack(phase(p, 0.3f + i * 0.07f, 0.7f + i * 0.07f))
        if (s > 0f) bar(color, x, 32f, len, s, 5f)
    }
}
