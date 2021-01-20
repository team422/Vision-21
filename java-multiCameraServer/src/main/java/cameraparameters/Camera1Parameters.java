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

    public Camera1Parameters(){
        //the camera's intrinsic calibration parameters printed and copied from the calibration program
        intrinsic.put(0, 0, 1139.799113533916);
        intrinsic.put(0, 1, 0.0);
        intrinsic.put(0, 2, 659.814616987545);
        intrinsic.put(1, 0, 0.0);
        intrinsic.put(1, 1, 1141.6840488928026);
        intrinsic.put(1, 2, 325.594684252985);
        intrinsic.put(2, 0, 0.0);
        intrinsic.put(2, 1, 0.0);
        intrinsic.put(2, 2, 1.0);

        //the camera's distortion  parameters printed and copied from the calibration program
        distortion.put(0, 0, 0.12904742889483867);
        distortion.put(1, 0, -1.045511843341881);
        distortion.put(2, 0, -0.006297485063064625);
        distortion.put(3, 0, 0.0014780742475828256);
        distortion.put(4, 0, 1.9880640078182654);
    }

}