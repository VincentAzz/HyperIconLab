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
val AppMaterialSymbols.design_services: ImageVector
    get() {
        if (_design_services != null) {
            return _design_services!!
        }
        _design_services =
            ImageVector.Builder(
                name = "design_services",
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
                        moveTo(8.8f, 10.95f)
                        lineTo(10.95f, 8.77f)
                        lineTo(9.55f, 7.35f)
                        lineToRelative(-0.4f, 0.4f)
                        quadTo(8.88f, 8.02f, 8.46f, 8.04f)
                        reflectiveQuadTo(7.75f, 7.75f)
                        reflectiveQuadTo(7.45f, 7.04f)
                        quadToRelative(0f, -0.41f, 0.3f, -0.71f)
                        lineTo(8.13f, 5.95f)
                        lineTo(7f, 4.82f)
                        lineTo(4.83f, 7f)
                        lineTo(8.8f, 10.95f)
                        close()
                        moveTo(17f, 19.18f)
                        lineTo(19.18f, 17f)
                        lineTo(18.05f, 15.88f)
                        lineToRelative(-0.4f, 0.38f)
                        quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
                        reflectiveQuadToRelative(-0.7f, -0.3f)
                        reflectiveQuadToRelative(-0.3f, -0.7f)
                        reflectiveQuadToRelative(0.3f, -0.7f)
                        lineToRelative(0.38f, -0.4f)
                        lineTo(15.2f, 13.05f)
                        lineTo(13.05f, 15.2f)
                        lineTo(17f, 19.18f)
                        close()
                        moveTo(17.6f, 5f)
                        lineToRelative(1.43f, 1.43f)
                        lineTo(17.6f, 5f)
                        close()
                        moveTo(4f, 21f)
                        quadTo(3.58f, 21f, 3.29f, 20.71f)
                        quadTo(3f, 20.43f, 3f, 20f)
                        verticalLineTo(17.18f)
                        quadToRelative(0f, -0.2f, 0.08f, -0.39f)
                        reflectiveQuadTo(3.3f, 16.45f)
                        lineTo(7.38f, 12.38f)
                        lineTo(3.05f, 8.05f)
                        quadTo(2.63f, 7.63f, 2.63f, 7f)
                        reflectiveQuadTo(3.05f, 5.95f)
                        lineToRelative(2.9f, -2.9f)
                        quadTo(6.38f, 2.63f, 7f, 2.64f)
                        reflectiveQuadTo(8.05f, 3.07f)
                        lineTo(12.4f, 7.4f)
                        lineTo(16.18f, 3.6f)
                        quadToRelative(0.3f, -0.3f, 0.68f, -0.45f)
                        reflectiveQuadTo(17.63f, 3f)
                        quadToRelative(0.4f, 0f, 0.78f, 0.15f)
                        reflectiveQuadTo(19.08f, 3.6f)
                        lineTo(20.4f, 4.95f)
                        quadToRelative(0.3f, 0.3f, 0.45f, 0.68f)
                        reflectiveQuadTo(21f, 6.4f)
                        reflectiveQuadTo(20.85f, 7.16f)
                        reflectiveQuadTo(20.4f, 7.82f)
                        lineToRelative(-3.78f, 3.8f)
                        lineToRelative(4.33f, 4.32f)
                        quadToRelative(0.42f, 0.43f, 0.42f, 1.05f)
                        reflectiveQuadToRelative(-0.42f, 1.05f)
                        lineToRelative(-2.9f, 2.9f)
                        quadTo(17.63f, 21.38f, 17f, 21.38f)
                        reflectiveQuadTo(15.95f, 20.95f)
                        lineTo(11.63f, 16.63f)
                        lineTo(7.55f, 20.7f)
                        quadTo(7.4f, 20.85f, 7.21f, 20.93f)
                        reflectiveQuadTo(6.83f, 21f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(6.4f)
                        lineTo(16.2f, 9.23f)
                        lineTo(14.78f, 7.8f)
                        lineTo(5f, 17.6f)
                        verticalLineTo(19f)
                        close()
                        moveTo(15.5f, 8.52f)
                        lineTo(14.78f, 7.8f)
                        lineTo(16.2f, 9.23f)
                        lineTo(15.5f, 8.52f)
                        close()
                    }
                }
                .build()
        return _design_services!!
    }

private var _design_services: ImageVector? = null
