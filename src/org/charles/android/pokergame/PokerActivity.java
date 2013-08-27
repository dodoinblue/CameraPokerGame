
package org.charles.android.pokergame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PokerActivity extends Activity {

    private static final int NUMBER_OF_CARDS = 4;
    private Camera mCamera;
    private SurfaceView mLiveView;
    private SurfaceHolder mLiveViewHolder;
    private Button mDetect;
    private ImageView mPhoto;

    private String PHOTO_PATH = Environment.getExternalStorageDirectory() + "/poker.png";
    private String EDIT_PATH = Environment.getExternalStorageDirectory() + "/poker_edited.png";
    private boolean mLoaded = false;

    private Bitmap mBitmap;
    private Button mOperation;
    private int mHeight;
    private int mWidth;
    private State mState;
    private Mat image;
    private Canvas mCanvas;
    private Paint mPaint;

    private enum State {
        CAPTURE,
        DISPLAY,
        PROCESSED,
    }


    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            log("surfaceCreated");
            mLoaded = true;
            initLiveView();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            log("surfaceChanged");
            mHeight = mCamera.getParameters().getPreviewSize().height;
            mWidth = mCamera.getParameters().getPreviewSize().width;
            log("Surface size: " + mWidth + "x" + mHeight);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            log("surfaceDestroyed");
            mLoaded = false;
        }
    };
    private View.OnClickListener mOnViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view == mDetect) {
                onDetect();
            } else if (view == mOperation) {
                log("mOperation clicked");
                onProcessimage();
            }
        }
    };

    private void onProcessimage() {
        if(mState != State.DISPLAY) {
            log("Wrong state, returned");
            return;
        }
        image = new Mat();
        image = Highgui.imread(PHOTO_PATH);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(image, image, new Size(1, 1), 1000);
        Imgproc.threshold(image, image, 120, 255, Imgproc.THRESH_BINARY);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);
        Highgui.imwrite(EDIT_PATH, image);
        ArrayList<MatOfPoint> largestContours = findLargestContours(contours);
        drawContours(largestContours);

        for(MatOfPoint mop: largestContours) {
            rectifyCard(mop);
            // TODO: fix this.
            break;
        }
    }

    private void rectifyCard(MatOfPoint card) {
        MatOfPoint2f card2f = new MatOfPoint2f(card.toArray());
        double peri = Imgproc.arcLength(card2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(card2f,approx, 0.02*peri, true);
        // TODO: this is not the same input with example.
        RotatedRect rect = Imgproc.minAreaRect(card2f);

        Mat h= new Mat(Highgui.imread(PHOTO_PATH), new Rect(0,0,449, 449));
        Mat transform = Imgproc.getPerspectiveTransform(approx, h);
        Mat processedImg = new Mat();
        Imgproc.warpPerspective(Highgui.imread(PHOTO_PATH), processedImg, transform,
                new Size(450.0, 450.0));

        Highgui.imwrite(EDIT_PATH, processedImg);
        displayPhoto(EDIT_PATH);
    }

    private ArrayList<MatOfPoint> findLargestContours(ArrayList<MatOfPoint> contours) {
        ArrayList<SortableMatOfPoint> sortable = new ArrayList<SortableMatOfPoint>();
        ArrayList<MatOfPoint> result = new ArrayList<MatOfPoint>();
        for(MatOfPoint mop : contours) {
            sortable.add(new SortableMatOfPoint(mop));
        }
        Collections.sort(sortable, Collections.reverseOrder());
        //TODO: use comparator here, instead of a comparable class.
        for(int i=0; i< NUMBER_OF_CARDS; i++) {
            result.add(sortable.get(i).getmMOP());
            log("Area of MatOfPoint " + i + " :" + sortable.get(i).getArea());
        }
        return result;
    }

    private void drawContours(ArrayList<MatOfPoint> contours) {
        int i = 0;
        for(MatOfPoint m : contours) {
            i++;
            log("=== CONTOUR " + i + " ===");
            log("Area is " + Imgproc.contourArea(m));
            for(Point p : m.toList()) {
//                log("Point: (" + p.x + ", " + p.y + ")");
                mCanvas.drawPoint((float) p.x, (float) p.y, mPaint);
            }
//            if (i>10) break;
        }
        mPhoto.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
    }

    private void onDetect() {
        mLiveView.setVisibility(View.INVISIBLE);
        mPhoto.setVisibility(View.VISIBLE);
        displayPhoto(PHOTO_PATH);
        mState = State.DISPLAY;


        mBitmap = Bitmap.createBitmap(mPhoto.getWidth(), mPhoto.getHeight(),
                Bitmap.Config.RGB_565);

        mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
    }

    private void displayPhoto(String file) {
        Bitmap b = BitmapFactory.decodeFile(file);
        displayPhoto(b);
    }

    private void displayPhoto(Bitmap b) {
        mPhoto.setImageBitmap(b);
        log("mPhoto size: " + mPhoto.getWidth() + "x" + mPhoto.getHeight());
    }

    private void log(String s) {
        Log.i("Charles_TAG", s);
    }

    public void initLiveView() {
        if (mCamera == null) {
            log("initPreview called when mCamera is not available. (before onResume)");
            return;
        }
        requestParameters();
        try {
            mCamera.setPreviewDisplay(mLiveViewHolder);
        } catch (Exception e) {
            log(e.getMessage());
        }
        mCamera.startPreview();
    }

    public void requestParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(1920, 1080);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width * 100 / size.height  == 640 * 100 / 480 ) {
                log("size.width * 100 / size.height = " + size.width * 100 / size.height);
                log("640 * 100 / 480 = " + 640 * 100 / 480);
                log("Aspect Ratio match! Setting Preview size to : " + size.width + "x" + size
                        .height);
                parameters.setPreviewSize(size.width, size.height);
                break;
            }
        }
        mCamera.setParameters(parameters);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poker_activity);
        log("onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
                new OpenCVLoaderCallback(this));

        mLiveView = (SurfaceView) findViewById(R.id.live_view);
        mLiveViewHolder = mLiveView.getHolder();

        mDetect = (Button) findViewById(R.id.detect);
        mDetect.setOnClickListener(mOnViewClickListener);

        mPhoto = (ImageView) findViewById(R.id.photo);
        mPhoto.setOnClickListener(mOnViewClickListener);

        mOperation = (Button) findViewById(R.id.operation);
        mOperation.setOnClickListener(mOnViewClickListener);

        getWindow().getDecorView().setSystemUiVisibility(View
                .SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        mLiveViewHolder.addCallback(mSurfaceHolderCallback);
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        if (mLoaded) {
            log("starting preview");
            initLiveView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
        mLiveViewHolder.removeCallback(mSurfaceHolderCallback);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // TODO: reset activity.
            log("onKeyUp: BACK");
        }
        return super.onKeyUp(keyCode, event);
    }

    private class OpenCVLoaderCallback extends BaseLoaderCallback {
        Context mContext;
        public OpenCVLoaderCallback(Context AppContext) {
            super(AppContext);
            mContext = AppContext;
        }

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Toast.makeText(mContext, "LibLoaded", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.onManagerConnected(status);
                    Toast.makeText(mContext, "Lib NOT Load", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
