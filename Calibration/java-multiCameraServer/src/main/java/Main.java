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
    
    //Initialize Variables
    sideLength = 2.46; //centimeters
    boardSize = new Size(8,6);
    foundCorners = new MatOfPoint2f();
    allProjectedCorners = new ArrayList<Mat>();
    realCornersArray = new Point3[48];
    realCornersTemplate = new MatOfPoint3f();
    allRealCorners = new ArrayList<Mat>();
    frameSize = new Size();
    intrinsic = new Mat();
    distortion = new Mat();
    rotation = new ArrayList<Mat>();
    translation = new ArrayList<Mat>();


    //Assign all the files in the folder calibrationInput to an array of Files
    //calibrationInput is a folder on the Pi containing .jpg images of a chessboard pattern 
    File calibDir = new File("calibrationInput/");
    File[] files = calibDir.listFiles();
    
  
    for (File i : files){
      path = i.getAbsolutePath();
      sourceImg = Imgcodecs.imread(path);
      boolean success = Calib3d.findChessboardCorners(sourceImg, boardSize, foundCorners);
      if (success){
        allProjectedCorners.add(foundCorners);
        frameSize = sourceImg.size();
      }
      else{
        System.out.println("failed to find corners in " + path);
      }
    }
    System.out.println("Found " + allProjectedCorners.size() + " chessboard patterns in " + files.length + " files");
  
    int index = 0;
    for (int y = 0; y < boardSize.height; y++){
      for (int x = 0; x < boardSize.width; x++){
        Point3 rPoint = new Point3(x*sideLength, y*sideLength, 0);
        realCornersArray[index] = rPoint;
        index++;
      }
    }
    realCornersTemplate.fromArray(realCornersArray);
    System.out.println("realCornersTemplate size is " + realCornersTemplate.size());

    for (int j = 0; j < allProjectedCorners.size(); j++){
      allRealCorners.add(j, realCornersTemplate);
    }

    // if (allProjectedCorners.size() == allRealCorners.size()){
    //   System.out.println("same number of views: " + allRealCorners.size());
    // }
    // else {
    //   System.out.println("image points had " + allProjectedCorners.size() + " views and object points had " + allRealCorners.size() + " views");
    // }

    // if (allProjectedCorners.get(0).size() == allRealCorners.get(0).size()){
    //   System.out.println("same number of points per view: " + allRealCorners.get(1).size());
    // }
    // else {
    //   System.out.println("image points view had " + allProjectedCorners.get(0).size() + " points and object points view had " + allRealCorners.get(0).size() + " points");
    // }

    // System.out.println("final path is " + path);
    // //Print coordinates of corners found in the last image to determine what order the corners are in
    // for (int i = 0; i < foundCorners.rows(); i++){
    //   double[] point = foundCorners.get(i, 0);
    //   System.out.println("Corner " + i + "'s coordinates are: (" + point[0] + "," + point[1] + ")");
    // }
    


    calibrationOutput = Calib3d.calibrateCamera(allRealCorners, allProjectedCorners, frameSize, intrinsic, distortion, rotation, translation);
    System.out.println("calibration output is " + calibrationOutput);
  }

}