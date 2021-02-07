package cameraparameters;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;


public class Camera1Parameters {
    //Camera 1 is a Team Microsoft Lifecam currently used for research
    //Create a new class like this for every camera calibrated for 3D vision

    //Use CvType.CV_64FC1 in the Mat constructor to match the type output by calibrateCamera
    public static Mat intrinsic = new Mat(new Size(3,3), CvType.CV_64FC1);
    public static Mat distortion = new Mat(new Size(1,5), CvType.CV_64FC1);    
    public static double hFOV = 61.3727248; //degrees
    public static double height;
    

    public static void setParameters(){
        //the camera's intrinsic calibration parameters printed and copied from the calibration program
        intrinsic.put(0, 0, 1139.7991135329983);
        intrinsic.put(0, 1, 0.0);
        intrinsic.put(0, 2, 659.8146169863334);
        intrinsic.put(1, 0, 0.0);
        intrinsic.put(1, 1, 1141.6840488922544);
        intrinsic.put(1, 2, 325.5946842533636);
        intrinsic.put(2, 0, 0.0);
        intrinsic.put(2, 1, 0.0);
        intrinsic.put(2, 2, 1.0);

        //the camera's distortion  parameters printed and copied from the calibration program
        distortion.put(0, 0, 0.12904742891185897);
        distortion.put(1, 0, -1.0455118434290487);
        distortion.put(2, 0, -0.006297485062493555);
        distortion.put(3, 0, 0.001478074246428523);
        distortion.put(4, 0, 1.9880640079548846);
    }

}