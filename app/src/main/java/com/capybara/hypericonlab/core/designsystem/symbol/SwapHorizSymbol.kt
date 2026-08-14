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
val AppMaterialSymbols.swap_horiz: ImageVector
    get() {
        if (_swap_horiz != null) {
            return _swap_horiz!!
        }
        _swap_horiz =
            ImageVector.Builder(
                name = "swap_horiz",
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
                        moveTo(5.83f, 16f)
                        lineTo(7.7f, 17.88f)
                        quadToRelative(0.28f, 0.27f, 0.28f, 0.69f)
                        reflectiveQuadTo(7.7f, 19.27f)
                        quadToRelative(-0.3f, 0.3f, -0.71f, 0.3f)
                        reflectiveQuadTo(6.28f, 19.27f)
                        lineTo(2.7f, 15.7f)
                        quadTo(2.55f, 15.55f, 2.49f, 15.38f)
                        reflectiveQuadTo(2.43f, 15f)
                        reflectiveQuadTo(2.49f, 14.63f)
                        reflectiveQuadTo(2.7f, 14.3f)
                        lineTo(6.3f, 10.7f)
                        quadTo(6.6f, 10.4f, 7f, 10.41f)
                        reflectiveQuadToRelative(0.7f, 0.31f)
                        quadToRelative(0.28f, 0.3f, 0.29f, 0.7f)
                        reflectiveQuadTo(7.7f, 12.13f)
                        lineTo(5.83f, 14f)
                        horizontalLineTo(12f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(13f, 15f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(12f, 16f)
                        horizontalLineTo(5.83f)
                        close()
                        moveTo(18.18f, 10f)
                        horizontalLineTo(12f)
                        quadTo(11.58f, 10f, 11.29f, 9.71f)
                        reflectiveQuadTo(11f, 9f)
                        quadTo(11f, 8.57f, 11.29f, 8.29f)
                        reflectiveQuadTo(12f, 8f)
                        horizontalLineToRelative(6.18f)
                        lineTo(16.3f, 6.13f)
                        quadTo(16.03f, 5.85f, 16.03f, 5.44f)
                        quadToRelative(0f, -0.41f, 0.27f, -0.71f)
                        quadToRelative(0.3f, -0.3f, 0.71f, -0.3f)
                        quadToRelative(0.41f, 0f, 0.71f, 0.3f)
                        lineTo(21.3f, 8.3f)
                        quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
                        reflectiveQuadTo(21.58f, 9f)
                        reflectiveQuadTo(21.51f, 9.38f)
                        reflectiveQuadTo(21.3f, 9.7f)
                        lineToRelative(-3.6f, 3.6f)
                        quadTo(17.4f, 13.6f, 17f, 13.59f)
                        quadTo(16.6f, 13.58f, 16.3f, 13.27f)
                        quadToRelative(-0.27f, -0.3f, -0.29f, -0.7f)
                        reflectiveQuadToRelative(0.29f, -0.7f)
                        lineTo(18.18f, 10f)
                        close()
                    }
                }
                .build()
        return _swap_horiz!!
    }

private var _swap_horiz: ImageVector? = null
