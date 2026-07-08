#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>

#define LOG_TAG "StickerJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Convert Android ARGB_8888 Bitmap to cv::Mat (RGBA, CV_8UC4).
 * The caller must NOT unlock the bitmap until done with the Mat.
 */
static cv::Mat bitmapToMat(JNIEnv *env, jobject bitmap) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to get bitmap info");
        return cv::Mat();
    }

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to lock bitmap pixels");
        return cv::Mat();
    }

    // Android ARGB_8888: RGBA byte order on little-endian ARM
    cv::Mat mat(info.height, info.width, CV_8UC4, pixels);
    return mat.clone(); // Clone so we can unlock the bitmap immediately
}

/**
 * Extract alpha channel from RGBA Mat.
 */
static cv::Mat extractAlpha(const cv::Mat &rgba) {
    cv::Mat alpha;
    cv::extractChannel(rgba, alpha, 3); // Alpha is channel 3 in RGBA
    return alpha;
}

/**
 * Create a RGBA Mat from alpha channel and fill color.
 * Result has the same alpha, with RGB set to fillColor.
 */
static cv::Mat alphaToRGBA(const cv::Mat &alpha, const cv::Scalar &fillColor) {
    std::vector<cv::Mat> channels(4);
    // R, G, B from fillColor multiplied by alpha/255
    cv::Mat alphaFloat;
    alpha.convertTo(alphaFloat, CV_32FC1, 1.0 / 255.0);

    for (int c = 0; c < 3; c++) {
        channels[c] = cv::Mat(alpha.size(), CV_8UC1, fillColor[c]);
    }
    channels[3] = alpha.clone();

    cv::Mat result;
    cv::merge(channels, result);
    return result;
}

/**
 * Convert cv::Mat (RGBA, CV_8UC4) to Android Bitmap.
 */
static jobject matToBitmap(JNIEnv *env, const cv::Mat &mat) {
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmap = env->GetStaticMethodID(
            bitmapClass,
            "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
    );

    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888",
                                                   "Landroid/graphics/Bitmap$Config;");
    jobject config = env->GetStaticObjectField(configClass, argb8888Field);

    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmap, mat.cols, mat.rows,
                                                 config);

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
        memcpy(pixels, mat.data, mat.total() * mat.elemSize());
        AndroidBitmap_unlockPixels(env, bitmap);
    }

    return bitmap;
}

/**
 * Alpha-composite src over dst.
 * result = src over dst (standard "over" compositing, non-premultiplied alpha)
 */
static cv::Mat alphaComposite(const cv::Mat &src, const cv::Mat &dst) {
    CV_Assert(src.size() == dst.size());
    CV_Assert(src.type() == CV_8UC4 && dst.type() == CV_8UC4);

    cv::Mat result = dst.clone();
    for (int y = 0; y < src.rows; y++) {
        const uint8_t *srcRow = src.ptr<uint8_t>(y);
        uint8_t *dstRow = result.ptr<uint8_t>(y);
        for (int x = 0; x < src.cols; x++) {
            int i = x * 4;
            float srcA = srcRow[i + 3] / 255.0f;
            float dstA = dstRow[i + 3] / 255.0f;

            // Standard "over" compositing
            float outA = srcA + dstA * (1.0f - srcA);
            if (outA > 0.001f) {
                float invOutA = 1.0f / outA;
                for (int c = 0; c < 3; c++) {
                    float outC =
                            (srcRow[i + c] * srcA + dstRow[i + c] * dstA * (1.0f - srcA)) * invOutA;
                    dstRow[i + c] = cv::saturate_cast<uint8_t>(outC);
                }
                dstRow[i + 3] = cv::saturate_cast<uint8_t>(outA * 255.0f);
            } else {
                dstRow[i] = dstRow[i + 1] = dstRow[i + 2] = dstRow[i + 3] = 0;
            }
        }
    }
    return result;
}

