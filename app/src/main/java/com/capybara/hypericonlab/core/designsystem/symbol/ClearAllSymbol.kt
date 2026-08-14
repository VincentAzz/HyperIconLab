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
val AppMaterialSymbols.clear_all: ImageVector
    get() {
        if (_clear_all != null) {
            return _clear_all!!
        }
        _clear_all =
            ImageVector.Builder(
                name = "clear_all",
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
                        moveTo(4f, 17f)
                        quadTo(3.58f, 17f, 3.29f, 16.71f)
                        quadTo(3f, 16.43f, 3f, 16f)
                        reflectiveQuadTo(3.29f, 15.29f)
                        reflectiveQuadTo(4f, 15f)
                        horizontalLineTo(16f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(17f, 16f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(16f, 17f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(6f, 13f)
                        quadTo(5.58f, 13f, 5.29f, 12.71f)
                        quadTo(5f, 12.43f, 5f, 12f)
                        reflectiveQuadTo(5.29f, 11.29f)
                        reflectiveQuadTo(6f, 11f)
                        horizontalLineTo(18f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(19f, 12f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(18f, 13f)
                        horizontalLineTo(6f)
                        close()
                        moveTo(8f, 9f)
                        quadTo(7.58f, 9f, 7.29f, 8.71f)
                        reflectiveQuadTo(7f, 8f)
                        quadTo(7f, 7.57f, 7.29f, 7.29f)
                        reflectiveQuadTo(8f, 7f)
                        horizontalLineTo(20f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(21f, 8f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(20f, 9f)
                        horizontalLineTo(8f)
                        close()
                    }
                }
                .build()
        return _clear_all!!
    }

private var _clear_all: ImageVector? = null
