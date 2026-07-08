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
val AppMaterialSymbols.menu_open: ImageVector
    get() {
        if (_menu_open != null) {
            return _menu_open!!
        }
        _menu_open =
            ImageVector.Builder(
                name = "menu_open",
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
                        moveTo(4f, 18f)
                        quadTo(3.58f, 18f, 3.29f, 17.71f)
                        quadTo(3f, 17.43f, 3f, 17f)
                        reflectiveQuadTo(3.29f, 16.29f)
                        reflectiveQuadTo(4f, 16f)
                        horizontalLineTo(15f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(16f, 17f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(15f, 18f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(18.9f, 16.3f)
                        lineTo(15.3f, 12.7f)
                        quadTo(15f, 12.4f, 15f, 12f)
                        reflectiveQuadToRelative(0.3f, -0.7f)
                        lineTo(18.9f, 7.7f)
                        quadTo(19.18f, 7.43f, 19.6f, 7.43f)
                        reflectiveQuadTo(20.3f, 7.7f)
                        reflectiveQuadToRelative(0.28f, 0.7f)
                        reflectiveQuadTo(20.3f, 9.1f)
                        lineTo(17.4f, 12f)
                        lineToRelative(2.9f, 2.9f)
                        quadToRelative(0.28f, 0.27f, 0.28f, 0.7f)
                        reflectiveQuadTo(20.3f, 16.3f)
                        quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
                        reflectiveQuadTo(18.9f, 16.3f)
                        close()
                        moveTo(4f, 13f)
                        quadTo(3.58f, 13f, 3.29f, 12.71f)
                        quadTo(3f, 12.43f, 3f, 12f)
                        reflectiveQuadTo(3.29f, 11.29f)
                        reflectiveQuadTo(4f, 11f)
                        horizontalLineToRelative(8f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(13f, 12f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(12f, 13f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(4f, 8f)
                        quadTo(3.58f, 8f, 3.29f, 7.71f)
                        quadTo(3f, 7.43f, 3f, 7f)
                        reflectiveQuadTo(3.29f, 6.29f)
                        reflectiveQuadTo(4f, 6f)
                        horizontalLineTo(15f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(16f, 7f)
                        reflectiveQuadTo(15.71f, 7.71f)
                        reflectiveQuadTo(15f, 8f)
                        horizontalLineTo(4f)
                        close()
                    }
                }
                .build()
        return _menu_open!!
    }

private var _menu_open: ImageVector? = null
