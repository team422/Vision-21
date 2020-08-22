/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.*;
import java.io.File;
import org.opencv.core.Core;


public final class Main {
    //Variables
    private static String path;
    private static Mat sourceImg;
    private static double sideLength;
    private static Size boardSize;
    private static MatOfPoint2f foundCorners;
    private static List<Mat> allProjectedCorners;
    private static Point3[] realCornersArray;
    private static MatOfPoint3f realCornersTemplate;
    private static List<Mat> allRealCorners;
    private static Size frameSize;
    public static Mat intrinsic;
    public static Mat distortion;
    private static List<Mat> rotation;
    private static List<Mat> translation;
    private static double calibrationOutput;

  /**
   * Main.
   */
  public static void main(String... args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  
    
    //Initialize constant variables
    sideLength = 2.46; //centimeters
    boardSize = new Size(8,6);

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
      path = i.getAbsolutePath();
      sourceImg = Imgcodecs.imread(path);
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
    However, the matrix of 2d coordinates does not correspond to the rows/columns of the actual corners (e.g. 8x6), but is instead the total number of points by one (e.g. 48x1), so we must store the 3d points in the same way 
    So we use two for loops, the y loop keeping track of the rows and the x loop keeping track of the points within a row, and a index variable to keep track of the number of the points overall
    */ 
    int index = 0;
    for (int y = 0; y < boardSize.height; y++){
      for (int x = 0; x < boardSize.width; x++){
        //Create a point and add it to an array of points
        //The origin of the real world coordinate system can be arbitrarily picked as one of the corners and the x and y coordinates of all the corners set by measuring one chessboard square
        //The z coordinate can always be 0 because the pattern should be flat when the pictures are taken, so all the points are in the same plane
        Point3 rPoint = new Point3(x*sideLength, y*sideLength, 0);
        realCornersArray[index] = rPoint;
        index++;
      }
    }
    //Fill the matrix of 3d points realCornersTemplate with the points in the array
    realCornersTemplate.fromArray(realCornersArray);
    //Add as many copies of realCornersTemplate to the list of matrices allRealCorners as there are matrices of the chessboard corners in allProjectedCorners
    //The same template can be used every time because the real world points did not change while the pictures were taken
    for (int j = 0; j < allProjectedCorners.size(); j++){
      allRealCorners.add(j, realCornersTemplate);
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
    a double related to the accuracy of the calibration - smaller is more accurate, between 1 and 0 is good: calibrationOutput
    */
    calibrationOutput = Calib3d.calibrateCamera(allRealCorners, allProjectedCorners, frameSize, intrinsic, distortion, rotation, translation);
    System.out.println("calibration output is " + calibrationOutput);
  }

}