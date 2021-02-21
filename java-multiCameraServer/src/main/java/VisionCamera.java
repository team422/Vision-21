import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.calib3d.*;
import java.io.File;

import java.lang.Math;

/**
* A class that handles position estimation with different vision cameras
*/
public class VisionCamera {

    //Camera's intrinsic characteristics
    public Mat intrinsic;
    public Mat distortion;
    public Size frameSize;
    public double horizontalAspectRatio;
    public double verticalAspectRatio;
    public double diagonalFOV; //in degrees
    public double horizontalFOV; //in degrees
    public double verticalFOV; //in degrees

    //Camera's physical position on robot
    public double height; //distance from floor in inches
    public double forwardOffset; //distance from center of robot in inches, positive forward, negative backward
    public double horizontalOffset; //distance from center of robot in inches, positive right, negative left
    public double verticalAngle; //angle from horizontal line to direction camera points in degrees
    public double horizontalAngle; //angle from direction robot faces to direction camera points in degrees

    //Names to handle sets of properties for one camera
    public static enum CameraName {
        HomeTesting
    }

    public VisionCamera(CameraName name) {
        if (name == CameraName.HomeTesting) {
            //Set the camera's intrinsic calibration matrix, printed and copied from the calibration program
            this.intrinsic = new Mat(new Size(3,3), CvType.CV_64FC1);
            this.intrinsic.put(0, 0, 1139.7991135329983);
            this.intrinsic.put(0, 1, 0.0);
            this.intrinsic.put(0, 2, 659.8146169863334);
            this.intrinsic.put(1, 0, 0.0);
            this.intrinsic.put(1, 1, 1141.6840488922544);
            this.intrinsic.put(1, 2, 325.5946842533636);
            this.intrinsic.put(2, 0, 0.0);
            this.intrinsic.put(2, 1, 0.0);
            this.intrinsic.put(2, 2, 1.0);

            //Set the camera's distortion calibration matrix, printed and copied from the calibration program
            this.distortion = new Mat(new Size(1,5), CvType.CV_64FC1);    
            this.distortion.put(0, 0, 0.12904742891185897);
            this.distortion.put(1, 0, -1.0455118434290487);
            this.distortion.put(2, 0, -0.006297485062493555);
            this.distortion.put(3, 0, 0.001478074246428523);
            this.distortion.put(4, 0, 1.9880640079548846);

            this.frameSize = new Size(160,120);
            this.horizontalFOV = 57.154314;
            this.verticalFOV = 42.8657355;
            System.out.println("horizontal FOV is " + horizontalFOV + " degrees.");
            System.out.println("vertical FOV is " + verticalFOV + " degrees.");

            //Fill in physical position here:
            this.height = 10.5;
            this.verticalAngle = 0;
        }
    }

