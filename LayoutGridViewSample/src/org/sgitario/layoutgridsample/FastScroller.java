package org.sgitario.layoutgridsample;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;

/**
 * Helper class for AbsListView to draw and control the Fast Scroll thumb
 */
class FastScroller {
   
    // Minimum number of pages to justify showing a fast scroll thumb
    private static int MIN_PAGES = 4;
    // Scroll thumb not showing
    private static final int STATE_NONE = 0;
    // Not implemented yet - fade-in transition
    private static final int STATE_ENTER = 1;
    // Scroll thumb visible and moving along with the scrollbar
    private static final int STATE_VISIBLE = 2;
    // Scroll thumb being dragged by user
    private static final int STATE_DRAGGING = 3;
    // Scroll thumb fading out due to inactivity timeout
    private static final int STATE_EXIT = 4;
    
    private Drawable mThumbDrawable;
    private Drawable mOverlayDrawable;

    private int mThumbH;
    private int mThumbW;
    private int mThumbY;

    private RectF mOverlayPos;
    private int mOverlaySize = 104;

    private AbsListView mList;
    private boolean mScrollCompleted;
    private int mVisibleItem;
    private Paint mPaint;
    private int mListOffset;
    private int mItemCount = -1;
    private boolean mLongList;
    
    private Object [] mSections;
    private String mSectionText;
    private boolean mDrawOverlay;
    private ScrollFade mScrollFade;
    
    private int mState;
    
    private Handler mHandler = new Handler();
    
    private BaseAdapter mListAdapter;
    private SectionIndexer mSectionIndexer;

    private boolean mChangedBounds;
    
    public FastScroller(Context context, AbsListView listView) {
        mList = listView;
        init(context);
    }

    public void setState(int state) {
        switch (state) {
            case STATE_NONE:
                mHandler.removeCallbacks(mScrollFade);
                mList.invalidate();
                break;
            case STATE_VISIBLE:
                if (mState != STATE_VISIBLE) { // Optimization
                    resetThumbPos();
                }
                // Fall through
            case STATE_DRAGGING:
                mHandler.removeCallbacks(mScrollFade);
                break;
            case STATE_EXIT:
                int viewWidth = mList.getWidth();
                mList.invalidate(viewWidth - mThumbW, mThumbY, viewWidth, mThumbY + mThumbH);
                break;
        }
        mState = state;
    }
    
    public int getState() {
        return mState;
    }
    
    private void resetThumbPos() {
        final int viewWidth = mList.getWidth();
        // Bounds are always top right. Y coordinate get's translated during draw
        mThumbDrawable.setBounds(viewWidth - mThumbW, 0, viewWidth, mThumbH);
        mThumbDrawable.setAlpha(ScrollFade.ALPHA_MAX);
    }
    
    private void useThumbDrawable(Drawable drawable) {
        mThumbDrawable = drawable;
        mThumbW = 64; //mCurrentThumb.getIntrinsicWidth();
        mThumbH = 52; //mCurrentThumb.getIntrinsicHeight();
        mChangedBounds = true;
    }

    private void init(Context context) {
        // Get both the scrollbar states drawables
        final Resources res = context.getResources();
        useThumbDrawable(res.getDrawable(
                R.drawable.scrollbar_handle_accelerated_anim2));
        
        mOverlayDrawable = res.getDrawable(
                R.drawable.menu_submenu_background);
        
        mScrollCompleted = true;

        getSections();
        
        mOverlayPos = new RectF();
        mScrollFade = new ScrollFade();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(mOverlaySize / 2);
        TypedArray ta = context.getTheme().obtainStyledAttributes(new int[] { 
                android.R.attr.textColorPrimary });
        ColorStateList textColor = ta.getColorStateList(ta.getIndex(0));
        int textColorNormal = textColor.getDefaultColor();
        mPaint.setColor(textColorNormal);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        
        mState = STATE_NONE;
    }
    
    void stop() {
        setState(STATE_NONE);
    }
    
    boolean isVisible() {
        return !(mState == STATE_NONE);
    }
    
    public void draw(Canvas canvas) {
        
        if (mState == STATE_NONE) {
            // No need to draw anything
            return;
        }

        final int y = mThumbY;
        final int viewWidth = mList.getWidth();
        final FastScroller.ScrollFade scrollFade = mScrollFade;

        int alpha = -1;
        if (mState == STATE_EXIT) {
            alpha = scrollFade.getAlpha();
            if (alpha < ScrollFade.ALPHA_MAX / 2) {
                mThumbDrawable.setAlpha(alpha * 2);
            }
            int left = viewWidth - (mThumbW * alpha) / ScrollFade.ALPHA_MAX;
            mThumbDrawable.setBounds(left, 0, viewWidth, mThumbH);
            mChangedBounds = true;
        }

        canvas.translate(0, y);
        mThumbDrawable.draw(canvas);
        canvas.translate(0, -y);

        // If user is dragging the scroll bar, draw the alphabet overlay
        if (mState == STATE_DRAGGING && mDrawOverlay) {
            mOverlayDrawable.draw(canvas);
            final Paint paint = mPaint;
            float descent = paint.descent();
            final RectF rectF = mOverlayPos;
            canvas.drawText(mSectionText, (int) (rectF.left + rectF.right) / 2,
                    (int) (rectF.bottom + rectF.top) / 2 + mOverlaySize / 4 - descent, paint);
        } else if (mState == STATE_EXIT) {
            if (alpha == 0) { // Done with exit
                setState(STATE_NONE);
            } else {
                mList.invalidate(viewWidth - mThumbW, y, viewWidth, y + mThumbH);            
            }
        }
    }

