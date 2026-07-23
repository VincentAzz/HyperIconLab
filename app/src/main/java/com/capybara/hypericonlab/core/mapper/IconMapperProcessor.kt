package com.capybara.hypericonlab.core.mapper

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringWriter
import java.util.regex.Pattern

/**
 * icon_mapper 条目完整信息（含应用名、包名、drawable），用于资产浏览与搜索。
 */
data class IconMapperEntry(
    val name: String,
    val packageName: String,
    val drawable: String
)

// mapper处理器
object IconMapperProcessor {

    private const val TAG = "IconMapperProcessor"
    private val COMPONENT_PATTERN = Pattern.compile("ComponentInfo\\{([^/]+)/.*?\\}")

    fun parseComponentInfo(component: String): String? {
        val matcher = COMPONENT_PATTERN.matcher(component)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    fun convertIconMapper(
        appfilterFile: File,
        outputFile: File,
        altMapperFile: File? = null
    ) {
        val uniquePackages = mutableMapOf<String, Pair<String, String>>()

        Timber.tag(TAG).d("Starting conversion. Appfilter: ${appfilterFile.absolutePath}")

        // Parse appfilter.xml
        if (appfilterFile.exists()) {
            try {
                val parser = Xml.newPullParser()
                parser.setInput(appfilterFile.inputStream(), "UTF-8")
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component") ?: ""
                        val name = parser.getAttributeValue(null, "name") ?: ""
                        val drawable = parser.getAttributeValue(null, "drawable") ?: ""

                        if (component.isNotEmpty() && drawable.isNotEmpty()) {
                            val pkg = parseComponentInfo(component)
                            if (pkg != null) {
                                uniquePackages[pkg] = Pair(name, drawable)
                            }
                        }
                    }
                    eventType = parser.next()
                }
                Timber.tag(TAG).d("Parsed ${uniquePackages.size} items from appfilter")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error parsing appfilter")
            }
        } else {
            Timber.tag(TAG).e("Appfilter file not found!")
        }

        // Merge alt mapper (icon_mapper_alt.xml)
        if (altMapperFile != null && altMapperFile.exists()) {
            try {
                val parser = Xml.newPullParser()
                parser.setInput(altMapperFile.inputStream(), "UTF-8")
                var eventType = parser.eventType
                var altCount = 0
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val pkg = parser.getAttributeValue(null, "package") ?: ""
                        val name = parser.getAttributeValue(null, "name") ?: ""
                        val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                        if (pkg.isNotEmpty() && drawable.isNotEmpty()) {
                            uniquePackages[pkg] = Pair(name, drawable)
                            altCount++
                        }
                    }
                    eventType = parser.next()
                }
                Timber.tag(TAG).d("Merged $altCount items from alt mapper")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error parsing alt mapper")
            }
        }

        // Write to icon_mapper.xml
        Timber.tag(TAG)
            .d("Writing final mapper with ${uniquePackages.size} items to ${outputFile.absolutePath}")
        writeXml(uniquePackages, outputFile)
    }

    private fun writeXml(data: Map<String, Pair<String, String>>, outputFile: File) {
        try {
            val serializer: XmlSerializer = Xml.newSerializer()
            val writer = StringWriter()
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", null)

            serializer.text("\n")
            serializer.startTag(null, "resources")
            serializer.text("\n")

            data.toSortedMap().forEach { (pkg, pair) ->
                serializer.text("    ")
                serializer.startTag(null, "item")
                serializer.attribute(null, "name", pair.first)
                serializer.attribute(null, "package", pkg)
                serializer.attribute(null, "drawable", pair.second)
                serializer.endTag(null, "item")
                serializer.text("\n")
            }

            serializer.endTag(null, "resources")
            serializer.endDocument()

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                fos.write(writer.toString().toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            Timber.tag(TAG).d("XML write complete. File size: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error writing XML")
        }
    }

    /**
     * Parses the icon_mapper.xml into a Map<PackageName, DrawableName>.
     */
    fun parseIconMapper(xmlFile: File): Map<String, String> {
        val mapper = mutableMapOf<String, String>()
        if (!xmlFile.exists()) {
            Timber.tag(TAG).e("Mapper file not found for parsing: ${xmlFile.absolutePath}")
            return mapper
        }
        try {
            xmlFile.inputStream().use { stream ->
                parseIconMapperInternal(stream, xmlFile.name, mapper)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing mapper file")
        }
        return mapper
    }

    /**
     * 从 [InputStream] 直接解析 icon_mapper.xml，避免落盘。
     * 调用方负责关闭流。
     */
    fun parseIconMapper(inputStream: InputStream): Map<String, String> {
        val mapper = mutableMapOf<String, String>()
        try {
            parseIconMapperInternal(inputStream, "inputStream", mapper)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing mapper from stream")
        }
        return mapper
    }

    // 解析 mapper XML 内部实现，由 File/InputStream 重载共用
    private fun parseIconMapperInternal(
        stream: InputStream,
        nameForLog: String,
        mapper: MutableMap<String, String>
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val pkg = parser.getAttributeValue(null, "package")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (pkg != null && drawable != null) {
                    mapper[pkg] = drawable
                }
            }
            eventType = parser.next()
        }
        Timber.tag(TAG).d("Parsed ${mapper.size} items from $nameForLog")
    }

    /**
     * 从 [InputStream] 解析 icon_mapper.xml，返回保留应用名的完整条目列表。
     * 供资产浏览页使用（需通过应用名/包名搜索）。调用方负责关闭流。
     */
    fun parseIconMapperEntries(inputStream: InputStream): List<IconMapperEntry> {
        val entries = mutableListOf<IconMapperEntry>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val name = parser.getAttributeValue(null, "name") ?: ""
                    val pkg = parser.getAttributeValue(null, "package") ?: ""
                    val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                    if (pkg.isNotEmpty() && drawable.isNotEmpty()) {
                        entries.add(
                            IconMapperEntry(
                                name = name,
                                packageName = pkg,
                                drawable = drawable
                            )
                        )
                    }
                }
                eventType = parser.next()
            }
            Timber.tag(TAG).d("Parsed ${entries.size} entries from inputStream")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing mapper entries from stream")
        }
        return entries
    }
}