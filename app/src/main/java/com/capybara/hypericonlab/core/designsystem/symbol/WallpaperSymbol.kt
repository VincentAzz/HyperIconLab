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
val AppMaterialSymbols.wallpaper: ImageVector
    get() {
        if (_wallpaper != null) {
            return _wallpaper!!
        }
        _wallpaper =
            ImageVector.Builder(
                name = "wallpaper",
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
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(14f)
                        quadTo(3f, 13.58f, 3.29f, 13.29f)
                        reflectiveQuadTo(4f, 13f)
                        reflectiveQuadToRelative(0.71f, 0.29f)
                        reflectiveQuadTo(5f, 14f)
                        verticalLineToRelative(5f)
                        horizontalLineToRelative(5f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(11f, 20f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(10f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveToRelative(14f, 0f)
                        horizontalLineTo(14f)
                        quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
                        quadTo(13f, 20.43f, 13f, 20f)
                        reflectiveQuadToRelative(0.29f, -0.71f)
                        reflectiveQuadTo(14f, 19f)
                        horizontalLineToRelative(5f)
                        verticalLineTo(14f)
                        quadToRelative(0f, -0.43f, 0.29f, -0.71f)
                        reflectiveQuadTo(20f, 13f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(21f, 14f)
                        verticalLineToRelative(5f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        close()
                        moveTo(11.25f, 16f)
                        lineToRelative(2.6f, -3.48f)
                        quadToRelative(0.15f, -0.2f, 0.4f, -0.2f)
                        reflectiveQuadToRelative(0.4f, 0.2f)
                        lineTo(17.4f, 16.2f)
                        quadToRelative(0.2f, 0.25f, 0.05f, 0.53f)
                        reflectiveQuadTo(17f, 17f)
                        horizontalLineTo(7f)
                        quadTo(6.7f, 17f, 6.55f, 16.73f)
                        reflectiveQuadTo(6.6f, 16.2f)
                        lineToRelative(2f, -2.68f)
                        quadTo(8.75f, 13.33f, 9f, 13.33f)
                        reflectiveQuadToRelative(0.4f, 0.2f)
                        lineTo(11.25f, 16f)
                        close()
                        moveTo(3f, 5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineToRelative(5f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(11f, 4f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(10f, 5f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(5f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(4f, 11f)
                        reflectiveQuadTo(3.29f, 10.71f)
                        quadTo(3f, 10.43f, 3f, 10f)
                        verticalLineTo(5f)
                        close()
                        moveTo(21f, 5f)
                        verticalLineToRelative(5f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(20f, 11f)
                        reflectiveQuadTo(19.29f, 10.71f)
                        quadTo(19f, 10.43f, 19f, 10f)
                        verticalLineTo(5f)
                        horizontalLineTo(14f)
                        quadTo(13.58f, 5f, 13.29f, 4.71f)
                        reflectiveQuadTo(13f, 4f)
                        quadTo(13f, 3.57f, 13.29f, 3.29f)
                        reflectiveQuadTo(14f, 3f)
                        horizontalLineToRelative(5f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(21f, 5f)
                        close()
                        moveTo(14.43f, 9.57f)
                        quadTo(14f, 9.15f, 14f, 8.5f)
                        reflectiveQuadTo(14.43f, 7.43f)
                        reflectiveQuadTo(15.5f, 7f)
                        reflectiveQuadToRelative(1.07f, 0.43f)
                        reflectiveQuadTo(17f, 8.5f)
                        reflectiveQuadTo(16.58f, 9.57f)
                        reflectiveQuadTo(15.5f, 10f)
                        reflectiveQuadTo(14.43f, 9.57f)
                        close()
                    }
                }
                .build()
        return _wallpaper!!
    }

private var _wallpaper: ImageVector? = null
