package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.capybara.hypericonlab.core.designsystem.animation.DampedDragAnimation
import com.capybara.hypericonlab.core.designsystem.theme.TabRowRoundedCorner.TabRowBarCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.TabRowRoundedCorner.TabRowIndicatorCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign


enum class FloatingTabRowAlignment {
    START,
    CENTER
}


enum class FloatingTabRowWidthMode {
    WRAP_CONTENT,
    FILL
}

/**
 * @param tabs Tab 标签文本列表
 * @param selectedIndex 当前选中的 tab 索引
 * @param onSelected tab 切换回调
 * @param containerColor Bar 背景颜色，默认跟随 ConfigCard 卡片背景色
 * @param indicatorColor 指示器颜色，默认跟随 StyleChip 选中色
 * @param barCornerRadius Bar 圆角半径，默认使用 CardCornerRadius
 * @param indicatorCornerRadius 指示器圆角半径，默认使用 ChipCornerRadius
 * @param indicatorPadding Bar 与指示器之间的 padding（上下内缩），默认 4.dp
 * @param alignment 整个 TabRow 的对齐方式，默认靠左（仅 WRAP_CONTENT 模式生效）
 * @param widthMode 宽度模式，WRAP_CONTENT 按文本内容自动宽度，FILL 铺满父容器且 tab 项均分宽度
 */
