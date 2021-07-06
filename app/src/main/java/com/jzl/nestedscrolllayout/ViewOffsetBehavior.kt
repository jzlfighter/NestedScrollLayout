package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout

/** Behavior will automatically sets up a [ViewOffsetHelper] on a [View].  */
open class ViewOffsetBehavior<V: View>: CoordinatorLayout.Behavior<V> {
    private var viewOffsetHelper: ViewOffsetHelper? = null
    private var tempTopBottomOffset = 0
    private var tempLeftRightOffset = 0

    constructor() {}
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs) {}

    @CallSuper
    override fun onLayoutChild(
        parent: CoordinatorLayout, child: V, layoutDirection: Int
    ): Boolean {
        // First let lay the child out
        layoutChild(parent, child, layoutDirection)
        if (viewOffsetHelper == null) {
            viewOffsetHelper = ViewOffsetHelper(child)
        }
        child.post {
            viewOffsetHelper?.onViewLayout()
            viewOffsetHelper?.applyOffsets()
        }
        if (tempTopBottomOffset != 0) {
            viewOffsetHelper?.setTopAndBottomOffset(tempTopBottomOffset)
            tempTopBottomOffset = 0
        }
        if (tempLeftRightOffset != 0) {
            viewOffsetHelper?.setLeftAndRightOffset(tempLeftRightOffset)
            tempLeftRightOffset = 0
        }
        return true
    }

    protected open fun layoutChild(
        parent: CoordinatorLayout, child: V, layoutDirection: Int
    ) {
        // Let the parent lay it out by default
        parent.onLayoutChild(child, layoutDirection)
    }

    fun setTopAndBottomOffset(offset: Int): Boolean {
        tempTopBottomOffset = if (viewOffsetHelper != null) {
            return viewOffsetHelper!!.setTopAndBottomOffset(offset)
        } else {
            offset
        }
        return false
    }

    fun setLeftAndRightOffset(offset: Int): Boolean {
        tempLeftRightOffset = if (viewOffsetHelper != null) {
            return viewOffsetHelper!!.setLeftAndRightOffset(offset)
        } else {
            offset
        }
        return false
    }

    val topAndBottomOffset: Int
        get() = if (viewOffsetHelper != null) viewOffsetHelper!!.topAndBottomOffset else 0
    val leftAndRightOffset: Int
        get() = if (viewOffsetHelper != null) viewOffsetHelper!!.leftAndRightOffset else 0
    var isVerticalOffsetEnabled: Boolean
        get() = viewOffsetHelper != null && viewOffsetHelper!!.isVerticalOffsetEnabled
        set(verticalOffsetEnabled) {
            if (viewOffsetHelper != null) {
                viewOffsetHelper!!.isVerticalOffsetEnabled = verticalOffsetEnabled
            }
        }
    var isHorizontalOffsetEnabled: Boolean
        get() = viewOffsetHelper != null && viewOffsetHelper!!.isHorizontalOffsetEnabled
        set(horizontalOffsetEnabled) {
            if (viewOffsetHelper != null) {
                viewOffsetHelper!!.isHorizontalOffsetEnabled = horizontalOffsetEnabled
            }
        }
}
