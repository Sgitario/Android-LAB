package org.sgitario.layoutgridsample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.Adapter;
import android.widget.Checkable;
import android.widget.EdgeEffect;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.OverScroller;

/**
 * Layout view group that place each item depending on a specified layout. If
 * the layout is not supplied, this view works like normal grid view.
 * 
 * @author jhilario
 * 
 */
public class LayoutViewGroup extends GridView {
	private static final String TAG = "Container";

	// ViewPool class
	protected Queue<View> viewpool;

	// Not used yet, but we'll probably need to
	// prevent layout in <code>layout()</code> method
	private boolean preventLayout = false;

	protected LayoutAdapter mLayout;
	protected Adapter mAdapter;
	protected Map<Object, PlaceHolder> frames = null;

	/**
	 * The X position of the active ViewPort
	 */
	protected int viewPortX = 0;

	/**
	 * The Y position of the active ViewPort
	 */
	protected int viewPortY = 0;

	/**
	 * The scrollable width in pixels. This is usually computed as the
	 * difference between the width of the container and the contentWidth as
	 * computed by the layout.
	 */
	protected int mScrollableWidth;

	/**
	 * The scrollable height in pixels. This is usually computed as the
	 * difference between the height of the container and the contentHeight as
	 * computed by the layout.
	 */
	protected int mScrollableHeight;

	private VelocityTracker mVelocityTracker = null;
	private float deltaX = -1f;
	private float deltaY = -1f;

	private int maxFlingVelocity;
	private int minFlingVelocity;
	private int overflingDistance;
	private int touchSlop;

	private Runnable mTouchModeReset;
	private Runnable mPerformClick;
	private Runnable mPendingCheckForTap;
	private Runnable mPendingCheckForLongPress;

	private OverScroller scroller;

	protected EdgeEffect mLeftEdge, mRightEdge, mTopEdge, mBottomEdge;

	/**
	 * Holds the checked items when the Container is in CHOICE_MODE_MULTIPLE
	 */
	protected SparseBooleanArray mCheckStates = null;

	ActionMode mChoiceActionMode;

	/**
	 * Wraps the callback for MultiChoiceMode
	 */
	MultiChoiceModeWrapper mMultiChoiceModeCallback;

	/**
	 * Normal list that does not indicate choices
	 */
	public static final int CHOICE_MODE_NONE = 0;

	/**
	 * The list allows up to one choice
	 */
	public static final int CHOICE_MODE_SINGLE = 1;

	/**
	 * The list allows multiple choices
	 */
	public static final int CHOICE_MODE_MULTIPLE = 2;

	/**
	 * The list allows multiple choices in a modal selection mode
	 */
	public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

	/**
	 * The value of the current ChoiceMode
	 * 
	 * @see <a href=
	 *      "http://developer.android.com/reference/android/widget/AbsListView.html#attr_android:choiceMode"
	 *      >List View's Choice Mode</a>
	 */
	int mChoiceMode = CHOICE_MODE_NONE;

	private LayoutParams params = new LayoutParams(0, 0);

	private PlaceHolder beginTouchAt;

	/**
	 * When Layout is computed, should scroll positions be recalculated? When a
	 * new layout is set, the Container can try to make sure an item that was
	 * visible in one layout is also visible in the new layout. However when
	 * data is just invalidated and additional data is loaded, you don't want
	 * the Viewport to be jumping around.
	 */
	private boolean shouldRecalculateScrollWhenComputingLayout = true;

	private OnTouchModeChangedListener mOnTouchModeChangedListener;

	public void setOnTouchModeChangedListener(
			OnTouchModeChangedListener onTouchModeChangedListener) {
		mOnTouchModeChangedListener = onTouchModeChangedListener;
	}

	public LayoutViewGroup(Context context) {
		super(context);
		init(context);
	}

	public LayoutViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public LayoutViewGroup(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	protected void init(Context context) {

		viewpool = new LinkedBlockingQueue<View>();
		frames = new HashMap<Object, PlaceHolder>();

		ViewConfiguration configuration = ViewConfiguration.get(context);
		maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
		minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
		overflingDistance = configuration.getScaledOverflingDistance();

		touchSlop = configuration.getScaledTouchSlop();

		scroller = new OverScroller(context);

		setEdgeEffectsEnabled(true);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		logLifecycleEvent(" onMeasure ");
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int afterWidth = MeasureSpec.getSize(widthMeasureSpec);
		int afterHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		if (this.mLayout != null) {
			mLayout.setDimensions(afterWidth, afterHeight);
		}

		if (mLayout == null) {
			logLifecycleEvent("Nothing to do: returning");
			return;

		}

		computeLayout(afterWidth, afterHeight);
	}
	
	public void dataInvalidated(boolean moveToLeft) {
		logLifecycleEvent("Data Invalidated");
		if (mLayout == null) {
			return;
		}
		
		shouldRecalculateScrollWhenComputingLayout = false;
		requestLayout();
	}

	public void dataInvalidated() {
		dataInvalidated(false);
	}

	/**
	 * The heart of the system. Calls the layout to get the frames needed,
	 * decides which view should be kept in focus if view transitions are going
	 * to happen and then kicks off animation changes if things have changed
	 * 
	 * @param w
	 *            Width of the viewport. Since right now we don't support
	 *            margins and padding, this is width of the container.
	 * @param h
	 *            Height of the viewport. Since right now we don't support
	 *            margins and padding, this is height of the container.
	 */
	protected void computeLayout(int w, int h) {
		List<Object> items = new ArrayList<Object>();
		for (int index = 0; index < mAdapter.getCount(); index++) {
			items.add(mAdapter.getItem(index));
		}
		
		mLayout.prepareLayout(items);
		computeViewPort(mLayout);
		Map<Object, PlaceHolder> oldFrames = frames;

		frames = new HashMap<Object, PlaceHolder>();
		copyFrames(mLayout.getItemProxies(viewPortX, viewPortY), frames);
		
		animateChanges(getViewChanges(oldFrames, frames));
	}

	/**
	 * Copies the frames from one HashMap into another. The items are cloned
	 * cause we modify the rectangles of the items as they are moving
	 */
	protected void copyFrames(Map<Object, PlaceHolder> srcFrames,
			Map<Object, PlaceHolder> destFrames) {
		Iterator<?> it = srcFrames.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> pairs = (Map.Entry<?, ?>) it.next();
			PlaceHolder pr = (PlaceHolder) pairs.getValue();
			pr = PlaceHolder.clone(pr);
			destFrames.put(pairs.getKey(), pr);
		}
	}

