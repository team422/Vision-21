/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.CellEditorListener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;

import edu.wpi.first.networktables.*;

import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;
import edu.wpi.first.wpilibj.Relay.Value;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

import cameraparameters.Camera1Parameters;


/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  @SuppressWarnings("MemberName")
  public static class SwitchedCameraConfig {
    public String name;
    public String key;
  };

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();
  public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
  public static List<VideoSource> cameras = new ArrayList<>();
  
  //booleans for running vision threads
  public static boolean runGoalThread = false;
  public static boolean runCellThread = false;


  private Main() {
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
      parseError("camera '" + cam.name + "': could not read path");
      return false;
    }
    cam.path = pathElement.getAsString();

    // stream properties
    cam.streamConfig = config.get("stream");

    cam.config = config;

    cameraConfigs.add(cam);
    return true;
  }

  /**
   * Read single switched camera configuration.
   */
  public static boolean readSwitchedCameraConfig(JsonObject config) {
    SwitchedCameraConfig cam = new SwitchedCameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read switched camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement keyElement = config.get("key");
    if (keyElement == null) {
      parseError("switched camera '" + cam.name + "': could not read key");
      return false;
    }
    cam.key = keyElement.getAsString();

    switchedCameraConfigs.add(cam);
    return true;
  }

  /**
   * Read configuration file.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
      return false;
    }
    team = teamElement.getAsInt();

    // ntmode (optional)
    if (obj.has("ntmode")) {
      String str = obj.get("ntmode").getAsString();
      if ("client".equalsIgnoreCase(str)) {
        server = false;
      } else if ("server".equalsIgnoreCase(str)) {
        server = true;
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    if (obj.has("switched cameras")) {
      JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
      for (JsonElement camera : switchedCameras) {
        if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  //returns camera feed as VideoSource and streams feed to an MjpegServer
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }





  /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
    System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

    NetworkTableInstance.getDefault()
        .getEntry(config.key)
        .addListener(event -> {
              if (event.value.isDouble()) {
                int i = (int) event.value.getDouble();
                if (i >= 0 && i < cameras.size()) {
                  server.setSource(cameras.get(i));
                }
              } else if (event.value.isString()) {
                String str = event.value.getString();
                for (int i = 0; i < cameraConfigs.size(); i++) {
                  if (str.equals(cameraConfigs.get(i).name)) {
                    server.setSource(cameras.get(i));
                    break;
                  }
                }
              }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    return server;
  }



  /**
   * Main.
   */
  public static void main(String... args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }
 
    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    //Create/call the table visionTable and assign its entries to NetworkTableEntry variables
    NetworkTable table = ntinst.getTable("visionTable");
    NetworkTableEntry goalRunnerEntry = table.getEntry("GoalVisionRunner");
    NetworkTableEntry goalLateralTranslationEntry = table.getEntry("GoalVisionLateralTranslation");
    NetworkTableEntry goalLongitudinalTranslationEntry = table.getEntry("GoalVisionLongitudinalTranslation");
    NetworkTableEntry goalRotationEntry = table.getEntry("GoalVisionRotation");
    NetworkTableEntry cellRunnerEntry = table.getEntry("CellVisionRunner");
    NetworkTableEntry cellLateralTranslationEntry = table.getEntry("CellVisionLateralTranslation");
    NetworkTableEntry cellLongitudinalTranslationEntry = table.getEntry("CellVisionLongitudinalTranslation");
    NetworkTableEntry cellRotationEntry = table.getEntry("CellVisionRotation");


    // start cameras
    for (CameraConfig config : cameraConfigs) {
      cameras.add(startCamera(config));
    }

    // start switched cameras
    for (SwitchedCameraConfig config : switchedCameraConfigs) {
      startSwitchedCamera(config);
    }
    // start image processing on camera 0 if present
    if (cameras.size() >= 1) {
      
      //Add a new camera stream for displaying the contour on
      CameraServer inst = CameraServer.getInstance();
      CvSource goalDrawnVideo = inst.putVideo("Goal Vision Stream", 160, 120); //160 and 120 are the frame's width and height found in the FRCVision web dashboard under Vision Settings
      CvSource cellDrawnVideo = inst.putVideo("Powercell Vision Stream", 160, 120);
      
      //Use recorded calibration for camera 1
      Camera1Parameters.setParameters();
      //New calibration for camera 1:
      //Calibration.calibrate("calibration1Input/", 0.972, new Size(8,6), Camera1Parameters.intrinsic, Camera1Parameters.distortion);
      MatOfPoint3f worldGoalPoints = new MatOfPoint3f();
      worldGoalPoints.put(0, 0, new double[]{-19.630, 98.188, 0}); //top left
      worldGoalPoints.put(1, 0, new double[]{-9.815, 81.188, 0}); //bottom left
      worldGoalPoints.put(2, 0, new double[]{9.815, 81.188, 0}); //bottom right
      worldGoalPoints.put(3, 0, new double[]{19.630, 98.188, 0}); //top right

      VisionThread goalVisionThread = new VisionThread(cameras.get(0),
      new GoalPipeline(goalRunnerEntry), pipeline -> {
        if (goalRunnerEntry.getBoolean(false)) {
          System.out.println("GoalPipeline found " + GoalPipeline.convexHullsOutput.size() + " contours.");
          
          if(GoalPipeline.convexHullsOutput.size()>0){
            //Identify the largest contour by comparing each contour to the largest so far, with "largest so far" starting at -1
            double maxSize = -1;
            int maxSizeIndex = -1;
            for(int i = 0; i < GoalPipeline.convexHullsOutput.size(); i++ ) {
              if (Imgproc.contourArea(GoalPipeline.convexHullsOutput.get(i))> maxSize) {
                maxSize = Imgproc.contourArea(GoalPipeline.convexHullsOutput.get(i));
                maxSizeIndex = i;
              }
            }
            MatOfPoint largestContour = new MatOfPoint();
            GoalPipeline.convexHullsOutput.get(maxSizeIndex).copyTo(largestContour);

            //Approximate the convex hulls to a polygon with less vertices
            MatOfPoint2f approximationInput = new MatOfPoint2f();
            largestContour.convertTo(approximationInput, CvType.CV_32FC2);
            MatOfPoint2f polygonCorners = new MatOfPoint2f();
            Imgproc.approxPolyDP(approximationInput, polygonCorners, Imgproc.arcLength(approximationInput, true)/65, true);
            
            MatOfPoint2f reorderedCorners = new MatOfPoint2f();
            polygonCorners.copyTo(reorderedCorners);

            //Reorder the points so that they are always go top left, bottom left, bottom right, top right
            if(polygonCorners.size().height == 4.0){
              //Find average coordinates to compare each point to
              double avgX = (polygonCorners.get(0,0)[0] + polygonCorners.get(1,0)[0] + polygonCorners.get(2,0)[0] + polygonCorners.get(3,0)[0])/4;
              double avgY = (polygonCorners.get(0,0)[1] + polygonCorners.get(1,0)[1] + polygonCorners.get(2,0)[1] + polygonCorners.get(3,0)[1])/4;

              //compare each point to the average coordinate and assign it a position in reorderedCorners depending on how it compares.
              for (int i = 0; i < 4; i++) {
                if (polygonCorners.get(i,0)[0] <= avgX && polygonCorners.get(i,0)[1] <= avgY) { //top left
                  reorderedCorners.put(0, 0, polygonCorners.get(i, 0));
                }
                else if (polygonCorners.get(i,0)[0] <= avgX && polygonCorners.get(i,0)[1] >= avgY) { //bottom left
                    reorderedCorners.put(1, 0, polygonCorners.get(i, 0));
                }
                else if (polygonCorners.get(i, 0)[0] >= avgX && polygonCorners.get(i,0)[1] >= avgY) { //bottom right
                  reorderedCorners.put(2, 0, polygonCorners.get(i, 0));
                }
                else if (polygonCorners.get(i, 0)[0] >= avgX && polygonCorners.get(i,0)[1] <= avgY) { //top right
                  reorderedCorners.put(3, 0, polygonCorners.get(i, 0));
                }
                else {
                  System.out.println("something went wrong while ordering points");
                }
            }

            }
            else {
              System.out.println("found the wrong number of points");
            }

            //Draw the polygon corners 
            for (int i = 0; i < reorderedCorners.size().height; i++){
              Imgproc.circle(GoalPipeline.drawnExampleGoalImg, new Point(reorderedCorners.get(i, 0)[0],reorderedCorners.get(i, 0)[1]), 5, new Scalar((i+1)*63,(i+1)*63,(i+1)*63), -1);
              Imgproc.circle(GoalPipeline.drawnExampleGoalImg, new Point(reorderedCorners.get(i, 0)[0],reorderedCorners.get(i, 0)[1]), 5, new Scalar(0,0,255), 2);
            }
            
            List<Mat> translation = new ArrayList<Mat>();
            List<Mat> rotation = new ArrayList<Mat>();
            Calib3d.solvePnPGeneric(worldGoalPoints, reorderedCorners, Camera1Parameters.intrinsic, Camera1Parameters.distortion, rotation, translation);

            goalLateralTranslationEntry.forceSetDouble(translation.get(0).get(0, 0)[0]);
            goalLongitudinalTranslationEntry.forceSetDouble(translation.get(0).get(2, 0)[0]);
            goalRotationEntry.forceSetDouble(Calibration.rVecToHeading(rotation.get(0)));

            goalDrawnVideo.putFrame(GoalPipeline.drawnExampleGoalImg);
          }
        }
      });

      // MatOfPoint3f worldCellPoints = new MatOfPoint3f();
      // Mat worldCellPointsGeneric = new Mat(4,1,CvType.CV_32FC3);
      // worldCellPointsGeneric.copyTo(worldCellPoints);

      // worldCellPoints.put(0, 0, new double[]{0, 7, 0}); //top
      // worldCellPoints.put(1, 0, new double[]{3.5, 3.5, 0}); //right
      // worldCellPoints.put(2, 0, new double[]{0, 0, 0}); //bottom
      // worldCellPoints.put(3, 0, new double[]{-3.5, 3.5, 0}); //left

      VisionThread cellVisionThread = new VisionThread(cameras.get(0),
      new CellPipeline(cellRunnerEntry), pipeline -> {
        if (cellRunnerEntry.getBoolean(true)) {
          System.out.println("CellPipeline found " + CellPipeline.findContoursOutput.size() + " contours.");
          
          if(CellPipeline.findContoursOutput.size()>0){
            //Identify the largest contour by comparing each contour to the largest so far, with "largest so far" starting at -1
            double maxSize = -1;
            int maxSizeIndex = -1;
            for(int i = 0; i < CellPipeline.findContoursOutput.size(); i++ ) {
              if (Imgproc.contourArea(CellPipeline.findContoursOutput.get(i))> maxSize) {
                maxSize = Imgproc.contourArea(CellPipeline.findContoursOutput.get(i));
                maxSizeIndex = i;
              }
            }
            MatOfPoint largestContour = new MatOfPoint();
            CellPipeline.findContoursOutput.get(maxSizeIndex).copyTo(largestContour);

            Imgproc.drawContours(CellPipeline.drawnFrame, CellPipeline.findContoursOutput, maxSizeIndex, new Scalar(255,255,0));

            //Identify the center X coordinate of a rectangle drawn around the largest contour
            MatOfPoint2f circleInput = new MatOfPoint2f();
            largestContour.convertTo(circleInput, CvType.CV_32FC2);
            Point boundCircCenter = new Point();
            float[] boundCircRadius = new float[1];
            Imgproc.minEnclosingCircle(circleInput, boundCircCenter, boundCircRadius);
            Imgproc.circle(CellPipeline.drawnFrame, boundCircCenter, (int)boundCircRadius[0], new Scalar(255,255,255));
            
            double heading = ((boundCircCenter.x - (CellPipeline.drawnFrame.width() / 2)) / CellPipeline.drawnFrame.width()) * Camera1Parameters.hFOV;
            System.out.println("heading is " + heading + " degrees");
            
            //3D CALIBRATION THINGS
            // MatOfPoint2f orderedCirclePoints = new MatOfPoint2f();
            // Mat orderecCirclePointsGeneric = new Mat(4,1,CvType.CV_32FC2);
            // orderecCirclePointsGeneric.copyTo(orderedCirclePoints);
            // orderedCirclePoints.put(0, 0, new double[]{boundCircCenter.x, boundCircCenter.y - boundCircRadius[0]}); //the top of the ball
            // Imgproc.circle(CellPipeline.drawnFrame, new Point(orderedCirclePoints.get(0, 0)[0],orderedCirclePoints.get(0, 0)[1]), 3, new Scalar(0,0,255), -1);
            // orderedCirclePoints.put(1, 0, new double[]{boundCircCenter.x + boundCircRadius[0], boundCircCenter.y}); //the right side of the ball
            // orderedCirclePoints.put(2, 0, new double[]{boundCircCenter.x, boundCircCenter.y + boundCircRadius[0]}); //the bottom of the ball
            // orderedCirclePoints.put(3, 0, new double[]{boundCircCenter.x - boundCircRadius[0], boundCircCenter.y}); //the left side of the ball
            // for (int i = 0; i < orderedCirclePoints.size().height; i++){
            //   Imgproc.circle(CellPipeline.drawnFrame, new Point(orderedCirclePoints.get(i, 0)[0],orderedCirclePoints.get(i, 0)[1]), 3, new Scalar((i+1)*63,(i+1)*63,(i+1)*63), -1);
            //   Imgproc.circle(CellPipeline.drawnFrame, new Point(orderedCirclePoints.get(i, 0)[0],orderedCirclePoints.get(i, 0)[1]), 3, new Scalar(0,0,255), 1);
            // 

            // List<Mat> translation = new ArrayList<Mat>();
            // List<Mat> rotation = new ArrayList<Mat>();
            // Calib3d.solvePnPGeneric(worldCellPoints, orderedCirclePoints, Camera1Parameters.intrinsic, Camera1Parameters.distortion, rotation, translation);

            // System.out.print("lateral translation is " + translation.get(0).get(0, 0)[0]);
            // System.out.print("longitudinal translation is " + translation.get(0).get(2, 0)[0]);
            // System.out.println("rotation is " + Calibration.rVecToHeading(rotation.get(0)));

            // cellLateralTranslationEntry.forceSetDouble(translation.get(0).get(0, 0)[0]);
            // cellLongitudinalTranslationEntry.forceSetDouble(translation.get(0).get(2, 0)[0]);
            // cellRotationEntry.forceSetDouble(Calibration.rVecToHeading(rotation.get(0)));

            //CORRECTION THINGS
            // //Identify the center X coordinate of a rectangle drawn around the largest contour
            // Rect boundRect = Imgproc.boundingRect(largestContour);
            // double centerX = boundRect.x + (boundRect.width / 2);

            // //Find a correction value between -1 and 1 based on the center of the rectangle and the center of the frame
            // double correction = (centerX - (CellPipeline.drawnFrame.width()/2)) * (2/CellPipeline.drawnFrame.width());

            // correctionEntry.forceSetDouble(correction);

            /*
            Use OpenCV's drawContours method with parameters:
            An image in the Mat format to be drawn on: drawnFrame, the copy of the pipeline input
            The set of contours where the contour to draw is located: GoalPipeline.convexHullsOutput
            The index of the specific contour from the set to draw: maxSizeIndex, the index of the largest contour
            A scalar with a BGR color to draw in: (255,255,0) or bluish green
            A thickness to draw in: 2
            A line format: 8, which is recommended, so go with that
            */
            // Imgproc.drawContours(CellPipeline.drawnFrame, CellPipeline.findContoursOutput, maxSizeIndex, new Scalar(255,255,0), 2, 4);
          

            cellDrawnVideo.putFrame(CellPipeline.drawnFrame);
          }
        }
      });

      goalVisionThread.start();
      cellVisionThread.start();
      
    }
    // loop forever
    for (;;) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}