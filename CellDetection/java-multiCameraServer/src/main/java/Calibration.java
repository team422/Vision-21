import org.opencv.core.*;

public class Calibration {
    public static Mat intrinsic = new Mat();
    public static Mat distortion = new Mat();    

    public static void Calibration(){
        //the camera's intrinsic calibration parameters printed and copied from the calibration program
        intrinsic.put(0, 0, 1120.8160561700065);
        intrinsic.put(0, 1, 0.0);
        intrinsic.put(0, 2, 656.054389942778);
        intrinsic.put(1, 0, 0.0);
        intrinsic.put(1, 1, 1144.5971588325647);
        intrinsic.put(1, 2, 350.24822705285294);
        intrinsic.put(2, 0, 0.0);
        intrinsic.put(2, 1, 0.0);
        intrinsic.put(2, 2, 1.0);

        //the camera's distortion  parameters printed and copied from the calibration program
        distortion.put(0, 0, 0.636234224324957);
        distortion.put(0, 1, 1.931669266711606);
        distortion.put(0, 2, -0.14417476629206144);
        distortion.put(0, 3, -0.0444445544087256);
        distortion.put(0, 4, -17.204290075349995);
    }
}