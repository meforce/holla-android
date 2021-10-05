package com.hollabrowser.meforce.html.incognito

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the incognito HTML.
 */
@FileStream("app/src/main/html/private.html")
interface IncognitoPageReader {

    fun provideHtml(): String

}