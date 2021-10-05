package com.hollabrowser.meforce.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import androidx.annotation.AttrRes
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.utils.ThemeUtils

/**
 * Create a new transition drawable with the specified list of layers. At least
 * 2 layers are required for this drawable to work properly.
 */
class BackgroundDrawable(
    context: Context,
    @AttrRes first: Int = R.attr.haloColor,
    @AttrRes second: Int = R.attr.colorSurface
) : TransitionDrawable(
    arrayOf<Drawable>(
        ColorDrawable(ThemeUtils.getColor(context, first)),
        ColorDrawable(ThemeUtils.getColor(context, second))
    )
)
{

    var isSelected: Boolean = false

    override fun startTransition(durationMillis: Int) {
        if (!isSelected) {
            super.startTransition(durationMillis)
        }
        isSelected = true
    }

    override fun reverseTransition(duration: Int) {
        if (isSelected) {
            super.reverseTransition(duration)
        }
        isSelected = false
    }

}
