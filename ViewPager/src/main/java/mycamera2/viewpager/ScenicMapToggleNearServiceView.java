package mycamera2.viewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * @desc ${TODD}
 */

public class ScenicMapToggleNearServiceView extends View {

    private Drawable drawableOn;
    private Drawable drawableOff;
    private Drawable drawableBg;
    private boolean toggleState;
    private float bgPaddingTop;
    private float bgPaddingBottom;
    private float bgLayoutHeight;
    private float bgLayoutWidth;
    private float toggleLayoutHeight;
    private float toggleLayoutWidth;
    private int viewHeight;
    private int viewWidth;
    private int maxMoveY;
    private boolean isFirstDraw = true;
    private float lastRawY;
    private float rawY;
    private float moveY;
    private boolean isBeingDrag;
    private Rect rect;
    private boolean isActionMove;
    private float rawX;
    private OnToggleChangedListener listener;


    public ScenicMapToggleNearServiceView(Context context) {
        this(context, null);
        // TODO: 2017/6/12 不做处理
    }

    public ScenicMapToggleNearServiceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScenicMapToggleNearServiceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    public void setListener(OnToggleChangedListener listener) {
        this.listener = listener;
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ScenicMapToggleNearServiceView, defStyleAttr, 0);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.ScenicMapToggleNearServiceView_on:
                    drawableOn = a.getDrawable(attr);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_off:
                    drawableOff = a.getDrawable(attr);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_bg:
                    drawableBg = a.getDrawable(attr);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_state:
                    toggleState = a.getBoolean(attr, true);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_bg_paddingTop:
                    bgPaddingTop = a.getDimension(attr, 0);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_bg_paddingBottom:
                    bgPaddingBottom = a.getDimension(attr, 0);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_bg_layout_height:
                    bgLayoutHeight = a.getDimension(attr, 0);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_bg_layout_width:
                    bgLayoutWidth = a.getDimension(attr, 0);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_toggle_layout_height:
                    toggleLayoutHeight = a.getDimension(attr, 0);
                    break;
                case R.styleable.ScenicMapToggleNearServiceView_toggle_layout_width:
                    toggleLayoutWidth = a.getDimension(attr, 0);
                    break;
            }
        }
        a.recycle();
        if (null == drawableOn || null == drawableOff || null == drawableBg) {
            return;
        }
        // 初始化bg的宽高,并且缩放
        float ratio = drawableBg.getIntrinsicWidth() * 1.0f / drawableBg.getIntrinsicHeight();
        if (bgLayoutWidth == 0 || bgLayoutHeight == 0) {
            if (bgLayoutWidth == 0 && bgLayoutHeight != 0) {
                bgLayoutWidth = bgLayoutHeight * ratio;
            }
            if (bgLayoutHeight == 0 && bgLayoutWidth != 0) {
                bgLayoutHeight = bgLayoutWidth / ratio;
            }
            if (bgLayoutHeight == 0 && bgLayoutWidth == 0) {
                bgLayoutHeight = drawableBg.getIntrinsicHeight();
                bgLayoutWidth = drawableBg.getIntrinsicWidth();
            }
            if (bgLayoutHeight != 0 && bgLayoutWidth != 0) {
                drawableBg = ImageConversionUtils.zoomDrawable(drawableBg, bgLayoutWidth, bgLayoutHeight);
            }
        }
        // 初始化开关的宽高,并且缩放
        ratio = Math.max(drawableOn.getIntrinsicWidth() * 1.0f / drawableOn.getIntrinsicHeight(), drawableOff.getIntrinsicWidth() * 1.0f / drawableOff.getIntrinsicHeight());
        if (toggleLayoutHeight == 0 || toggleLayoutWidth == 0) {
            if (toggleLayoutWidth == 0 && toggleLayoutHeight != 0) {
                toggleLayoutWidth = toggleLayoutHeight * ratio;
            }
            if (toggleLayoutHeight == 0 && toggleLayoutWidth != 0) {
                toggleLayoutHeight = toggleLayoutWidth / ratio;
            }
            if (toggleLayoutHeight == 0 && toggleLayoutWidth == 0) {
                if (ratio == drawableOn.getIntrinsicWidth() * 1.0f / drawableOn.getIntrinsicHeight()) {
                    toggleLayoutHeight = drawableOn.getIntrinsicHeight();
                    toggleLayoutWidth = drawableOn.getIntrinsicWidth();
                } else {
                    toggleLayoutHeight = drawableOff.getIntrinsicHeight();
                    toggleLayoutWidth = drawableOff.getIntrinsicWidth();
                }
            }
            if (toggleLayoutHeight != 0 && toggleLayoutWidth != 0) {
                drawableOn = ImageConversionUtils.zoomDrawable(drawableOn, toggleLayoutWidth, toggleLayoutHeight);
                drawableOff = ImageConversionUtils.zoomDrawable(drawableOff, toggleLayoutWidth, toggleLayoutHeight);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /**
         * 不考虑以下这两个属性的设定
         *      android:layout_width="wrap_content"
         *      android:layout_height="wrap_content"
         */
        viewHeight = getMeasuredLength(heightMeasureSpec, false);
        viewWidth = getMeasuredLength(heightMeasureSpec, true);
        setMeasuredDimension(viewWidth, viewHeight);
        if (toggleLayoutHeight != 0) {
            maxMoveY = (int) (viewHeight - toggleLayoutHeight);
        } else {
            maxMoveY = viewHeight - Math.min(drawableOn.getIntrinsicHeight(), drawableOff.getIntrinsicHeight());
        }
    }

    private int getMeasuredLength(int length, boolean isWidth) {
        int specMode = MeasureSpec.getMode(length);
        int specSize = MeasureSpec.getSize(length);
        int size;
        int padding = isWidth ? getPaddingLeft() + getPaddingRight()
                : (int) (getPaddingTop() + getPaddingBottom() + bgPaddingBottom + bgPaddingTop);
        if (isWidth) {// 宽
            // 如果指定了开关的宽高，控件的宽由bgLayoutHeight决定，否则由图片的初始的宽决定
            if (toggleLayoutHeight != 0 && toggleLayoutWidth != 0) {
                size = (int) (padding + toggleLayoutWidth);
            } else {
                size = padding + Math.max(drawableOn.getIntrinsicWidth(), drawableOff.getIntrinsicWidth());
            }
        } else {// 高
            // 如果指定了bg的宽高，控件的高由toggleLayoutWidth决定，否则由bg的初始的高决定
            if (bgLayoutWidth != 0 && bgLayoutHeight != 0) {
                size = (int) (padding + bgLayoutHeight);
            } else {
                size = padding + drawableBg.getIntrinsicHeight();
            }
        }
        return size;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isBeingDrag) {
            transformToggle();
        } else {
            if (toggleState) {// 开
                moveY = 0;
                transformToggle();
            } else {// 关
                moveY = maxMoveY;
                transformToggle();
            }
        }
        if (isFirstDraw) {
            int left = (int) ((viewWidth - bgLayoutWidth) / 2);
            int right = (int) (bgLayoutWidth + left);
            int top = (int) bgPaddingTop;
            int bottom = (int) (bgLayoutHeight + top);
            drawableBg.setBounds(left, top, right, bottom);
            isFirstDraw = false;
        }

        drawableBg.draw(canvas);
        drawableOff.draw(canvas);
        drawableOn.draw(canvas);
    }

    private void transformToggle() {
        drawableOn.setAlpha((int) (255 * (1 - moveY * 1.0f / maxMoveY)));
        drawableOff.setAlpha((int) (255 * moveY * 1.0f / maxMoveY));
        drawableOn.setBounds(0, (int) moveY, (int) toggleLayoutWidth, (int) (toggleLayoutHeight + moveY));
        drawableOff.setBounds(0, (int) moveY, (int) toggleLayoutWidth, (int) (toggleLayoutHeight + moveY));
        if (moveY == 0 || moveY == maxMoveY) {
            if (rect == null) {
                int[] location = new int[2];
                getLocationOnScreen(location);
                rect = new Rect(location[0], location[1] + (int) moveY, location[0] + (int) toggleLayoutWidth, location[1] + (int) (toggleLayoutHeight + moveY));
            } else {
                int[] location = new int[2];
                getLocationOnScreen(location);
                rect.set(location[0], location[1] + (int) moveY, location[0] + (int) toggleLayoutWidth, location[1] + (int) (toggleLayoutHeight + moveY));
            }
        }
    }

    public void setToggle(boolean toggleState) {
        this.toggleState = toggleState;
        invalidate();
        if (null != listener) {
            listener.onToggle(toggleState);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                } else {
                    return false;
                }
                lastRawY = event.getRawY();
                rawY = event.getRawY();
                rawX = event.getRawX();
                isActionMove = false;
                if (toggleState) {
                    moveY = 0;
                } else {
                    moveY = maxMoveY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                isBeingDrag = true;
                float currentRawY = event.getRawY();
                float currentRawX = event.getRawX();
                moveY += currentRawY - lastRawY;
                moveY += currentRawY - lastRawY;
                lastRawY = currentRawY;
                if (!isActionMove && (Math.abs(currentRawY - rawY) >= 5 || Math.abs(currentRawX - rawX) >= 5)) {
                    //滑动了,不能触发点击事件
                    isActionMove = true;
                }
                if (moveY <= 0) {
                    moveY = 0;
                } else if (moveY >= maxMoveY) {
                    moveY = maxMoveY;
                }
                transformToggle();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isBeingDrag = false;
                if (isActionMove) {
                    if (moveY > maxMoveY / 2) {
                        setToggle(false);
                        // selectNearService();
                    } else if (moveY <= maxMoveY / 2) {
                        setToggle(true);
                        // selectSubScenic();
                    }
                } else {// 点击
                    if (moveY < maxMoveY / 2) {
                        // selectNearService();
                        setToggle(false);
                    } else {
                        setToggle(true);
                        // selectSubScenic();
                    }
                }
                break;
        }
        return true;
    }

    private interface OnToggleChangedListener {
        void onToggle(boolean toggleState);
    }

    public void recycle() {
        drawableOn = null;
        drawableOff = null;
        drawableBg = null;
        listener = null;
    }
}
