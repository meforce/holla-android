package com.hollabrowser.meforce.browser.bookmarks

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.animation.AnimationUtils
import com.hollabrowser.meforce.browser.BookmarksView
import com.hollabrowser.meforce.controller.UIController
import com.hollabrowser.meforce.database.Bookmark
import com.hollabrowser.meforce.database.bookmark.BookmarkRepository
import com.hollabrowser.meforce.databinding.BookmarkDrawerViewBinding
import com.hollabrowser.meforce.di.DatabaseScheduler
import com.hollabrowser.meforce.di.MainScheduler
import com.hollabrowser.meforce.di.NetworkScheduler
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.dialog.StyxDialogBuilder
import com.hollabrowser.meforce.extensions.inflater
import com.hollabrowser.meforce.favicon.FaviconModel
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.*
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject

/**
 * The view that displays bookmarks in a list and some controls.
 */
@SuppressLint("ViewConstructor")
class BookmarksDrawerView @JvmOverloads constructor(
        context: Context,
        private val activity: Activity,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        userPreferences: UserPreferences
) : LinearLayout(context, attrs, defStyleAttr), BookmarksView {

    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject internal lateinit var bookmarksDialogBuilder: StyxDialogBuilder
    @Inject internal lateinit var faviconModel: FaviconModel
    @Inject lateinit var userPreferences: UserPreferences
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:NetworkScheduler internal lateinit var networkScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    private val uiController: UIController

    // Adapter
    private var iAdapter: BookmarksAdapter
    // Drag & drop support
    private var iItemTouchHelper: ItemTouchHelper? = null

    // Colors
    private var scrollIndex: Int = 0

    private var bookmarksSubscription: Disposable? = null
    private var bookmarkUpdateSubscription: Disposable? = null

    private val uiModel = BookmarkUiModel()

    var iBinding: BookmarkDrawerViewBinding

    private var addBookmarkView: ImageView? = null

    init {

        context.injector.inject(this)

        uiController = context as UIController

        iBinding = BookmarkDrawerViewBinding.inflate(context.inflater,this, true)

        iBinding.uiController = uiController


        iBinding.bookmarkBackButton.setOnClickListener {
            if (!uiModel.isCurrentFolderRoot()) {
                setBookmarksShown(null, true)
                iBinding.listBookmarks.layoutManager?.scrollToPosition(scrollIndex)
            }
        }

        addBookmarkView = findViewById(R.id.menuItemAddBookmark)
        addBookmarkView?.setOnClickListener { uiController.bookmarkButtonClicked() }

        iAdapter = BookmarksAdapter(
                context,
                uiController,
                faviconModel,
                networkScheduler,
                mainScheduler,
                ::showBookmarkMenu,
                ::openBookmark
        )

        iBinding.listBookmarks.apply {
            // Reverse layout if using bottom tool bars
            // LinearLayoutManager.setReverseLayout is also adjusted from BrowserActivity.setupToolBar
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, userPreferences.toolbarsBottom)
            adapter = iAdapter
        }

        // Enable drag & drop but not swipe
        val callback: ItemTouchHelper.Callback = ItemDragDropSwipeHelper(iAdapter, aLongPressDragEnabled = true, aSwipeEnabled = false)
        iItemTouchHelper = ItemTouchHelper(callback)
        iItemTouchHelper?.attachToRecyclerView(iBinding.listBookmarks)

        setBookmarksShown(null, true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        bookmarksSubscription?.dispose()
        bookmarkUpdateSubscription?.dispose()

        iAdapter.cleanupSubscriptions()
    }

    private fun updateBookmarkIndicator(url: String) {
        bookmarkUpdateSubscription?.dispose()
        bookmarkUpdateSubscription = bookmarkModel.isBookmark(url)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { isBookmark ->
                bookmarkUpdateSubscription = null
                addBookmarkView?.isSelected = isBookmark
                addBookmarkView?.isEnabled = !url.isSpecialUrl() && !url.isHomeUri() && !url.isBookmarkUri() && !url.isHistoryUri()
            }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> setBookmarksShown(null, false)
        is Bookmark.Entry -> iAdapter.deleteItem(BookmarksViewModel(bookmark))
    }

    /**
     *
     */
    private fun setBookmarksShown(folder: String?, animate: Boolean) {
        bookmarksSubscription?.dispose()
        bookmarksSubscription = bookmarkModel.getBookmarksFromFolderSorted(folder)
            .concatWith(Single.defer {
                if (folder == null) {
                    bookmarkModel.getFoldersSorted()
                } else {
                    Single.just(emptyList())
                }
            })
            .toList()
            .map { it.flatten() }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { bookmarksAndFolders ->
                uiModel.currentFolder = folder
                setBookmarkDataSet(bookmarksAndFolders, animate)
                iBinding.textTitle.text = if (folder.isNullOrBlank()) resources.getString(R.string.action_bookmarks) else folder
            }
    }

    /**
     *
     */
    private fun setBookmarkDataSet(items: List<Bookmark>, animate: Boolean) {
        iAdapter.updateItems(items.map { BookmarksViewModel(it) })
        val resource = if (uiModel.isCurrentFolderRoot()) {
            R.drawable.round_star_border_24
        } else {
            R.drawable.ic_action_back
        }

        if (animate) {
            iBinding.bookmarkBackButton.let {
                val transition = AnimationUtils.createRotationTransitionAnimation(it, resource)
                it.startAnimation(transition)
            }
        } else {
            iBinding.bookmarkBackButton.setImageResource(resource)
        }
    }

    /**
     *
     */
    private fun showBookmarkMenu(bookmark: Bookmark): Boolean {
        (context as AppCompatActivity?)?.let {
            when (bookmark) {
                is Bookmark.Folder -> bookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(it, uiController, bookmark)
                is Bookmark.Entry -> bookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(it, uiController, bookmark)
            }
        }
        return true
    }

    /**
     *
     */
    private fun openBookmark(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> {
            scrollIndex = (iBinding.listBookmarks.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            setBookmarksShown(bookmark.title, true)
        }
        is Bookmark.Entry -> uiController.bookmarkItemClicked(bookmark)
    }

    override fun navigateBack() {
        if (uiModel.isCurrentFolderRoot()) {
            uiController.onBackButtonPressed()
        } else {
            setBookmarksShown(null, true)
            iBinding.listBookmarks.layoutManager?.scrollToPosition(scrollIndex)
        }
    }

    override fun handleUpdatedUrl(url: String) {
        updateBookmarkIndicator(url)
        val folder = uiModel.currentFolder
        setBookmarksShown(folder, false)
    }

}