    public void calibrate(String inputPath, double sideLengthInches, Size boardSize){

        //empty variables
        List<Mat> allProjectedCorners = new ArrayList<Mat>();
        List<Mat> allRealCorners = new ArrayList<Mat>();
        List<Mat> rotation = new ArrayList<Mat>();    
        List<Mat> translation = new ArrayList<Mat>();
        Size frameSize = new Size();  

        /*
        Detect corners of a chessboard pattern in a series of images
        For each image, assign the 2d coordinates of the corners of the detected pattern in the image  to a matrix
        Assign all the matrices to a list of matrices
        */

        //Assign all the files in the calibration input images folder to an array of Files 
        File calibDir = new File(inputPath);
        File[] files = calibDir.listFiles();
        //Look for chessboard corner points in every file in the folder
        for (File i : files){
            //Get the .jpg file as the Mat sourceImage
            //OpenCV's Mat datatype is a multi-channel matrix, in this case a matrix of pixels with RGB color values for each
            String path = i.getAbsolutePath();
            Mat sourceImg = Imgcodecs.imread(path);
            //Look for a chessboard pattern in the Mat and assign the corner points to the Mat of 2d points foundCorners
            //the method returns true if successful
            MatOfPoint2f foundCorners = new MatOfPoint2f();
            boolean success = Calib3d.findChessboardCorners(sourceImg, boardSize, foundCorners);
            if (success){
                //add foundCorners to the list of matrices allProjectedCorners and record the size of the image
                allProjectedCorners.add(foundCorners);
                frameSize = sourceImg.size();
            }
            else{
                System.out.println("failed to find corners in " + path);
                //print the path of the file without valid corners so it can be deleted
            }
        }
    
        /*
        Build a matrix of real-world 3d coordinates of chessboard corners to correspond to the matrix of 2d corner coordinates found in the camera projection
        Both sets of points must be in the same order
        The findChessboardCorners method normally orders the points the same way lines are read: 
        starting in the top left corner, going from left to right for each row, and starting at the left each new row
        However, the matrix of 2d coordinates does not correspond to the rows/columns of the actual corners (e.g. 8x6), but instead has a row for each corner (e.g. 1x48), so we must store the 3d points in the same way 
        So we use two for loops, the y loop keeping track of the rows and the x loop keeping track of the points within a row, and a index variable to keep track of the number of the points overall
        */
        Mat realCornersTemplate = new Mat(new Size(1, boardSize.width * boardSize.height), CvType.CV_32FC3);
        int index = 0;
        for (int y = 0; y < boardSize.height; y++){
            for (int x = 0; x < boardSize.width; x++){
                //The origin of the real world coordinate system can be arbitrarily picked as one of the corners and the x and y coordinates of all the corners set by measuring one chessboard square
                //The z coordinate can always be 0 because the pattern should be flat when the pictures are taken, so all the points are in the same plane
                realCornersTemplate.put(index, 0, new double[]{x*sideLengthInches, -y*sideLengthInches, 0});
                index++;
            }
        }
        //Add as many copies of realCornersTemplate to the list of matrices allRealCorners as there are matrices of the chessboard corners in allProjectedCorners
        //The same template can be used every time because the real world points did not change while the pictures were taken
        for (int i = 0; i < allProjectedCorners.size(); i++){
            allRealCorners.add(i, realCornersTemplate);
        }

        /*
        Call OpenCV's calibrateCamera method and print the output. The parts of the method are:
        Inputs:
        list of matrices of 3d chessboard corners coordinates in the world: allRealCorners
        list of matrices of 2d chessboard coordinates in the camera projection, found using findChessboardCorners: allProjectedCorners
        size of the images the 2d coordinates are from
        Outputs:
        matrix of camera properties that can be used to calculate how an image is projected from world coordinates to projected coordinates: intrinsic
        matrix of camera properties that can be used to remove distortion in images: distortion
        list of matrices of values representing the camera's rotation in relation to the world coordinates: rotation
        list of matrices of values representing the camera's translation in relation to the world coordinates: translation
        Return value:
        a double related to the accuracy of the calibration - smaller is more accurate, under 1 is good: calibrationOutput
        */
        double calibrationOutput = Calib3d.calibrateCamera(allRealCorners, allProjectedCorners, frameSize, this.intrinsic, this.distortion, rotation, translation);
        System.out.println("calibration output is " + calibrationOutput);

        //Print the intrinsic and distortion matrices so they can be defined as constants in other programs
        //Use the same double for loop method for going through the matrices as for setting up realCornersTemplate
        //Every data point in an OpenCV matrix is a double array, but in this case the arrays only contain one number
        System.out.println("intrinsic matrix size is " + this.intrinsic.size());
        for (int row = 0; row < this.intrinsic.rows(); row++){
            for (int col = 0; col < this.intrinsic.cols(); col++){
                System.out.println("Intrinsic matrix row " + row + ", column " + col + " is: " + this.intrinsic.get(row, col)[0]);
            }
        }
        
        System.out.println("distortion matrix size is " + this.distortion.size());
        for (int row = 0; row < this.distortion.rows(); row++){
            for (int col = 0; col < this.distortion.cols(); col++){
                System.out.println("Distortion matrix row " + row + ", column " + col + " is: " + this.distortion.get(row, col)[0]);
            }
        }

        //Print the extrinsic parameters for comparison to images
        for (int i = 0; i < translation.size(); i++){
            System.out.print("file # " + i + " is " + files[i].getAbsolutePath());
            System.out.print(" translation is x:" + translation.get(i).get(0,0)[0] + ", y:" + translation.get(i).get(1,0)[0] + ", z:" + translation.get(i).get(2,0)[0]);
            Mat rMatrix = new Mat();
            Calib3d.Rodrigues(rotation.get(i), rMatrix);
            double sy = Math.sqrt(Math.pow(rMatrix.get(0, 0)[0], 2) + Math.pow(rMatrix.get(1, 0)[0], 2));
            System.out.println(", heading is " + Math.toDegrees(Math.atan2(-rMatrix.get(2, 0)[0], sy)) + " degrees");
        }
    } 

    public double estimateCellHeading(Point circleCenter){
        return ((circleCenter.x - (this.frameSize.width / 2)) / this.frameSize.width) * this.horizontalFOV;
    }

    public double estimateCellDistance(Point circleCenter, double heading){
        double cellAngle = -((circleCenter.y - (this.frameSize.height / 2)) / this.frameSize.height) * this.verticalFOV;
        double longitudinalDistance = -(this.height - 3.5) * (1 / Math.cos(Math.toRadians(90 - (this.verticalAngle + cellAngle))));
        return Math.abs(longitudinalDistance * (1/Math.cos(Math.toRadians(heading)))); 
    }

    public static double rVecToHeading(Mat rVector) {
        Mat rMatrix = new Mat();
        Calib3d.Rodrigues(rVector, rMatrix);
        double sy = Math.sqrt(Math.pow(rMatrix.get(0, 0)[0], 2) + Math.pow(rMatrix.get(1, 0)[0], 2));
        return Math.toDegrees(Math.atan2(-rMatrix.get(2, 0)[0], sy));
    }


}