    void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mThumbDrawable != null) {
            mThumbDrawable.setBounds(w - mThumbW, 0, w, mThumbH);
        }
        final RectF pos = mOverlayPos;
        pos.left = (w - mOverlaySize) / 2;
        pos.right = pos.left + mOverlaySize;
        pos.top = h / 10; // 10% from top
        pos.bottom = pos.top + mOverlaySize;
        if (mOverlayDrawable != null) {
            mOverlayDrawable.setBounds((int) pos.left, (int) pos.top,
                (int) pos.right, (int) pos.bottom);
        }
    }
    
    void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, 
            int totalItemCount) {
        // Are there enough pages to require fast scroll? Recompute only if total count changes
        if (mItemCount != totalItemCount && visibleItemCount > 0) {
            mItemCount = totalItemCount;
            mLongList = mItemCount / visibleItemCount >= MIN_PAGES;
        }
        if (!mLongList) {
            if (mState != STATE_NONE) {
                setState(STATE_NONE);
            }
            return;
        }
        if (totalItemCount - visibleItemCount > 0 && mState != STATE_DRAGGING ) {
            mThumbY = ((mList.getHeight() - mThumbH) * firstVisibleItem) 
                    / (totalItemCount - visibleItemCount);
            if (mChangedBounds) {
                resetThumbPos();
                mChangedBounds = false;
            }
        }
        mScrollCompleted = true;
        if (firstVisibleItem == mVisibleItem) {
            return;
        }
        mVisibleItem = firstVisibleItem;
        if (mState != STATE_DRAGGING) {
            setState(STATE_VISIBLE);
            mHandler.postDelayed(mScrollFade, 1500);
        }
    }

    private void getSections() {
        Adapter adapter = mList.getAdapter();
        mSectionIndexer = null;
        if (adapter instanceof HeaderViewListAdapter) {
            mListOffset = ((HeaderViewListAdapter)adapter).getHeadersCount();
            adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
        }
        if (adapter instanceof SectionIndexer) {
            mListAdapter = (BaseAdapter) adapter;
            mSectionIndexer = (SectionIndexer) adapter;
            mSections = mSectionIndexer.getSections();
            
        } else {
            mListAdapter = (BaseAdapter) adapter;
            mSections = new String[] { " " };
        }
    }

    private void scrollTo(float position) {
        int count = mList.getCount();
        mScrollCompleted = false;
        float fThreshold = (1.0f / count) / 8;
        final Object[] sections = mSections;
        int sectionIndex;
        if (sections != null && sections.length > 1) {
            final int nSections = sections.length;
            int section = (int) (position * nSections);
            if (section >= nSections) {
                section = nSections - 1;
            }
            int exactSection = section;
            sectionIndex = section;
            int index = mSectionIndexer.getPositionForSection(section);
            // Given the expected section and index, the following code will
            // try to account for missing sections (no names starting with..)
            // It will compute the scroll space of surrounding empty sections
            // and interpolate the currently visible letter's range across the
            // available space, so that there is always some list movement while
            // the user moves the thumb.
            int nextIndex = count;
            int prevIndex = index;
            int prevSection = section;
            int nextSection = section + 1;
            // Assume the next section is unique
            if (section < nSections - 1) {
                nextIndex = mSectionIndexer.getPositionForSection(section + 1);
            }
            
            // Find the previous index if we're slicing the previous section
            if (nextIndex == index) {
                // Non-existent letter
                while (section > 0) {
                    section--;
                    prevIndex = mSectionIndexer.getPositionForSection(section);
                    if (prevIndex != index) {
                        prevSection = section;
                        sectionIndex = section;
                        break;
                    } else if (section == 0) {
                        // When section reaches 0 here, sectionIndex must follow it.
                        // Assuming mSectionIndexer.getPositionForSection(0) == 0.
                        sectionIndex = 0;
                        break;
                    }
                }
            }
            // Find the next index, in case the assumed next index is not
            // unique. For instance, if there is no P, then request for P's 
            // position actually returns Q's. So we need to look ahead to make
            // sure that there is really a Q at Q's position. If not, move 
            // further down...
            int nextNextSection = nextSection + 1;
            while (nextNextSection < nSections &&
                    mSectionIndexer.getPositionForSection(nextNextSection) == nextIndex) {
                nextNextSection++;
                nextSection++;
            }
            // Compute the beginning and ending scroll range percentage of the
            // currently visible letter. This could be equal to or greater than
            // (1 / nSections). 
            float fPrev = (float) prevSection / nSections;
            float fNext = (float) nextSection / nSections;
            if (prevSection == exactSection && position - fPrev < fThreshold) {
                index = prevIndex;
            } else {
                index = prevIndex + (int) ((nextIndex - prevIndex) * (position - fPrev) 
                    / (fNext - fPrev));
            }
            // Don't overflow
            if (index > count - 1) index = count - 1;
            
            if (mList instanceof ExpandableListView) {
                ExpandableListView expList = (ExpandableListView) mList;
                expList.setSelectionFromTop(expList.getFlatListPosition(
                        ExpandableListView.getPackedPositionForGroup(index + mListOffset)), 0);
            } else if (mList instanceof ListView) {
                ((ListView)mList).setSelectionFromTop(index + mListOffset, 0);
            } else {
                mList.setSelection(index + mListOffset);
            }
        } else {
            int index = (int) (position * count);
            if (mList instanceof ExpandableListView) {
                ExpandableListView expList = (ExpandableListView) mList;
                expList.setSelectionFromTop(expList.getFlatListPosition(
                        ExpandableListView.getPackedPositionForGroup(index + mListOffset)), 0);
            } else if (mList instanceof ListView) {
                ((ListView)mList).setSelectionFromTop(index + mListOffset, 0);
            } else {
                mList.setSelection(index + mListOffset);
            }
            sectionIndex = -1;
        }

        if (sectionIndex >= 0) {
            String text = mSectionText = sections[sectionIndex].toString();
            mDrawOverlay = (text.length() != 1 || text.charAt(0) != ' ') &&
                    sectionIndex < sections.length;
        } else {
            mDrawOverlay = false;
        }
    }

    private void cancelFling() {
        // Cancel the list fling
        MotionEvent cancelFling = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        mList.onTouchEvent(cancelFling);
        cancelFling.recycle();
    }
    
    boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mState > STATE_NONE && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (ev.getX() > mList.getWidth() - mThumbW && ev.getY() >= mThumbY &&
                    ev.getY() <= mThumbY + mThumbH) {
                setState(STATE_DRAGGING);
                return true;
            }
        }
        return false;
    }

    boolean onTouchEvent(MotionEvent me) {
        if (mState == STATE_NONE) {
            return false;
        }
        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            if (me.getX() > mList.getWidth() - mThumbW
                    && me.getY() >= mThumbY 
                    && me.getY() <= mThumbY + mThumbH) {
                
                setState(STATE_DRAGGING);
                if (mListAdapter == null && mList != null) {
                    getSections();
                }

                cancelFling();
                return true;
            }
        } else if (me.getAction() == MotionEvent.ACTION_UP) {
            if (mState == STATE_DRAGGING) {
                setState(STATE_VISIBLE);
                final Handler handler = mHandler;
                handler.removeCallbacks(mScrollFade);
                handler.postDelayed(mScrollFade, 1000);
                return true;
            }
        } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
            if (mState == STATE_DRAGGING) {
                final int viewHeight = mList.getHeight();
                // Jitter
                int newThumbY = (int) me.getY() - mThumbH + 10;
                if (newThumbY < 0) {
                    newThumbY = 0;
                } else if (newThumbY + mThumbH > viewHeight) {
                    newThumbY = viewHeight - mThumbH;
                }
                if (Math.abs(mThumbY - newThumbY) < 2) {
                    return true;
                }
                mThumbY = newThumbY;
                // If the previous scrollTo is still pending
                if (mScrollCompleted) {
                    scrollTo((float) mThumbY / (viewHeight - mThumbH));
                }
                return true;
            }
        }
        return false;
    }
    
    public class ScrollFade implements Runnable {
        
        long mStartTime;
        long mFadeDuration;
        static final int ALPHA_MAX = 208;
        static final long FADE_DURATION = 200;
        
        void startFade() {
            mFadeDuration = FADE_DURATION;
            mStartTime = SystemClock.uptimeMillis();
            setState(STATE_EXIT);
        }
        
        int getAlpha() {
            if (getState() != STATE_EXIT) {
                return ALPHA_MAX;
            }
            int alpha;
            long now = SystemClock.uptimeMillis();
            if (now > mStartTime + mFadeDuration) {
                alpha = 0;
            } else {
                alpha = (int) (ALPHA_MAX - ((now - mStartTime) * ALPHA_MAX) / mFadeDuration); 
            }
            return alpha;
        }
        
        public void run() {
            if (getState() != STATE_EXIT) {
                startFade();
                return;
            }
            
            if (getAlpha() > 0) {
                mList.invalidate();
            } else {
                setState(STATE_NONE);
            }
        }
    }
}
