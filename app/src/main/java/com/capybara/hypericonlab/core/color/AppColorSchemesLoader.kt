package com.capybara.hypericonlab.core.color

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Loads and parses the app_color_schemes.xml from assets.
 */
object AppColorSchemesLoader {

    /**
     * Loads color schemes into a Map for fast lookup.
     */
    fun loadFromAssets(context: Context): Map<String, Pair<String, String>> {
        val schemes = mutableMapOf<String, Pair<String, String>>()
        try {
            val inputStream: InputStream =
                context.assets.open("color_schemes/app_color_schemes.xml")
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
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return schemes
    }
}