	/**
	 * Adds a view based on the current viewport. If we can get a view from the
	 * ViewPool, we dont need to construct a new instance, else we will based on
	 * the View class returned by the <code>Adapter</code>
	 * 
	 * @param freeflowItem
	 *            <code>FreeFlowItem</code> instance that determines the View
	 *            being positioned
	 */
	protected void addAndMeasureViewIfNeeded(PlaceHolder freeflowItem) {
		View view;
		if (freeflowItem.view == null) {
			view = mAdapter.getView(freeflowItem.itemIndex, viewpool.poll(), this);

			freeflowItem.view = view;
			prepareViewForAddition(view, freeflowItem);
			addView(view, getChildCount(), params);
		}

		view = freeflowItem.view;

		int widthSpec = MeasureSpec.makeMeasureSpec(freeflowItem.frame.width(),
				MeasureSpec.EXACTLY);
		int heightSpec = MeasureSpec.makeMeasureSpec(
				freeflowItem.frame.height(), MeasureSpec.EXACTLY);
		view.measure(widthSpec, heightSpec);
	}

	/**
	 * Does all the necessary work right before a view is about to be laid out.
	 * 
	 * @param view
	 *            The View that will be added to the Container
	 * @param freeflowItem
	 *            The <code>FreeFlowItem</code> instance that represents the
	 *            view that will be positioned
	 */
	protected void prepareViewForAddition(View view, PlaceHolder freeflowItem) {
		if (view instanceof Checkable) {
			((Checkable) view).setChecked(isChecked(freeflowItem.itemIndex));
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		

	}

	protected void doLayout(PlaceHolder freeflowItem) {
		View view = freeflowItem.view;
		Rect frame = freeflowItem.frame;
		int l = frame.left - viewPortX;
		int t = frame.top - viewPortY;
		int r = frame.right - viewPortX;
		int b = frame.bottom - viewPortY;
		
		view.layout(l, t, r, b);
	}

	/**
	 * Sets the layout on the Container. If a previous layout was already
	 * applied, this causes the views to animate to the new layout positions.
	 * Scroll positions will also be reset.
	 * 
	 * @see FreeFlowLayout
	 * @param newLayout
	 */
	public void setLayout(LayoutAdapter newLayout) {
		if (newLayout == mLayout || newLayout == null) {
			return;
		}
		
		stopScrolling();
		mLayout = newLayout;
		shouldRecalculateScrollWhenComputingLayout = true;

		viewPortX = 0;
		viewPortY = 0;

		logLifecycleEvent("Setting layout");
		requestLayout();

	}

	/**
	 * Stops the scrolling immediately
	 */
	public void stopScrolling() {
		if (!scroller.isFinished()) {
			scroller.forceFinished(true);
		}
		removeCallbacks(flingRunnable);
		resetAllCallbacks();
		mTouchMode = TOUCH_MODE_REST;

		if (mOnTouchModeChangedListener != null) {
			mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
		}
	}

	/**
	 * Resets all Runnables that are checking on various statuses
	 */
	protected void resetAllCallbacks() {
		if (mPendingCheckForTap != null) {
			removeCallbacks(mPendingCheckForTap);
			mPendingCheckForTap = null;
		}

		if (mPendingCheckForLongPress != null) {
			removeCallbacks(mPendingCheckForLongPress);
			mPendingCheckForLongPress = null;
		}
		if (mTouchModeReset != null) {
			removeCallbacks(mTouchModeReset);
			mTouchModeReset = null;
		}
		if (mPerformClick != null) {
			removeCallbacks(mPerformClick);
			mPerformClick = null;
		}
	}

	/**
	 * @return The layout currently applied to the Container
	 */
	public LayoutAdapter getLayout() {
		return mLayout;
	}

	/**
	 * Computes the Rectangle that defines the ViewPort. The Container tries to
	 * keep the view at the top left of the old layout visible in the new
	 * layout.
	 * 
	 * @see getViewportTop
	 * @see getViewportLeft
	 * 
	 */
	protected void computeViewPort(LayoutAdapter newLayout) {
		if (mLayout == null || frames == null || frames.size() == 0) {
			viewPortX = 0;
			viewPortY = 0;
			return;
		}

		Object data = null;
		int lowestPosition = Integer.MAX_VALUE;

		// Find the frame of of the first item in the first section in the
		// current set of frames defining the viewport
		// Changing layout will then keep this item in the viewport of the new
		// layout
		// TODO: Need to make sure this item is actually being shown in the
		// viewport and not just in some offscreen buffer
		for (PlaceHolder fd : frames.values()) {
			if (fd.itemIndex < lowestPosition) {
				data = fd.data;
				lowestPosition = fd.itemIndex;
			}
		}

		PlaceHolder freeflowItem = newLayout.getPlaceHolderForItem(data);
		freeflowItem = PlaceHolder.clone(freeflowItem);
		if (freeflowItem == null) {
			viewPortX = 0;
			viewPortY = 0;
			return;
		}

		Rect vpFrame = freeflowItem.frame;

		viewPortX = vpFrame.left;
		viewPortY = vpFrame.top;
		mScrollableWidth = mLayout.getContentWidth() - getWidth();
		mScrollableHeight = mLayout.getContentHeight() - getHeight();

		if (mScrollableWidth < 0) {
			mScrollableWidth = 0;
		}
		if (mScrollableHeight < 0) {
			mScrollableHeight = 0;
		}

		if (viewPortX > mScrollableWidth)
			viewPortX = mScrollableWidth;

		if (viewPortY > mScrollableHeight)
			viewPortY = mScrollableHeight;

	}

	/**
	 * Returns the actual frame for a view as its on stage. The FreeFlowItem's
	 * frame object always represents the position it wants to be in but actual
	 * frame may be different based on animation etc.
	 * 
	 * @param freeflowItem
	 *            The freeflowItem to get the <code>Frame</code> for
	 * @return The Frame for the freeflowItem or null if that view doesn't exist
	 */
	public Rect getActualFrame(final PlaceHolder freeflowItem) {
		View v = freeflowItem.view;
		if (v == null) {
			return null;
		}

		Rect of = new Rect();
		of.left = (int) (v.getLeft() + v.getTranslationX());
		of.top = (int) (v.getTop() + v.getTranslationY());
		of.right = (int) (v.getRight() + v.getTranslationX());
		of.bottom = (int) (v.getBottom() + v.getTranslationY());

		return of;

	}

	/**
	 * Returns the <code>FreeFlowItem</code> representing the data passed in IF
	 * that item is being rendered in the Container.
	 * 
	 * @param dataItem
	 *            The data object being rendered in a View managed by the
	 *            Container, null otherwise
	 * @return
	 */
	public PlaceHolder getFreeFlowItem(Object dataItem) {
		for (PlaceHolder item : frames.values()) {
			if (item.data.equals(dataItem)) {
				return item;
			}
		}
		return null;
	}
	
	public Collection<PlaceHolder> getDisplayedFreeFlowItems() {
		return this.frames.values();
	}

	/**
	 * TODO: This should be renamed to layoutInvalidated, since the layout isn't
	 * changed
	 */
	public void layoutChanged() {
		logLifecycleEvent("layoutChanged");
		requestLayout();
	}

	protected boolean isAnimatingChanges = false;

	private void animateChanges(LayoutChangeset changeSet) {
		logLifecycleEvent("animating changes: " + changeSet.toString());
		if (changeSet.added.size() == 0 && changeSet.removed.size() == 0
				&& changeSet.moved.size() == 0) {
			return;
		}
		
		for (PlaceHolder freeflowItem : changeSet.getAdded()) {
			addAndMeasureViewIfNeeded(freeflowItem);
			doLayout(freeflowItem);
		}

		if (isAnimatingChanges) {
			layoutAnimator.cancel();
		}
		
		isAnimatingChanges = true;
		
		layoutAnimator.animateChanges(changeSet, this);
	}

	/**
	 * This method is called by the <code>LayoutAnimator</code> instance once
	 * all transition animations have been completed.
	 * 
	 * @param anim
	 *            The LayoutAnimator instance that reported change complete.
	 */
	public void onLayoutChangeAnimationsCompleted(FreeFlowLayoutAnimator anim) {
		// preventLayout = false;
		isAnimatingChanges = false;
		logLifecycleEvent("layout change animations complete");
		for (PlaceHolder freeflowItem : anim.getChangeSet().getRemoved()) {
			View v = freeflowItem.view;
			removeView(v);
			returnItemToPoolIfNeeded(freeflowItem);
		}
		
		if (moveScrollToLeftAfterUpdated) {
			scrollToItem(0, true);
		}
	}

	public LayoutChangeset getViewChanges(Map<Object, PlaceHolder> oldFrames,
			Map<Object, PlaceHolder> newFrames) {
		return getViewChanges(oldFrames, newFrames, false);
	}

	public LayoutChangeset getViewChanges(Map<Object, PlaceHolder> oldFrames,
			Map<Object, PlaceHolder> newFrames, boolean moveEvenIfSame) {

		// cleanupViews();
		LayoutChangeset change = new LayoutChangeset();

		if (oldFrames == null) {
			markAdapterDirty = false;
			for (FreeFlowItem freeflowItem : newFrames.values()) {
				change.addToAdded(freeflowItem);
			}

			return change;
		}

		if (markAdapterDirty) {
			markAdapterDirty = false;
			for (FreeFlowItem freeflowItem : newFrames.values()) {
				change.addToAdded(freeflowItem);
			}

			for (FreeFlowItem freeflowItem : oldFrames.values()) {
				change.addToDeleted(freeflowItem);
			}

			return change;
		}

		Iterator<?> it = newFrames.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> m = (Map.Entry<?, ?>) it.next();
			FreeFlowItem freeflowItem = (FreeFlowItem) m.getValue();

			if (oldFrames.get(m.getKey()) != null) {

				FreeFlowItem old = oldFrames.remove(m.getKey());
				freeflowItem.view = old.view;

				// if (moveEvenIfSame || !old.compareRect(((FreeFlowItem)
				// m.getValue()).frame)) {

				if (moveEvenIfSame
						|| !old.frame
								.equals(((FreeFlowItem) m.getValue()).frame)) {

					change.addToMoved(freeflowItem,
							getActualFrame(freeflowItem));
				}
			} else {
				change.addToAdded(freeflowItem);
			}

		}

		for (FreeFlowItem freeflowItem : oldFrames.values()) {
			change.addToDeleted(freeflowItem);
		}

		frames = newFrames;

		return change;
	}

