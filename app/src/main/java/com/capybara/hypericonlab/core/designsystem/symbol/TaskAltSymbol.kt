package com.capybara.hypericonlab.core.designsystem.symbol

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val AppMaterialSymbols.task_alt: ImageVector
    get() {
        if (_task_alt != null) {
            return _task_alt!!
        }
        _task_alt =
            ImageVector.Builder(
                name = "task_alt",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(12f, 22f)
                        quadTo(9.93f, 22f, 8.1f, 21.21f)
                        quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                        quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                        reflectiveQuadTo(2f, 12f)
                        quadTo(2f, 9.92f, 2.79f, 8.1f)
                        quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                        quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                        quadTo(9.93f, 2f, 12f, 2f)
                        quadToRelative(1.2f, 0f, 2.34f, 0.27f)
                        reflectiveQuadToRelative(2.19f, 0.8f)
                        quadToRelative(0.38f, 0.2f, 0.49f, 0.6f)
                        quadToRelative(0.11f, 0.4f, -0.14f, 0.75f)
                        reflectiveQuadTo(16.21f, 4.88f)
                        quadTo(15.8f, 4.97f, 15.4f, 4.77f)
                        quadTo(14.6f, 4.4f, 13.74f, 4.2f)
                        reflectiveQuadTo(12f, 4f)
                        quadTo(8.68f, 4f, 6.34f, 6.34f)
                        reflectiveQuadTo(4f, 12f)
                        reflectiveQuadToRelative(2.34f, 5.66f)
                        reflectiveQuadTo(12f, 20f)
                        reflectiveQuadToRelative(5.66f, -2.34f)
                        reflectiveQuadTo(20f, 12f)
                        quadToRelative(0f, -0.2f, -0.01f, -0.39f)
                        reflectiveQuadTo(19.95f, 11.23f)
                        quadTo(19.9f, 10.8f, 20.11f, 10.41f)
                        quadTo(20.33f, 10.02f, 20.75f, 9.9f)
                        quadToRelative(0.4f, -0.13f, 0.75f, 0.08f)
                        quadToRelative(0.35f, 0.2f, 0.4f, 0.6f)
                        quadToRelative(0.05f, 0.35f, 0.07f, 0.7f)
                        quadTo(22f, 11.63f, 22f, 12f)
                        quadToRelative(0f, 2.07f, -0.79f, 3.9f)
                        reflectiveQuadToRelative(-2.14f, 3.17f)
                        quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                        reflectiveQuadTo(12f, 22f)
                        close()
                        moveTo(10.6f, 13.8f)
                        lineTo(19.9f, 4.47f)
                        quadTo(20.18f, 4.2f, 20.59f, 4.19f)
                        quadTo(21f, 4.17f, 21.3f, 4.47f)
                        quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
                        reflectiveQuadTo(21.3f, 5.88f)
                        lineTo(11.3f, 15.9f)
                        quadTo(11f, 16.2f, 10.6f, 16.2f)
                        reflectiveQuadTo(9.9f, 15.9f)
                        lineTo(7.05f, 13.05f)
                        quadTo(6.78f, 12.77f, 6.78f, 12.35f)
                        reflectiveQuadToRelative(0.28f, -0.7f)
                        quadToRelative(0.27f, -0.27f, 0.7f, -0.27f)
                        reflectiveQuadToRelative(0.7f, 0.27f)
                        lineTo(10.6f, 13.8f)
                        close()
                    }
                }
                .build()
        return _task_alt!!
    }

private var _task_alt: ImageVector? = null
