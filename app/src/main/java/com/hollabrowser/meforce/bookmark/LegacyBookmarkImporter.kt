package com.hollabrowser.meforce.bookmark

import com.hollabrowser.meforce.database.Bookmark
import com.hollabrowser.meforce.database.bookmark.BookmarkExporter
import java.io.InputStream
import javax.inject.Inject

/**
 * A [BookmarkImporter] that imports bookmark files that were produced by BookmarkExporter.
 */
class LegacyBookmarkImporter @Inject constructor() : BookmarkImporter {

    override fun importBookmarks(inputStream: InputStream): List<Bookmark.Entry> {
        return BookmarkExporter.importBookmarksFromFileStream(inputStream)
    }

}
