package com.hollabrowser.meforce.browser.sessions

import android.annotation.SuppressLint
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.controller.UIController
import com.hollabrowser.meforce.dialog.BrowserDialog
import com.hollabrowser.meforce.extensions.resizeAndShow
import com.hollabrowser.meforce.extensions.toast
import com.hollabrowser.meforce.utils.FileNameInputFilter
import com.hollabrowser.meforce.utils.ItemDragDropSwipeViewHolder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * The [RecyclerView.ViewHolder] for our session list.
 * That represents an item in our list, basically one session.
 */
@SuppressLint("InflateParams")
class SessionViewHolder(
        view: View,
        private val iUiController: UIController
) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener, ItemDragDropSwipeViewHolder {


    // Using view binding won't give us much
    val textName: TextView = view.findViewById(R.id.text_name)
    val textTabCount: TextView = view.findViewById(R.id.text_tab_count)
    val buttonEdit: ImageView = view.findViewById(R.id.button_edit)
    val buttonDelete: View = view.findViewById(R.id.button_delete)
    val iCardView: MaterialCardView = view.findViewById(R.id.layout_background)

    init {
        // Delete a session
        buttonDelete.setOnClickListener {
            // Just don't delete current session for now
            if (iUiController.getTabModel().iCurrentSessionName == session().name) {
                it.context.toast(R.string.session_cant_delete_current)
            } else {
                MaterialAlertDialogBuilder(it.context)
                        .setCancelable(true)
                        .setTitle(R.string.session_prompt_confirm_deletion_title)
                        .setMessage(it.context.getString(R.string.session_prompt_confirm_deletion_message,session().name))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // User confirmed deletion, go ahead then
                            iUiController.getTabModel().deleteSession(textName.tag as String)
                            // Persist our session list after removing one
                            iUiController.getTabModel().saveSessions()
                            // Refresh our list
                            (it.context as BrowserActivity).apply {
                                sessionsMenu.updateSessions()
                            }
                        }
                        .resizeAndShow()
            }
        }

        // Edit session name
        buttonEdit.setOnClickListener{
            val dialogView = LayoutInflater.from(it.context).inflate(R.layout.dialog_edit_text, null)
            val textView = dialogView.findViewById<EditText>(R.id.dialog_edit_text)
            // Make sure user can only enter valid filename characters
            textView.filters = arrayOf<InputFilter>(FileNameInputFilter())

            // Init our text field with current name
            textView.setText(session().name)
            textView.selectAll()
            //textView.requestFocus()



            //textView.showSoftInputOnFocus
            /*
            textView.setOnFocusChangeListener{ view, hasFocus ->
                if (hasFocus) {
                    dialog?.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);
                }
            }
             */

            BrowserDialog.showCustomDialog(it.context as AppCompatActivity) {
                setTitle(R.string.session_name_prompt)
                setView(dialogView)
                setPositiveButton(R.string.action_ok) { _, _ ->
                    val newName = textView.text.toString()
                    // Check if session exists already to display proper error message
                    if (iUiController.getTabModel().isValidSessionName(newName)) {
                        // Proceed with session rename
                        iUiController.getTabModel().renameSession(textName.tag as String,newName)
                        // Make sure we update adapter list and thus edited item too
                        (iUiController as BrowserActivity).sessionsMenu.updateSessions()
                    } else {
                        // We already have a session with that name, display an error message
                        context.toast(R.string.session_already_exists)
                    }
                }
            }
        }

        // Session item clicked
        iCardView.setOnClickListener{

            if (!iUiController.getTabModel().isInitialized) {
                // We are still busy loading a session
                it.context.toast(R.string.busy)
                return@setOnClickListener
            }

            // User wants to switch session
            session().name.let { sessionName ->
                (it.context as BrowserActivity).apply {
                    presenter?.switchToSession(sessionName)
                    if (!isEditModeEnabled()) {
                        sessionsMenu.dismiss()
                    } else {
                        // Update our list, notably current item
                        iUiController.getTabModel().doOnceAfterInitialization {
                            sessionsMenu.updateSessions()
                            sessionsMenu.scrollToCurrentSession()
                        }
                    }
                }
            }
        }

        iCardView.setOnLongClickListener(this)
    }

    private fun session() = iUiController.getTabModel().session(textName.tag as String)

    override fun onClick(v: View) {
        if (v === buttonDelete) {
            //uiController.tabCloseClicked(adapterPosition)
        } else {
            return
        }
    }

    override fun onLongClick(v: View): Boolean {
        //uiController.showCloseDialog(adapterPosition)
        //return true
        return false
    }

    // From ItemTouchHelperViewHolder
    // Start dragging
    override fun onItemOperationStart() {
        iCardView.isDragged = true
    }

    // From ItemTouchHelperViewHolder
    // Stopped dragging
    override fun onItemOperationStop() {
        iCardView.isDragged = false
    }

    /**
     * Tell this view holder to start observing edit mode changes.
     */
    fun observeEditMode(observable: Observable<Boolean>): Disposable {
        return observable
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { editMode ->

                    if (editMode) {
                        buttonEdit.visibility = View.VISIBLE
                        buttonDelete.visibility = View.VISIBLE
                    } else {
                        buttonEdit.visibility = View.GONE
                        buttonDelete.visibility = View.GONE
                    }

                }
    }

    /**
     * Provide a string representation of our tab count. Return an empty string if tab count is not available.
     * Tab count may not be available for recovered sessions for instance.
     */
    fun tabCountLabel() = if (session().tabCount>0) session().tabCount.toString() else ""

    /**
     * Tell if edit mode is currently enabled
     */
    fun isEditModeEnabled() = buttonEdit.visibility == View.VISIBLE

}
