package com.capybara.hypericonlab.core.designsystem.color.ctc

data class CTCColor(
    val name: String,
    val hex: String
)

data class CTCColorScheme(
    val mainName: String,
    val lightBg: CTCColor,
    val lightPrimary: CTCColor,
    val darkBg: CTCColor,
    val darkPrimary: CTCColor,
    val neutralBg: CTCColor,
    val neutralPrimary: CTCColor
)

data class CTCContrastScheme(
    val name: String,
    val bg: CTCColor,
    val primary: CTCColor
)

object CTCPresets {
    val MonochromaticSchemes = listOf(
        CTCColorScheme(
            mainName = "朱红",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("朱红", "#ED5126"),
            darkBg = CTCColor("深牵牛紫", "#1C0D1A"),
            darkPrimary = CTCColor("淡藏花红", "#F6AD8F"),
            neutralBg = CTCColor("朱红", "#ED5126"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "群青",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("群青", "#1772B4"),
            darkBg = CTCColor("燕颔蓝", "#131824"),
            darkPrimary = CTCColor("长春花蓝", "#7EC0EE"),
            neutralBg = CTCColor("群青", "#1772B4"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "藤黄",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("藤黄", "#FFD111"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("茉莉黄", "#F8DF72"),
            neutralBg = CTCColor("藤黄", "#FFD111"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "黛蓝",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("黛蓝", "#2A3C5C"),
            darkBg = CTCColor("燕颔蓝", "#131824"),
            darkPrimary = CTCColor("霜蓝", "#4A6FA6"),
            neutralBg = CTCColor("黛蓝", "#2A3C5C"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "竹青",
            lightBg = CTCColor("白雪藤", "#E8F4F0"),
            lightPrimary = CTCColor("竹青", "#00A86B"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("莹翠", "#3EB489"),
            neutralBg = CTCColor("竹青", "#00A86B"),
            neutralPrimary = CTCColor("白雪藤", "#E8F4F0")
        ),
        CTCColorScheme(
            mainName = "天青",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("天青", "#6C9BCA"),
            darkBg = CTCColor("燕颔蓝", "#131824"),
            darkPrimary = CTCColor("浅灰蓝", "#C2D1E0"),
            neutralBg = CTCColor("天青", "#6C9BCA"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "茜色",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("茜色", "#FF4D4D"),
            darkBg = CTCColor("深牵牛紫", "#1C0D1A"),
            darkPrimary = CTCColor("淡绯", "#F2CAC9"),
            neutralBg = CTCColor("茜色", "#FF4D4D"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "黛紫",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("黛紫", "#5D3A6F"),
            darkBg = CTCColor("乌梅紫", "#1E131D"),
            darkPrimary = CTCColor("蕈紫", "#815C94"),
            neutralBg = CTCColor("黛紫", "#5D3A6F"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "绛紫",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("绛紫", "#8E354A"),
            darkBg = CTCColor("深牵牛紫", "#1C0D1A"),
            darkPrimary = CTCColor("酢酱草红", "#C5708B"),
            neutralBg = CTCColor("绛紫", "#8E354A"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "枣红",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("枣红", "#7C1823"),
            darkBg = CTCColor("深牵牛紫", "#1C0D1A"),
            darkPrimary = CTCColor("唐菖蒲红", "#DE1C31"),
            neutralBg = CTCColor("枣红", "#7C1823"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "碧青",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("碧青", "#5CB3CC"),
            darkBg = CTCColor("燕颔蓝", "#131824"),
            darkPrimary = CTCColor("湖水蓝", "#B0D5DF"),
            neutralBg = CTCColor("碧青", "#5CB3CC"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "荷叶绿",
            lightBg = CTCColor("白雪藤", "#E8F4F0"),
            lightPrimary = CTCColor("荷叶绿", "#1A6840"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("翠微", "#3CB371"),
            neutralBg = CTCColor("荷叶绿", "#1A6840"),
            neutralPrimary = CTCColor("白雪藤", "#E8F4F0")
        ),
        CTCColorScheme(
            mainName = "松花",
            lightBg = CTCColor("嫩菊绿", "#F0F5E5"),
            lightPrimary = CTCColor("松花", "#B6D7A8"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("艾背绿", "#DFECD5"),
            neutralBg = CTCColor("松花", "#B6D7A8"),
            neutralPrimary = CTCColor("嫩菊绿", "#F0F5E5")
        ),
        CTCColorScheme(
            mainName = "玄青",
            lightBg = CTCColor("霜色", "#E8F4F8"),
            lightPrimary = CTCColor("玄青", "#2B3A4F"),
            darkBg = CTCColor("燕颔蓝", "#131824"),
            darkPrimary = CTCColor("霜蓝", "#4A6FA6"),
            neutralBg = CTCColor("玄青", "#2B3A4F"),
            neutralPrimary = CTCColor("霜色", "#E8F4F8")
        ),
        CTCColorScheme(
            mainName = "胭脂泪",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("胭脂泪", "#9D4E5C"),
            darkBg = CTCColor("深牵牛紫", "#1C0D1A"),
            darkPrimary = CTCColor("山黎豆红", "#C27C88"),
            neutralBg = CTCColor("胭脂泪", "#9D4E5C"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "汉绣绿",
            lightBg = CTCColor("嫩菊绿", "#F0F5E5"),
            lightPrimary = CTCColor("汉绣绿", "#2E7D32"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("柳绿", "#70C870"),
            neutralBg = CTCColor("汉绣绿", "#2E7D32"),
            neutralPrimary = CTCColor("嫩菊绿", "#F0F5E5")
        ),
        CTCColorScheme(
            mainName = "鎏金",
            lightBg = CTCColor("蚌肉白", "#F9F1DB"),
            lightPrimary = CTCColor("鎏金", "#D4AF37"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("象牙黄", "#F0D695"),
            neutralBg = CTCColor("鎏金", "#D4AF37"),
            neutralPrimary = CTCColor("蚌肉白", "#F9F1DB")
        ),
        CTCColorScheme(
            mainName = "深绿",
            lightBg = CTCColor("白雪藤", "#E8F4F0"),
            lightPrimary = CTCColor("深绿", "#0D5E3A"),
            darkBg = CTCColor("玄黑", "#0E100F"),
            darkPrimary = CTCColor("翠青", "#2AAE6F"),
            neutralBg = CTCColor("深绿", "#0D5E3A"),
            neutralPrimary = CTCColor("白雪藤", "#E8F4F0")
        )
    )

    val ContrastSchemes = listOf(
        CTCContrastScheme(
            "乳白 & 灰蓝",
            CTCColor("乳白", "#F9F4DC"),
            CTCColor("灰蓝", "#21373D")
        ),
        CTCContrastScheme(
            "乳白 & 栗紫",
            CTCColor("乳白", "#F9F4DC"),
            CTCColor("栗紫", "#5A191B")
        ),
        CTCContrastScheme(
            "秋葵黄 & 玄色",
            CTCColor("秋葵黄", "#EED045"),
            CTCColor("玄色", "#1A1A1A")
        ),
        CTCContrastScheme(
            "朱砂红 & 杏仁黄",
            CTCColor("朱砂红", "#D92121"),
            CTCColor("杏仁黄", "#F7E8AA")
        ),
        CTCContrastScheme(
            "朱砂红 & 明黄",
            CTCColor("朱砂红", "#D92121"),
            CTCColor("明黄", "#FFD700")
        ),
        CTCContrastScheme(
            "枣红 & 乳白",
            CTCColor("枣红", "#7C1823"),
            CTCColor("乳白", "#F9F4DC")
        ),
        CTCContrastScheme(
            "浅苋菜紫 & 玄黑",
            CTCColor("浅苋菜紫", "#D8BFD8"),
            CTCColor("玄黑", "#0E100F")
        ),
        CTCContrastScheme(
            "姚黄 & 墨紫",
            CTCColor("姚黄", "#D0DEAA"),
            CTCColor("墨紫", "#310F1B")
        ),
        CTCContrastScheme(
            "篾黄 & 晶石紫",
            CTCColor("篾黄", "#F7DE98"),
            CTCColor("晶石紫", "#1F2040")
        ),
        CTCContrastScheme(
            "云杉绿 & 奶黄色",
            CTCColor("云杉绿", "#15231B"),
            CTCColor("奶黄色", "#F9E4A5")
        ),
        CTCContrastScheme(
            "莽丛绿 & 菊蕾白",
            CTCColor("莽丛绿", "#141E1B"),
            CTCColor("菊蕾白", "#E9DDB6")
        ),
        CTCContrastScheme(
            "海天蓝 & 暗龙胆紫",
            CTCColor("海天蓝", "#C6E6E8"),
            CTCColor("暗龙胆紫", "#22202E")
        ),
        CTCContrastScheme(
            "东方既白 & 古鼎灰",
            CTCColor("东方既白", "#E6F2FF"),
            CTCColor("古鼎灰", "#36292F")
        ),
        CTCContrastScheme(
            "靛蓝 & 梨花白",
            CTCColor("靛蓝", "#4B0082"),
            CTCColor("梨花白", "#F8F5F0")
        ),
        CTCContrastScheme(
            "墨色 & 杏子",
            CTCColor("墨色", "#1D1B1C"),
            CTCColor("杏子", "#FDDA9F")
        ),
        CTCContrastScheme(
            "水牛灰 & 乳白",
            CTCColor("水牛灰", "#2F2F35"),
            CTCColor("乳白", "#F9F4DC")
        ),
        CTCContrastScheme(
            "槲寄生绿 & 乳白",
            CTCColor("槲寄生绿", "#2B312C"),
            CTCColor("乳白", "#F9F4DC")
        ),
        CTCContrastScheme(
            "松花 & 钢蓝",
            CTCColor("松花", "#B6D7A8"),
            CTCColor("钢蓝", "#0F1423")
        )
    )
}
