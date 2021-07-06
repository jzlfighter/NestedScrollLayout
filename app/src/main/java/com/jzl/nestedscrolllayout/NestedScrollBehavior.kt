package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

/**
 *
 * @author jiangzailong
 */
class NestedScrollBehavior(context: Context, attrs: AttributeSet?):
    AppBarLayout.Behavior(context, attrs) {

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        if ((directTargetChild.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior is NestedScrollBottomSheetBehavior) {
            return false
        }
        return super.onStartNestedScroll(
            parent,
            child,
            directTargetChild,
            target,
            nestedScrollAxes,
            type
        )
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        Log.d(TAG, "onNestedPreScroll: $dy,${consumed[1]},$type")
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        Log.d(TAG, "onNestedScroll: $dyConsumed,$dyUnconsumed,${consumed[1]},$type")
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
    }

    companion object {
        const val TAG = "NestedScrollBehavior"
    }
}
