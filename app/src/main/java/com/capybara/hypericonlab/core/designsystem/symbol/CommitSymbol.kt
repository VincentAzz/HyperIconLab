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
val AppMaterialSymbols.commit: ImageVector
    get() {
        if (_commit != null) {
            return _commit!!
        }
        _commit =
            ImageVector.Builder(
                name = "commit",
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
                        moveTo(8.81f, 15.86f)
                        quadTo(7.45f, 14.73f, 7.1f, 13f)
                        horizontalLineTo(3f)
                        quadTo(2.58f, 13f, 2.29f, 12.71f)
                        quadTo(2f, 12.43f, 2f, 12f)
                        reflectiveQuadTo(2.29f, 11.29f)
                        reflectiveQuadTo(3f, 11f)
                        horizontalLineTo(7.1f)
                        quadTo(7.45f, 9.27f, 8.81f, 8.14f)
                        reflectiveQuadTo(12f, 7f)
                        reflectiveQuadToRelative(3.19f, 1.14f)
                        reflectiveQuadTo(16.9f, 11f)
                        horizontalLineTo(21f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(22f, 12f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(21f, 13f)
                        horizontalLineTo(16.9f)
                        quadToRelative(-0.35f, 1.72f, -1.71f, 2.86f)
                        reflectiveQuadTo(12f, 17f)
                        quadTo(10.18f, 17f, 8.81f, 15.86f)
                        close()
                        moveTo(12f, 15f)
                        quadToRelative(1.25f, 0f, 2.13f, -0.88f)
                        reflectiveQuadTo(15f, 12f)
                        reflectiveQuadTo(14.13f, 9.88f)
                        reflectiveQuadTo(12f, 9f)
                        reflectiveQuadTo(9.88f, 9.88f)
                        reflectiveQuadTo(9f, 12f)
                        reflectiveQuadToRelative(0.88f, 2.13f)
                        reflectiveQuadTo(12f, 15f)
                        close()
                    }
                }
                .build()
        return _commit!!
    }

private var _commit: ImageVector? = null
