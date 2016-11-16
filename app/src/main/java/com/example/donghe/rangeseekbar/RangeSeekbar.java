
package com.example.donghe.rangeseekbar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import de.greenrobot.event.EventBus;

/**
 * seekbar 标尺刻度 支持左右滑动
 *
 * @author hedong
 * @time 2016/10/09
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class RangeSeekbar extends View {

    private static final int DEFAULT_DURATION = 150;

    private static final int SENSITIVITY_IN_DP = 5;

    private static final float PADDING_LEFT = 10f;
    private static final float PADDING_TOP = 10f;
    private static final float PADDING_RIGHT = 10f;
    private static final float PADDING_BOTTOM = 10f;
    private static final float GAP = 5f;
    private static final float MARK_SIZE = 6f;
    private static final float SEEKBAR_HEIGHT = 15f;
    private static final float TEST_SIZE = 12f;
    private static final float CURSOR_WIDTH = 24f;
    private static final float CURSOR_HEIGHT = CURSOR_WIDTH * 36f / 30f;
    private static final float CURSOR_HINT_WIDTH = 42f;
    private static final float CURSOR_HINT_HEIGHT = CURSOR_HINT_WIDTH * 64f / 52f;


    private enum DIRECTION {
        LEFT, RIGHT;
    }

    private int mDuration;

    /**
     * 左右两边滑动的游标
     */
    private Scroller mLeftScroller;
    private Scroller mRightScroller;

    /**
     * 游标背景
     */
    private Drawable mLeftCursorBG;
    private Drawable mRightCursorBG;

    /**
     * 选择状态
     */
    private int[] mPressedEnableState = new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled};
    private int[] mUnPresseEanabledState = new int[]{-android.R.attr.state_pressed, android.R.attr.state_enabled};

    /**
     * 刻度数字的选择和未选择的亚瑟变化
     */
    private int mTextColorNormal;
    private int mTextColorSelected;
    private int mSeekbarColorSelected;

    /**
     * Hseekbar 的高度
     */
    private int mSeekbarHeight;

    private int mGap;

    private int mMarkSize;
    /**
     * 文字大小
     */
    private int mTextSize;

    /**
     * 刻度之间的宽度
     */
    private int mPartLength;

    /**
     * 刻度数字
     */
    private CharSequence[] mTextArray;

    private float[] mTextWidthArray;
    private int mTextHeight;

    private Rect mPaddingRect;
    private Rect mLeftCursorRect;
    private Rect mRightCursorRect;

    private RectF mSeekbarRect;
    private RectF mSeekbarRectSelected;

    private float mLeftCursorIndex = 0;
    private float mRightCursorIndex = 1.0f;
    private int mLeftCursorNextIndex = 0;
    private int mRightCursorNextIndex = 1;

    private Paint mPaint;

    private int mLeftPointerLastX;
    private int mRightPointerLastX;

    private int mLeftPointerID = -1;
    private int mRightPointerID = -1;

    private boolean mLeftHited;
    private boolean mRightHited;

    private int mRightBoundary;

    private OnCursorChangeListener mListener;

    private Rect[] mClickRectArray;
    private int mClickIndex = -1;
    private int mClickDownLastX = -1;
    private int mClickDownLastY = -1;

    private float mTextDrawBottom;

    private float mDensity;

    private int mCursorH;

    private int mCursorW;

    private int mCursorHintH;

    private int mCursorHintW;


    public RangeSeekbar(Context context) {
        this(context, null, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


        applyConfig(context, attrs);

        mLeftCursorRect = new Rect();
        mRightCursorRect = new Rect();

        mSeekbarRect = new RectF();
        mSeekbarRectSelected = new RectF();

        mLeftScroller = new Scroller(context, new DecelerateInterpolator());
        mRightScroller = new Scroller(context, new DecelerateInterpolator());

        mDensity = getContext().getResources().getDisplayMetrics().density;

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);

        mGap = (int) (GAP * mDensity + 0.5f);
        mMarkSize = (int) (MARK_SIZE * mDensity + 0.5f);
        mCursorH = (int) (CURSOR_HEIGHT * mDensity + 0.5f);
        mCursorW = (int) (CURSOR_WIDTH * mDensity + 0.5f);
        mCursorHintH = (int) (CURSOR_HINT_HEIGHT * mDensity + 0.5f);
        mCursorHintW = (int) (CURSOR_HINT_WIDTH * mDensity + 0.5f);

        initPadding();
        initData();
        initPaint();
        initTextBoundsArray();
    }


    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);

        initPadding();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int heightNeeded = mPaddingRect.top + mCursorHintH + mGap + mTextHeight + mGap + mMarkSize + mGap + mSeekbarHeight + mGap + mCursorH + mPaddingRect.bottom;

        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }

        mTextDrawBottom = mPaddingRect.top + mCursorHintH + mGap + mTextHeight;

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        mSeekbarRect.left = mPaddingRect.left + mCursorH / 2f;
        mSeekbarRect.right = widthSize - mPaddingRect.right - mCursorH / 2f;
        mSeekbarRect.top = mTextDrawBottom + mGap + mMarkSize + mGap;
        mSeekbarRect.bottom = mSeekbarRect.top + mSeekbarHeight / 2;

        mSeekbarRectSelected.top = mSeekbarRect.top;
        mSeekbarRectSelected.bottom = mSeekbarRect.bottom;

        mPartLength = (int) ((mSeekbarRect.right - mSeekbarRect.left) / (mTextArray.length - 1) + 0.5f);
        mRightBoundary = (int) (mSeekbarRect.right + mCursorH / 2);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initData() {
        if (mTextArray == null || mTextArray.length == 0) {
            mTextArray = new String[]{"0", "5", "10", "15", "20", "25", "40", "60", "80", "100", "100+"};
        }
        mLeftCursorIndex = 0;
        mRightCursorIndex = mTextArray.length - 1;
        mRightCursorNextIndex = (int) mRightCursorIndex;

        mTextWidthArray = new float[mTextArray.length];
        mClickRectArray = new Rect[mTextArray.length];
    }

    private void initPadding() {
        if (mPaddingRect == null) {
            mPaddingRect = new Rect();
        }
        mPaddingRect.left = (int) (PADDING_LEFT * mDensity + getPaddingLeft() + 0.5f);
        mPaddingRect.top = (int) (PADDING_TOP * mDensity + getPaddingTop() + 0.5f);
        mPaddingRect.right = (int) (PADDING_RIGHT * mDensity + getPaddingRight() + 0.5f);
        mPaddingRect.bottom = (int) (PADDING_BOTTOM * mDensity + getPaddingBottom() + 0.5f);

    }

    private void applyConfig(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekbar);

        mDuration = a.getInteger(R.styleable.RangeSeekbar_autoMoveDuration, DEFAULT_DURATION);

        mLeftCursorBG = a.getDrawable(R.styleable.RangeSeekbar_leftCursorBackground);
        mRightCursorBG = a.getDrawable(R.styleable.RangeSeekbar_rightCursorBackground);

        if (mLeftCursorBG == null)
            mLeftCursorBG = context.getResources().getDrawable(R.mipmap.range_seek_bar_cursor_bg);
        if (mRightCursorBG == null)
            mRightCursorBG = context.getResources().getDrawable(R.mipmap.range_seek_bar_hint_cursor_bg_1);

        mTextColorNormal = a.getColor(R.styleable.RangeSeekbar_textColorNormal, Color.parseColor("#9e998f"));
        mTextColorSelected = a.getColor(R.styleable.RangeSeekbar_textColorSelected, Color.parseColor("#db4437"));
        mSeekbarColorSelected = a.getColor(R.styleable.RangeSeekbar_seekbarColorSelected, Color.parseColor("#db4437"));

        mSeekbarHeight = (int) a.getDimension(R.styleable.RangeSeekbar_seekbarHeight, SEEKBAR_HEIGHT * metrics.density);

        mTextSize = (int) a.getDimension(R.styleable.RangeSeekbar_textSize, TEST_SIZE * metrics.scaledDensity);
        mGap = (int) a.getDimension(R.styleable.RangeSeekbar_spaceBetween, GAP * metrics.density);

        mTextArray = a.getTextArray(R.styleable.RangeSeekbar_markTextArray);
        a.recycle();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);
        mPaint.setTextSize(mTextSize);
        mPaint.setStrokeWidth(0.1f * mDensity);
    }

    private void initTextBoundsArray() {
        if (mTextArray != null && mTextArray.length > 0) {
            final int length = mTextArray.length;
            for (int i = 0; i < length; i++) {
                mTextWidthArray[i] = mPaint.measureText(mTextArray[i].toString());
            }
        }

        FontMetrics fm = mPaint.getFontMetrics();
        mTextHeight = (int) Math.ceil(fm.leading - fm.ascent) - 2;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*** 把数字放到seekBar上去 ***/
        final int length = mTextArray.length;
        mPaint.setTextSize(mTextSize);
        for (int i = 0; i < length; i++) {
            if ((i > mLeftCursorIndex && i < mRightCursorIndex) || (i == mLeftCursorIndex || i == mRightCursorIndex)) {
                mPaint.setColor(mTextColorSelected);
            } else {
                mPaint.setColor(mTextColorNormal);
            }

            final String text2draw = mTextArray[i].toString();
            final float textWidth = mTextWidthArray[i];

            float markX = mSeekbarRect.left + i * mPartLength;
            if (i == 0) markX += mPaint.getStrokeWidth();
            else if (i == length - 1) markX -= mPaint.getStrokeWidth();
            float textDrawLeft = markX - textWidth / 2f;
            canvas.drawText(text2draw, textDrawLeft, mTextDrawBottom, mPaint);
            canvas.drawLine(markX, mTextDrawBottom + mGap, markX, mTextDrawBottom + mGap + mMarkSize, mPaint);

            Rect rect = mClickRectArray[i];
            if (rect == null) {
                rect = new Rect();
                rect.top = mPaddingRect.top;
                rect.bottom = rect.top + mTextHeight + mGap + mSeekbarHeight;
                rect.left = (int) textDrawLeft;
                rect.right = (int) (rect.left + textWidth);
                mClickRectArray[i] = rect;
            }
        }

        /*** 画 seekbar ***/
        mSeekbarRectSelected.left = mSeekbarRect.left + mPartLength * mLeftCursorIndex;
        mSeekbarRectSelected.right = mSeekbarRect.left + mPartLength * mRightCursorIndex;

        if (mLeftCursorIndex == 0 && mRightCursorIndex == length - 1) {
            mPaint.setColor(mSeekbarColorSelected);
            canvas.drawRect(mSeekbarRect, mPaint);
        } else {
            mPaint.setColor(mSeekbarColorSelected);
            mPaint.setStyle(Style.STROKE);
            canvas.drawRect(mSeekbarRect, mPaint);
            mPaint.setStyle(Style.FILL);

            mPaint.setColor(mSeekbarColorSelected);
            canvas.drawRect(mSeekbarRectSelected, mPaint);
        }


        /*** 画游标 ***/
        // left cursor first
        final int leftWidth = mCursorW;
        final int leftHieght = mCursorH;
        final int leftLeft = (int) (mSeekbarRectSelected.left - (float) leftWidth / 2);
        final int leftTop = (int) (mSeekbarRect.bottom + mGap + 0.5f);
        mLeftCursorRect.left = leftLeft;
        mLeftCursorRect.top = leftTop;
        mLeftCursorRect.right = leftLeft + leftWidth;
        mLeftCursorRect.bottom = leftTop + leftHieght;
        mLeftCursorBG.setBounds(mLeftCursorRect);
        mLeftCursorBG.draw(canvas);

        //左边
        int index0 = (int) (mLeftCursorIndex + 0.5f);
        mPaint.setColor(Color.WHITE);
        String text2draw0 = mTextArray[index0].toString();
        if (index0 == 0) {
            text2draw0 = "0";
        }

        final float textDrawLeft0 = leftLeft + (leftWidth - mTextWidthArray[index0]) / 2f;
        final float textDrawBottom0 = mLeftCursorRect.bottom - (leftHieght - mTextHeight) / 1.5f;

        canvas.drawText(text2draw0, textDrawLeft0, textDrawBottom0, mPaint);

        // right cursor second
        final int rightWidth = leftWidth;
        final int rightHeight = leftHieght;
        final int rightLeft1 = (int) (mSeekbarRectSelected.right - (float) rightWidth / 2f);
        int right = (int) (rightLeft1 <= mPaddingRect.left ? mPaddingRect.left : rightLeft1);
        final int rightTop = (int) mPaddingRect.top * 6;
        mRightCursorRect.left = right;
        mRightCursorRect.top = rightTop;
        mRightCursorRect.right = right + rightWidth;
        mRightCursorRect.bottom = rightTop + rightHeight;
        mRightCursorBG.setBounds(mRightCursorRect);
        mRightCursorBG.draw(canvas);

        //右边
        int index1 = (int) (mRightCursorIndex + 0.5f);
        mPaint.setColor(Color.WHITE);
//        mPaint.setTextSize(mTextSize);
        String text2draw1 = mTextArray[index1].toString();

        if (index1 == mTextArray.length - 1) {
            text2draw1 = "100+";
        }

        final float textDrawLeft1 = right + (rightWidth - mTextWidthArray[index1]) / 2f;
        final float textDrawBottom1 = mRightCursorRect.bottom - (rightHeight - mTextHeight) / 1.5f;

        canvas.drawText(text2draw1, textDrawLeft1, textDrawBottom1, mPaint);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null && (mLeftHited || mRightHited)) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        // For multiple touch
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                handleTouchDown(event);

                break;
            case MotionEvent.ACTION_POINTER_DOWN:

                handleTouchDown(event);

                break;
            case MotionEvent.ACTION_MOVE:

                handleTouchMove(event);

                break;
            case MotionEvent.ACTION_POINTER_UP:

                handleTouchUp(event);

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                handleTouchUp(event);
                mClickIndex = -1;
                mClickDownLastX = -1;
                mClickDownLastY = -1;

                break;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int downX = (int) event.getX(actionIndex);
        final int downY = (int) event.getY(actionIndex);


        float result = mDensity * SENSITIVITY_IN_DP;
        int space = (int) (result + 0.5f);
        Rect left = new Rect(mLeftCursorRect.left - space, mLeftCursorRect.top - space, mLeftCursorRect.right + space, mLeftCursorRect.bottom + space);
        Rect right = new Rect(mRightCursorRect.left - space, mRightCursorRect.top - space, mRightCursorRect.right + space, mRightCursorRect.bottom + space);
        if (left.contains(downX, downY)) {
            if (mLeftHited) {
                return;
            }

            // If hit, change state of drawable, and record id of touch pointer.
            mLeftPointerLastX = downX;
            mLeftCursorBG.setState(mPressedEnableState);
            mLeftPointerID = event.getPointerId(actionIndex);
            mLeftHited = true;

            invalidate();
        } else if (right.contains(downX, downY)) {
            if (mRightHited) {
                return;
            }

            mRightPointerLastX = downX;
            mRightCursorBG.setState(mPressedEnableState);
            mRightPointerID = event.getPointerId(actionIndex);
            mRightHited = true;

            invalidate();
        } else {
            // If touch x-y not be contained in cursor,
            // then we check if it in click areas
            final int clickBoundaryTop = mClickRectArray[0].top;
            final int clickBoundaryBottom = mClickRectArray[0].bottom;
            mClickDownLastX = downX;
            mClickDownLastY = downY;

            // Step one : if in boundary of total Y.
            if (downY < clickBoundaryTop || downY > clickBoundaryBottom) {
                mClickIndex = -1;
                return;
            }

            // Step two: find nearest mark in x-axis
            final int partIndex = (int) ((downX - mSeekbarRect.left) / mPartLength);
            final int partDelta = (int) ((downX - mSeekbarRect.left) % mPartLength);
            if (partDelta < mPartLength / 2) {
                mClickIndex = partIndex;
            } else if (partDelta > mPartLength / 2) {
                mClickIndex = partIndex + 1;
            }

            if (mClickIndex == mLeftCursorIndex || mClickIndex == mRightCursorIndex) {
                mClickIndex = -1;
                return;
            }

            // Step three: check contain
            if (mClickIndex != -1 && !mClickRectArray[mClickIndex].contains(downX, downY)) {
                mClickIndex = -1;
            }
        }

    }

    private void handleTouchUp(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int actionID = event.getPointerId(actionIndex);

        if (actionID == mLeftPointerID) {
            if (!mLeftHited) {
                return;
            }

            // step 1:Calculate the offset with lower mark.
            final int lower = (int) Math.floor(mLeftCursorIndex);
            final int higher = (int) Math.ceil(mLeftCursorIndex);

            final float offset = mLeftCursorIndex - lower;
//            if (offset != 0) {

            // step 2:Decide which mark will go to.
            if (offset < 0.5f) {
                mLeftCursorNextIndex = lower;
            } else if (offset > 0.5f) {
                mLeftCursorNextIndex = higher;

                if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1 && mLeftCursorNextIndex == mRightCursorNextIndex) {
                    // Left can not go to the higher, just to the lower one.
                    mLeftCursorNextIndex = lower;
                }
            }

            // step 3: Move to.
            if (!mLeftScroller.computeScrollOffset()) {
                final int fromX = (int) (mLeftCursorIndex * mPartLength);

                mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0, mDuration);

                triggleCallback(true, mLeftCursorNextIndex);
            }
            if (mLeftCursorIndex < 0.5) {
                mLeftCursorIndex = 0;
            }
            if (mLeftCursorIndex >= 0.5 && mLeftCursorIndex < 1.5) {
                mLeftCursorIndex = 1;
            }

            if (mLeftCursorIndex >= 1.5 && mLeftCursorIndex < 2.5) {
                mLeftCursorIndex = 2;
            }

            if (mLeftCursorIndex >= 2.5 && mLeftCursorIndex < 3.5) {
                mLeftCursorIndex = 3;
            }

            if (mLeftCursorIndex >= 3.5 && mLeftCursorIndex < 4.5) {
                mLeftCursorIndex = 4;
            }

            if (mLeftCursorIndex >= 4.5 && mLeftCursorIndex < 5.5) {
                mLeftCursorIndex = 5;
            }

            if (mLeftCursorIndex >= 5.5 && mLeftCursorIndex < 6.5) {
                mLeftCursorIndex = 6;
            }

            if (mLeftCursorIndex >= 6.5 && mLeftCursorIndex < 7.5) {
                mLeftCursorIndex = 7;
            }

            if (mLeftCursorIndex >= 7.5 && mLeftCursorIndex < 8.5) {
                mLeftCursorIndex = 8;
            }

            if (mLeftCursorIndex >= 8.5 && mLeftCursorIndex < 9.5) {
                mLeftCursorIndex = 9;
            }
            EventBus.getDefault().post(new RangBarEvent(((int) mLeftCursorIndex), 1));
