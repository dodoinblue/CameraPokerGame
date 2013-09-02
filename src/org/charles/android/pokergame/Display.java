package org.charles.android.pokergame;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.opencv.core.Mat;

import java.util.HashMap;

/**
 * Created by Charles on 13-8-31.
 */
public class Display extends View {

    HashMap<String, Mat> mToBeDisplayed = new HashMap<String, Mat>();

    public Display(Context context) {
        super(context);
        log("Display(Context context)");
    }

    public Display(Context context, AttributeSet attrs) {
        super(context, attrs);
        log("Display(Context context, AttributeSet attrs)");
    }

    public Display(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        log("Display(Context context, AttributeSet attrs, int defStyle)");
    }

    private void log(String s) {
        Log.i("Charles_TAG", "Display :: " + s);
    }
    private void init() {
        log("init");
        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("mDisplay is clicked.");
                toggleDisplayContent();
            }
        });

        this.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                loadImageFromFile();
                return true;
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        log("onFinishInflate");

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void toggleDisplayContent() {
    }

    public void loadImageFromFile() {

    }

    public void showImage(String originalImage) {

    }
}
