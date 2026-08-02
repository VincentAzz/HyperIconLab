package com.capybara.hypericonlab.iconpack.ui.shape

// from Kyant0/Shapes - https://github.com/kyant0/Shapes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn

@Immutable
class UnevenRoundedRectangle(
    val cornerRadii: RectangleCornerRadii,
    override val style: RoundedCornerStyle = RoundedCornerStyle.Continuous
) : RoundedRectangularShape {

    constructor(
        topStart: Dp = 0.dp,
        topEnd: Dp = 0.dp,
        bottomEnd: Dp = 0.dp,
        bottomStart: Dp = 0.dp,
        style: RoundedCornerStyle = RoundedCornerStyle.Continuous
    ) : this(
        cornerRadii = RectangleCornerRadii(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart
        ),
        style = style
    )

    override fun corners(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): RoundedRectangularShape.Corners {
        val maxRadius = size.minDimension * 0.5f
        val topStart = with(density) {
            cornerRadii.topStart.toPx()
        }.fastCoerceIn(0f, maxRadius)
        val topEnd = with(density) {
            cornerRadii.topEnd.toPx()
        }.fastCoerceIn(0f, maxRadius)
        val bottomEnd = with(density) {
            cornerRadii.bottomEnd.toPx()
        }.fastCoerceIn(0f, maxRadius)
        val bottomStart = with(density) {
            cornerRadii.bottomStart.toPx()
        }.fastCoerceIn(0f, maxRadius)

        return if (layoutDirection == LayoutDirection.Ltr) {
            RoundedRectangularShape.Corners(
                topLeft = topStart,
                topRight = topEnd,
                bottomRight = bottomEnd,
                bottomLeft = bottomStart
            )
        } else {
            RoundedRectangularShape.Corners(
                topLeft = topEnd,
                topRight = topStart,
                bottomRight = bottomStart,
                bottomLeft = bottomEnd
            )
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val corners = corners(size, layoutDirection, density)
        return roundedRectangleOutline(
            size = size,
            topLeft = corners.topLeft,
            topRight = corners.topRight,
            bottomRight = corners.bottomRight,
            bottomLeft = corners.bottomLeft,
            style = style
        )
    }

    override fun copy(style: RoundedCornerStyle): RoundedRectangularShape =
        UnevenRoundedRectangle(cornerRadii = cornerRadii, style = style)
}