	@Override
	public void requestLayout() {
		if (!preventLayout) {
			/**
			 * Ends up with a call to <code>onMeasure</code> where all the logic
			 * lives
			 */
			super.requestLayout();
		}

	}

	/**
	 * The Viewport defines the rectangular "window" that the container is
	 * actually showing of the entire view.
	 * 
	 * @return The left (x) of the viewport within the entire container
	 */
	public int getViewportLeft() {
		return viewPortX;
	}

	/**
	 * The Viewport defines the rectangular "window" that the container is
	 * actually showing of the entire view.
	 * 
	 * @return The top (y) of the viewport within the entire container
	 * 
	 */
	public int getViewportTop() {
		return viewPortY;
	}

	/**
	 * Indicates that we are not in the middle of a touch gesture
	 */
	public static final int TOUCH_MODE_REST = -1;

	/**
	 * Indicates we just received the touch event and we are waiting to see if
	 * the it is a tap or a scroll gesture.
	 */
	public static final int TOUCH_MODE_DOWN = 0;

	/**
	 * Indicates the touch has been recognized as a tap and we are now waiting
	 * to see if the touch is a longpress
	 */
	public static final int TOUCH_MODE_TAP = 1;

	/**
	 * Indicates we have waited for everything we can wait for, but the user's
	 * finger is still down
	 */
	public static final int TOUCH_MODE_DONE_WAITING = 2;

