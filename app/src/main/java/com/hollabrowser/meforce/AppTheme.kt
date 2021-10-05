package com.hollabrowser.meforce

import com.hollabrowser.meforce.preference.IntEnum

/**
 * The available app themes.
 */
enum class AppTheme(override val value: Int) : IntEnum {
    DEFAULT(0),
    LIGHT(1),
    DARK(2),
    BLACK(3)
}
