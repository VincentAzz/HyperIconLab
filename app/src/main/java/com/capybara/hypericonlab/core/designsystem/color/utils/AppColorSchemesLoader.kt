package com.capybara.hypericonlab.core.designsystem.color.utils

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object AppColorSchemesLoader {

    fun loadFromAssets(context: Context): Map<String, Pair<String, String>> {
        return try {
            val inputStream: InputStream =
                context.assets.open("color_schemes/app_color_schemes.xml")
            loadFromStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    // 从任意 InputStream 解析 color_schemes（支持 assets 和云端两种来源）
    fun loadFromStream(inputStream: InputStream): Map<String, Pair<String, String>> {
        val schemes = mutableMapOf<String, Pair<String, String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val pkg = parser.getAttributeValue(null, "package")
                    val fg = parser.getAttributeValue(null, "fg_color")
                    val bg = parser.getAttributeValue(null, "bg_color")

                    if (pkg != null && fg != null && bg != null) {
                        schemes[pkg] = Pair(fg, bg)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {
            }
        }
        return schemes
    }
}