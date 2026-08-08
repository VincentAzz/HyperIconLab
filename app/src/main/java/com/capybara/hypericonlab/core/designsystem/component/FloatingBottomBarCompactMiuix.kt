package com.capybara.hypericonlab.core.designsystem.component

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.capybara.hypericonlab.core.designsystem.animation.DampedDragAnimation
import com.capybara.hypericonlab.core.designsystem.animation.InteractiveHighlight
import com.capybara.hypericonlab.core.designsystem.liquidglass.InnerShadow
import com.capybara.hypericonlab.core.designsystem.liquidglass.innerShadow
import com.capybara.hypericonlab.core.designsystem.liquidglass.lens
import com.capybara.hypericonlab.core.designsystem.liquidglass.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberCombinedBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.vibrancy
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactHeight
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactIndicatorHeight
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactIndicatorPadding
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.sign

val LocalFloatingBottomBarReportPosition =
    staticCompositionLocalOf { { _: Int, _: Float, _: Float -> } }

@Composable
fun FloatingBarCompactItem(
    selectedIndex: () -> Int,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    type: FloatingBottomBarCompactType,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val selected = selectedIndex() == index
    val scale = LocalFloatingBottomBarTabScale.current
    val contentColor = LocalFloatingBottomBarContentColor.current
    val reportPosition = LocalFloatingBottomBarReportPosition.current

    val sizeAnimationSpec =
        remember { spring<IntSize>(stiffness = Spring.StiffnessMediumLow) }

    Row(
        modifier = modifier
            .onGloballyPositioned { coords ->
                reportPosition(index, coords.positionInParent().x, coords.size.width.toFloat())
            }
            .clip(rememberKyantCapsuleShape())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .padding(horizontal = 16.dp)
            .animateContentSize(sizeAnimationSpec),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            val showIcon = when (type) {
                FloatingBottomBarCompactType.ICON_ONLY -> true
                FloatingBottomBarCompactType.TEXT_ONLY -> false
                FloatingBottomBarCompactType.MIXED_ICON -> true
                FloatingBottomBarCompactType.MIXED_TEXT -> selected
            }

            val showText = when (type) {
                FloatingBottomBarCompactType.ICON_ONLY -> false
                FloatingBottomBarCompactType.TEXT_ONLY -> true
                FloatingBottomBarCompactType.MIXED_ICON -> selected
                FloatingBottomBarCompactType.MIXED_TEXT -> true
            }

            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn() + expandHorizontally(
                    animationSpec = sizeAnimationSpec,
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut() + shrinkHorizontally(
                    animationSpec = sizeAnimationSpec,
                    shrinkTowards = Alignment.Start
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = showIcon && showText,
                enter = fadeIn() + expandHorizontally(
                    animationSpec = sizeAnimationSpec,
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut() + shrinkHorizontally(
                    animationSpec = sizeAnimationSpec,
                    shrinkTowards = Alignment.Start
                )
            ) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(
                    animationSpec = sizeAnimationSpec,
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(
                    animationSpec = sizeAnimationSpec,
                    shrinkTowards = Alignment.Start
                )
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
fun FloatingBottomBarCompactMiuix(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    m3Backdrop: LayerBackdrop? = null,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    isStandardBlurEnabled: Boolean = false,
    colors: FloatingBottomBarColors = FloatingBottomBarDefaults.colors(),
    barHeight: Dp = FloatingBottomBarCompactHeight,
    indicatorPadding: Dp = FloatingBottomBarCompactIndicatorPadding,
    content: @Composable RowScope.() -> Unit
) {
    val isInDark = AppTheme.isDark
    val pillShape = rememberKyantCapsuleShape()
    val effectShape = CircleShape
    val containerColor =
        if (isBlurEnabled || isStandardBlurEnabled) colors.containerColor.copy(0.4f) else colors.containerColor

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

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

    var currentIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex()) }

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.15f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                val currentValue = anim.value
                val index = currentValue.toInt().fastCoerceIn(0, tabsCount - 1)
                val tabWidth = tabWidths[index] ?: 0f
                if (tabWidth == 0f) return@DampedDragAnimation false

                val nextIndex = (index + 1).fastCoerceIn(0, tabsCount - 1)
                val fraction = currentValue - index
                val x1 = tabPositions[index] ?: 0f
                val x2 = tabPositions[nextIndex] ?: 0f
                val indicatorXLocal = lerp(x1, x2, fraction)

                val globalTouchX = if (isLtr) {
                    indicatorXLocal + offset.x
                } else {
                    totalWidthPx - indicatorXLocal - tabWidth + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                val anim = holder.instance ?: return@DampedDragAnimation
                val index = anim.value.toInt().fastCoerceIn(0, tabsCount - 1)
                val currentTabWidth = tabWidths[index] ?: 100f

                updateValue(
                    (targetValue + dragAmount.x / currentTabWidth * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                )
                animationScope.launch {
                    offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                }
            }
        ).also { holder.instance = it }
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
        snapshotFlow { selectedIndex() }.collectLatest { currentIndex = it }
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            dampedDragAnimation.animateToValue(index.toFloat())
            onSelected(index)
        }
    }

    val interactiveHighlight =
        if (isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            remember(animationScope) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { size, _ ->
                        Offset(
                            if (isLtr) indicatorX + indicatorWidth / 2f + panelOffset
                            else size.width - (indicatorX + indicatorWidth / 2f) + panelOffset,
                            size.height / 2f
                        )
                    }
                )
            }
        } else {
            null
        }

    val indicatorSpecular = remember {
        Highlight(
            width = 1.dp,
            alpha = 1f,
            style = BloomStroke(
                color = Color.White.copy(alpha = 0.12f),
                innerBlurRadius = 2.0.dp,
                primaryLight = LightSource(
                    position = LightPosition(
                        0.5f,
                        -0.3f,
                        -0.05f
                    ),
                    color = Color.White,
                    intensity = 1f,
                ),
                secondaryLight = LightSource(
                    position = LightPosition(
                        0.5f,
                        0.8f,
                        -0.5f
                    ),
                    color = Color.White,
                    intensity = 0.4f,
                ),
                dualPeak = true,
            ),
        )
    }

    val baseHighlight = indicatorSpecular
    val pillHighlight = indicatorSpecular

    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    Box(
        modifier = modifier.wrapContentWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        CompositionLocalProvider(
            LocalFloatingBottomBarContentColor provides colors.contentColor,
            LocalFloatingBottomBarReportPosition provides { index, x, width ->
                tabPositions[index] = x
                tabWidths[index] = width
            }
        ) {
            Row(
                Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .dropShadow(
                        shape = effectShape,
                        shadow = Shadow(
                            radius = 10.dp,
                            color = Color.Black,
                            alpha = if (isInDark) 0.2f else 0.1f,
                        ),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .then(
                        if (isBlurEnabled) {
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { effectShape },
                                    effects = {
                                        vibrancy()
                                        blur(4.dp.toPx(), 4.dp.toPx())
                                        lens(
                                            refractionHeight = 20.dp.toPx(),
                                            refractionAmount = 20.dp.toPx(),
                                        )
                                    },
                                    highlight = { baseHighlight.copy(alpha = 0.75f) },
                                    layerBlock = {
                                        clip = true
                                        shape = pillShape
                                        val width = size.width.coerceAtLeast(1f)
                                        val s = lerp(
                                            1f,
                                            1f + 12.dp.toPx() / width,
                                            dampedDragAnimation.pressProgress
                                        )
                                        scaleX = s
                                        scaleY = s
                                    },
                                    onDrawSurface = { drawRect(containerColor) },
                                )
                        } else if (isStandardBlurEnabled) {
                            Modifier
                                .clip(pillShape)
                                .material3BlurEffect(
                                    backdrop = m3Backdrop,
                                    shape = effectShape
                                )
                                .background(Color.Transparent, pillShape)
                        } else {
                            Modifier.background(containerColor, pillShape)
                        }
                    )
                    .then(if (isBlurEnabled && interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                    .height(barHeight)
                    .padding(indicatorPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        if (isBlurEnabled) {
            CompositionLocalProvider(
                LocalFloatingBottomBarTabScale provides {
                    lerp(1f, 1.15f, dampedDragAnimation.pressProgress)
                },
                LocalFloatingBottomBarContentColor provides colors.activeContentColor
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { effectShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx(),
                                )
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight?.modifier ?: Modifier)
                        .height(FloatingBottomBarCompactIndicatorHeight)
                        .padding(horizontal = indicatorPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    content()
                }
            }
        }

        if (indicatorWidth > 0f) {
            val tabWidthDp = with(density) { indicatorWidth.toDp() }
            val innerHeight = barHeight - indicatorPadding * 2
            val indicatorXDp = with(density) { indicatorX.toDp() }

            if (isBlurEnabled) {
                Box(
                    Modifier
                        .padding(start = indicatorPadding + indicatorXDp)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .then(interactiveHighlight?.gestureModifier ?: Modifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { effectShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    refractionHeight = 8.dp.toPx() * progress,
                                    refractionAmount = 12.dp.toPx() * progress,
                                    depthEffect = true,
                                    chromaticAberration = 0.5f,
                                )
                            },
                            highlight = { pillHighlight.copy(alpha = dampedDragAnimation.pressProgress) },
                            layerBlock = {
                                clip = true
                                shape = pillShape
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = if (!isInDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(
                                        alpha = 0.1f
                                    ),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                        .innerShadow(shape = effectShape) {
                            InnerShadow(
                                radius = 6.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        }
                        .height(innerHeight)
                        .width(tabWidthDp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(innerHeight)
                        .width(tabWidthDp)
                        .graphicsLayer {
                            translationX = (indicatorPadding + indicatorXDp).toPx() + panelOffset
                        }
                        .then(dampedDragAnimation.modifier)
                        .clip(pillShape)
                        .background(colors.indicatorColor.copy(alpha = 0.15f), pillShape),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CompositionLocalProvider(LocalFloatingBottomBarContentColor provides colors.activeContentColor) {
                        val currentIndicatorX = indicatorX
                        Row(
                            Modifier
                                .clearAndSetSemantics {}
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .requiredWidth(with(density) { totalWidthPx.toDp() })
                                .height(innerHeight)
                                .graphicsLayer {
                                    translationX =
                                        if (isLtr) -currentIndicatorX else currentIndicatorX
                                    compositingStrategy =
                                        CompositingStrategy.Offscreen
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
