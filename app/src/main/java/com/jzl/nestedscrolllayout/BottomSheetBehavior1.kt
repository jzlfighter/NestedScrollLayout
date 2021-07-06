package com.jzl.nestedscrolllayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.R
import com.google.android.material.internal.ViewUtils
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.abs

internal open class BottomSheetBehavior1: BottomBehavior<KeepBottomSheetLayout> {
    /** Callback for monitoring events about bottom sheets.  */
    abstract class BottomSheetCallback() {
        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState The new state. This will be one of [.STATE_DRAGGING], [     ][.STATE_SETTLING], [.STATE_EXPANDED], [.STATE_COLLAPSED], [     ][.STATE_HIDDEN], or [.STATE_HALF_EXPANDED].
         */
        abstract fun onStateChanged(bottomSheet: View, @State newState: Int)

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset increases
         * as this bottom sheet is moving upward. From 0 to 1 the sheet is between collapsed and
         * expanded states and from -1 to 0 it is between hidden and collapsed states.
         */
        abstract fun onSlide(bottomSheet: View, slideOffset: Float)
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
        STATE_EXPANDED,
        STATE_COLLAPSED,
        STATE_DRAGGING,
        STATE_SETTLING,
        STATE_HIDDEN,
        STATE_HALF_EXPANDED
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class State()

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
        flag = true,
        value = [SAVE_PEEK_HEIGHT, SAVE_FIT_TO_CONTENTS, SAVE_HIDEABLE, SAVE_SKIP_COLLAPSED, SAVE_ALL, SAVE_NONE]
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class SaveFlags()
    /**
     * Returns the save flags.
     *
     * @see .setSaveFlags
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_saveFlags
     */
    /**
     * Sets save flags to be preserved in bottomsheet on configuration change.
     *
     * @param flags bitwise int of [.SAVE_PEEK_HEIGHT], [.SAVE_FIT_TO_CONTENTS], [     ][.SAVE_HIDEABLE], [.SAVE_SKIP_COLLAPSED], [.SAVE_ALL] and [.SAVE_NONE].
     * @see .getSaveFlags
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_saveFlags
     */
    @get:SaveFlags
    @SaveFlags
    var saveFlags = SAVE_NONE
    private var fitToContents = true
    private var updateImportantForAccessibilityOnSiblings = false
    private var maximumVelocity = 0f

    /** Peek height set by the user.  */
    private var peekHeight = 0

    /** Whether or not to use automatic peek height.  */
    private var peekHeightAuto = false

    /** Minimum peek height permitted.  */
    @get:VisibleForTesting
    var peekHeightMin = 0
        private set

    /** Peek height gesture inset buffer to ensure enough swipeable space.  */
    private var peekHeightGestureInsetBuffer = 0

    /** True if Behavior has a non-null value for the @shapeAppearance attribute  */
    private var shapeThemingEnabled = false
    private var materialShapeDrawable: MaterialShapeDrawable? = null
    private var gestureInsetBottom = 0
    /**
     * Returns whether this bottom sheet should adjust it's position based on the system gesture area.
     */
    /**
     * Sets whether this bottom sheet should adjust it's position based on the system gesture area on
     * Android Q and above.
     *
     *
     * Note: the bottom sheet will only adjust it's position if it would be unable to be scrolled
     * upwards because the peekHeight is less than the gesture inset margins,(because that would cause
     * a gesture conflict), gesture navigation is enabled, and this `ignoreGestureInsetBottom`
     * flag is false.
     */
    var isGestureInsetBottomIgnored = false

    /** Default Shape Appearance to be used in bottomsheet  */
    private var shapeAppearanceModelDefault: ShapeAppearanceModel? = null
    private var isShapeExpanded = false
    private var settleRunnable: SettleRunnable? = null
    private var interpolatorAnimator: ValueAnimator? = null
    private var expandedOffset = 0
    var fitToContentsOffset = 0
    var halfExpandedOffset = 0
    private var halfExpandedRatio = 0.5f
    var collapsedOffset = 0
    var elevation = -1f
    private var hideable = false
    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden after it
     * is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden after it
     * is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    var skipCollapsed = false

    /**
     * Sets whether this bottom sheet is can be collapsed/expanded by dragging. Note: When disabling
     * dragging, an app will require to implement a custom way to expand/collapse the bottom sheet
     *
     * @param draggable `false` to prevent dragging the sheet to collapse and expand
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_draggable
     */
    var isDraggable = true

    @State
    private var state = STATE_COLLAPSED
    var viewDragHelper: ViewDragHelper? = null
    private var ignoreEvents = false
    private var lastNestedScrollDy = 0
    private var nestedScrolled = false
    private var childHeight = 0
    var parentWidth = 0
    var parentHeight = 0
    var viewRef: WeakReference<KeepBottomSheetLayout>? = null
    var nestedScrollingChildRef: WeakReference<View?>? = null
    private val callbacks = ArrayList<BottomSheetCallback>()
    private var velocityTracker: VelocityTracker? = null
    var activePointerId = 0
    private var initialY = 0
    var touchingScrollingChild = false
    private var importantForAccessibilityMap: MutableMap<View, Int>? = null

