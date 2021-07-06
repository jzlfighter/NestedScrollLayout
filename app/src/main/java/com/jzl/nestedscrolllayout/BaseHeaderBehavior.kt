package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 *
 * @author jiangzailong
 */
class BaseHeaderBehavior(context: Context?, attrs: AttributeSet?):
    HeaderBehavior<KeepAppBarLayout>(context, attrs) {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepAppBarLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        Log.d(TAG, "onStartNestedScroll: $axes,$type")
        return true
    }

    override fun onNestedScrollAccepted(
        coordinatorLayout: CoordinatorLayout,
        child: KeepAppBarLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ) {
        Log.d(TAG, "onNestedScrollAccepted: $axes,$type")
        super.onNestedScrollAccepted(
            coordinatorLayout,
            child,
            directTargetChild,
            target,
            axes,
            type
        )
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepAppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (DEBUG_SCROLL_EVENT) {
            Log.d(TAG, "onNestedPreScroll: $dy,${consumed[1]},$type")
        }
        if (dy < 0) {
            //We're scrolling down
            if (topAndBottomOffset < 0) {
                scroll(coordinatorLayout, child, dy, -child.getDownNestedPreScrollRange(), 0)
            }
        } else {
            if (topAndBottomOffset < 0) {
                scroll(coordinatorLayout, child, dy, -child.getDownNestedPreScrollRange(), 0)
            }
        }
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepAppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (DEBUG_SCROLL_EVENT) {
            Log.d(TAG, "onNestedScroll: $dyConsumed,$dyUnconsumed")
        }
        if (dyUnconsumed < 0) {
            // If the scrolling view is scrolling up but not consuming, it's probably be at
            // the bottom of it's content
            consumed[1] = scroll(
                coordinatorLayout,
                child,
                dyUnconsumed,
                -child.getUpNestedScrollRange(),
                0
            )
        } else {
            if (topAndBottomOffset == 0) {
                consumed[1] = scroll(
                    coordinatorLayout,
                    child,
                    dyUnconsumed,
                    -child.getDownNestedPreScrollRange(),
                    0
                )
            }
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepAppBarLayout,
        target: View,
        type: Int
    ) {
        Log.d(TAG, "onStopNestedScroll: $type")
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    companion object {
        const val DEBUG_SCROLL_EVENT = true
        const val TAG = "BaseHeaderBehavior"
    }
}
