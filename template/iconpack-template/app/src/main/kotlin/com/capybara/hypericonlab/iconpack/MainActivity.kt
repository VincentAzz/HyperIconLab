package com.capybara.hypericonlab.iconpack

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

private object PreviewConfig {
    const val GRID_COLUMN_WIDTH_DP = 104
    const val PAGE_PADDING_DP = 12
    const val ITEM_PADDING_DP = 8
    const val ICON_SIZE_DP = 64
    const val LABEL_TEXT_SIZE_SP = 12f
}

/**
 * 图标包预览页：读取 CI 生成的预览索引，展示槽位图标并提供本地搜索。
 */
class MainActivity : Activity() {
    private val allEntries = mutableListOf<IconEntry>()
    private lateinit var adapter: IconAdapter
    private lateinit var countView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadEntries()
        setContentView(createContent())
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(PreviewConfig.PAGE_PADDING_DP),
                dp(PreviewConfig.PAGE_PADDING_DP),
                dp(PreviewConfig.PAGE_PADDING_DP),
                0
            )
        }

        val searchView = SearchView(this).apply {
            isIconifiedByDefault = false
            queryHint = getString(R.string.search_hint)
        }
        root.addView(
            searchView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        countView = TextView(this).apply {
            setPadding(
                dp(PreviewConfig.ITEM_PADDING_DP),
                dp(PreviewConfig.ITEM_PADDING_DP),
                0,
                dp(PreviewConfig.ITEM_PADDING_DP)
            )
        }
        root.addView(countView)

        val grid = GridView(this).apply {
            numColumns = GridView.AUTO_FIT
            columnWidth = dp(PreviewConfig.GRID_COLUMN_WIDTH_DP)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(PreviewConfig.ITEM_PADDING_DP)
            adapter = IconAdapter(this@MainActivity, allEntries).also {
                this@MainActivity.adapter = it
            }
        }
        root.addView(
            grid,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        updateCount(allEntries.size)
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(query: String?): Boolean {
                    updateCount(adapter.filter(query))
                    return true
                }
            }
        )
        return root
    }

    private fun loadEntries() {
        try {
            resources.getXml(R.xml.preview_icons).use { parser ->
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") {
                        continue
                    }
                    val drawable = parser.getAttributeValue(null, "drawable") ?: continue
                    val drawableId = resources.getIdentifier(drawable, "drawable", packageName)
                    if (drawableId == 0) continue

                    allEntries += IconEntry(
                        name = parser.getAttributeValue(null, "name"),
                        packageName = parser.getAttributeValue(null, "package"),
                        drawable = drawable,
                        drawableId = drawableId
                    )
                }
            }
        } catch (_: Exception) {
            // 原型预览失败时保持空列表，模板静态校验负责报告资源问题。
        }
    }

    private fun updateCount(count: Int) {
        countView.text = getString(R.string.icon_count_format, count)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()
}

private data class IconEntry(
    val name: String?,
    val packageName: String?,
    val drawable: String,
    val drawableId: Int
) {
    val searchableText: String
        get() = "${name.orEmpty()} ${packageName.orEmpty()} $drawable".lowercase(Locale.ROOT)
}

private class IconAdapter(
    private val context: Context,
    private val source: List<IconEntry>
) : BaseAdapter() {
    private val visible = source.toMutableList()

    fun filter(query: String?): Int {
        val keyword = query.orEmpty().trim().lowercase(Locale.ROOT)
        visible.clear()
        visible += source.filter { keyword.isEmpty() || keyword in it.searchableText }
        notifyDataSetChanged()
        return visible.size
    }

    override fun getCount(): Int = visible.size

    override fun getItem(position: Int): IconEntry = visible[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val entry = getItem(position)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padding = context.dp(PreviewConfig.ITEM_PADDING_DP)
            setPadding(padding, padding, padding, padding)

            addView(
                ImageView(context).apply {
                    setImageResource(entry.drawableId)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(
                    context.dp(PreviewConfig.ICON_SIZE_DP),
                    context.dp(PreviewConfig.ICON_SIZE_DP)
                )
            )

            addView(
                TextView(context).apply {
                    text = entry.name.takeUnless { it.isNullOrEmpty() } ?: entry.drawable
                    textSize = PreviewConfig.LABEL_TEXT_SIZE_SP
                    setTextColor(context.resolvePrimaryTextColor())
                    gravity = Gravity.CENTER
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}

private fun Context.dp(value: Int): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value.toFloat(),
    resources.displayMetrics
).toInt()

private fun Context.resolvePrimaryTextColor(): Int {
    val value = TypedValue()
    return if (theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)) {
        value.data
    } else {
        Color.DKGRAY
    }
}
