package org.charles.android.pokergame;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Charles on 13-8-27.
 * TODO: This could be replace by a Comparator<T>
 */
public class SortableMatOfPoint implements Comparable<SortableMatOfPoint> {

    private MatOfPoint mMOP;

    SortableMatOfPoint(MatOfPoint m) {
        this.mMOP = m;
    }

    public double getArea() {
        return Imgproc.contourArea(mMOP);
    }

    public MatOfPoint getmMOP() {
        return mMOP;
    }

    @Override
    public int compareTo(SortableMatOfPoint sortableMatOfPoint) {
        return (int) (this.getArea() * 10) - (int) (sortableMatOfPoint.getArea() * 10);
    }
}
