package com.example.paintapp.viewHolder.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class PaintView extends View {

    private Bitmap btnBackground, btnView;
    private Paint mPaint = new Paint();
    private Path mPath = new Path();
    private int colorBackground,sizeBrush,sizeEraser;
    private float mX,mY;
    private Canvas mCanvas;
    private final int DIFFERENCE_SPACE = 4;
    private ArrayList<Bitmap> listAction = new ArrayList<>();


    public PaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {

        sizeEraser = sizeBrush = 12;
        colorBackground = Color.WHITE;

        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(toPx(sizeBrush));

    }

    private Bitmap drawableToBitmap(Drawable drawable) {

        if(drawable instanceof BitmapDrawable){
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);

        drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        drawable.draw(c);

        return bitmap;

    }

    private float toPx(int sizeBrush) {
        return sizeBrush*(getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        btnBackground = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        btnView = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(btnView);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        canvas.drawColor(colorBackground);
        canvas.drawBitmap(btnBackground, 0, 0,  null);
        canvas.drawBitmap(btnView, 0, 0, null);
    }

    public void setBackgroundColor(int color) {
        colorBackground = color;
        invalidate();
    }

    public void setSizeBrush(int s){
        sizeBrush = s;
        mPaint.setStrokeWidth(toPx(sizeBrush));
    }

    public void setBrushColor(int color){
        mPaint.setColor(color);
    }

    public void setSizeEraser(int s){
        sizeEraser = s;
        mPaint.setStrokeWidth(toPx(sizeEraser));
    }

    public void enableEraser(){
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void disableEraser(){
        mPaint.setXfermode(null);
        mPaint.setShader(null);
        mPaint.setMaskFilter(null);
    }

    public void addLastAction(Bitmap bitmap){
        listAction.add(bitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                touchStart(x,y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x,y);
                break;
            case MotionEvent.ACTION_UP:
                    touchUp();
                    addLastAction(getBitmap());
                break;
        }

        return true;
    }

    private void touchUp() {
        mPath.reset();
    }

    private void touchMove(float x, float y) {

        float dx = Math.abs(x-mX);
        float dy = Math.abs(y-mY);

        if(dx >= DIFFERENCE_SPACE || dy >= DIFFERENCE_SPACE){

            mPath.quadTo(x,y, (x+mX)/2, (y+mY)/2);

            mY = y;
            mX = x;

            mCanvas.drawPath(mPath,mPaint);
            invalidate();
        }
    }

    private void touchStart(float x, float y) {
        mPath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    public Bitmap getBitmap(){

        this.setDrawingCacheEnabled(true);

        this.buildDrawingCache();

        Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());

        this.setDrawingCacheEnabled(false);

        return bitmap;
    }

    private boolean isCanvasCleared = false;
    private Bitmap lastClearedBitmap;

    public void clearAll() {
        if (!isCanvasCleared) {
            lastClearedBitmap = Bitmap.createBitmap(btnView);
            isCanvasCleared = true;
        }
        listAction.clear();
        btnView = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(btnView);
        invalidate();
    }

    public void returnLastAction() {
        if (isCanvasCleared) {
            btnView = Bitmap.createBitmap(lastClearedBitmap);
            mCanvas = new Canvas(btnView);
            listAction.add(btnView);
            isCanvasCleared = false;
            invalidate();
        } else if (!listAction.isEmpty()) {
            listAction.remove(listAction.size() - 1);
            if (!listAction.isEmpty()) {
                btnView = listAction.get(listAction.size() - 1);
            } else {
                btnView = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            }
            mCanvas = new Canvas(btnView);
            invalidate();
        }
    }


}
