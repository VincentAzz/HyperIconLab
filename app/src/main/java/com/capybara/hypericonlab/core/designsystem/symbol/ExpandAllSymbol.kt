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
public val AppMaterialSymbols.expand_all: ImageVector
    get() {
        if (_expand_all != null) {
            return _expand_all!!
        }
        _expand_all =
            ImageVector.Builder(
                name = "expand_all",
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
                        moveTo(12f, 19.15f)
                        lineToRelative(3.88f, -3.87f)
                        quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
                        quadToRelative(0.4f, 0f, 0.7f, 0.3f)
                        reflectiveQuadToRelative(0.3f, 0.71f)
                        reflectiveQuadToRelative(-0.3f, 0.71f)
                        lineToRelative(-3.85f, 3.88f)
                        quadTo(12.85f, 21.15f, 12f, 21.15f)
                        reflectiveQuadTo(10.58f, 20.58f)
                        lineTo(6.7f, 16.7f)
                        quadTo(6.4f, 16.4f, 6.41f, 15.99f)
                        reflectiveQuadTo(6.73f, 15.28f)
                        reflectiveQuadToRelative(0.71f, -0.3f)
                        reflectiveQuadToRelative(0.71f, 0.3f)
                        lineTo(12f, 19.15f)
                        close()
                        moveTo(12f, 4.85f)
                        lineTo(8.15f, 8.7f)
                        quadTo(7.85f, 9f, 7.45f, 8.99f)
                        quadTo(7.05f, 8.98f, 6.75f, 8.7f)
                        quadTo(6.45f, 8.4f, 6.44f, 7.99f)
                        quadTo(6.43f, 7.57f, 6.73f, 7.27f)
                        lineTo(10.58f, 3.42f)
                        quadTo(11.15f, 2.85f, 12f, 2.85f)
                        reflectiveQuadToRelative(1.43f, 0.57f)
                        lineToRelative(3.85f, 3.85f)
                        quadToRelative(0.3f, 0.3f, 0.29f, 0.71f)
                        reflectiveQuadTo(17.25f, 8.7f)
                        quadToRelative(-0.3f, 0.28f, -0.7f, 0.29f)
                        reflectiveQuadTo(15.85f, 8.7f)
                        lineTo(12f, 4.85f)
                        close()
                    }
                }
                .build()
        return _expand_all!!
    }

private var _expand_all: ImageVector? = null
