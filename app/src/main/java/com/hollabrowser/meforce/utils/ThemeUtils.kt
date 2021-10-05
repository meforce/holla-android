package com.hollabrowser.meforce.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.hollabrowser.meforce.R

object ThemeUtils {

    private val sTypedValue = TypedValue()

    /**
     * Gets the primary color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary color of the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getPrimaryColor(context: Context): Int {
        return getColor(context, R.attr.haloColor)
    }

    /**
     * Gets the primary dark color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary dark color of the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getPrimaryColorDark(context: Context): Int {
        return getColor(context, R.attr.colorPrimaryDark)
    }

    /**
     * Gets the surface color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the surface color of the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getSurfaceColor(context: Context): Int {
        return getColor(context, R.attr.haloColor)
    }

    /**
     * Gets on surface color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the on surface color of the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getOnSurfaceColor(context: Context): Int {
        return getColor(context, R.attr.colorOnSurface)
    }

    /**
     * Get background color from current theme.
     * Though in fact this should be the same as surface color anyway.
     * I believe that attribute is mostly used for backward compatibility issues.
     *
     * @param context the context to get the theme from.
     * @return the background color as defined in the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getBackgroundColor(context: Context): Int {
        return getColor(context, android.R.attr.colorBackground)
    }

    /**
     * Gets the accent color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the accent color of the current theme.
     */
    @JvmStatic
    @ColorInt
    fun getAccentColor(context: Context): Int {
        return getColor(context, R.attr.colorAccent)
    }

    /**
     * Gets the color of the status bar as set in styles
     * for the current theme.
     *
     * @param context the context to get the theme from.
     * @return the status bar color of the current theme.
     */
    @JvmStatic
    @ColorInt
    @TargetApi(21)
    fun getStatusBarColor(context: Context): Int {
        return getColor(context, android.R.attr.statusBarColor)
    }

    @JvmStatic
    @ColorInt
    fun getSearchBarColor(context: Context): Int {
        return getColor(context, R.attr.colorSurface)
    }

    @JvmStatic
    @ColorInt
    fun getSearchBarTextColor(context: Context): Int {
        return getColor(context, R.attr.colorOnSurface)
    }

    /**
     * Gets the color attribute from the current theme.
     *
     * @param context  the context to get the theme from.
     * @param resource the color attribute resource.
     * @return the color for the given attribute.
     */
    @JvmStatic
    @ColorInt
    fun getColor(context: Context, @AttrRes resource: Int): Int {
        val a: TypedArray = context.obtainStyledAttributes(sTypedValue.data, intArrayOf(resource))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    /**
     * Gets the icon color for the light theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @JvmStatic
    @ColorInt
    private fun getIconLightThemeColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.icon_light_theme)
    }

    /**
     * Gets the icon color for the dark theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @JvmStatic
    @ColorInt
    private fun getIconDarkThemeColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.icon_dark_theme)
    }

    /**
     * Gets the color icon for the light or
     * dark theme.
     *
     * @param context the context to use.
     * @param dark    true for the dark theme,
     * false for the light theme.
     * @return the color of the icon.
     */
    @JvmStatic
    @ColorInt
    fun getIconThemeColor(context: Context, dark: Boolean): Int {
        return if (dark) getIconDarkThemeColor(context) else getIconLightThemeColor(context)
    }

    @JvmStatic
    private fun getVectorDrawable(context: Context, drawableId: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        return drawable!!
    }

    // http://stackoverflow.com/a/38244327/1499541
    @JvmStatic
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable: Drawable = getVectorDrawable(context, drawableId)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Gets the icon with an applied color filter
     * for the correct theme.
     *
     * @param context the context to use.
     * @param res     the drawable resource to use.
     * @param dark    true for icon suitable for use with a dark theme,
     * false for icon suitable for use with a light theme.
     * @return a themed icon.
     */
    @JvmStatic
    fun createThemedBitmap(context: Context, @DrawableRes res: Int, dark: Boolean): Bitmap {
        val color: Int = if (dark) getIconDarkThemeColor(context) else getIconLightThemeColor(context)
        val sourceBitmap: Bitmap = getBitmapFromVectorDrawable(context, res)
        val resultBitmap = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height,
                Bitmap.Config.ARGB_8888)
        val p = Paint()
        val filter: ColorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        p.colorFilter = filter
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, p)
        sourceBitmap.recycle()
        return resultBitmap
    }

    /**
     * Gets the edit text text color for the current theme.
     *
     * @param context the context to use.
     * @return a text color.
     */
    @ColorInt
    @JvmStatic
    fun getTextColor(context: Context): Int {
        return getColor(context, android.R.attr.editTextColor)
    }


    /**
     *
     */
    @JvmStatic
    fun getSearchBarColor(requestedColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(requestedColor)
        return if (luminance > 0.9) {
            // Too bright, make it darker then
            DrawableUtils.mixColor(0.00f, requestedColor, Color.BLACK)
        } else {
            // Make search text field background lighter
            DrawableUtils.mixColor(0.20f, requestedColor, Color.WHITE)
        }
    }

    /**
     *
     */
    @JvmStatic
    fun getSearchBarFocusedColor(requestedColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(requestedColor)
        return if (luminance > 0.9) {
            // Too bright, make it darker then
            DrawableUtils.mixColor(0.35f, requestedColor, Color.BLACK)
        } else {
            // Make search text field background lighter
            DrawableUtils.mixColor(0.35f, requestedColor, Color.WHITE)
        }
    }

}
