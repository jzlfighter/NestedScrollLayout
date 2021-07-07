package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView

/**
 *
 * @author jiangzailong
 */
class KeepAppBarLayout(context: Context, attrs: AttributeSet?): NestedScrollView(context, attrs) {

    fun getUpNestedScrollRange(): Int {
        return height
    }

    fun getDownNestedPreScrollRange(): Int {
        return height
    }

    fun getOffset(): Int {
        return ((layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? BaseHeaderBehavior)?.topAndBottomOffset ?: 0
    }
}
