package com.jzl.nestedscrolllayout

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 *
 * @author jiangzailong
 */
class KeepBottomSheetLayout(context: Context, attrs: AttributeSet?):
    ConstraintLayout(context, attrs) {

    fun getUpNestedScrollRange(): Int {
        return height
    }

    fun getDownNestedPreScrollRange(): Int {
        return height
    }
}