	/**
	 * Indicates the touch gesture is a scroll
	 */
	public static final int TOUCH_MODE_SCROLL = 3;

	/**
	 * Indicates the view is in the process of being flung
	 */
	public static final int TOUCH_MODE_FLING = 4;

	/**
	 * Indicates the touch gesture is an overscroll - a scroll beyond the
	 * beginning or end.
	 */
	public static final int TOUCH_MODE_OVERSCROLL = 5;

	/**
	 * Indicates the view is being flung outside of normal content bounds and
	 * will spring back.
	 */
	public static final int TOUCH_MODE_OVERFLING = 6;

	/**
	 * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP,
	 * TOUCH_MODE_SCROLL, or TOUCH_MODE_DONE_WAITING
	 */
	int mTouchMode = TOUCH_MODE_REST;

	/**
	 * The duration for which the scroller will wait before deciding whether the
	 * user was actually trying to stop the scroll or swuipe again to increase
	 * the velocity
	 */
	protected final int FLYWHEEL_TIMEOUT = 40;

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		super.onTouchEvent(event);
		if (mLayout == null || !this.isEnabled()) {
			return false;
		}

		// flag to check if laid out items are wide or tall enough
		// to require scrolling
		boolean canScroll = false;

		if (this.mLayout.getContentWidth() > getWidth()) {
			canScroll = true;
		}