    constructor() {}
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        peekHeightGestureInsetBuffer =
            context.resources.getDimensionPixelSize(R.dimen.mtrl_min_touch_target_size)
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout)
        shapeThemingEnabled = a.hasValue(R.styleable.BottomSheetBehavior_Layout_shapeAppearance)
        createShapeValueAnimator()
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            elevation =
                a.getDimension(R.styleable.BottomSheetBehavior_Layout_android_elevation, -1f)
        }
        var value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight)
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data)
        } else {
            setPeekHeight(
                a.getDimensionPixelSize(
                    R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO
                )
            )
        }
        setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false))
        isGestureInsetBottomIgnored =
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_gestureInsetBottomIgnored, false)
        setFitToContents(
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true)
        )
        skipCollapsed =
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)
        isDraggable =
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_draggable, true)
        saveFlags = a.getInt(
            R.styleable.BottomSheetBehavior_Layout_behavior_saveFlags,
            SAVE_NONE
        )
        setHalfExpandedRatio(
            a.getFloat(R.styleable.BottomSheetBehavior_Layout_behavior_halfExpandedRatio, 0.5f)
        )
        value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_expandedOffset)
        if (value != null && value.type == TypedValue.TYPE_FIRST_INT) {
            setExpandedOffset(value.data)
        } else {
            setExpandedOffset(
                a.getDimensionPixelOffset(
                    R.styleable.BottomSheetBehavior_Layout_behavior_expandedOffset, 0
                )
            )
        }
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        maximumVelocity = configuration.scaledMaximumFlingVelocity.toFloat()
    }

    override fun onSaveInstanceState(
        parent: CoordinatorLayout,
        child: KeepBottomSheetLayout
    ): Parcelable {
        return SavedState(super.onSaveInstanceState(parent, child), this)
    }

    override fun onRestoreInstanceState(
        parent: CoordinatorLayout, child: KeepBottomSheetLayout, state: Parcelable
    ) {
        val ss = state as SavedState
        super.onRestoreInstanceState(parent, child, ss.superState!!)
        // Restore Optional State values designated by saveFlags
        restoreOptionalState(ss)
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            this.state = STATE_COLLAPSED
        } else {
            this.state = ss.state
        }
    }

    override fun onAttachedToLayoutParams(layoutParams: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(layoutParams)
        // These may already be null, but just be safe, explicitly assign them. This lets us know the
        // first time we layout with this behavior by checking (viewRef == null).
        viewRef = null
        viewDragHelper = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        // Release references so we don't run unnecessary codepaths while not attached to a view.
        viewRef = null
        viewDragHelper = null
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout, child: KeepBottomSheetLayout, layoutDirection: Int
    ): Boolean {
        super.onLayoutChild(parent, child, layoutDirection)
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.fitsSystemWindows = true
        }
        if (viewRef == null) {
            // First layout with this behavior.
            peekHeightMin =
                parent.resources.getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min)
            setSystemGestureInsets(child)
            viewRef = WeakReference(child)
            // Only set MaterialShapeDrawable as background if shapeTheming is enabled, otherwise will
            // default to android:background declared in styles or layout.
            if (shapeThemingEnabled && materialShapeDrawable != null) {
                ViewCompat.setBackground(child, materialShapeDrawable)
            }
            // Set elevation on MaterialShapeDrawable
            if (materialShapeDrawable != null) {
                // Use elevation attr if set on bottomsheet; otherwise, use elevation of child view.
                materialShapeDrawable!!.elevation =
                    if (elevation == -1f) ViewCompat.getElevation(child) else elevation
                // Update the material shape based on initial state.
                isShapeExpanded = state == STATE_EXPANDED
                materialShapeDrawable!!.interpolation = if (isShapeExpanded) 0f else 1f
            }
            updateAccessibilityActions()
            if (ViewCompat.getImportantForAccessibility(child)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            ) {
                ViewCompat.setImportantForAccessibility(
                    child,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
                )
            }
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback)
        }
        val savedTop = child.top
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection)
        // Offset the bottom sheet
        parentWidth = parent.width
        parentHeight = parent.height
        childHeight = child.height
        fitToContentsOffset = Math.max(0, parentHeight - childHeight)
        calculateHalfExpandedOffset()
        calculateCollapsedOffset()
        if (state == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, getExpandedOffset())
        } else if (state == STATE_HALF_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, halfExpandedOffset)
        } else if (hideable && state == STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, parentHeight)
        } else if (state == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, collapsedOffset)
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.top)
        }
        nestedScrollingChildRef = WeakReference(findScrollingChild(child))
        return true
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout, child: KeepBottomSheetLayout, event: MotionEvent
    ): Boolean {
        if (!child.isShown || !isDraggable || topAndBottomOffset != 0) {
            ignoreEvents = true
            return false
        }
        val action = event.actionMasked
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val initialX = event.x.toInt()
                initialY = event.y.toInt()
                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                if (state != STATE_SETTLING) {
                    val scroll =
                        if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.actionIndex)
                        touchingScrollingChild = true
                    }
                }
                ignoreEvents = (activePointerId == MotionEvent.INVALID_POINTER_ID
                    && !parent.isPointInChildBounds(child, initialX, initialY))
            }
            else -> {
            }
        }
        if (!ignoreEvents
            && viewDragHelper != null && viewDragHelper!!.shouldInterceptTouchEvent(event)
        ) {
            return true
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        val scroll = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
        return (action == MotionEvent.ACTION_MOVE && scroll != null && !ignoreEvents
            && state != STATE_DRAGGING && !parent.isPointInChildBounds(
            scroll,
            event.x.toInt(),
            event.y.toInt()
        )
            && viewDragHelper != null && Math.abs(initialY - event.y) > viewDragHelper!!.touchSlop)
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout, child: KeepBottomSheetLayout, event: MotionEvent
    ): Boolean {
        if (!child.isShown) {
            return false
        }
        val action = event.actionMasked
        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (viewDragHelper != null) {
            viewDragHelper!!.processTouchEvent(event)
        }
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (viewDragHelper != null && action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (abs(initialY - event.y) > viewDragHelper!!.touchSlop) {
                viewDragHelper!!.captureChildView(child, event.getPointerId(event.actionIndex))
            }
        }
        return !ignoreEvents
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        lastNestedScrollDy = 0
        nestedScrolled = false
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
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
        //        if (type == ViewCompat.TYPE_NON_TOUCH) {
        //            // Ignore fling here. The ViewDragHelper handles it.
        //            return
        //        }

        // child 被拉动到页面中间位置
        if (topAndBottomOffset < 0) {
            if (abs(topAndBottomOffset) < child.getDownNestedPreScrollRange() - peekHeight) {
                if (dy < 0) {
                    // We're scrolling down
                    consumed[1] = scroll(
                        coordinatorLayout,
                        child,
                        dy,
                        -(child.getDownNestedPreScrollRange() - peekHeight),
                        0
                    )
                } else {
                    // We're scrolling up
                    consumed[1] = scroll(
                        coordinatorLayout,
                        child,
                        dy,
                        -(child.getDownNestedPreScrollRange() - peekHeight),
                        0
                    )
                }
            }
            return
        }

        val scrollingChild =
            if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
        if (target !== scrollingChild) {
            return
        }
        val currentTop = child.top
        val newTop = currentTop - dy
        if (dy > 0) { // Upward
            if (newTop < getExpandedOffset()) {
                consumed[1] = currentTop - getExpandedOffset()
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(STATE_EXPANDED)
            } else {
                if (!isDraggable) {
                    // Prevent dragging
                    return
                }
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= collapsedOffset || hideable) {
                    if (!isDraggable) {
                        // Prevent dragging
                        return
                    }
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setStateInternal(STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - collapsedOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setStateInternal(STATE_COLLAPSED)
                }
            }
        }
        dispatchOnSlide(child.top)
        lastNestedScrollDy = dy
        nestedScrolled = true
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
        if (dyUnconsumed > 0) {
            // If the scrolling view is scrolling down but not consuming, it's probably be at
            // the bottom of it's content
            consumed[1] =
                scroll(
                    coordinatorLayout,
                    child,
                    dyUnconsumed,
                    -child.getUpNestedScrollRange() + peekHeight,
                    0
                )
        } else {
            // 吸顶状态下来列表到头
            consumed[1] =
                scroll(
                    coordinatorLayout,
                    child,
                    dyUnconsumed,
                    -child.getUpNestedScrollRange() + peekHeight,
                    0
                )
        }
        if (dyUnconsumed == 0) {
            // The scrolling view may scroll to the top of its content without updating the actions, so
            // update here.
            updateAccessibilityActions()
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        target: View,
        type: Int
    ) {
        if (child!!.top == getExpandedOffset()) {
            setStateInternal(STATE_EXPANDED)
            return
        }
        if (nestedScrollingChildRef == null || target !== nestedScrollingChildRef!!.get() || !nestedScrolled) {
            return
        }
        val top: Int
        val targetState: Int
        if (lastNestedScrollDy > 0) {
            if (fitToContents) {
                top = fitToContentsOffset
                targetState = STATE_EXPANDED
            } else {
                val currentTop = child.top
                if (currentTop > halfExpandedOffset) {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                } else {
                    top = expandedOffset
                    targetState = STATE_EXPANDED
                }
            }
        } else if (hideable && shouldHide(child, yVelocity)) {
            top = parentHeight
            targetState = STATE_HIDDEN
        } else if (lastNestedScrollDy == 0) {
            val currentTop = child.top
            if (fitToContents) {
                if (Math.abs(currentTop - fitToContentsOffset) < Math.abs(currentTop - collapsedOffset)) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                } else {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                }
            } else {
                if (currentTop < halfExpandedOffset) {
                    if (currentTop < Math.abs(currentTop - collapsedOffset)) {
                        top = expandedOffset
                        targetState = STATE_EXPANDED
                    } else {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    }
                } else {
                    if (Math.abs(currentTop - halfExpandedOffset) < Math.abs(currentTop - collapsedOffset)) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                }
            }
        } else {
            if (fitToContents) {
                top = collapsedOffset
                targetState = STATE_COLLAPSED
            } else {
                // Settle to nearest height.
                val currentTop = child.top
                if (Math.abs(currentTop - halfExpandedOffset) < Math.abs(currentTop - collapsedOffset)) {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                } else {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                }
            }
        }
        startSettlingAnimation(child, targetState, top, false)
        nestedScrolled = false
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: KeepBottomSheetLayout,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return if (nestedScrollingChildRef != null) {
            (target === nestedScrollingChildRef!!.get()
                && (state != STATE_EXPANDED
                || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)))
        } else {
            false
        }
    }

    /**
     * @return whether the height of the expanded sheet is determined by the height of its contents,
     * or if it is expanded in two stages (half the height of the parent container, full height of
     * parent container).
     */
    fun isFitToContents(): Boolean {
        return fitToContents
    }

    /**
     * Sets whether the height of the expanded sheet is determined by the height of its contents, or
     * if it is expanded in two stages (half the height of the parent container, full height of parent
     * container). Default value is true.
     *
     * @param fitToContents whether or not to fit the expanded sheet to its contents.
     */
    fun setFitToContents(fitToContents: Boolean) {
        if (this.fitToContents == fitToContents) {
            return
        }
        this.fitToContents = fitToContents

        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (viewRef != null) {
            calculateCollapsedOffset()
        }
        // Fix incorrect expanded settings depending on whether or not we are fitting sheet to contents.
        setStateInternal(if (this.fitToContents && state == STATE_HALF_EXPANDED) STATE_EXPANDED else state)
        updateAccessibilityActions()
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or [     ][.PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun setPeekHeight(peekHeight: Int) {
        setPeekHeight(peekHeight, false)
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed while optionally animating between the
     * old height and the new height.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or [     ][.PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @param animate Whether to animate between the old height and the new height.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun setPeekHeight(peekHeight: Int, animate: Boolean) {
        var layout = false
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true
                layout = true
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false
            this.peekHeight = Math.max(0, peekHeight)
            layout = true
        }
        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (layout) {
            updatePeekHeight(animate)
        }
    }

    private fun updatePeekHeight(animate: Boolean) {
        if (viewRef != null) {
            calculateCollapsedOffset()
            if (state == STATE_COLLAPSED) {
                val view = viewRef!!.get()
                if (view != null) {
                    if (animate) {
                        settleToStatePendingLayout(state)
                    } else {
                        view.requestLayout()
                    }
                }
            }
        }
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet in pixels, or [.PEEK_HEIGHT_AUTO] if the
     * sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun getPeekHeight(): Int {
        return if (peekHeightAuto) PEEK_HEIGHT_AUTO else peekHeight
    }

    /**
     * Determines the height of the BottomSheet in the [.STATE_HALF_EXPANDED] state. The
     * material guidelines recommended a value of 0.5, which results in the sheet filling half of the
     * parent. The height of the BottomSheet will be smaller as this ratio is decreased and taller as
     * it is increased. The default value is 0.5.
     *
     * @param ratio a float between 0 and 1, representing the [.STATE_HALF_EXPANDED] ratio.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_halfExpandedRatio
     */
    fun setHalfExpandedRatio(@FloatRange(from = 0.0, to = 1.0) ratio: Float) {
        if (ratio <= 0 || ratio >= 1) {
            throw IllegalArgumentException("ratio must be a float value between 0 and 1")
        }
        halfExpandedRatio = ratio
        // If sheet is already laid out, recalculate the half expanded offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (viewRef != null) {
            calculateHalfExpandedOffset()
        }
    }

    /**
     * Determines the top offset of the BottomSheet in the [.STATE_EXPANDED] state when
     * fitsToContent is false. The default value is 0, which results in the sheet matching the
     * parent's top.
     *
     * @param offset an integer value greater than equal to 0, representing the [     ][.STATE_EXPANDED] offset. Value must not exceed the offset in the half expanded state.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_expandedOffset
     */
    fun setExpandedOffset(offset: Int) {
        if (offset < 0) {
            throw IllegalArgumentException("offset must be greater than or equal to 0")
        }
        expandedOffset = offset
    }

    /**
     * Returns the current expanded offset. If `fitToContents` is true, it will automatically
     * pick the offset depending on the height of the content.
     *
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_expandedOffset
     */
    fun getExpandedOffset(): Int {
        return if (fitToContents) fitToContentsOffset else expandedOffset
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable `true` to make this bottom sheet hideable.
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    fun setHideable(hideable: Boolean) {
        if (this.hideable != hideable) {
            this.hideable = hideable
            if (!hideable && state == STATE_HIDDEN) {
                // Lift up to collapsed state
                setState(STATE_COLLAPSED)
            }
            updateAccessibilityActions()
        }
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    @Deprecated("use {@link #addBottomSheetCallback(BottomSheetCallback)} and {@link\n" + "   *     #removeBottomSheetCallback(BottomSheetCallback)} instead")
    fun setBottomSheetCallback(callback: BottomSheetCallback?) {
        Log.w(
            TAG,
            "BottomSheetBehavior now supports multiple callbacks. `setBottomSheetCallback()` removes"
                + " all existing callbacks, including ones set internally by library authors, which"
                + " may result in unintended behavior. This may change in the future. Please use"
                + " `addBottomSheetCallback()` and `removeBottomSheetCallback()` instead to set your"
                + " own callbacks."
        )
        callbacks.clear()
        if (callback != null) {
            callbacks.add(callback)
        }
    }

    /**
     * Adds a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetCallback(callback: BottomSheetCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    /**
     * Removes a previously added callback.
     *
     * @param callback The callback to remove.
     */
    fun removeBottomSheetCallback(callback: BottomSheetCallback) {
        callbacks.remove(callback)
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of [.STATE_COLLAPSED], [.STATE_EXPANDED], [.STATE_HIDDEN],
     * or [.STATE_HALF_EXPANDED].
     */
    fun setState(@State state: Int) {
        if (state == this.state) {
            return
        }
        if (viewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if ((state == STATE_COLLAPSED
                    ) || (state == STATE_EXPANDED
                    ) || (state == STATE_HALF_EXPANDED
                    ) || (hideable && state == STATE_HIDDEN)
            ) {
                this.state = state
            }
            return
        }
        settleToStatePendingLayout(state)
    }

    private fun settleToStatePendingLayout(@State state: Int) {
        val child = viewRef!!.get() ?: return
        // Start the animation; wait until a pending layout if there is one.
        val parent = child.parent
        if ((parent != null) && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)) {
            val finalState = state
            child.post { settleToState(child, finalState) }
        } else {
            settleToState(child, state)
        }
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of [.STATE_EXPANDED], [.STATE_HALF_EXPANDED], [.STATE_COLLAPSED],
     * [.STATE_DRAGGING], [.STATE_SETTLING], or [.STATE_HALF_EXPANDED].
     */
    @State
    fun getState(): Int {
        return state
    }

    fun setStateInternal(@State state: Int) {
        if (this.state == state) {
            return
        }
        this.state = state
        if (viewRef == null) {
            return
        }
        val bottomSheet: KeepBottomSheetLayout = viewRef!!.get() ?: return
        if (state == STATE_EXPANDED) {
            updateImportantForAccessibility(true)
        } else if ((state == STATE_HALF_EXPANDED) || (state == STATE_HIDDEN) || (state == STATE_COLLAPSED)) {
            updateImportantForAccessibility(false)
        }
        updateDrawableForTargetState(state)
        for (i in callbacks.indices) {
            callbacks[i].onStateChanged(bottomSheet, state)
        }
        updateAccessibilityActions()
    }

    private fun updateDrawableForTargetState(@State state: Int) {
        if (state == STATE_SETTLING) {
            // Special case: we want to know which state we're settling to, so wait for another call.
            return
        }
        val expand = state == STATE_EXPANDED
        if (isShapeExpanded != expand) {
            isShapeExpanded = expand
            if (materialShapeDrawable != null && interpolatorAnimator != null) {
                if (interpolatorAnimator!!.isRunning) {
                    interpolatorAnimator!!.reverse()
                } else {
                    val to = if (expand) 0f else 1f
                    val from = 1f - to
                    interpolatorAnimator!!.setFloatValues(from, to)
                    interpolatorAnimator!!.start()
                }
            }
        }
    }

    private fun calculatePeekHeight(): Int {
        if (peekHeightAuto) {
            val desiredHeight = Math.max(peekHeightMin, parentHeight - parentWidth * 9 / 16)
            return Math.min(desiredHeight, childHeight)
        }
        return if (!isGestureInsetBottomIgnored && gestureInsetBottom > 0) {
            Math.max(peekHeight, gestureInsetBottom + peekHeightGestureInsetBuffer)
        } else peekHeight
    }

    private fun calculateCollapsedOffset() {
        val peek = calculatePeekHeight()
        if (fitToContents) {
            collapsedOffset = Math.max(parentHeight - peek, fitToContentsOffset)
        } else {
            collapsedOffset = parentHeight - peek
        }
    }

    private fun calculateHalfExpandedOffset() {
        halfExpandedOffset = (parentHeight * (1 - halfExpandedRatio)).toInt()
    }

    private fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    private fun restoreOptionalState(ss: SavedState) {
        if (saveFlags == SAVE_NONE) {
            return
        }
        if (saveFlags == SAVE_ALL || (saveFlags and SAVE_PEEK_HEIGHT) == SAVE_PEEK_HEIGHT) {
            peekHeight = ss.peekHeight
        }
        if ((saveFlags == SAVE_ALL
                || (saveFlags and SAVE_FIT_TO_CONTENTS) == SAVE_FIT_TO_CONTENTS)
        ) {
            fitToContents = ss.fitToContents
        }
        if (saveFlags == SAVE_ALL || (saveFlags and SAVE_HIDEABLE) == SAVE_HIDEABLE) {
            hideable = ss.hideable
        }
        if ((saveFlags == SAVE_ALL
                || (saveFlags and SAVE_SKIP_COLLAPSED) == SAVE_SKIP_COLLAPSED)
        ) {
            skipCollapsed = ss.skipCollapsed
        }
    }

    fun shouldHide(child: View, yvel: Float): Boolean {
        if (skipCollapsed) {
            return true
        }
        if (child.top < collapsedOffset) {
            // It should not hide, but collapse.
            return false
        }
        val peek = calculatePeekHeight()
        val newTop = child.top + yvel * HIDE_FRICTION
        return Math.abs(newTop - collapsedOffset) / peek.toFloat() > HIDE_THRESHOLD
    }

    @VisibleForTesting
    fun findScrollingChild(view: View?): View? {
        if (ViewCompat.isNestedScrollingEnabled((view)!!)) {
            return view
        }
        if (view is ViewGroup) {
            val group = view
            var i = 0
            val count = group.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(group.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }
        return null
    }

    private fun createMaterialShapeDrawable(
        context: Context,
        attrs: AttributeSet?,
        hasBackgroundTint: Boolean,
        bottomSheetColor: ColorStateList? = null
    ) {
        if (shapeThemingEnabled) {
            shapeAppearanceModelDefault =
                ShapeAppearanceModel.builder(context, attrs, R.attr.bottomSheetStyle, DEF_STYLE_RES)
                    .build()
            materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModelDefault!!)
            materialShapeDrawable!!.initializeElevationOverlay(context)
            if (hasBackgroundTint && bottomSheetColor != null) {
                materialShapeDrawable!!.fillColor = bottomSheetColor
            } else {
                // If the tint isn't set, use the theme default background color.
                val defaultColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorBackground, defaultColor, true)
                materialShapeDrawable!!.setTint(defaultColor.data)
            }
        }
    }

    private fun createShapeValueAnimator() {
        interpolatorAnimator = ValueAnimator.ofFloat(0f, 1f)
        interpolatorAnimator?.duration = CORNER_ANIMATION_DURATION.toLong()
        interpolatorAnimator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            if (materialShapeDrawable != null) {
                materialShapeDrawable!!.interpolation = value
            }
        }
    }

    /**
     * Ensure the peek height is at least as large as the bottom gesture inset size so that the sheet
     * can always be dragged, but only when the inset is required by the system.
     */
    @SuppressLint("RestrictedApi")
    private fun setSystemGestureInsets(child: View) {
        if ((VERSION.SDK_INT >= VERSION_CODES.Q) && !isGestureInsetBottomIgnored && !peekHeightAuto) {
            ViewUtils.doOnApplyWindowInsets(
                child
            ) { view, insets, initialPadding ->
                gestureInsetBottom = insets.mandatorySystemGestureInsets.bottom
                updatePeekHeight( /* animate= */false)
                insets
            }
        }
    }

    private val yVelocity: Float
        private get() {
            if (velocityTracker == null) {
                return 0f
            }
            velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity)
            return velocityTracker!!.getYVelocity(activePointerId)
        }

    fun settleToState(child: View, state: Int) {
        var state = state
        var top: Int
        if (state == STATE_COLLAPSED) {
            top = collapsedOffset
        } else if (state == STATE_HALF_EXPANDED) {
            top = halfExpandedOffset
            if (fitToContents && top <= fitToContentsOffset) {
                // Skip to the expanded state if we would scroll past the height of the contents.
                state = STATE_EXPANDED
                top = fitToContentsOffset
            }
        } else if (state == STATE_EXPANDED) {
            top = getExpandedOffset()
        } else if (hideable && state == STATE_HIDDEN) {
            top = parentHeight
        } else {
            throw IllegalArgumentException("Illegal state argument: $state")
        }
        startSettlingAnimation(child, state, top, false)
    }

    fun startSettlingAnimation(
        child: View,
        state: Int,
        top: Int,
        settleFromViewDragHelper: Boolean
    ) {
        val startedSettling = (viewDragHelper != null
            && (if (settleFromViewDragHelper) viewDragHelper!!.settleCapturedViewAt(
            child.left,
            top
        ) else viewDragHelper!!.smoothSlideViewTo(child, child.left, top)))
        if (startedSettling) {
            setStateInternal(STATE_SETTLING)
            // STATE_SETTLING won't animate the material shape, so do that here with the target state.
            updateDrawableForTargetState(state)
            if (settleRunnable == null) {
                // If the singleton SettleRunnable instance has not been instantiated, create it.
                settleRunnable = SettleRunnable(child, state)
            }
            // If the SettleRunnable has not been posted, post it with the correct state.
            if (settleRunnable?.isPosted == false) {
                settleRunnable?.targetState = state
                ViewCompat.postOnAnimation(child, settleRunnable)
                settleRunnable?.isPosted = true
            } else {
                // Otherwise, if it has been posted, just update the target state.
                settleRunnable?.targetState = state
            }
        } else {
            setStateInternal(state)
        }
    }

    private val dragCallback: ViewDragHelper.Callback = object: ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (state == STATE_DRAGGING) {
                return false
            }
            if (touchingScrollingChild) {
                return false
            }
            if (state == STATE_EXPANDED && activePointerId == pointerId) {
                val scroll =
                    if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
                if (scroll != null && scroll.canScrollVertically(-1)) {
                    // Let the content scroll up
                    return false
                }
            }
            return viewRef != null && viewRef!!.get() === child
        }

        override fun onViewPositionChanged(
            changedView: View, left: Int, top: Int, dx: Int, dy: Int
        ) {
            dispatchOnSlide(top)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING && isDraggable) {
                setStateInternal(STATE_DRAGGING)
            }
        }

        private fun releasedLow(child: View): Boolean {
            // Needs to be at least half way to the bottom.
            return child.top > (parentHeight + getExpandedOffset()) / 2
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top: Int
            @State val targetState: Int
            if (yvel < 0) { // Moving up
                if (fitToContents) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                } else {
                    val currentTop = releasedChild.top
                    if (currentTop > halfExpandedOffset) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = expandedOffset
                        targetState = STATE_EXPANDED
                    }
                }
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                // Hide if the view was either released low or it was a significant vertical swipe
                // otherwise settle to closest expanded state.
                if (((Math.abs(xvel) < Math.abs(yvel) && yvel > SIGNIFICANT_VEL_THRESHOLD)
                        || releasedLow(releasedChild))
                ) {
                    top = parentHeight
                    targetState = STATE_HIDDEN
                } else if (fitToContents) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                } else if ((Math.abs(releasedChild.top - expandedOffset)
                        < Math.abs(releasedChild.top - halfExpandedOffset))
                ) {
                    top = expandedOffset
                    targetState = STATE_EXPANDED
                } else {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                }
            } else if (yvel == 0f || Math.abs(xvel) > Math.abs(yvel)) {
                // If the Y velocity is 0 or the swipe was mostly horizontal indicated by the X velocity
                // being greater than the Y velocity, settle to the nearest correct height.
                val currentTop = releasedChild.top
                if (fitToContents) {
                    if ((Math.abs(currentTop - fitToContentsOffset)
                            < Math.abs(currentTop - collapsedOffset))
                    ) {
                        top = fitToContentsOffset
                        targetState = STATE_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                } else {
                    if (currentTop < halfExpandedOffset) {
                        if (currentTop < Math.abs(currentTop - collapsedOffset)) {
                            top = expandedOffset
                            targetState = STATE_EXPANDED
                        } else {
                            top = halfExpandedOffset
                            targetState = STATE_HALF_EXPANDED
                        }
                    } else {
                        if ((Math.abs(currentTop - halfExpandedOffset)
                                < Math.abs(currentTop - collapsedOffset))
                        ) {
                            top = halfExpandedOffset
                            targetState = STATE_HALF_EXPANDED
                        } else {
                            top = collapsedOffset
                            targetState = STATE_COLLAPSED
                        }
                    }
                }
            } else { // Moving Down
                if (fitToContents) {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                } else {
                    // Settle to the nearest correct height.
                    val currentTop = releasedChild.top
                    if ((Math.abs(currentTop - halfExpandedOffset)
                            < Math.abs(currentTop - collapsedOffset))
                    ) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                }
            }
            startSettlingAnimation(releasedChild, targetState, top, true)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return MathUtils.clamp(
                top, getExpandedOffset(), if (hideable) parentHeight else collapsedOffset
            )
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return if (hideable) {
                parentHeight
            } else {
                collapsedOffset
            }
        }
    }

    fun dispatchOnSlide(top: Int) {
        val bottomSheet: View? = viewRef!!.get()
        if (bottomSheet != null && !callbacks.isEmpty()) {
            val slideOffset =
                if ((top > collapsedOffset || collapsedOffset == getExpandedOffset())) (collapsedOffset - top).toFloat() / (parentHeight - collapsedOffset) else (collapsedOffset - top).toFloat() / (collapsedOffset - getExpandedOffset())
            for (i in callbacks.indices) {
                callbacks[i].onSlide(bottomSheet, slideOffset)
            }
        }
    }

    /**
     * Disables the shaped corner [ShapeAppearanceModel] interpolation transition animations.
     * Will have no effect unless the sheet utilizes a [MaterialShapeDrawable] with set shape
     * theming properties. Only For use in UI testing.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    fun disableShapeAnimations() {
        // Sets the shape value animator to null, prevents animations from occuring during testing.
        interpolatorAnimator = null
    }

    private inner class SettleRunnable internal constructor(
        private val view: View,
        @field:State @param:State var targetState: Int
    ):
        Runnable {
        var isPosted = false
        override fun run() {
            if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this)
            } else {
                setStateInternal(targetState)
            }
            isPosted = false
        }
    }

    /** State persisted across instances  */
    protected class SavedState: AbsSavedState {
        @State
        val state: Int
        var peekHeight = 0
        var fitToContents = false
        var hideable = false
        var skipCollapsed = false

        @JvmOverloads
        constructor(source: Parcel, loader: ClassLoader? = null): super(source, loader) {
            state = source.readInt()
            peekHeight = source.readInt()
            fitToContents = source.readInt() == 1
            hideable = source.readInt() == 1
            skipCollapsed = source.readInt() == 1
        }

        constructor(superState: Parcelable?, behavior: BottomSheetBehavior1): super(
            (superState)!!
        ) {
            state = behavior.state
            peekHeight = behavior.peekHeight
            fitToContents = behavior.fitToContents
            hideable = behavior.hideable
            skipCollapsed = behavior.skipCollapsed
        }

        /**
         * This constructor does not respect flags: [BottomSheetBehavior1.SAVE_PEEK_HEIGHT], [ ][BottomSheetBehavior1.SAVE_FIT_TO_CONTENTS], [BottomSheetBehavior1.SAVE_HIDEABLE], [ ][BottomSheetBehavior1.SAVE_SKIP_COLLAPSED]. It is as if [BottomSheetBehavior1.SAVE_NONE]
         * were set.
         *
         */
        @Deprecated("Use {@link #SavedState(Parcelable, BottomSheetBehavior)} instead.")
        constructor(superstate: Parcelable?, state: Int): super((superstate)!!) {
            this.state = state
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(state)
            out.writeInt(peekHeight)
            out.writeInt(if (fitToContents) 1 else 0)
            out.writeInt(if (hideable) 1 else 0)
            out.writeInt(if (skipCollapsed) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object: ClassLoaderCreator<SavedState> {
                override fun createFromParcel(`in`: Parcel, loader: ClassLoader): SavedState {
                    return SavedState(`in`, loader)
                }

                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`, null)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    /**
     * Sets whether the BottomSheet should update the accessibility status of its [ ] siblings when expanded.
     *
     *
     * Set this to true if the expanded state of the sheet blocks access to siblings (e.g., when
     * the sheet expands over the full screen).
     */
    fun setUpdateImportantForAccessibilityOnSiblings(
        updateImportantForAccessibilityOnSiblings: Boolean
    ) {
        this.updateImportantForAccessibilityOnSiblings = updateImportantForAccessibilityOnSiblings
    }

    private fun updateImportantForAccessibility(expanded: Boolean) {
        if (viewRef == null) {
            return
        }
        val viewParent = viewRef!!.get()!!.parent
        if (viewParent !is CoordinatorLayout) {
            return
        }
        val parent = viewParent
        val childCount = parent.childCount
        if ((VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) && expanded) {
            if (importantForAccessibilityMap == null) {
                importantForAccessibilityMap = HashMap(childCount)
            } else {
                // The important for accessibility values of the child views have been saved already.
                return
            }
        }
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (child === viewRef!!.get()) {
                continue
            }
            if (expanded) {
                // Saves the important for accessibility value of the child view.
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                    importantForAccessibilityMap!![child] = child.importantForAccessibility
                }
                if (updateImportantForAccessibilityOnSiblings) {
                    ViewCompat.setImportantForAccessibility(
                        child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    )
                }
            } else {
                if ((updateImportantForAccessibilityOnSiblings
                        && (importantForAccessibilityMap != null
                        ) && importantForAccessibilityMap!!.containsKey(child))
                ) {
                    // Restores the original important for accessibility value of the child view.
                    ViewCompat.setImportantForAccessibility(
                        child,
                        (importantForAccessibilityMap!![child])!!
                    )
                }
            }
        }
        if (!expanded) {
            importantForAccessibilityMap = null
        }
    }

    private fun updateAccessibilityActions() {
        if (viewRef == null) {
            return
        }
        val child = viewRef!!.get() ?: return
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_COLLAPSE)
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_EXPAND)
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_DISMISS)
        if (hideable && state != STATE_HIDDEN) {
            addAccessibilityActionForState(
                child,
                AccessibilityActionCompat.ACTION_DISMISS,
                STATE_HIDDEN
            )
        }
        when (state) {
            STATE_EXPANDED -> {
                val nextState = if (fitToContents) STATE_COLLAPSED else STATE_HALF_EXPANDED
                addAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_COLLAPSE, nextState
                )
            }
            STATE_HALF_EXPANDED -> {
                addAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_COLLAPSE, STATE_COLLAPSED
                )
                addAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_EXPAND, STATE_EXPANDED
                )
            }
            STATE_COLLAPSED -> {
                val nextState = if (fitToContents) STATE_EXPANDED else STATE_HALF_EXPANDED
                addAccessibilityActionForState(
                    child,
                    AccessibilityActionCompat.ACTION_EXPAND,
                    nextState
                )
            }
            else -> {
            }
        }
    }

    private fun addAccessibilityActionForState(
        child: KeepBottomSheetLayout, action: AccessibilityActionCompat, state: Int
    ) {
        ViewCompat.replaceAccessibilityAction(
            child,
            action,
            null,
            AccessibilityViewCommand { view, arguments ->
                setState(state)
                true
            })
    }

    companion object {
        /** The bottom sheet is dragging.  */
        const val STATE_DRAGGING = 1

        /** The bottom sheet is settling.  */
        const val STATE_SETTLING = 2

        /** The bottom sheet is expanded.  */
        const val STATE_EXPANDED = 3

        /** The bottom sheet is collapsed.  */
        const val STATE_COLLAPSED = 4

        /** The bottom sheet is hidden.  */
        const val STATE_HIDDEN = 5

        /** The bottom sheet is half-expanded (used when mFitToContents is false).  */
        const val STATE_HALF_EXPANDED = 6

        /**
         * Peek at the 16:9 ratio keyline of its parent.
         *
         *
         * This can be used as a parameter for [.setPeekHeight]. [.getPeekHeight]
         * will return this when the value is set.
         */
        const val PEEK_HEIGHT_AUTO = -1

        /** This flag will preserve the peekHeight int value on configuration change.  */
        const val SAVE_PEEK_HEIGHT = 0x1

        /** This flag will preserve the fitToContents boolean value on configuration change.  */
        const val SAVE_FIT_TO_CONTENTS = 1 shl 1

        /** This flag will preserve the hideable boolean value on configuration change.  */
        const val SAVE_HIDEABLE = 1 shl 2

        /** This flag will preserve the skipCollapsed boolean value on configuration change.  */
        const val SAVE_SKIP_COLLAPSED = 1 shl 3

        /** This flag will preserve all aforementioned values on configuration change.  */
        const val SAVE_ALL = -1

        /**
         * This flag will not preserve the aforementioned values set at runtime if the view is destroyed
         * and recreated. The only value preserved will be the positional state, e.g. collapsed, hidden,
         * expanded, etc. This is the default behavior.
         */
        const val SAVE_NONE = 0
        private val TAG = "BottomSheetBehavior"
        private val SIGNIFICANT_VEL_THRESHOLD = 500
        private val HIDE_THRESHOLD = 0.5f
        private val HIDE_FRICTION = 0.1f
        private val CORNER_ANIMATION_DURATION = 500
        private val DEF_STYLE_RES = R.style.Widget_Design_BottomSheet_Modal

    }
}
