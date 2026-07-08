package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap

/**
 * JNI bridge to opencv-mobile native sticker processing.
 * Mirrors the Python IconGeneratorStickerStroke logic exactly.
 */
object StickerNativeProcessor {

    init {
        System.loadLibrary("sticker_jni")
    }

    /**
     * Draw white stroke around the icon.
     * Replicates Python IconGeneratorStickerStroke.draw_stroke().
     *
     * @param src         original icon bitmap (ARGB_8888)
     * @param strokeWidth stroke width in pixels
     * @return padded bitmap with white stroke + original icon pasted on top
     */
    external fun nativeDrawStroke(src: Bitmap, strokeWidth: Int): Bitmap

    /**
     * Get the stroke mask only (no icon pasted).
     * @param src original icon bitmap
     * @param strokeWidth stroke width in pixels
     * @return RGBA bitmap with white stroke on transparent background
     */
    external fun nativeGetStrokeMask(src: Bitmap, strokeWidth: Int): Bitmap

    /**
     * Detect and fill closed areas (holes) in the icon.
     * Replicates Python IconGeneratorStickerStroke.detect_closed_areas() + fill_closed_areas().
     *
     * @param src       stroked icon bitmap (ARGB_8888, with padding from drawStroke)
     * @param fillColor fill color as ARGB int (e.g. 0xFFB0C4DE)
     * @return bitmap with holes filled, or original if no holes found
     */
    external fun nativeFillHoles(src: Bitmap, fillColor: Int): Bitmap

    /**
     * Get the hole mask for the icon (for glow effect).
     * Replicates Python IconGeneratorStickerStroke.detect_closed_areas().
     *
     * @param src stroked icon bitmap (ARGB_8888, with padding from drawStroke)
     * @return RGBA bitmap where holes are white (255,255,255,255), rest transparent
     */
    external fun nativeGetHoleMask(src: Bitmap): Bitmap
}