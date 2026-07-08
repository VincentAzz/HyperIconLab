package com.capybara.hypericonlab.core.designsystem.symbol

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols

@Suppress("CheckReturnValue")
val AppMaterialSymbols.invert_colors: ImageVector
    get() {
        if (_invert_colors != null) {
            return _invert_colors!!
        }
        _invert_colors =
            ImageVector.Builder(
                name = "invert_colors",
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
                        moveTo(12f, 21f)
                        quadTo(8.68f, 21f, 6.34f, 18.69f)
                        reflectiveQuadTo(4f, 13.1f)
                        quadTo(4f, 11.45f, 4.63f, 10.05f)
                        reflectiveQuadTo(6.35f, 7.55f)
                        lineToRelative(4.6f, -4.53f)
                        quadToRelative(0.22f, -0.2f, 0.5f, -0.31f)
                        reflectiveQuadTo(12f, 2.6f)
                        quadToRelative(0.28f, 0f, 0.55f, 0.11f)
                        reflectiveQuadToRelative(0.5f, 0.31f)
                        lineToRelative(4.6f, 4.53f)
                        quadToRelative(1.1f, 1.1f, 1.72f, 2.5f)
                        reflectiveQuadTo(20f, 13.1f)
                        quadToRelative(0f, 3.28f, -2.34f, 5.59f)
                        reflectiveQuadTo(12f, 21f)
                        close()
                        moveToRelative(0f, -2f)
                        verticalLineTo(4.8f)
                        lineTo(7.75f, 9f)
                        quadTo(6.88f, 9.82f, 6.44f, 10.86f)
                        reflectiveQuadTo(6f, 13.1f)
                        quadToRelative(0f, 2.43f, 1.75f, 4.16f)
                        reflectiveQuadTo(12f, 19f)
                        close()
                    }
                }
                .build()
        return _invert_colors!!
    }

private var _invert_colors: ImageVector? = null
