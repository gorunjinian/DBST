package com.gorunjinian.dbst

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar

/**
 * Helper class to animate only the title text in a toolbar
 * without affecting other toolbar elements like menu buttons
 */
class ToolbarTitleAnimator(private val toolbar: MaterialToolbar) {

    // Reference to the title TextView within the toolbar
    private var titleTextView: TextView? = null

    init {
        // Find the title TextView inside the toolbar after it's laid out
        toolbar.post {
            findTitleTextView(toolbar)
        }
    }

    /**
     * Find the TextView that contains the title
     * This uses a straightforward approach examining child views
     */
    private fun findTitleTextView(view: View) {
        // If this is a TextView that holds the title text
        if (view is TextView && view.text == toolbar.title) {
            titleTextView = view
            return
        }

        // For ViewGroups, search recursively through children
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                findTitleTextView(view.getChildAt(i))
            }
        }
    }

    /**
     * Animate the toolbar title change with a fade transition
     * This only affects the title text, not other toolbar elements
     */
    fun animateTitleChange(newTitle: String) {
        // Skip if title isn't changing
        if (toolbar.title == newTitle) return

        // Get reference to actual title text view
        val titleView = titleTextView ?: run {
            // If we couldn't find the title view yet, try again
            findTitleTextView(toolbar)
            titleTextView
        }

        // If we found the title view, animate it
        if (titleView != null) {
            // Fade out current title
            titleView.animate()
                .alpha(0f)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Update the title text
                        toolbar.title = newTitle

                        // Fade in new title
                        titleView.alpha = 0f
                        titleView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .setInterpolator(DecelerateInterpolator())
                            .setListener(null)
                            .start()
                    }
                })
                .start()
        } else {
            // Fallback: just change the title without animation
            toolbar.title = newTitle
        }
    }
}