package com.hollabrowser.meforce.browser.sessions

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.EditText
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.controller.UIController
import com.hollabrowser.meforce.databinding.SessionListBinding
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.dialog.BrowserDialog
import com.hollabrowser.meforce.extensions.toast
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.FileNameInputFilter
import com.hollabrowser.meforce.utils.ItemDragDropSwipeHelper
import com.hollabrowser.meforce.utils.Utils
import javax.inject.Inject

@SuppressLint("InflateParams")
class SessionsPopupWindow(
    layoutInflater: LayoutInflater,
    aBinding: SessionListBinding = SessionListBinding.inflate(layoutInflater)
) : PopupWindow(aBinding.root, WRAP_CONTENT, WRAP_CONTENT, true) {

    var iUiController: UIController
    var iAdapter: SessionsAdapter
    var iBinding: SessionListBinding = aBinding
    var iAnchor: View? = null
    private var iItemTouchHelper: ItemTouchHelper? = null

    @Inject lateinit var userPreferences: UserPreferences

    init {
        aBinding.root.context.injector.inject(this)
        PopupWindowCompat.setWindowLayoutType(this, WindowManager.LayoutParams.FIRST_SUB_WINDOW + 5)
        elevation = 100F
        iUiController = aBinding.root.context as UIController
        iAdapter = SessionsAdapter(iUiController)
        animationStyle = R.style.AnimationMenu
        setBackgroundDrawable(ColorDrawable())
        aBinding.buttonNewSession.setOnClickListener { view ->
            val dialogView = LayoutInflater.from(aBinding.root.context).inflate(R.layout.dialog_edit_text, null)
            val textView = dialogView.findViewById<EditText>(R.id.dialog_edit_text)
            // Make sure user can only enter valid filename characters
            textView.filters = arrayOf<InputFilter>(FileNameInputFilter())

            BrowserDialog.showCustomDialog(aBinding.root.context as AppCompatActivity) {
                setTitle(R.string.session_name_prompt)
                setView(dialogView)
                setPositiveButton(R.string.action_ok) { _, _ ->
                    val name = textView.text.toString()
                    // Check if session exists already
                    if (iUiController.getTabModel().isValidSessionName(name)) {
                        // That session does not exist yet, add it then
                        iUiController.getTabModel().iSessions.let {
                            it.add(Session(name, 1))
                            // Switch to our newly added session
                            (view.context as BrowserActivity).apply {
                                presenter?.switchToSession(name)
                                // Close session dialog after creating and switching to new session
                                sessionsMenu.dismiss()
                            }
                            // Update our session list
                            //iAdapter.showSessions(it)
                        }
                    } else {
                        // We already have a session with that name, display an error message
                        context.toast(R.string.session_already_exists)
                    }
                }
            }
        }
        aBinding.buttonSaveSession.setOnClickListener { view ->
            val dialogView = LayoutInflater.from(aBinding.root.context).inflate(R.layout.dialog_edit_text, null)
            val textView = dialogView.findViewById<EditText>(R.id.dialog_edit_text)
            // Make sure user can only enter valid filename characters
            textView.filters = arrayOf<InputFilter>(FileNameInputFilter())

            iUiController.getTabModel().let { tabs ->
                BrowserDialog.showCustomDialog(aBinding.root.context as AppCompatActivity) {
                    setTitle(R.string.session_name_prompt)
                    setView(dialogView)
                    setPositiveButton(R.string.action_ok) { _, _ ->
                        val name = textView.text.toString()
                        // Check if session exists already
                        if (tabs.isValidSessionName(name)) {
                            // That session does not exist yet, add it then
                            tabs.iSessions.let {
                                // Save current session session first
                                tabs.saveState()
                                // Add new session
                                it.add(Session(name, tabs.currentSession().tabCount))
                                // Set it as current session
                                tabs.iCurrentSessionName = name
                                // Save current tabs that our newly added session
                                tabs.saveState()
                                // Switch to our newly added session
                                (view.context as BrowserActivity).apply {
                                    // Close session dialog after creating and switching to new session
                                    sessionsMenu.dismiss()
                                }

                                // Show user we did switch session
                                view.context.apply {
                                    toast(getString(R.string.session_switched, name))
                                }

                                // Update our session list
                                //iAdapter.showSessions(it)
                            }
                        } else {
                            // We already have a session with that name, display an error message
                            context.toast(R.string.session_already_exists)
                        }
                    }
                }
            }
        }
        aBinding.buttonEditSessions.setOnClickListener {

            // Toggle edit mode
            iAdapter.iEditModeEnabledObservable.value?.let { editModeEnabled ->
                // Change button icon
                if (!editModeEnabled) {
                    aBinding.buttonEditSessions.setImageResource(R.drawable.ic_secured)
                } else {
                    aBinding.buttonEditSessions.setImageResource(R.drawable.ic_edit)
                }
                // Notify our observers of edit mode change
                iAdapter.iEditModeEnabledObservable.onNext(!editModeEnabled)

                // Just close and reopen our menu as our layout change animation is really ugly
                dismiss()
                iAnchor?.let {
                    (iUiController as BrowserActivity).mainHandler.post { show(it,!editModeEnabled,false) }
                }
                // We still broadcast the change above and do a post to avoid getting some items caught not fully animated, even though animations are disabled.
                // Android layout animation crap, just don't ask, sometimes it's a blessing other times it's a nightmare...
            }
        }
        aBinding.recyclerViewSessions.apply {
            //setLayerType(View.LAYER_TYPE_NONE, null)
            //(itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, userPreferences.toolbarsBottom)
            adapter = iAdapter
            setHasFixedSize(false)
        }
        val callback: ItemTouchHelper.Callback = ItemDragDropSwipeHelper(iAdapter,
            aLongPressDragEnabled = true,
            aSwipeEnabled = false
        )
        iItemTouchHelper = ItemTouchHelper(callback)
        iItemTouchHelper?.attachToRecyclerView(iBinding.recyclerViewSessions)
    }


    /**
     *
     */
    fun show(aAnchor: View, aEdit: Boolean = false, aShowCurrent: Boolean = true) {
        // Disable edit mode when showing our menu
        iAdapter.iEditModeEnabledObservable.onNext(aEdit)
        if (aEdit) {
            iBinding.buttonEditSessions.setImageResource(R.drawable.ic_secured)
        } else {
            iBinding.buttonEditSessions.setImageResource(R.drawable.ic_edit)
        }

        iAnchor = aAnchor
        //showAsDropDown(aAnchor, 0, 0)

        // Get our anchor location
        val anchorLoc = IntArray(2)
        aAnchor.getLocationInWindow(anchorLoc)
        //
        val gravity = if (userPreferences.toolbarsBottom) Gravity.BOTTOM or Gravity.END else Gravity.TOP or Gravity.END
        val yOffset = if (userPreferences.toolbarsBottom) (contentView.context as BrowserActivity).iBinding.root.height - anchorLoc[1] else anchorLoc[1]+aAnchor.height
        // Show our popup menu from the right side of the screen below our anchor
        showAtLocation(aAnchor, gravity,
                // Offset from the right screen edge
                Utils.dpToPx(10F),
                // Below our anchor
                yOffset)

        //dimBehind()
        // Show our sessions
        updateSessions()
        if (aShowCurrent) {
            // Make sure current session is on the screen
            scrollToCurrentSession()
        }
    }

    /**
     *
     */
    fun scrollToCurrentSession() {
        iBinding.recyclerViewSessions.smoothScrollToPosition(iUiController.getTabModel().currentSessionIndex())
    }

    /**
     *
     */
    fun updateSessions() {
        if (!iBinding.recyclerViewSessions.isComputingLayout) {
            iAdapter.showSessions(iUiController.getTabModel().iSessions)
        }
    }

}
