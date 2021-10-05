package com.hollabrowser.meforce.browser

import com.hollabrowser.meforce.preference.IntEnum

/**
 * The available Block JavaScript choices.
 */
enum class JavaScriptChoice(override val value: Int) : IntEnum {
    NONE(0),
    WHITELIST(1),
    BLACKLIST(2)
}
