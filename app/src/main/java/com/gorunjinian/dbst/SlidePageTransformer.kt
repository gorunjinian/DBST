package com.gorunjinian.dbst

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Custom ViewPager2 animation that creates a smooth slide-and-fade effect
 * when transitioning between pages.
 */
class SlidePageTransformer : ViewPager2.PageTransformer {

    // How much the page should fade during transition
    private val minimumAlpha = 0.5f
    // How much the page should scale during transition
    private val minimumScale = 0.85f

    override fun transformPage(view: View, position: Float) {
        view.apply {
            // Calculate base values based on the position
            val pageWidth = width
            val pageHeight = height

            when {
                // Page is far off-screen to the right
                position < -1 -> {
                    // Hide the page
                    alpha = 0f
                }

                // Page is sliding off the left of the screen or sliding in from the right
                position <= 0 -> {
                    // Counteract the default slide transition
                    translationX = 0f
                    // Fade based on position
                    alpha = 1f + position * (1f - minimumAlpha)
                    // Scale the page down
                    val scaleFactor = minimumScale + (1f - minimumScale) * (1f - abs(position))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    // Move the page based on its diminished size
                    translationX = pageWidth * -position
                }

                // Page is sliding off the right of screen or sliding in from the left
                position <= 1 -> {
                    // Counteract the default slide transition
                    translationX = 0f
                    // Fade based on position
                    alpha = 1f - position * (1f - minimumAlpha)
                    // Scale the page down
                    val scaleFactor = minimumScale + (1f - minimumScale) * (1f - abs(position))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    // Move the page based on its diminished size
                    translationX = pageWidth * -position
                }

                // Page is far off-screen to the left
                else -> {
                    // Hide the page
                    alpha = 0f
                }
            }
        }
    }
}