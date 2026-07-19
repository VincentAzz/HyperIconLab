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
public val AppMaterialSymbols.category: ImageVector
    get() {
        if (_category != null) {
            return _category!!
        }
        _category =
            ImageVector.Builder(
                name = "category",
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
                        moveTo(7.43f, 9.48f)
                        lineTo(11.15f, 3.4f)
                        quadTo(11.3f, 3.15f, 11.53f, 3.04f)
                        reflectiveQuadTo(12f, 2.92f)
                        reflectiveQuadToRelative(0.48f, 0.11f)
                        reflectiveQuadTo(12.85f, 3.4f)
                        lineToRelative(3.72f, 6.08f)
                        quadTo(16.73f, 9.73f, 16.73f, 10f)
                        quadToRelative(0f, 0.27f, -0.13f, 0.5f)
                        reflectiveQuadToRelative(-0.35f, 0.36f)
                        quadTo(16.03f, 11f, 15.73f, 11f)
                        horizontalLineTo(8.28f)
                        quadTo(7.98f, 11f, 7.75f, 10.86f)
                        quadTo(7.53f, 10.73f, 7.4f, 10.5f)
                        quadTo(7.28f, 10.27f, 7.28f, 10f)
                        reflectiveQuadTo(7.43f, 9.48f)
                        close()
                        moveTo(17.5f, 22f)
                        quadToRelative(-1.88f, 0f, -3.19f, -1.31f)
                        reflectiveQuadTo(13f, 17.5f)
                        reflectiveQuadToRelative(1.31f, -3.19f)
                        reflectiveQuadTo(17.5f, 13f)
                        reflectiveQuadToRelative(3.19f, 1.31f)
                        reflectiveQuadTo(22f, 17.5f)
                        reflectiveQuadToRelative(-1.31f, 3.19f)
                        reflectiveQuadTo(17.5f, 22f)
                        close()
                        moveTo(3f, 20.5f)
                        verticalLineToRelative(-6f)
                        quadTo(3f, 14.08f, 3.29f, 13.79f)
                        reflectiveQuadTo(4f, 13.5f)
                        horizontalLineToRelative(6f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(11f, 14.5f)
                        verticalLineToRelative(6f)
                        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
                        reflectiveQuadTo(10f, 21.5f)
                        horizontalLineTo(4f)
                        quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
                        quadTo(3f, 20.93f, 3f, 20.5f)
                        close()
                        moveTo(17.5f, 20f)
                        quadToRelative(1.05f, 0f, 1.78f, -0.73f)
                        reflectiveQuadTo(20f, 17.5f)
                        reflectiveQuadTo(19.28f, 15.73f)
                        reflectiveQuadTo(17.5f, 15f)
                        reflectiveQuadToRelative(-1.77f, 0.72f)
                        reflectiveQuadTo(15f, 17.5f)
                        reflectiveQuadToRelative(0.73f, 1.77f)
                        reflectiveQuadTo(17.5f, 20f)
                        close()
                        moveTo(5f, 19.5f)
                        horizontalLineTo(9f)
                        verticalLineToRelative(-4f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(4f)
                        close()
                        moveTo(10.05f, 9f)
                        horizontalLineToRelative(3.9f)
                        lineTo(12f, 5.85f)
                        lineTo(10.05f, 9f)
                        close()
                        moveTo(12f, 9f)
                        close()
                        moveTo(9f, 15.5f)
                        close()
                        moveToRelative(8.5f, 2f)
                        close()
                    }
                }
                .build()
        return _category!!
    }

private var _category: ImageVector? = null
