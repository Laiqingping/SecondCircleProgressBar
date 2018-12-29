package com.dinuscxj.progressbar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgressBar extends View {
    private static final int DEFAULT_MAX = 100;
    private static final float MAX_DEGREE = 360.0f;
    private static final float LINEAR_START_DEGREE = 90.0f;

    private static final int LINE = 0;
    private static final int SOLID = 1;
    private static final int SOLID_LINE = 2;

    private static final int LINEAR = 0;
    private static final int RADIAL = 1;
    private static final int SWEEP = 2;

    private static final int DEFAULT_START_DEGREE = -90;

    private static final int DEFAULT_LINE_COUNT = 45;

    private static final float DEFAULT_LINE_WIDTH = 4.0f;
    private static final float DEFAULT_PROGRESS_TEXT_SIZE = 11.0f;
    private static final float DEFAULT_PROGRESS_STROKE_WIDTH = 1.0f;

    private static final String COLOR_FFF2A670 = "#fff2a670";
    private static final String COLOR_FFD3D3D5 = "#ffe3e3e5";

    private static final String COLOR_TRANSPARENT = "#00000000";

    private final RectF mProgressFirstRectF = new RectF();
    private final Rect mProgressFirstTextRect = new Rect();

    private final Paint mProgressFirstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressFirstBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mProgressFirstTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private float mRadius;
    private float mCenterX;
    private float mCenterY;

    private int mProgressFirst;
    private int mProgressSecond;
    private int mMax = DEFAULT_MAX;

    //Only work well in the Line Style, represents the line count of the rings included
    private int mLineCount;
    //Only work well in the Line Style, Height of the line of the progress bar
    private float mLineWidth;

    //Stroke width of the progress of the progress bar
    private float mProgressFirstStrokeWidth;

    //Text size of the progress of the progress bar
    private float mProgressFirstTextSize;

    //Start color of the first progress of the progress bar
    private int mProgressFirstFirstStartColor;
    //End color of the first progress of the progress bar
    private int mProgressFirstFirstEndColor;
    //Color of the second progress value of the progress bar

    private int mProgressFirstSecondStartColor;
    //Color of the second progress value of the progress bar
    private int mProgressFirstSecondEndColor;


    private int mProgressFirstTextColor;
    //Background color of the progress of the progress bar
    private int mProgressFirstBackgroundColor;

    //the rotate degree of the canvas, default is -90.
    private int mStartDegree;

    // whether draw the background only outside the progress area or not
    private boolean mDrawBackgroundOutsideProgress;

    //Format the current progress value to the specified format
    private ProgressFormatter mProgressFirstFormatter = new DefaultProgressFormatter();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LINE, SOLID, SOLID_LINE})
    private @interface Style {
    }

    //The style of the progress color
    @Style
    private int mStyle;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LINEAR, RADIAL, SWEEP})
    private @interface ShaderMode {
    }

    //The Shader of mProgressFirstPaint
    @ShaderMode
    private int mShader;
    //The Stroke Cap of mProgressFirstPaint and mProgressFirstBackgroundPaint
    private Paint.Cap mCap;

    public CircleProgressBar(Context context) {
        this(context, null);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
        initPaint();
    }

    /**
     * Basic data initialization
     */
    @SuppressWarnings("ResourceType")
    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBar);

        mLineCount = a.getInt(R.styleable.CircleProgressBar_line_count, DEFAULT_LINE_COUNT);

        mStyle = a.getInt(R.styleable.CircleProgressBar_style, LINE);
        mShader = a.getInt(R.styleable.CircleProgressBar_progress_shader, LINEAR);
        mCap = a.hasValue(R.styleable.CircleProgressBar_progress_stroke_cap) ?
                Paint.Cap.values()[a.getInt(R.styleable.CircleProgressBar_progress_stroke_cap, 0)] : Paint.Cap.BUTT;

        mLineWidth = a.getDimensionPixelSize(R.styleable.CircleProgressBar_line_width, UnitUtils.dip2px(getContext(), DEFAULT_LINE_WIDTH));
        mProgressFirstTextSize = a.getDimensionPixelSize(R.styleable.CircleProgressBar_progress_text_size, UnitUtils.dip2px(getContext(), DEFAULT_PROGRESS_TEXT_SIZE));
        mProgressFirstStrokeWidth = a.getDimensionPixelSize(R.styleable.CircleProgressBar_progress_stroke_width, UnitUtils.dip2px(getContext(), DEFAULT_PROGRESS_STROKE_WIDTH));

        mProgressFirstFirstStartColor = a.getColor(R.styleable.CircleProgressBar_progress_first_start_color, Color.parseColor(COLOR_FFF2A670));
        mProgressFirstFirstEndColor = a.getColor(R.styleable.CircleProgressBar_progress_first_end_color, Color.parseColor(COLOR_FFF2A670));
        mProgressFirstSecondStartColor = a.getColor(R.styleable.CircleProgressBar_progress_second_start_color,Color.parseColor(COLOR_TRANSPARENT));
        mProgressFirstSecondEndColor = a.getColor(R.styleable.CircleProgressBar_progress_second_end_color,Color.parseColor(COLOR_TRANSPARENT));
        mProgressFirstTextColor = a.getColor(R.styleable.CircleProgressBar_progress_text_color, Color.parseColor(COLOR_FFF2A670));
        mProgressFirstBackgroundColor = a.getColor(R.styleable.CircleProgressBar_progress_background_color, Color.parseColor(COLOR_FFD3D3D5));

        mStartDegree = a.getInt(R.styleable.CircleProgressBar_progress_start_degree, DEFAULT_START_DEGREE);
        mDrawBackgroundOutsideProgress = a.getBoolean(R.styleable.CircleProgressBar_drawBackgroundOutsideProgress, false);

        a.recycle();
    }

    /**
     * Paint initialization
     */
    private void initPaint() {
        mProgressFirstTextPaint.setTextAlign(Paint.Align.CENTER);
        mProgressFirstTextPaint.setTextSize(mProgressFirstTextSize);

        mProgressFirstPaint.setStyle(mStyle == SOLID ? Paint.Style.FILL : Paint.Style.STROKE);
        mProgressFirstPaint.setStrokeWidth(mProgressFirstStrokeWidth);
        mProgressFirstPaint.setColor(mProgressFirstFirstStartColor);
        mProgressFirstPaint.setStrokeCap(mCap);
        mProgressSecondPaint.setStyle(mStyle == SOLID ? Paint.Style.FILL : Paint.Style.STROKE);
        mProgressSecondPaint.setStrokeWidth(mProgressFirstStrokeWidth);
        mProgressSecondPaint.setColor(mProgressFirstSecondStartColor);
        mProgressSecondPaint.setStrokeCap(mCap);

        mProgressFirstBackgroundPaint.setStyle(mStyle == SOLID ? Paint.Style.FILL : Paint.Style.STROKE);
        mProgressFirstBackgroundPaint.setStrokeWidth(mProgressFirstStrokeWidth);
        mProgressFirstBackgroundPaint.setColor(mProgressFirstBackgroundColor);
        mProgressFirstBackgroundPaint.setStrokeCap(mCap);
    }

    /**
     * The progress bar color gradient,
     * need to be invoked in the {@link #onSizeChanged(int, int, int, int)}
     */
    private void updateProgressShader() {
        if (mProgressFirstFirstStartColor != mProgressFirstFirstEndColor) {
            Shader shader = null;
            Shader shader2 = null;
            switch (mShader) {
                case LINEAR: {
                    shader = new LinearGradient(mProgressFirstRectF.left, mProgressFirstRectF.top,
                            mProgressFirstRectF.left, mProgressFirstRectF.bottom,
                            mProgressFirstFirstStartColor, mProgressFirstFirstEndColor, Shader.TileMode.CLAMP);
                    shader2 = new LinearGradient(mProgressFirstRectF.left, mProgressFirstRectF.top,
                            mProgressFirstRectF.left, mProgressFirstRectF.bottom,
                            mProgressFirstSecondStartColor, mProgressFirstSecondEndColor, Shader.TileMode.CLAMP);
                    Matrix matrix = new Matrix();
                    matrix.setRotate(LINEAR_START_DEGREE, mCenterX, mCenterY);
                    shader.setLocalMatrix(matrix);
                    break;
                }
                case RADIAL: {
                    shader = new RadialGradient(mCenterX, mCenterY, mRadius,
                            mProgressFirstFirstStartColor, mProgressFirstFirstEndColor, Shader.TileMode.CLAMP);
                    shader2 = new RadialGradient(mCenterX, mCenterY, mRadius,
                            mProgressFirstSecondStartColor, mProgressFirstSecondEndColor, Shader.TileMode.CLAMP);
                    break;
                }
                case SWEEP: {
                    //arc = radian * radius
                    float radian = (float) (mProgressFirstStrokeWidth / Math.PI * 2.0f / mRadius);
                    float rotateDegrees = (float) (
                            -(mCap == Paint.Cap.BUTT && mStyle == SOLID_LINE ? 0 : Math.toDegrees(radian)));

                    shader = new SweepGradient(mCenterX, mCenterY, new int[]{mProgressFirstFirstStartColor, mProgressFirstFirstEndColor},
                            new float[]{0.0f, 1.0f});
                    shader2 = new SweepGradient(mCenterX, mCenterY, new int[]{mProgressFirstSecondStartColor, mProgressFirstSecondEndColor},
                            new float[]{0.0f, 1.0f});
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotateDegrees, mCenterX, mCenterY);
                    shader.setLocalMatrix(matrix);
                    break;
                }
                default:
                    break;
            }

            mProgressFirstPaint.setShader(shader);
            mProgressSecondPaint.setShader(shader2);
        } else {
            mProgressFirstPaint.setShader(null);
            mProgressFirstPaint.setColor(mProgressFirstFirstStartColor);
            mProgressSecondPaint.setShader(null);
            mProgressSecondPaint.setColor(mProgressFirstSecondStartColor);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.rotate(mStartDegree, mCenterX, mCenterY);
        drawProgress(canvas);
        canvas.restore();

        drawProgressText(canvas);
    }

    private void drawProgressText(Canvas canvas) {
        if (mProgressFirstFormatter == null) {
            return;
        }

        CharSequence progressText = mProgressFirstFormatter.format(mProgressFirst, mMax);

        if (TextUtils.isEmpty(progressText)) {
            return;
        }

        mProgressFirstTextPaint.setTextSize(mProgressFirstTextSize);
        mProgressFirstTextPaint.setColor(mProgressFirstTextColor);

        mProgressFirstTextPaint.getTextBounds(String.valueOf(progressText), 0, progressText.length(), mProgressFirstTextRect);
        canvas.drawText(progressText, 0, progressText.length(), mCenterX, mCenterY + mProgressFirstTextRect.height() / 2, mProgressFirstTextPaint);
    }

    private void drawProgress(Canvas canvas) {
        switch (mStyle) {
            case SOLID:
                drawSolidProgress(canvas);
                break;
            case SOLID_LINE:
                drawSolidLineProgress(canvas);
                break;
            case LINE:
            default:
                drawLineProgress(canvas);
                break;
        }
    }

    /**
     * In the center of the drawing area as a reference point , rotate the canvas
     */
    private void drawLineProgress(Canvas canvas) {
        float unitDegrees = (float) (2.0f * Math.PI / mLineCount);
        float outerCircleRadius = mRadius;
        float interCircleRadius = mRadius - mLineWidth;

        int progressLineCount = (int) ((float) mProgressFirst / (float) mMax * mLineCount);

        int progressLintSecondCount = (int) ((float) mProgressSecond / (float) mMax * mLineCount);

        for (int i = 0; i < mLineCount; i++) {
            float rotateDegrees = i * -unitDegrees;

            float startX = mCenterX + (float) Math.cos(rotateDegrees) * interCircleRadius;
            float startY = mCenterY - (float) Math.sin(rotateDegrees) * interCircleRadius;

            float stopX = mCenterX + (float) Math.cos(rotateDegrees) * outerCircleRadius;
            float stopY = mCenterY - (float) Math.sin(rotateDegrees) * outerCircleRadius;

            if (mDrawBackgroundOutsideProgress) {
                if (i >= progressLineCount) {
                    canvas.drawLine(startX, startY, stopX, stopY, mProgressFirstBackgroundPaint);
                }
            } else {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressFirstBackgroundPaint);
            }

            if(i < progressLintSecondCount){
                canvas.drawLine(startX, startY, stopX, stopY, mProgressSecondPaint);
            }
            if (i < progressLineCount) {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressFirstPaint);
            }
        }
    }

    /**
     * Just draw arc
     */
    private void drawSolidProgress(Canvas canvas) {
        if (mDrawBackgroundOutsideProgress) {
            float startAngle = MAX_DEGREE * mProgressFirst / mMax;
            float sweepAngle = MAX_DEGREE - startAngle;
            canvas.drawArc(mProgressFirstRectF, startAngle, sweepAngle, true, mProgressFirstBackgroundPaint);
        } else {
            canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE, true, mProgressFirstBackgroundPaint);
        }
        canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE * mProgressSecond / mMax, true, mProgressSecondPaint);
        canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE * mProgressFirst / mMax, true, mProgressFirstPaint);
    }

    /**
     * Just draw arc
     */
    private void drawSolidLineProgress(Canvas canvas) {
        if (mDrawBackgroundOutsideProgress) {
            float startAngle = MAX_DEGREE * mProgressFirst / mMax;
            float sweepAngle = MAX_DEGREE - startAngle;
            canvas.drawArc(mProgressFirstRectF, startAngle, sweepAngle, false, mProgressFirstBackgroundPaint);
        } else {
            canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE, false, mProgressFirstBackgroundPaint);
        }
        canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE * mProgressSecond / mMax, false, mProgressSecondPaint);
        canvas.drawArc(mProgressFirstRectF, 0.0f, MAX_DEGREE * mProgressFirst / mMax, false, mProgressFirstPaint);
    }

    /**
     * When the size of CircleProgressBar changed, need to re-adjust the drawing area
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;
        mCenterY = h / 2;

        mRadius = Math.min(mCenterX, mCenterY);
        mProgressFirstRectF.top = mCenterY - mRadius;
        mProgressFirstRectF.bottom = mCenterY + mRadius;
        mProgressFirstRectF.left = mCenterX - mRadius;
        mProgressFirstRectF.right = mCenterX + mRadius;

        updateProgressShader();

        //Prevent the progress from clipping
        mProgressFirstRectF.inset(mProgressFirstStrokeWidth / 2, mProgressFirstStrokeWidth / 2);
    }

    public void setProgressFormatter(ProgressFormatter progressFormatter) {
        this.mProgressFirstFormatter = progressFormatter;
        invalidate();
    }

    public void setProgressStrokeWidth(float progressStrokeWidth) {
        this.mProgressFirstStrokeWidth = progressStrokeWidth;
        mProgressFirstRectF.inset(mProgressFirstStrokeWidth / 2, mProgressFirstStrokeWidth / 2);
        invalidate();
    }

    public void setProgressTextSize(float progressTextSize) {
        this.mProgressFirstTextSize = progressTextSize;
        invalidate();
    }

    public void setProgressStartColor(int progressStartColor) {
        this.mProgressFirstFirstStartColor = progressStartColor;
        updateProgressShader();
        invalidate();
    }

    public void setProgressEndColor(int progressEndColor) {
        this.mProgressFirstFirstEndColor = progressEndColor;
        updateProgressShader();
        invalidate();
    }

    public void setProgressTextColor(int progressTextColor) {
        this.mProgressFirstTextColor = progressTextColor;
        invalidate();
    }

    public void setProgressBackgroundColor(int progressBackgroundColor) {
        this.mProgressFirstBackgroundColor = progressBackgroundColor;
        mProgressFirstBackgroundPaint.setColor(mProgressFirstBackgroundColor);
        invalidate();
    }

    public void setLineCount(int lineCount) {
        this.mLineCount = lineCount;
        invalidate();
    }

    public void setLineWidth(float lineWidth) {
        this.mLineWidth = lineWidth;
        invalidate();
    }

    public void setStyle(@Style int style) {
        this.mStyle = style;
        mProgressFirstPaint.setStyle(mStyle == SOLID ? Paint.Style.FILL : Paint.Style.STROKE);
        mProgressFirstBackgroundPaint.setStyle(mStyle == SOLID ? Paint.Style.FILL : Paint.Style.STROKE);
        invalidate();
    }

    public void setShader(@ShaderMode int shader) {
        mShader = shader;
        updateProgressShader();
        invalidate();
    }

    public void setCap(Paint.Cap cap) {
        mCap = cap;
        mProgressFirstPaint.setStrokeCap(cap);
        mProgressFirstBackgroundPaint.setStrokeCap(cap);
        invalidate();
    }

    public void setProgressFirst(int progress) {
        this.mProgressFirst = progress;
        invalidate();
    }

    public void setProgressSecond(int progress) {
        this.mProgressSecond = progress;
        invalidate();
    }

    public void setMax(int max) {
        this.mMax = max;
        invalidate();
    }

    public int getProgress() {
        return mProgressFirst;
    }

    public int getMax() {
        return mMax;
    }

    public int getStartDegree() {
        return mStartDegree;
    }

    public void setStartDegree(int startDegree) {
        this.mStartDegree = startDegree;
        invalidate();
    }

    public boolean isDrawBackgroundOutsideProgress() {
        return mDrawBackgroundOutsideProgress;
    }

    public void setDrawBackgroundOutsideProgress(boolean drawBackgroundOutsideProgress) {
        this.mDrawBackgroundOutsideProgress = drawBackgroundOutsideProgress;
        invalidate();
    }

    public interface ProgressFormatter {
        CharSequence format(int progress, int max);
    }

    private static final class DefaultProgressFormatter implements ProgressFormatter {
        private static final String DEFAULT_PATTERN = "%d%%";

        @Override
        public CharSequence format(int progress, int max) {
            return String.format(DEFAULT_PATTERN, (int) ((float) progress / (float) max * 100));
        }
    }

    private static final class SavedState extends BaseSavedState {
        int progress;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.progress = mProgressFirst;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setProgressFirst(ss.progress);
    }

}
