package com.hollabrowser.meforce

import com.hollabrowser.meforce.preference.IntEnum

/**
 * The available accent themes.
 */
enum class AccentTheme(override val value: Int) : IntEnum {
    BLUE(0),
    PINK(1),
    PURPLE(2),
    DEEP_PURPLE(3),
    INDIGO(4),
    DEFAULT_ACCENT(5),
    LIGHT_BLUE(6),
    CYAN(7),
    TEAL(8),
    GREEN(9),
    LIGHT_GREEN(10),
    LIME(11),
    YELLOW(12),
    AMBER(13),
    ORANGE(14),
    DEEP_ORANGE(15),
    BROWN(16)
}
