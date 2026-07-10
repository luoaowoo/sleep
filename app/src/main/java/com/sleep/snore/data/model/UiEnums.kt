package com.sleep.snore.data.model

/** 主题强调色选项 */
enum class AccentColor {
    INDIGO,
    BLUE,
    GREEN,
    ORANGE,
    RED,
    CYAN,
    PINK,
    CUSTOM
}

/** 字号缩放选项 */
enum class FontScale {
    SMALL,
    STANDARD,
    LARGE
}

/** 卡片圆角风格选项 */
enum class CardCornerStyle {
    STANDARD,
    SOFT,
    SHARP
}

/** 鼾声检测灵敏度选项，默认 MEDIUM */
enum class Sensitivity {
    LOW,
    MEDIUM,
    HIGH
}