extern "C" {

/**
 * Draw white stroke around the icon.
 * Replicates Python IconGeneratorStickerStroke.draw_stroke().
 *
 * @param env       JNI environment
 * @param clazz     calling class
 * @param srcBitmap original icon bitmap (ARGB_8888)
 * @param strokeWidth stroke width in pixels
 * @return padded bitmap with white stroke + original icon pasted on top
 */
JNIEXPORT jobject JNICALL
Java_com_capybara_hypericonlab_core_image_StickerNativeProcessor_nativeDrawStroke(
        JNIEnv *env, jclass clazz, jobject srcBitmap, jint strokeWidth) {

    if (strokeWidth < 1) strokeWidth = 1;

    // Read source bitmap
    cv::Mat srcRGBA = bitmapToMat(env, srcBitmap);
    if (srcRGBA.empty()) {
        LOGE("nativeDrawStroke: empty source bitmap");
        return nullptr;
    }
    AndroidBitmap_unlockPixels(env, srcBitmap);

    int srcW = srcRGBA.cols;
    int srcH = srcRGBA.rows;

    // 1. Extract alpha channel
    cv::Mat alpha = extractAlpha(srcRGBA);

    // 2. Morphological close to bridge small gaps
    int kernelSize = strokeWidth / 2;
    if (kernelSize % 2 == 0) kernelSize += 1;
    if (kernelSize < 1) kernelSize = 1;
    cv::Mat kernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(kernelSize, kernelSize));
    cv::Mat alphaClosed;
    cv::morphologyEx(alpha, alphaClosed, cv::MORPH_CLOSE, kernel);

    // 3. Find outer contours
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(alphaClosed, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

    // 4. Create padded output (same as Python: padding = strokeWidth * 2)
    int padding = strokeWidth * 2;
    int outW = srcW + padding;
    int outH = srcH + padding;
    cv::Mat strokeLayer(outH, outW, CV_8UC4, cv::Scalar(0, 0, 0, 0));

    // 5. Draw white stroke for each contour
    cv::Scalar white(255, 255, 255, 255);
    for (const auto &contour: contours) {
        std::vector<cv::Point> approx;
        cv::approxPolyDP(contour, approx, 0.0, true);
        if (approx.size() < 2) continue;

        std::vector<cv::Point> offsetPoints;
        offsetPoints.reserve(approx.size());
        for (const auto &pt: approx) {
            offsetPoints.push_back(cv::Point(pt.x + strokeWidth, pt.y + strokeWidth));
        }

        // Use a slightly larger thickness for blurring
        cv::polylines(strokeLayer, offsetPoints, true, white, strokeWidth, cv::LINE_AA);
    }

    // --- Quality Optimization: Smooth the stroke layer ---
    cv::Mat alphaStroke;
    cv::extractChannel(strokeLayer, alphaStroke, 3);
    // Apply a small Gaussian blur to smooth the anti-aliased edges further
    cv::GaussianBlur(alphaStroke, alphaStroke, cv::Size(3, 3), 0);
    // Re-insert the smoothed alpha
    std::vector<cv::Mat> strokeChannels(4);
    cv::split(strokeLayer, strokeChannels);
    strokeChannels[3] = alphaStroke;
    cv::merge(strokeChannels, strokeLayer);
    // ------------------------------------------------------

    // 6. Paste original icon at offset (strokeWidth, strokeWidth)
    // Use the alpha channel as mask
    cv::Rect roi(strokeWidth, strokeWidth, srcW, srcH);
    cv::Mat dstROI = strokeLayer(roi);

    for (int y = 0; y < srcH; y++) {
        const uint8_t *srcRow = srcRGBA.ptr<uint8_t>(y);
        uint8_t *dstRow = dstROI.ptr<uint8_t>(y);
        for (int x = 0; x < srcW; x++) {
            int i = x * 4;
            float srcA = srcRow[i + 3] / 255.0f;
            float dstA = dstRow[i + 3] / 255.0f;

            float outA = srcA + dstA * (1.0f - srcA);
            if (outA > 0.001f) {
                float invOutA = 1.0f / outA;
                for (int c = 0; c < 3; c++) {
                    float outC =
                            (srcRow[i + c] * srcA + dstRow[i + c] * dstA * (1.0f - srcA)) * invOutA;
                    dstRow[i + c] = cv::saturate_cast<uint8_t>(outC);
                }
                dstRow[i + 3] = cv::saturate_cast<uint8_t>(outA * 255.0f);
            }
        }
    }

    return matToBitmap(env, strokeLayer);
}

/**
 * Get the stroke mask only (no icon pasted).
 */
JNIEXPORT jobject JNICALL
Java_com_capybara_hypericonlab_core_image_StickerNativeProcessor_nativeGetStrokeMask(
        JNIEnv *env, jclass clazz, jobject srcBitmap, jint strokeWidth) {

    if (strokeWidth < 1) strokeWidth = 1;

    cv::Mat srcRGBA = bitmapToMat(env, srcBitmap);
    if (srcRGBA.empty()) return nullptr;
    AndroidBitmap_unlockPixels(env, srcBitmap);

    cv::Mat alpha = extractAlpha(srcRGBA);

    // 1. Morphological close to bridge small gaps (Required for detect_closed_areas)
    int kSize = strokeWidth / 2;
    if (kSize % 2 == 0) kSize += 1;
    if (kSize < 1) kSize = 1;
    cv::Mat kElement = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(kSize, kSize));
    cv::Mat alphaClosed;
    cv::morphologyEx(alpha, alphaClosed, cv::MORPH_CLOSE, kElement);

    // 2. Find outer contours
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(alphaClosed, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

    // 3. Create padded output (Point: Padding should match strokeWidth to avoid clipping)
    int padding = strokeWidth * 2;
    int outW = alpha.cols + padding;
    int outH = alpha.rows + padding;
    cv::Mat strokeLayer(outH, outW, CV_8UC4, cv::Scalar(0, 0, 0, 0));
    cv::Scalar white(255, 255, 255, 255);

    for (const auto &contour: contours) {
        std::vector<cv::Point> approx;
        cv::approxPolyDP(contour, approx, 0.0, true);
        if (approx.size() < 2) continue;
        std::vector<cv::Point> offsetPoints;
        for (const auto &pt: approx) {
            // Offset points to center the icon within padding
            offsetPoints.push_back(cv::Point(pt.x + strokeWidth, pt.y + strokeWidth));
        }
        // Draw polyline with thickness
        cv::polylines(strokeLayer, offsetPoints, true, white, strokeWidth, cv::LINE_AA);
    }

    // 4. Smooth the alpha channel
    cv::Mat alphaStroke;
    cv::extractChannel(strokeLayer, alphaStroke, 3);
    cv::GaussianBlur(alphaStroke, alphaStroke, cv::Size(3, 3), 0);

    std::vector<cv::Mat> finalChannels(4);
    cv::split(strokeLayer, finalChannels);
    finalChannels[3] = alphaStroke;
    cv::merge(finalChannels, strokeLayer);

    return matToBitmap(env, strokeLayer);
}

/**
 * Detect and fill closed areas (holes) in the icon.
 * Replicates Python IconGeneratorStickerStroke.detect_closed_areas() + fill_closed_areas().
 *
 * @param env        JNI environment
 * @param clazz      calling class
 * @param srcBitmap  stroked icon bitmap (ARGB_8888, with padding from drawStroke)
 * @param fillColor  fill color as ARGB int (e.g. 0xFFB0C4DE for light steel blue)
 * @return bitmap with holes filled, or original if no holes found
 */
JNIEXPORT jobject JNICALL
Java_com_capybara_hypericonlab_core_image_StickerNativeProcessor_nativeFillHoles(
        JNIEnv *env, jclass clazz, jobject srcBitmap, jint fillColor) {

    // Read source bitmap
    cv::Mat srcRGBA = bitmapToMat(env, srcBitmap);
    if (srcRGBA.empty()) {
        LOGE("nativeFillHoles: empty source bitmap");
        return nullptr;
    }
    AndroidBitmap_unlockPixels(env, srcBitmap);

    // 1. Extract alpha channel
    cv::Mat alpha = extractAlpha(srcRGBA);

    // 2. Find contours with hierarchy (RETR_CCOMP to detect holes)
    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;
    cv::findContours(alpha, contours, hierarchy, cv::RETR_CCOMP, cv::CHAIN_APPROX_SIMPLE);

    // 3. Identify closed areas: outer contours that have child contours (hierarchy[i][2] != -1)
    std::vector<std::vector<cv::Point>> closedContours;
    if (!hierarchy.empty()) {
        for (size_t i = 0; i < hierarchy.size(); i++) {
            if (hierarchy[i][2] != -1) { // Has child contour = closed area
                closedContours.push_back(contours[i]);
            }
        }
    }

    // 4. If no closed areas, return original bitmap
    if (closedContours.empty()) {
        return srcBitmap; // Caller should not release this - it's a new local ref
    }

    // 5. Create fill mask: draw filled polygons for closed contours
    cv::Mat mask = cv::Mat::zeros(alpha.size(), CV_8UC1);
    cv::drawContours(mask, closedContours, -1, cv::Scalar(255), cv::FILLED);

    // --- Quality Optimization: Smooth the fill mask ---
    cv::GaussianBlur(mask, mask, cv::Size(3, 3), 0);
    // ---------------------------------------------------

    // 6. Create fill layer: RGBA image with fill color where mask is non-zero
    uint8_t a = (fillColor >> 24) & 0xFF;
    uint8_t r = (fillColor >> 16) & 0xFF;
    uint8_t g = (fillColor >> 8) & 0xFF;
    uint8_t b = fillColor & 0xFF;

    cv::Mat fillLayer(alpha.size(), CV_8UC4, cv::Scalar(0, 0, 0, 0));
    for (int y = 0; y < mask.rows; y++) {
        const uint8_t *maskRow = mask.ptr<uint8_t>(y);
        uint8_t *fillRow = fillLayer.ptr<uint8_t>(y);
        for (int x = 0; x < mask.cols; x++) {
            if (maskRow[x] == 255) {
                int i = x * 4;
                fillRow[i] = r;
                fillRow[i + 1] = g;
                fillRow[i + 2] = b;
                fillRow[i + 3] = a;
            }
        }
    }

    // 7. Alpha composite fill layer over original icon
    cv::Mat result = alphaComposite(fillLayer, srcRGBA);

    return matToBitmap(env, result);
}

/**
 * Get the hole mask for the icon.
 * Replicates Python IconGeneratorStickerStroke.detect_closed_areas().
 *
 * @param env       JNI environment
 * @param clazz     calling class
 * @param srcBitmap stroked icon bitmap (ARGB_8888, with padding from drawStroke)
 * @return single-channel alpha bitmap where holes are white (255), rest transparent (0)
 */
JNIEXPORT jobject JNICALL
Java_com_capybara_hypericonlab_core_image_StickerNativeProcessor_nativeGetHoleMask(
        JNIEnv *env, jclass clazz, jobject srcBitmap) {

    // Read source bitmap
    cv::Mat srcRGBA = bitmapToMat(env, srcBitmap);
    if (srcRGBA.empty()) {
        LOGE("nativeGetHoleMask: empty source bitmap");
        return nullptr;
    }
    AndroidBitmap_unlockPixels(env, srcBitmap);

    // 1. Extract alpha channel
    cv::Mat alpha = extractAlpha(srcRGBA);

    // 2. Find contours with hierarchy (RETR_CCOMP to detect holes)
    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;
    cv::findContours(alpha, contours, hierarchy, cv::RETR_CCOMP, cv::CHAIN_APPROX_SIMPLE);

    // 3. Identify closed areas: outer contours that have child contours
    std::vector<std::vector<cv::Point>> closedContours;
    if (!hierarchy.empty()) {
        for (size_t i = 0; i < hierarchy.size(); i++) {
            if (hierarchy[i][2] != -1) {
                closedContours.push_back(contours[i]);
            }
        }
    }

    // 4. Create hole mask: 255 where holes are, 0 elsewhere
    cv::Mat mask = cv::Mat::zeros(alpha.size(), CV_8UC1);
    if (!closedContours.empty()) {
        cv::drawContours(mask, closedContours, -1, cv::Scalar(255), cv::FILLED);
        // --- Quality Optimization: Smooth the hole mask ---
        cv::GaussianBlur(mask, mask, cv::Size(3, 3), 0);
        // ---------------------------------------------------
    }

    // 5. Convert to RGBA bitmap for Android (use alpha channel for the mask)
    cv::Mat outRGBA(alpha.size(), CV_8UC4, cv::Scalar(0, 0, 0, 0));
    for (int y = 0; y < mask.rows; y++) {
        const uint8_t *maskRow = mask.ptr<uint8_t>(y);
        uint8_t *outRow = outRGBA.ptr<uint8_t>(y);
        for (int x = 0; x < mask.cols; x++) {
            if (maskRow[x] == 255) {
                int i = x * 4;
                outRow[i] = 255;     // R
                outRow[i + 1] = 255; // G
                outRow[i + 2] = 255; // B
                outRow[i + 3] = 255; // A
            }
        }
    }

    return matToBitmap(env, outRGBA);
}

} // extern "C"