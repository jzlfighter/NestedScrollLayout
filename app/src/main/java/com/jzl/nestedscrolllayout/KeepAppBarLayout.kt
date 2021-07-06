package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
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
}
