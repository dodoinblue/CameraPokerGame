
package org.charles.android.pokergame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokerActivity extends Activity {

    private static final int NUMBER_OF_CARDS = 1;
    private static final int CARD_WIDTH = 449;
    private static final int CARD_HEIGHT = 449;
    private Camera mCamera;
    private SurfaceView mLiveView;
    private SurfaceHolder mLiveViewHolder;
    private Button mDetect;
    private ImageView mPhoto;

    private String PHOTO_PATH = Environment.getExternalStorageDirectory() + "/poker.png";
    private String TRAINING_PATH = Environment.getExternalStorageDirectory() + "/10.jpg";
    private String EDIT_PATH = Environment.getExternalStorageDirectory() + "/poker_edited.png";
    private boolean mLoaded = false;

    private Bitmap mBitmap;
    private Button mOperation;
    private int mHeight;
    private int mWidth;
    private State mState;
    private Mat mImage;
    private Canvas mCanvas;
    private Paint mPaint;
    Mat mMidProduct;

    Mat diff_original;
    Mat diff_training;
    Mat diff_result;
    int display_seq = 0;

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            log("onPreviewFrame");
//            log("saving frame");
            log("Frame size: WxH" + mWidth + "x" + mHeight);

            try {
                final YuvImage image = new YuvImage(bytes, ImageFormat.NV21, mWidth, mHeight,
                        null);
                File file = new File(PHOTO_PATH);
                FileOutputStream stream = new FileOutputStream(file);
                image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 90, stream);
                stream.close();

                Bitmap picture = BitmapFactory.decodeFile(PHOTO_PATH);
                if (picture != null) {
                    if (picture.getWidth() > picture.getHeight()) {
                        log("Is landscape image true");
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(picture, 0, 0,
                                picture.getWidth(), picture.getHeight(), matrix, true);
                        log("rotatedBitmap size (WxH): " + rotatedBitmap.getWidth() + "x" +
                                rotatedBitmap.getHeight());
                        saveBitmap(rotatedBitmap);
                    }
                } else {
                    log("pic is null");
                }

                mLiveView.setVisibility(View.INVISIBLE);
                mPhoto.setVisibility(View.VISIBLE);
                displayPhoto(PHOTO_PATH);
                mState = State.DISPLAY;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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
//            log("Surface size: " + mWidth + "x" + mHeight);
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
                onIdentifyCards();
            } else if (view == mPhoto) {
                toggleDisplayContent();
            }
        }
    };

    private void toggleDisplayContent() {
        if(mState != State.PROCESSED) {
            return;
        }
        switch (display_seq) {
            case 0:
                // Display original card, grayed.
                displayMat(diff_original);
                display_seq ++;
                break;
            case 1:
                // Display training card, grayed.
                displayMat(diff_training);
                display_seq ++;
                break;
            case 2:
                // Display diff result, grayed.
                displayMat(diff_result);
                display_seq = 0;
                break;
        }
    }

    private void saveBitmap(Bitmap img) {
        File file = new File(PHOTO_PATH);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onIdentifyCards() {
        if(mState != State.DISPLAY) {
            log("Wrong state, returning");
            return;
        }

        // Read image
        mImage = Highgui.imread(PHOTO_PATH);

        ArrayList<Mat> cards = getGrayedRectifiedCards(mImage, 1);

        // Recognize cards
        Mat train = Highgui.imread(TRAINING_PATH);
        displayPhoto(TRAINING_PATH);
        ArrayList<Mat> training_cards = getGrayedRectifiedCards(train, 1);
//        displayMat(training_cards.get(0));

        Mat diff = imageDiff(cards.get(0), training_cards.get(0));
//        Highgui.imwrite(EDIT_PATH, diff);
//        displayPhoto(TRAINING_PATH);

        mState = State.PROCESSED;
    }

    private Mat imageDiff(Mat img1, Mat img2) {
        diff_original = new Mat();
        diff_training = new Mat();
        diff_result = new Mat();
        Imgproc.GaussianBlur(img1, diff_original, new Size (5, 5), 5);
        Imgproc.GaussianBlur(img2, diff_training, new Size (5, 5), 5);
        Mat diff = new Mat();
        Core.absdiff(diff_original, diff_training, diff);
        double threshold = Imgproc.threshold(diff, diff_result, 200, 255, Imgproc.THRESH_BINARY);
        return diff_result;
    }


    private ArrayList<Mat> getGrayedRectifiedCards(Mat mImage, final int numberOfCards) {
        // Preprocess
        mMidProduct = new Mat();
        Imgproc.cvtColor(mImage, mMidProduct, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mMidProduct, mMidProduct, new Size(1, 1), 1000);
        Imgproc.threshold(mMidProduct, mMidProduct, 120, 255, Imgproc.THRESH_BINARY);

        // Find contours
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mMidProduct, contours, new Mat(), Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);
        Highgui.imwrite(EDIT_PATH, mMidProduct);
        ArrayList<MatOfPoint> largestContours = findLargestContours(contours, numberOfCards);
        //        displayContours(largestContours);

        ArrayList<Mat> result = new ArrayList<Mat>();

        // Rectify each contour
        for(MatOfPoint contour: largestContours) {
            Mat rectifiedCard = rectifyCard(contour);
            result.add(rectifiedCard);
        }

        return result;
    }

    private Mat rectifyCard(MatOfPoint card) {
        MatOfPoint2f card2f = new MatOfPoint2f(card.toArray());
        double peri = Imgproc.arcLength(card2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(card2f,approx, 0.02*peri, true);
        Mat transform = getPerspectiveTransformation(loadPoints(approx.toArray()));
        Mat result = new Mat(CARD_WIDTH, CARD_HEIGHT, CvType.CV_8UC1);
        Imgproc.warpPerspective(mImage, result, transform, new Size(CARD_WIDTH, CARD_HEIGHT));

//        Highgui.imwrite(EDIT_PATH, result);
//        displayPhoto(EDIT_PATH);
        return result;
    }

    public ArrayList<Point> loadPoints(Point[] pts){
        ArrayList<Point> points = new ArrayList<Point>();

        for(int i = 0; i < pts.length; i++){
            points.add(new Point((float)pts[i].x, (float)pts[i].y));
        }

        return points;
    }

    private Mat getPerspectiveTransformation(ArrayList<Point> inputPoints) {
        Point[] canonicalPoints = new Point[4];
        canonicalPoints[0] = new Point(CARD_WIDTH, 0);
        canonicalPoints[1] = new Point(0, 0);
        canonicalPoints[2] = new Point(0, CARD_HEIGHT);
        canonicalPoints[3] = new Point(CARD_WIDTH, CARD_HEIGHT);

        MatOfPoint2f canonicalMarker = new MatOfPoint2f();
        canonicalMarker.fromArray(canonicalPoints);
        Point[] points = new Point[4];
        for (int i = 0; i < 4; i++) {
            points[i] = new Point(inputPoints.get(i).x, inputPoints.get(i).y);
        }
        MatOfPoint2f marker = new MatOfPoint2f(points);

        return Imgproc.getPerspectiveTransform(marker, canonicalMarker);
    }

    private ArrayList<MatOfPoint> findLargestContours(ArrayList<MatOfPoint> contours,
            int numberOfCards) {
        ArrayList<SortableMatOfPoint> sortable = new ArrayList<SortableMatOfPoint>();
        ArrayList<MatOfPoint> result = new ArrayList<MatOfPoint>();
        for(MatOfPoint mop : contours) {
            sortable.add(new SortableMatOfPoint(mop));
        }
        Collections.sort(sortable, Collections.reverseOrder());
        //TODO: use a comparator here, instead of a comparable class.
        for(int i=0; i< numberOfCards; i++) {
            result.add(sortable.get(i).getmMOP());
            log("Area of MatOfPoint " + i + ": " + sortable.get(i).getArea());
        }
        return result;
    }

    private void displayContours(ArrayList<MatOfPoint> contours) {
        for(MatOfPoint m : contours) {
            for (Point p : m.toList()) {
                mCanvas.drawPoint((float) p.x, (float) p.y, mPaint);
            }
        }
        mPhoto.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
    }

    private void displayPoints(ArrayList<Point> points) {
        for (Point p : points) {
            mCanvas.drawPoint((float) p.x, (float) p.y, mPaint);
            log("Drawing points: " + p.x + ", " + p.y);
        }
        mPhoto.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
    }

    private void onDetect() {
        mCamera.setOneShotPreviewCallback(mPreviewCallback);
    }

    private void displayPhoto(String file) {
        Bitmap b = BitmapFactory.decodeFile(file);
        displayPhoto(b);
    }

    private void displayPhoto(Bitmap b) {
        mPhoto.setImageBitmap(b);
        log("mPhoto size: " + mPhoto.getWidth() + "x" + mPhoto.getHeight());
    }

    private void displayMat(Mat m) {
        Highgui.imwrite(EDIT_PATH, m);
        displayPhoto(EDIT_PATH);
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