//            }

            // Reset values of parameters
            mLeftPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mLeftPointerID = -1;
            mLeftHited = false;

            invalidate();

        } else if (actionID == mRightPointerID) {
            if (!mRightHited) {
                return;
            }

            final int lower = (int) Math.floor(mRightCursorIndex);
            final int higher = (int) Math.ceil(mRightCursorIndex);

            final float offset = mRightCursorIndex - lower;
//            if (offset != 0) {

            if (offset > 0.5f) {
                mRightCursorNextIndex = higher;
            } else if (offset < 0.5f) {
                mRightCursorNextIndex = lower;
                if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1 && mRightCursorNextIndex == mLeftCursorNextIndex) {
                    mRightCursorNextIndex = higher;
                }
            }

            if (!mRightScroller.computeScrollOffset()) {
                final int fromX = (int) (mRightCursorIndex * mPartLength);

                mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0, mDuration);

                triggleCallback(false, mRightCursorNextIndex);
            }
            if (mRightCursorIndex > 0.5 && mRightCursorIndex < 1.5) {
                mRightCursorIndex = 1;
            }

            if (mRightCursorIndex >= 1.5 && mRightCursorIndex < 2.5) {
                mRightCursorIndex = 2;
            }

            if (mRightCursorIndex >= 2.5 && mRightCursorIndex < 3.5) {
                mRightCursorIndex = 3;
            }

            if (mRightCursorIndex >= 3.5 && mRightCursorIndex < 4.5) {
                mRightCursorIndex = 4;
            }

            if (mRightCursorIndex >= 4.5 && mRightCursorIndex < 5.5) {
                mRightCursorIndex = 5;
            }
            if (mRightCursorIndex >= 5.5 && mRightCursorIndex < 6.5) {
                mRightCursorIndex = 6;
            }
            if (mRightCursorIndex >= 6.5 && mRightCursorIndex < 7.5) {
                mRightCursorIndex = 7;
            }

            if (mRightCursorIndex >= 7.5 && mRightCursorIndex < 8.5) {
                mRightCursorIndex = 8;
            }
            if (mRightCursorIndex >= 8.5 && mRightCursorIndex < 9.5) {
                mRightCursorIndex = 9;
            }
            EventBus.getDefault().post(new RangBarEvent(((int) mRightCursorIndex), 2));
