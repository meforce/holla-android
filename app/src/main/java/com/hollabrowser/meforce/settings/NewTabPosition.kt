package com.hollabrowser.meforce.settings

import com.hollabrowser.meforce.preference.IntEnum

/**
 * An enum representing what detail level should be displayed in the search box.
 */
enum class NewTabPosition(override val value: Int) : IntEnum {
    BEFORE_CURRENT_TAB(0),
    AFTER_CURRENT_TAB(1),
    START_OF_TAB_LIST(2),
    END_OF_TAB_LIST(3)
}
