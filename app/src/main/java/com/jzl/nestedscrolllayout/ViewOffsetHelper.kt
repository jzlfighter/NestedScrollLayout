package com.jzl.nestedscrolllayout

import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat

class ViewOffsetHelper(private val view: View) {
    var layoutTop = 0
        private set
    var layoutLeft = 0
        private set
    var topAndBottomOffset = 0
        private set
    var leftAndRightOffset = 0
        private set
    var isVerticalOffsetEnabled = true
    var isHorizontalOffsetEnabled = true

    fun onViewLayout() {
        // Grab the original top and left
        layoutTop = view.top
        layoutLeft = view.left
    }

    fun applyOffsets() {
        ViewCompat.offsetTopAndBottom(view, topAndBottomOffset - (view.top - layoutTop))
        ViewCompat.offsetLeftAndRight(view, leftAndRightOffset - (view.left - layoutLeft))
    }

    /**
     * Set the top and bottom offset for this [ViewOffsetHelper]'s view.
     *
     * @param offset the offset in px.
     * @return true if the offset has changed
     */
    fun setTopAndBottomOffset(offset: Int): Boolean {
        if (isVerticalOffsetEnabled && topAndBottomOffset != offset) {
            topAndBottomOffset = offset
            applyOffsets()
            return true
        }
        return false
    }

    /**
     * Set the left and right offset for this [ViewOffsetHelper]'s view.
     *
     * @param offset the offset in px.
     * @return true if the offset has changed
     */
    fun setLeftAndRightOffset(offset: Int): Boolean {
        if (isHorizontalOffsetEnabled && leftAndRightOffset != offset) {
            leftAndRightOffset = offset
            applyOffsets()
            return true
        }
        return false
    }

}
