import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.calib3d.*;
import java.io.File;

public class Calibration {
    public static boolean newCalibration = true; //set to true for a new calibration if there are new pictures or measurements
    //Use 6 in the Mat constructor to match the type output by calibrateCamera
    public static Mat intrinsic = new Mat(new Size(3,3), 6);
    public static Mat distortion = new Mat(new Size(1,5), 6);    

    public static void setParameters(){
        //the camera's intrinsic calibration parameters printed and copied from the calibration program
        intrinsic.put(0, 0, 1120.806152144427);
        intrinsic.put(0, 1, 0.0);
        intrinsic.put(0, 2, 656.0544969441477);
        intrinsic.put(1, 0, 0.0);
        intrinsic.put(1, 1, 1144.5788932018418);
        intrinsic.put(1, 2, 350.2481344026639);
        intrinsic.put(2, 0, 0.0);
        intrinsic.put(2, 1, 0.0);
        intrinsic.put(2, 2, 1.0);

        //the camera's distortion  parameters printed and copied from the calibration program
        distortion.put(0, 0, 0.6362044891388661);
        distortion.put(0, 1, 1.931599892274302);
        distortion.put(0, 2, -0.14417110402084296);
        distortion.put(0, 3, -0.04444195384927059);
        distortion.put(0, 4, -17.203133890759332);
    }

    public static void calibrate(){
        //constant variables
        double sideLength = 0.972; //inches
        Size boardSize = new Size(8,6);

        //empty variables
        MatOfPoint2f foundCorners = new MatOfPoint2f();
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

        //Assign all the files in the folder calibrationInput to an array of Files
        //calibrationInput is a folder on the Pi containing .jpg images of a chessboard pattern 
        File calibDir = new File("calibrationInput/");
        File[] files = calibDir.listFiles();
        //Look for chessboard corner points in every file in the folder
        for (File i : files){
            //Get the .jpg file as the Mat sourceImage
            //OpenCV's Mat format is a matrix, in this case a matrix of pixels with color values for each
            String path = i.getAbsolutePath();
            Mat sourceImg = Imgcodecs.imread(path);
            //Look for a chessboard pattern in the Mat and assign the corner points to the Mat of 2d points foundCorners
            //the method returns true if successful
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
        Mat realCornersTemplate = new Mat(new Size(foundCorners.width(), foundCorners.height()), 21);
        int index = 0;
        for (int y = 0; y < boardSize.height; y++){
            for (int x = 0; x < boardSize.width; x++){
                //The origin of the real world coordinate system can be arbitrarily picked as one of the corners and the x and y coordinates of all the corners set by measuring one chessboard square
                //The z coordinate can always be 0 because the pattern should be flat when the pictures are taken, so all the points are in the same plane
                realCornersTemplate.put(index, 0, new double[]{x*sideLength, y*sideLength, 0});
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
        double calibrationOutput = Calib3d.calibrateCamera(allRealCorners, allProjectedCorners, frameSize, intrinsic, distortion, rotation, translation);
        System.out.println("calibration output is " + calibrationOutput);

        //Print the intrinsic and distortion matrices so they can be defined as constants in other programs
        //Use the same double for loop method for going through the matrices as for setting up realCornersTemplate
        //Every data point in an OpenCV matrix is a double array, but in this case the arrays only contain one number
        System.out.println("intrinsic matrix size is " + intrinsic.size());
        for (int row = 0; row < intrinsic.rows(); row++){
            for (int col = 0; col < intrinsic.cols(); col++){
                System.out.println("Intrinsic matrix row " + row + ", column " + col + " is: " + intrinsic.get(row, col)[0]);
            }
        }
        System.out.println("distortion matrix size is " + distortion.size());
        for (int row = 0; row < distortion.rows(); row++){
            for (int col = 0; col < distortion.cols(); col++){
                System.out.println("Distortion matrix row " + row + ", column " + col + " is: " + distortion.get(row, col)[0]);
            }
        }
        System.out.println("realCornersTemplate type is " + realCornersTemplate.type());
    }  
}