		if (mVelocityTracker == null && canScroll) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(event);
		}

		switch (event.getAction()) {
		case (MotionEvent.ACTION_DOWN):
			touchDown(event);
			break;
		case (MotionEvent.ACTION_MOVE):
			touchMove(event);
			break;
		case (MotionEvent.ACTION_UP):
			touchUp(event);
			break;
		case (MotionEvent.ACTION_CANCEL):
			touchCancel(event);
			break;
		}

		return true;

	}

	protected void touchDown(MotionEvent event) {

		/*
		 * Recompute this just to be safe. TODO: We should optimize this to be
		 * only calculated when a data or layout change happens
		 */
		mScrollableHeight = mLayout.getContentHeight() - getHeight();
		mScrollableWidth = mLayout.getContentWidth() - getWidth();

		if (mTouchMode == TOUCH_MODE_FLING) {
			// Wait for some time to see if the user is just trying
			// to speed up the scroll
			postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mTouchMode == TOUCH_MODE_DOWN) {
						if (mTouchMode == TOUCH_MODE_DOWN) {
							scroller.forceFinished(true);
						}
					}
				}
			}, FLYWHEEL_TIMEOUT);
		}

		beginTouchAt = ViewUtils.getItemAt(frames,
				(int) (viewPortX + event.getX()),
				(int) (viewPortY + event.getY()));

		deltaX = event.getX();
		deltaY = event.getY();

		mTouchMode = TOUCH_MODE_DOWN;

		if (mOnTouchModeChangedListener != null) {
			mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
		}

		if (mPendingCheckForTap != null) {
			removeCallbacks(mPendingCheckForTap);
			mPendingCheckForLongPress = null;
		}

		if (beginTouchAt != null) {
			mPendingCheckForTap = new CheckForTap();
		}
		postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());

	}

	protected void touchMove(MotionEvent event) {
		float xDiff = event.getX() - deltaX;
		float yDiff = event.getY() - deltaY;

		double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

		if (xDiff > 0 && viewPortX == 0) {
			if (mEdgeEffectsEnabled) {
				float str = (float) distance / getWidth();
				mLeftEdge.onPull(str);
				invalidate();
			}
			return;
		}

		if (xDiff < 0 && viewPortY == mScrollableWidth) {
			if (mEdgeEffectsEnabled) {
				float str = (float) distance / getWidth();
				mRightEdge.onPull(str);
				invalidate();
			}
			return;
		}

		if ((mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_REST)
				&& distance > touchSlop) {
			mTouchMode = TOUCH_MODE_SCROLL;

			if (mOnTouchModeChangedListener != null) {
				mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
			}

			if (mPendingCheckForTap != null) {
				removeCallbacks(mPendingCheckForTap);
				mPendingCheckForTap = null;
			}

		}

		if (mTouchMode == TOUCH_MODE_SCROLL) {
			// moveViewportBy(event.getX() - deltaX, event.getY() - deltaY, false);
			moveViewportBy(event.getX() - deltaX, 0, false);
			invokeOnItemScrollListeners();
			deltaX = event.getX();
			// deltaY = event.getY();
		}
	}

	protected void touchCancel(MotionEvent event) {
		mTouchMode = TOUCH_MODE_REST;

		if (mOnTouchModeChangedListener != null) {
			mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
		}

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}

		// requestLayout();

	}

	protected void touchUp(MotionEvent event) {
		if (mTouchMode == TOUCH_MODE_SCROLL
				|| mTouchMode == TOUCH_MODE_OVERFLING) {

			mVelocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);

			if (Math.abs(mVelocityTracker.getXVelocity()) > minFlingVelocity
					|| Math.abs(mVelocityTracker.getYVelocity()) > minFlingVelocity) {

				int maxX = mLayout.getContentWidth() - getWidth();
				int maxY = mLayout.getContentHeight() - getHeight();

				int allowedScrollOffset;
				if (mTouchMode == TOUCH_MODE_SCROLL) {
					allowedScrollOffset = 0;
				} else {
					allowedScrollOffset = overflingDistance;
				}

				scroller.fling(viewPortX, viewPortY,
						-(int) mVelocityTracker.getXVelocity(),
						-(int) mVelocityTracker.getYVelocity(), 0, maxX, 0,
						maxY, allowedScrollOffset, allowedScrollOffset);

				mTouchMode = TOUCH_MODE_FLING;

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}

				post(flingRunnable);

			} else {
				mTouchMode = TOUCH_MODE_REST;

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}
			}

		} else if (mTouchMode == TOUCH_MODE_DOWN
				|| mTouchMode == TOUCH_MODE_DONE_WAITING) {
			if (mTouchModeReset != null) {
				removeCallbacks(mTouchModeReset);
			}
			if (beginTouchAt != null && beginTouchAt.view != null) {
				beginTouchAt.view.setPressed(true);

				mTouchModeReset = new Runnable() {
					@Override
					public void run() {
						mTouchModeReset = null;
						mTouchMode = TOUCH_MODE_REST;

						if (mOnTouchModeChangedListener != null) {
							mOnTouchModeChangedListener
									.onTouchModeChanged(mTouchMode);
						}

						if (beginTouchAt != null && beginTouchAt.view != null) {
							beginTouchAt.view.setPressed(false);
						}
						if (mChoiceActionMode == null
								&& mOnItemSelectedListener != null) {
							mOnItemSelectedListener.onItemSelected(
									FreeFlowContainer.this,
									selectedFreeFlowItem);
						}
					}
				};
				selectedFreeFlowItem = beginTouchAt;
				postDelayed(mTouchModeReset,
						ViewConfiguration.getPressedStateDuration());

				mTouchMode = TOUCH_MODE_TAP;				
				if (Math.abs(event.getX() - deltaX) <= 20.0) {
					mPerformClick = new PerformClick();
					mPerformClick.run();
				}

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}
			} else {
				mTouchMode = TOUCH_MODE_REST;

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}
			}

		}
	}

	public FreeFlowItem getSelectedFreeFlowItem() {
		return selectedFreeFlowItem;
	}

	private Runnable flingRunnable = new Runnable() {

		@Override
		public void run() {
			if (scroller.isFinished()) {
				mTouchMode = TOUCH_MODE_REST;

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}

				invokeOnItemScrollListeners();
				return;
			}
			boolean more = scroller.computeScrollOffset();
			if (mEdgeEffectsEnabled) {
				checkEdgeEffectDuringScroll();
			}
			
			viewPortX = scroller.getCurrX();

			moveViewport(true);
			if (more) {
				post(flingRunnable);
			}
		}
	};

	protected void checkEdgeEffectDuringScroll() {
		if (mLeftEdge.isFinished() && viewPortX < 0) {
			mLeftEdge.onAbsorb((int) scroller.getCurrVelocity());
		}

		if (mRightEdge.isFinished()
				&& viewPortX > mLayout.getContentWidth() - getMeasuredWidth()) {
			mRightEdge.onAbsorb((int) scroller.getCurrVelocity());
		}

	}

	protected void moveViewportBy(float movementX, float movementY,
			boolean fling) {

		viewPortX = (int) (viewPortX - movementX);
		
		moveViewport(fling);
	}

	protected void moveViewPort(int left, int top, boolean isInFlingMode) {
		viewPortX = left;
		viewPortY = top;
		moveViewport(isInFlingMode);
	}

	/**
	 * Will move viewport to viewPortX and viewPortY values
	 * 
	 * @param isInFlingMode
	 *            Setting this
	 */
	protected void moveViewport(boolean isInFlingMode) {

		mScrollableWidth = mLayout.getContentWidth() - getWidth();
		if (mScrollableWidth < 0) {
			mScrollableWidth = 0;
		}
		mScrollableHeight = mLayout.getContentHeight() - getHeight();
		if (mScrollableHeight < 0) {
			mScrollableHeight = 0;
		}

		if (isInFlingMode) {
			if (viewPortX < 0 || viewPortX > mScrollableWidth || viewPortY < 0
					|| viewPortY > mScrollableHeight) {
				mTouchMode = TOUCH_MODE_OVERFLING;
			}
		} else {

			if (viewPortX < -overflingDistance) {
				viewPortX = -overflingDistance;
			} else if (viewPortX > mScrollableWidth + overflingDistance) {
				viewPortX = (mScrollableWidth + overflingDistance);
			}

			if (viewPortY < (int) (-overflingDistance)) {
				viewPortY = (int) -overflingDistance;
			} else if (viewPortY > mScrollableHeight + overflingDistance) {
				viewPortY = (int) (mScrollableHeight + overflingDistance);
			}

			if (mEdgeEffectsEnabled) {
				if (viewPortX <= 0) {
					mLeftEdge.onPull(viewPortX / (-overflingDistance));
				} else if (viewPortX >= mScrollableWidth) {
					mRightEdge.onPull((viewPortX - mScrollableWidth)
							/ (-overflingDistance));
				}

				if (viewPortY <= 0) {
					mTopEdge.onPull(viewPortY / (-overflingDistance));
				} else if (viewPortY >= mScrollableHeight) {
					mBottomEdge.onPull((viewPortY - mScrollableHeight)
							/ (-overflingDistance));
				}
			}
		}

		HashMap<Object, FreeFlowItem> oldFrames = new HashMap<Object, FreeFlowItem>();
		copyFrames(frames, oldFrames);
		frames = new HashMap<Object, FreeFlowItem>();
		copyFrames(mLayout.getItemProxies(viewPortX, viewPortY), frames);

		LayoutChangeset changeSet = getViewChanges(oldFrames, frames, true);

		for (FreeFlowItem freeflowItem : changeSet.added) {
			addAndMeasureViewIfNeeded(freeflowItem);
			doLayout(freeflowItem);
		}

		for (Pair<FreeFlowItem, Rect> freeflowItemPair : changeSet.moved) {
			doLayout(freeflowItemPair.first);
		}

		for (FreeFlowItem freeflowItem : changeSet.removed) {
			removeViewInLayout(freeflowItem.view);
			returnItemToPoolIfNeeded(freeflowItem);
		}

		invalidate();

	}

	protected boolean mEdgeEffectsEnabled = true;

	/**
	 * Controls whether the edge glows are enabled or not
	 */
	public void setEdgeEffectsEnabled(boolean val) {
		mEdgeEffectsEnabled = val;
		if (val) {
			Context context = getContext();
			setWillNotDraw(false);
			mLeftEdge = new EdgeEffect(context);
			mRightEdge = new EdgeEffect(context);
			mTopEdge = new EdgeEffect(context);
			mBottomEdge = new EdgeEffect(context);
		} else {
			setWillNotDraw(true);
			mLeftEdge = mRightEdge = mTopEdge = mBottomEdge = null;
		}
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		boolean needsInvalidate = false;

		final int height = getMeasuredHeight() - getPaddingTop()
				- getPaddingBottom();
		final int width = getMeasuredWidth();

		if (!mLeftEdge.isFinished()) {
			final int restoreCount = canvas.save();

			canvas.rotate(270);
			canvas.translate(-height + getPaddingTop(), 0);// width);
			mLeftEdge.setSize(height, width);

			needsInvalidate = mLeftEdge.draw(canvas);
			canvas.restoreToCount(restoreCount);
		}

		if (!mTopEdge.isFinished()) {
			final int restoreCount = canvas.save();

			mTopEdge.setSize(width, height);

			needsInvalidate = mTopEdge.draw(canvas);
			canvas.restoreToCount(restoreCount);
		}

		if (!mRightEdge.isFinished()) {
			final int restoreCount = canvas.save();

			canvas.rotate(90);
			canvas.translate(0, -width);// width);
			mRightEdge.setSize(height, width);

			needsInvalidate = mRightEdge.draw(canvas);
			canvas.restoreToCount(restoreCount);
		}

		if (!mBottomEdge.isFinished()) {
			final int restoreCount = canvas.save();

			canvas.rotate(180);
			canvas.translate(-width + getPaddingTop(), -height);

			mBottomEdge.setSize(width, height);

			needsInvalidate = mBottomEdge.draw(canvas);
			canvas.restoreToCount(restoreCount);
		}

		if (needsInvalidate) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	protected void returnItemToPoolIfNeeded(FreeFlowItem freeflowItem) {
		View v = freeflowItem.view;
		v.setTranslationX(0);
		v.setTranslationY(0);
		v.setRotation(0);
		v.setScaleX(1f);
		v.setScaleY(1f);
		v.setAlpha(1);
		viewpool.returnViewToPool(v);
	}

	public void setLayoutAnimator(FreeFlowLayoutAnimator anim) {
		layoutAnimator = anim;
	}

	public FreeFlowLayoutAnimator getLayoutAnimator() {
		return layoutAnimator;
	}

	public Map<Object, FreeFlowItem> getFrames() {
		return frames;
	}

	public void clearFrames() {
		removeAllViews();
		frames = null;
	}

	@Override
	public boolean shouldDelayChildPressedState() {
		return true;
	}

	public int getCheckedItemCount() {
		return mCheckStates.size();
	}

	public ArrayList<Integer> getCheckedItemPositions() {
		ArrayList<Integer> checked = new ArrayList<Integer>();
		for (int i = 0; i < mCheckStates.size(); i++) {
			checked.add(mCheckStates.keyAt(i));
		}

		return checked;
	}

	public void clearChoices() {
		mCheckStates.clear();
	}

	/**
	 * Defines the choice behavior for the Container allowing multi-select etc.
	 * 
	 * @see <a href=
	 *      "http://developer.android.com/reference/android/widget/AbsListView.html#attr_android:choiceMode"
	 *      >List View's Choice Mode</a>
	 */
	public void setChoiceMode(int choiceMode) {
		mChoiceMode = choiceMode;
		if (mChoiceActionMode != null) {
			mChoiceActionMode.finish();
			mChoiceActionMode = null;
		}
		if (mChoiceMode != CHOICE_MODE_NONE) {
			if (mCheckStates == null) {
				mCheckStates = new SparseBooleanArray();
			}
			if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
				clearChoices();
				setLongClickable(true);
			}
		}
	}

	boolean isLongClickable = false;

	@Override
	public void setLongClickable(boolean b) {
		isLongClickable = b;
	}

	@Override
	public boolean isLongClickable() {
		return isLongClickable;
	}

	public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
		if (mMultiChoiceModeCallback == null) {
			mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
		}
		mMultiChoiceModeCallback.setWrapped(listener);
	}

	final class CheckForTap implements Runnable {
		@Override
		public void run() {
			if (mTouchMode == TOUCH_MODE_DOWN) {
				mTouchMode = TOUCH_MODE_TAP;

				if (mOnTouchModeChangedListener != null) {
					mOnTouchModeChangedListener.onTouchModeChanged(mTouchMode);
				}

				if (beginTouchAt != null && beginTouchAt.view != null) {
					beginTouchAt.view.setPressed(true);
					// setPressed(true);
				}

				refreshDrawableState();
				final int longPressTimeout = ViewConfiguration
						.getLongPressTimeout();
				final boolean longClickable = isLongClickable();

				if (longClickable) {
					if (mPendingCheckForLongPress == null) {
						mPendingCheckForLongPress = new CheckForLongPress();
					}
					postDelayed(mPendingCheckForLongPress, longPressTimeout);
				} else {
					mTouchMode = TOUCH_MODE_DONE_WAITING;

					if (mOnTouchModeChangedListener != null) {
						mOnTouchModeChangedListener
								.onTouchModeChanged(mTouchMode);
					}
				}
			}
		}
	}

	private class CheckForLongPress implements Runnable {
		@Override
		public void run() {
			if (beginTouchAt == null) {
				// Assuming child that was being long pressed
				// is no longer valid
				return;
			}

			mCheckStates.clear();
			final View child = beginTouchAt.view;
			if (child != null) {
				boolean handled = false;
				// if (!mDataChanged) {
				handled = performLongPress();
				// }
				if (handled) {
					mTouchMode = TOUCH_MODE_REST;

					if (mOnTouchModeChangedListener != null) {
						mOnTouchModeChangedListener
								.onTouchModeChanged(mTouchMode);
					}

					// setPressed(false);
					child.setPressed(false);
				} else {
					mTouchMode = TOUCH_MODE_DONE_WAITING;

					if (mOnTouchModeChangedListener != null) {
						mOnTouchModeChangedListener
								.onTouchModeChanged(mTouchMode);
					}
				}
			}
		}
	}

	boolean performLongPress() {
		// CHOICE_MODE_MULTIPLE_MODAL takes over long press.
		if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
			if (mChoiceActionMode == null
					&& (mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
				setItemChecked(beginTouchAt.itemIndex, true);
				updateOnScreenCheckedViews();
				performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			}
			return true;
		}

		boolean handled = false;
		final long longPressId = mLayout.getAdapter().getItemId(beginTouchAt.itemIndex);
		if (mOnItemLongClickListener != null) {
			handled = mOnItemLongClickListener.onItemLongClick(this,
					beginTouchAt.view, beginTouchAt.itemIndex, longPressId);
		}
		if (!handled) {
			handled = super.showContextMenuForChild(this);
		}
		if (handled) {
			updateOnScreenCheckedViews();
			performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		}
		return handled;
	}

	class MultiChoiceModeWrapper implements MultiChoiceModeListener {

		private MultiChoiceModeListener mWrapped;

		public void setWrapped(MultiChoiceModeListener wrapped) {
			mWrapped = wrapped;
		}

		public boolean hasWrappedCallback() {
			return mWrapped != null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (mWrapped.onCreateActionMode(mode, menu)) {
				// Initialize checked graphic state?
				setLongClickable(false);
				return true;
			}
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return mWrapped.onPrepareActionMode(mode, menu);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return mWrapped.onActionItemClicked(mode, item);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mWrapped.onDestroyActionMode(mode);
			mChoiceActionMode = null;

			// Ending selection mode means deselecting everything.
			clearChoices();
			updateOnScreenCheckedViews();

			// rememberSyncState();
			requestLayout();

			setLongClickable(true);
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			mWrapped.onItemCheckedStateChanged(mode, position, id,
					checked);

			// If there are no items selected we no longer need the selection
			// mode.
			if (getCheckedItemCount() == 0) {
				mode.finish();
			}
		}
	}

	public interface OnTouchModeChangedListener {
		void onTouchModeChanged(int touchMode);
	}

	public interface MultiChoiceModeListener extends ActionMode.Callback {
		/**
		 * Called when an item is checked or unchecked during selection mode.
		 * 
		 * @param mode
		 *            The {@link ActionMode} providing the selection mode
		 * @param section
		 *            The Section of the item that was checked
		 * @param position
		 *            Adapter position of the item in the section that was
		 *            checked or unchecked
		 * @param id
		 *            Adapter ID of the item that was checked or unchecked
		 * @param checked
		 *            <code>true</code> if the item is now checked,
		 *            <code>false</code> if the item is now unchecked.
		 */
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked);
	}

	public void setItemChecked(int position, boolean value) {
		if (mChoiceMode == CHOICE_MODE_NONE) {
			return;
		}

		// Start selection mode if needed. We don't need to if we're unchecking
		// something.
		if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL
				&& mChoiceActionMode == null) {
			if (mMultiChoiceModeCallback == null
					|| !mMultiChoiceModeCallback.hasWrappedCallback()) {
				throw new IllegalStateException(
						"Container: attempted to start selection mode "
								+ "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was "
								+ "supplied. Call setMultiChoiceModeListener to set a callback.");
			}
			mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
		}

		if (mChoiceMode == CHOICE_MODE_MULTIPLE
				|| mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {

			setCheckedValue(position, value);
			if (mChoiceActionMode != null) {
				final long id = mLayout.getAdapter().getItemId(position);
				mMultiChoiceModeCallback.onItemCheckedStateChanged(
						mChoiceActionMode, position, id,
						value);
			}
		} else {
			setCheckedValue(position, value);
		}

		// if (!mInLayout && !mBlockLayoutRequests) {
		// mDataChanged = true;
		// rememberSyncState();
		requestLayout();
		// }
	}

	@Override
	public boolean performItemClick(View view, int position,
			long id) {
		boolean handled = false;
		boolean dispatchItemClick = true;
		if (mChoiceMode != CHOICE_MODE_NONE) {
			handled = true;
			boolean checkedStateChanged = false;

			if (mChoiceMode == CHOICE_MODE_MULTIPLE
					|| (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null)) {
				boolean checked = isChecked(position);
				checked = !checked;
				setCheckedValue(position, checked);

				if (mChoiceActionMode != null) {
					mMultiChoiceModeCallback.onItemCheckedStateChanged(
							mChoiceActionMode, position, id, checked);
					dispatchItemClick = false;
				}
				checkedStateChanged = true;
			} else if (mChoiceMode == CHOICE_MODE_SINGLE) {
				boolean checked = !isChecked(position);
				if (checked) {
					setCheckedValue(position, checked);
				}
				checkedStateChanged = true;
			}

			if (checkedStateChanged) {
				updateOnScreenCheckedViews();
			}
		}

		if (dispatchItemClick) {

			handled |= super.performItemClick(view, position, id);
		}

		return handled;
	}

	private class PerformClick implements Runnable {
		@Override
		public void run() {
			// if (mDataChanged) return;
			View view = beginTouchAt.view;
			if (view != null) {
				performItemClick(view, beginTouchAt.itemIndex, mLayout.getAdapter().getItemId(
								beginTouchAt.itemIndex));
			}
			// }
		}
	}

	/**
	 * Perform a quick, in-place update of the checked or activated state on all
	 * visible item views. This should only be called when a valid choice mode
	 * is active.
	 */
	private void updateOnScreenCheckedViews() {
		Iterator<Entry<Object, FreeFlowItem>> it = frames.entrySet().iterator();
		View child = null;
		while (it.hasNext()) {
			Map.Entry<Object, FreeFlowItem> pairs = (Map.Entry<Object, FreeFlowItem>) it
					.next();
			child = pairs.getValue().view;
			boolean isChecked = isChecked(pairs.getValue().itemIndex);
			if (child instanceof Checkable) {
				((Checkable) child).setChecked(isChecked);
			} else {
				child.setActivated(isChecked);
			}
		}
	}

	public boolean isChecked(int positionInSection) {
		for (int i = 0; i < mCheckStates.size(); i++) {
			Integer p = mCheckStates.keyAt(i);
			if (p == positionInSection) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the internal ArrayMap keeping track of checked states. Will not
	 * update the check UI.
	 */
	protected void setCheckedValue(int position,
			boolean val) {
		
		if (val == false) {
			mCheckStates.delete(position);
		} else {
			mCheckStates.put(position, val);
		}
	}

	public void addScrollListener(OnScrollListener listener) {
		if (!scrollListeners.contains(listener))
			scrollListeners.add(listener);
	}

	public void removeScrollListener(OnScrollListener listener) {
		scrollListeners.remove(listener);
	}

	public void scrollToItem(int itemIndex, boolean animate) {
		List<?> data;

		if ((data = mLayout.getAdapter().getData()) == null) {
			return;
		}

		if (itemIndex < 0 || itemIndex > data.size()) {
			return;
		}

		PlaceHolder freeflowItem = mLayout.getPlaceHolderForItem(data
				.get(itemIndex));
		freeflowItem = PlaceHolder.clone(freeflowItem);

		int newVPX = freeflowItem.frame.left;
		int newVPY = freeflowItem.frame.top;

		if (newVPX > mLayout.getContentWidth() - getMeasuredWidth())
			newVPX = mLayout.getContentWidth() - getMeasuredWidth();

		if (newVPY > mLayout.getContentHeight() - getMeasuredHeight())
			newVPY = mLayout.getContentHeight() - getMeasuredHeight();

		if (animate) {
			scroller.startScroll(viewPortX, viewPortY, (newVPX - viewPortX),
					(newVPY - viewPortY), 1500);
			post(flingRunnable);
		} else {
			moveViewportBy((viewPortX - newVPX), (viewPortY - newVPY), false);
		}
	}

	/**
	 * Returns the percentage of width scrolled. The values range from 0 to 1
	 * 
	 * @return
	 */
	public float getScrollPercentX() {
		if (mLayout == null)
			return 0;
		float w = mLayout.getContentWidth();
		float scrollableWidth = w - getWidth();
		if (scrollableWidth == 0)
			return 0;
		return viewPortX / scrollableWidth;
	}

	/**
	 * Returns the percentage of height scrolled. The values range from 0 to 1
	 * 
	 * @return
	 */
	public float getScrollPercentY() {
		if (mLayout == null)
			return 0;
		float ht = mLayout.getContentHeight();
		float scrollableHeight = ht - getHeight();
		if (scrollableHeight == 0)
			return 0;
		return viewPortY / scrollableHeight;
	}

}