@Composable
fun FloatingTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    barCornerRadius: Dp = TabRowBarCornerRadius,
    indicatorCornerRadius: Dp = TabRowIndicatorCornerRadius,
    indicatorPadding: Dp = 4.dp,
    alignment: FloatingTabRowAlignment = FloatingTabRowAlignment.START,
    widthMode: FloatingTabRowWidthMode = FloatingTabRowWidthMode.WRAP_CONTENT,
) {
    if (tabs.isEmpty()) return

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val barHeight: Dp = 48.dp
    val tabHorizontalPadding: Dp = 16.dp
    val innerHeight = barHeight - indicatorPadding * 2

    val barShape = rememberKyantCapsuleShape()
    val indicatorShape = rememberKyantCapsuleShape()

    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex) }
    val tabsCount = tabs.size

    class Holder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { Holder() }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1f,
            canDrag = { true },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                onSelected(targetIndex)
            },
            onDrag = { _, dragAmount ->
                val anim = holder.instance ?: return@DampedDragAnimation
                val index = anim.value.toInt().fastCoerceIn(0, tabsCount - 1)
                val currentTabWidth = tabWidths[index] ?: 100f
                updateValue(
                    (targetValue + dragAmount.x / currentTabWidth * if (isLtr) 1f else -1f).fastCoerceIn(
                        0f,
                        (tabsCount - 1).toFloat()
                    )
                )
                animationScope.launch {
                    offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                }
            }).also { holder.instance = it }
    }

    val indicatorX by remember {
        derivedStateOf {
            val value = dampedDragAnimation.value
            val index = value.toInt().fastCoerceIn(0, tabsCount - 1)
            val nextIndex = (index + 1).fastCoerceIn(0, tabsCount - 1)
            val fraction = value - index
            val x1 = tabPositions[index] ?: 0f
            val x2 = tabPositions[nextIndex] ?: 0f
            lerp(x1, x2, fraction)
        }
    }

    val indicatorWidth by remember {
        derivedStateOf {
            val value = dampedDragAnimation.value
            val index = value.toInt().fastCoerceIn(0, tabsCount - 1)
            val nextIndex = (index + 1).fastCoerceIn(0, tabsCount - 1)
            val fraction = value - index
            val w1 = tabWidths[index] ?: 0f
            val w2 = tabWidths[nextIndex] ?: 0f
            lerp(w1, w2, fraction)
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex.toFloat() != dampedDragAnimation.targetValue) {
            currentIndex = selectedIndex
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }

    val unselectedTextColor = MaterialTheme.colorScheme.onSurface
    val selectedTextColor = MaterialTheme.colorScheme.onPrimary
    val tabTextStyle = MaterialTheme.typography.titleSmall

    val onDragStopped: () -> Unit = {
        val targetIndex =
            dampedDragAnimation.targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
        currentIndex = targetIndex
        dampedDragAnimation.animateToValue(targetIndex.toFloat())
        animationScope.launch {
            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
        }
        onSelected(targetIndex)
    }

    val alignmentModifier = when (widthMode) {
        FloatingTabRowWidthMode.FILL -> Modifier.fillMaxWidth()
        FloatingTabRowWidthMode.WRAP_CONTENT -> when (alignment) {
            FloatingTabRowAlignment.START -> Modifier
            FloatingTabRowAlignment.CENTER -> Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        }
    }

    val isFill = widthMode == FloatingTabRowWidthMode.FILL

    val tabArrangement = if (isFill) Arrangement.Center else Arrangement.Start

    Box(
        modifier = modifier.then(alignmentModifier), contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .then(if (isFill) Modifier.fillMaxWidth() else Modifier)
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                }
                .graphicsLayer { translationX = panelOffset }
                .clip(barShape)
                .background(containerColor, barShape)
                .pointerInput(tabsCount) {
                    detectDragGestures(onDragStart = { dampedDragAnimation.press() }, onDragEnd = {
                        onDragStopped()
                        dampedDragAnimation.release()
                    }, onDragCancel = {
                        onDragStopped()
                        dampedDragAnimation.release()
                    }, onDrag = { change, dragAmount ->
                        change.consume()
                        val index = dampedDragAnimation.value.toInt().fastCoerceIn(0, tabsCount - 1)
                        val currentTabWidth = tabWidths[index] ?: 100f
                        dampedDragAnimation.updateValue(
                            (dampedDragAnimation.targetValue + dragAmount.x / currentTabWidth * if (isLtr) 1f else -1f).fastCoerceIn(
                                0f, (tabsCount - 1).toFloat()
                            )
                        )
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                        }
                    })
                }
                .height(barHeight)
                .padding(indicatorPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, label ->
                Row(
                    modifier = (if (isFill) Modifier.weight(1f) else Modifier)
                        .onGloballyPositioned { coords ->
                            tabPositions[index] = coords.positionInParent().x
                            tabWidths[index] = coords.size.width.toFloat()
                        }
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                currentIndex = index
                                dampedDragAnimation.animateToValue(index.toFloat())
                                onSelected(index)
                            })
                        .padding(horizontal = tabHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = tabArrangement,
                ) {
                    if (icons != null && index < icons.size) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = unselectedTextColor,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = label,
                        style = tabTextStyle,
                        color = unselectedTextColor,
                    )
                }
            }
        }

        if (indicatorWidth > 0f) {
            val tabWidthDp = with(density) { indicatorWidth.toDp() }

            Box(
                modifier = Modifier
                    .height(innerHeight)
                    .width(tabWidthDp)
                    .graphicsLayer {
                        translationX =
                            with(density) { indicatorPadding.toPx() } + indicatorX + panelOffset
                    }
                    .clip(indicatorShape)
                    .background(indicatorColor, indicatorShape),
                contentAlignment = Alignment.CenterStart) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .requiredWidth(with(density) { (totalWidthPx - 2 * indicatorPadding.toPx()).toDp() })
                        .height(innerHeight)
                        .graphicsLayer {
                            translationX = if (isLtr) -indicatorX else indicatorX
                            compositingStrategy = CompositingStrategy.Offscreen
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, label ->
                        Row(
                            modifier = (if (isFill) Modifier.weight(1f) else Modifier).padding(
                                horizontal = tabHorizontalPadding
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = tabArrangement,
                        ) {
                            if (icons != null && index < icons.size) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = null,
                                    tint = selectedTextColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = label,
                                style = tabTextStyle,
                                color = selectedTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
