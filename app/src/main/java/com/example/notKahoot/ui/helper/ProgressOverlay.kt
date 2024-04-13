package com.example.notKahoot.ui.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

// https://stackoverflow.com/a/29542951

/**
 * @param view         View to animate
 * @param toVisibility Visibility at the end of animation
 * @param toAlpha      Alpha at the end of animation. Defaults to 1 if becoming visible, 0 otherwise
 * @param duration     Animation duration in ms
 * @param bringToFront If true, make the view the front-most view
 */
fun animateView(view: View, toVisibility: Int, toAlpha: Float? = null, duration: Long = 200, bringToFront: Boolean = false) {
    val show = toVisibility == View.VISIBLE
    val alpha = toAlpha ?: (if (show) 1f else 0f)
    if (show) {
        view.alpha = 0f
        if (bringToFront) view.bringToFront()
    }
    view.visibility = View.VISIBLE
    view.animate()
        .setDuration(duration)
        .alpha(if (show) alpha else 0f)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = toVisibility
            }
        })
}