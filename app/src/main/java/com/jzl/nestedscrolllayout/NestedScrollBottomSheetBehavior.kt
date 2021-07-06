package com.jzl.nestedscrolllayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout

/**
 *
 * @author jiangzailong
 */
@SuppressLint("LongLogTag")
internal class NestedScrollBottomSheetBehavior(context: Context, attrs: AttributeSet?):
    BottomSheetBehavior1(context, attrs) {

    init {
        setHideable(false)
        setPeekHeight(200)
    }

    private val overlap = 200

    private var mAppBarLayout: AppBarLayout? = null

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        event: MotionEvent
    ): Boolean {
        Log.d(TAG, "onInterceptTouchEvent: ${event.action},${event.y}")
        //        val layoutParams =
        //            (mAppBarLayout?.getChildAt(0)?.layoutParams as? AppBarLayout.LayoutParams)
        //        when (ev.action) {
        //                    MotionEvent.ACTION_DOWN -> {
        //                layoutParams?.scrollFlags = SCROLL_FLAG_NO_SCROLL
        //            }
        //            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        //                layoutParams?.scrollFlags = SCROLL_FLAG_SCROLL
        //            }
        //        }
        return super.onInterceptTouchEvent(parent, child, event)
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        ev: MotionEvent
    ): Boolean {
        Log.d(TAG, "onTouchEvent: ${ev.action},${ev.y}")
        return super.onTouchEvent(parent, child, ev)
    }

    override fun getScrimColor(parent: CoordinatorLayout, child: KeepBottomSheetLayout): Int {
        return super.getScrimColor(parent, child)
    }

    override fun getScrimOpacity(parent: CoordinatorLayout, child: KeepBottomSheetLayout): Float {
        return super.getScrimOpacity(parent, child)
    }

    override fun blocksInteractionBelow(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout
    ): Boolean {
        return super.blocksInteractionBelow(parent, child)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        dependency: View
    ): Boolean {
        if (dependency is AppBarLayout) {
            mAppBarLayout = dependency
            return true
        }
        return false
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        dependency: View
    ): Boolean {
        return super.onDependentViewChanged(parent, child, dependency)
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        dependency: View
    ) {
        super.onDependentViewRemoved(parent, child, dependency)
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        return super.onMeasureChild(
            parent,
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        layoutDirection: Int
    ): Boolean {
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
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
        child: KeepBottomSheetLayout,
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
        child: KeepBottomSheetLayout,
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
        child: KeepBottomSheetLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        Log.d(TAG, "onNestedScroll: $dyConsumed,$dyUnconsumed")
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

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        target: View,
        type: Int
    ) {
        Log.d(TAG, "onStopNestedScroll: $type")
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
    }

    override fun onApplyWindowInsets(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        insets: WindowInsetsCompat
    ): WindowInsetsCompat {
        return super.onApplyWindowInsets(coordinatorLayout, child, insets)
    }

    override fun onRequestChildRectangleOnScreen(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        rectangle: Rect,
        immediate: Boolean
    ): Boolean {
        return super.onRequestChildRectangleOnScreen(coordinatorLayout, child, rectangle, immediate)
    }

    override fun onRestoreInstanceState(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        state: Parcelable
    ) {
        super.onRestoreInstanceState(parent, child, state)
    }

    override fun onSaveInstanceState(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout
    ): Parcelable {
        return super.onSaveInstanceState(parent, child)
    }

    override fun getInsetDodgeRect(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        rect: Rect
    ): Boolean {
        return super.getInsetDodgeRect(parent, child, rect)
    }

    companion object {
        const val TAG = "NestedScrollBottomSheetBehavior"
    }
}
