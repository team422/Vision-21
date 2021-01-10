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

}