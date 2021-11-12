package org.firstinspires.ftc.teamcode.opmodes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class FreightFrenzyPipeline extends OpenCvPipeline {
    enum BarcodeLocation {
        LEFT,
        RIGHT,
        MIDDLE,
        NONE
    }

    private int width = 320; // width of the image
    BarcodeLocation location;
    Telemetry telemetry;

    public FreightFrenzyPipeline(int width, Telemetry telemetry) {
        this.width = width;
        this.telemetry = telemetry;
    }


    @Override
    public Mat processFrame(Mat input) {
        // "Mat" stands for matrix, which is basically the image that the detector will process
        // the input matrix is the image coming from the camera
        // the function will return a matrix to be drawn on your phone's screen

        // The detector detects regular stones. The camera fits two stones.
        // If it finds one regular stone then the other must be the skystone.
        // If both are regular stones, it returns NONE to tell the robot to keep looking

        // Make a working copy of the input matrix in HSV
        Mat mat = new Mat();
        Imgproc.cvtColor(input, mat, Imgproc.COLOR_RGB2HSV);

        // if something is wrong, we assume there's no skystone
        if (mat.empty()) {
            location = BarcodeLocation.NONE;
            return input;
        }

        // We create a HSV range for yellow to detect regular stones
        // NOTE: In OpenCV's implementation,
        // Hue values are half the real value

        //PURP LED Scalars
        /*Scalar lowHSV = new Scalar(0, 100, 90);
        Scalar highHSV = new Scalar(255, 180, 180); */

        Scalar lowHSV = new Scalar(120, 40, 60); // lower bound HSV for yellow
        Scalar highHSV = new Scalar(160, 255, 255); // higher bound HSV for yellow
        Mat thresh = new Mat();

        // We'll get a black and white image. The white regions represent the regular stones.
        // inRange(): thresh[i][j] = {255,255,255} if mat[i][i] is within the range
        Core.inRange(mat, lowHSV, highHSV, thresh);
        //Core.bitwise_not(thresh, thresh);

        // Use Canny Edge Detection to find edges
        // you might have to tune the thresholds for hysteresis
        Mat edges = new Mat();
        Imgproc.Canny(thresh, edges, 100, 300);

        // https://docs.opencv.org/3.4/da/d0c/tutorial_bounding_rects_circles.html
        // Oftentimes the edges are disconnected. findContours connects these edges.
        // We then find the bounding rectangles of those contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }

        // Iterate and check whether the bounding boxes
        // cover left and/or right side of the image
        double left_x = 0.33 * width;
        double right_x = 0.66 * width;
        boolean left = false; // true if regular stone found on the left side
        boolean right = false; // "" "" on the right side
        boolean middle = false; // "" "" on the right side
        for (int i = 0; i != boundRect.length; i++) {
            // draw red bounding rectangles on mat
            // the mat has been converted to HSV so we need to use HSV as well
            location = BarcodeLocation.NONE;
            if (boundRect[i].height >= 50 && boundRect[i].width >= 50) {
                Imgproc.rectangle(mat, boundRect[i], new Scalar(255, 0, 0));
                Imgproc.rectangle(input, boundRect[i], new Scalar(255, 0, 0));

                int pos = boundRect[i].x + (boundRect[i].width/2);
                if (pos > right_x) {
                    right = true;
                    location = BarcodeLocation.RIGHT;
                }
                else if (pos >= left_x && pos <= right_x) {
                    middle = true;
                    location = BarcodeLocation.MIDDLE;
                }
                else if (pos < left_x) {
                    left = true;
                    location = BarcodeLocation.LEFT;
                }
                else {
                    location = BarcodeLocation.NONE;
                }

                /*
                telemetry.addData("X value", boundRect[i].x);
                telemetry.addData("X width", boundRect[i].width);
                telemetry.addData("X height", boundRect[i].height);
                telemetry.addData("pos", pos);
                */
            }

        }
        /*
        telemetry.addData("Location", location);
        telemetry.update();
        */

        return thresh; // return the mat with rectangles drawn
    }

    public BarcodeLocation getLocation() {
        return this.location;
    }
}