//            }

            mRightPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mRightPointerID = -1;
            mRightHited = false;

            invalidate();
        } else {
            final int pointerIndex = event.findPointerIndex(actionID);
            final int upX = (int) event.getX(pointerIndex);
            final int upY = (int) event.getY(pointerIndex);

            if (mClickIndex != -1 && mClickRectArray[mClickIndex].contains(upX, upY)) {
                // Find nearest cursor
                final float distance2LeftCursor = Math.abs(mLeftCursorIndex - mClickIndex);
                final float distance2Right = Math.abs(mRightCursorIndex - mClickIndex);

                final boolean moveLeft = distance2LeftCursor <= distance2Right;
                int fromX = 0;
                if (moveLeft) {
                    if (!mLeftScroller.computeScrollOffset()) {
                        mLeftCursorNextIndex = mClickIndex;
                        fromX = (int) (mLeftCursorIndex * mPartLength);
                        mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0, mDuration);

                        triggleCallback(true, mLeftCursorNextIndex);

                        invalidate();
                    }
                } else {
                    if (!mRightScroller.computeScrollOffset()) {
                        mRightCursorNextIndex = mClickIndex;
                        fromX = (int) (mRightCursorIndex * mPartLength);
                        mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0, mDuration);

                        triggleCallback(false, mRightCursorNextIndex);

                        invalidate();
                    }
                }
            }
        }

    }

    private void handleTouchMove(MotionEvent event) {
        if (mClickIndex != -1) {
            final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int x = (int) event.getX(actionIndex);
            final int y = (int) event.getY(actionIndex);

            if (!mClickRectArray[mClickIndex].contains(x, y)) {
                mClickIndex = -1;
            }
        }

        if (mLeftHited && mLeftPointerID != -1) {

            final int index = event.findPointerIndex(mLeftPointerID);
            final float x = event.getX(index);

            float deltaX = x - mLeftPointerLastX;
            mLeftPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT : DIRECTION.RIGHT);

            if (direction == DIRECTION.LEFT && mLeftCursorIndex == 0) {
                return;
            }

            if (mLeftCursorRect.left + deltaX < mPaddingRect.left) {
                mLeftCursorIndex = 0;
                invalidate();
                return;
            }

            if (mLeftCursorRect.right + deltaX >= mRightCursorRect.left) {

                if (mRightHited || mRightCursorIndex == mTextArray.length - 1 || mRightScroller.computeScrollOffset()) {
                    deltaX = mRightCursorRect.left - mLeftCursorRect.right;
                } else {
                    final int maxMarkIndex = mTextArray.length - 1;

                    if (mRightCursorIndex <= maxMarkIndex - 1) {
                        mRightCursorNextIndex = (int) (mRightCursorIndex + 1);

                        if (!mRightScroller.computeScrollOffset()) {
                            final int fromX = (int) (mRightCursorIndex * mPartLength);

                            mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0, mDuration);
                            triggleCallback(false, mRightCursorNextIndex);
                        }
                    }
                }
            }

            if (deltaX == 0) {
                return;
            }

            // Calculate the movement.
            final float moveX = deltaX / mPartLength;
            mLeftCursorIndex += moveX;

            invalidate();
        }

        if (mRightHited && mRightPointerID != -1) {

            final int index = event.findPointerIndex(mRightPointerID);
            final float x = event.getX(index);

            float deltaX = x - mRightPointerLastX;
            mRightPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT : DIRECTION.RIGHT);

            final int maxIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT && mRightCursorIndex == maxIndex) {
                return;
            }

            if (mRightCursorRect.right + deltaX > mRightBoundary) {
                deltaX = mRightBoundary - mRightCursorRect.right;
            }

            final int maxMarkIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT && mRightCursorIndex == maxMarkIndex) {
                return;
            }

            if (mRightCursorRect.left + deltaX < mLeftCursorRect.right) {
                if (mLeftHited || mLeftCursorIndex == 0 || mLeftScroller.computeScrollOffset()) {
                    deltaX = mLeftCursorRect.right - mRightCursorRect.left;
                } else {
                    if (mLeftCursorIndex >= 1) {
                        mLeftCursorNextIndex = (int) (mLeftCursorIndex - 1);

                        if (!mLeftScroller.computeScrollOffset()) {
                            final int fromX = (int) (mLeftCursorIndex * mPartLength);
                            mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0, mDuration);
                            triggleCallback(true, mLeftCursorNextIndex);
                        }
                    }
                }
            }

            if (deltaX == 0) {
                return;
            }

            final float moveX = deltaX / mPartLength;
            mRightCursorIndex += moveX;

            invalidate();
        }
    }

    @Override
    public void computeScroll() {

        if (mLeftScroller.computeScrollOffset()) {
            final int deltaX = mLeftScroller.getCurrX();
            mLeftCursorIndex = (float) deltaX / mPartLength;
            invalidate();
        }

        if (mRightScroller.computeScrollOffset()) {
            final int deltaX = mRightScroller.getCurrX();
            mRightCursorIndex = (float) deltaX / mPartLength;
            invalidate();
        }
        super.computeScroll();
    }

    private void triggleCallback(boolean isLeft, int location) {
        if (mListener == null) {
            return;
        }

        if (isLeft) {
            mListener.onLeftCursorChanged(location, mTextArray[location].toString());
        } else {
            mListener.onRightCursorChanged(location, mTextArray[location].toString());
        }
    }

    //获取左边游标的刻度值
    public int getLeftCursorIndex() {

        return (int) mLeftCursorIndex;
    }

    //获取左边游标的刻度值
    public int getRightCursorIndex() {
        return (int) mRightCursorIndex;
    }


    public interface OnCursorChangeListener {
        void onLeftCursorChanged(int location, String textMark);

        void onRightCursorChanged(int location, String textMark);
    }
}
