import java.util.ArrayList;
import java.util.List;

import edu.wpi.first.vision.VisionPipeline;

import org.opencv.core.*;

import org.opencv.imgproc.*;
import org.opencv.imgcodecs.Imgcodecs;

import edu.wpi.first.networktables.NetworkTableEntry;

/**
* CellPipeline class.
*
* <p>An OpenCV pipeline generated by GRIP.
*
* @author GRIP
*/
public class CellPipeline implements VisionPipeline {

	private NetworkTableEntry runnerEntry;

	//Outputs
	private Mat hslThresholdOutput = new Mat();
	private Mat cvDilate0Output = new Mat();
	private Mat cvErodeOutput = new Mat();
	private Mat cvDilate1Output = new Mat();
	public static ArrayList<MatOfPoint> findContoursOutput = new ArrayList<MatOfPoint>();
	public static Mat drawnFrame = new Mat();

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	/**
	 * This is the primary method that runs the entire pipeline and updates the outputs.
	 */
	@Override	public void process(Mat source0) {
		if (!runnerEntry.getBoolean(true)){return;}
		source0.copyTo(drawnFrame);

		// Step HSL_Threshold0:
		Mat hslThresholdInput = source0;
		double[] hslThresholdHue = {17.0, 30.267375599254272};
		double[] hslThresholdSaturation = {190.0, 255.0};
		double[] hslThresholdLuminance = {0.0, 200.0};
		hslThreshold(hslThresholdInput, hslThresholdHue, hslThresholdSaturation, hslThresholdLuminance, hslThresholdOutput);

		// // Step CV_dilate0:
		// Mat cvDilate0Src = hslThresholdOutput;
		// Mat cvDilate0Kernel = new Mat();
		// Point cvDilate0Anchor = new Point(-1, -1);
		// double cvDilate0Iterations = 2.0;
		// int cvDilate0Bordertype = Core.BORDER_DEFAULT;
		// Scalar cvDilate0Bordervalue = new Scalar(-1);
		// cvDilate(cvDilate0Src, cvDilate0Kernel, cvDilate0Anchor, cvDilate0Iterations, cvDilate0Bordertype, cvDilate0Bordervalue, cvDilate0Output);

		// Step CV_erode0:
		Mat cvErodeSrc = hslThresholdOutput;
		Mat cvErodeKernel = new Mat();
		Point cvErodeAnchor = new Point(-1, -1);
		double cvErodeIterations = 1.0;
		int cvErodeBordertype = Core.BORDER_DEFAULT;
		Scalar cvErodeBordervalue = new Scalar(-1);
		cvErode(cvErodeSrc, cvErodeKernel, cvErodeAnchor, cvErodeIterations, cvErodeBordertype, cvErodeBordervalue, cvErodeOutput);

		// Step CV_dilate1:
		Mat cvDilate1Src = cvErodeOutput;
		Mat cvDilate1Kernel = new Mat();
		Point cvDilate1Anchor = new Point(-1, -1);
		double cvDilate1Iterations = 1.0;
		int cvDilate1Bordertype = Core.BORDER_CONSTANT;
		Scalar cvDilate1Bordervalue = new Scalar(-1);
		cvDilate(cvDilate1Src, cvDilate1Kernel, cvDilate1Anchor, cvDilate1Iterations, cvDilate1Bordertype, cvDilate1Bordervalue, cvDilate1Output);

		// Step Find_Contours0:
		Mat findContoursInput = cvDilate1Output;
		boolean findContoursExternalOnly = true;
		findContours(findContoursInput, findContoursExternalOnly, findContoursOutput);

	}

	/**
	 * This method is a generated getter for the output of a HSL_Threshold.
	 * @return Mat output from HSL_Threshold.
	 */
	public Mat hslThresholdOutput() {
		return hslThresholdOutput;
	}

	/**
	 * This method is a generated getter for the output of a CV_dilate.
	 * @return Mat output from CV_dilate.
	 */
	public Mat cvDilate0Output() {
		return cvDilate0Output;
	}

	/**
	 * This method is a generated getter for the output of a CV_erode.
	 * @return Mat output from CV_erode.
	 */
	public Mat cvErodeOutput() {
		return cvErodeOutput;
	}

	/**
	 * This method is a generated getter for the output of a CV_dilate.
	 * @return Mat output from CV_dilate.
	 */
	public Mat cvDilate1Output() {
		return cvDilate1Output;
	}

	/**
	 * This method is a generated getter for the output of a Find_Contours.
	 * @return ArrayList<MatOfPoint> output from Find_Contours.
	 */
	public ArrayList<MatOfPoint> findContoursOutput() {
		return findContoursOutput;
	}


	/**
	 * Segment an image based on hue, saturation, and luminance ranges.
	 *
	 * @param input The image on which to perform the HSL threshold.
	 * @param hue The min and max hue
	 * @param sat The min and max saturation
	 * @param lum The min and max luminance
	 * @param output The image in which to store the output.
	 */
	private void hslThreshold(Mat input, double[] hue, double[] sat, double[] lum,
		Mat out) {
		Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2HLS);
		Core.inRange(out, new Scalar(hue[0], lum[0], sat[0]),
			new Scalar(hue[1], lum[1], sat[1]), out);
	}

	/**
	 * Expands area of lower value in an image.
	 * @param src the Image to erode.
	 * @param kernel the kernel for erosion.
	 * @param anchor the center of the kernel.
	 * @param iterations the number of times to perform the erosion.
	 * @param borderType pixel extrapolation method.
	 * @param borderValue value to be used for a constant border.
	 * @param dst Output Image.
	 */
	private void cvErode(Mat src, Mat kernel, Point anchor, double iterations,
		int borderType, Scalar borderValue, Mat dst) {
		if (kernel == null) {
			kernel = new Mat();
		}
		if (anchor == null) {
			anchor = new Point(-1,-1);
		}
		if (borderValue == null) {
			borderValue = new Scalar(-1);
		}
		Imgproc.erode(src, dst, kernel, anchor, (int)iterations, borderType, borderValue);
	}

	/**
	 * Expands area of higher value in an image.
	 * @param src the Image to dilate.
	 * @param kernel the kernel for dilation.
	 * @param anchor the center of the kernel.
	 * @param iterations the number of times to perform the dilation.
	 * @param borderType pixel extrapolation method.
	 * @param borderValue value to be used for a constant border.
	 * @param dst Output Image.
	 */
	private void cvDilate(Mat src, Mat kernel, Point anchor, double iterations,
	int borderType, Scalar borderValue, Mat dst) {
		if (kernel == null) {
			kernel = new Mat();
		}
		if (anchor == null) {
			anchor = new Point(-1,-1);
		}
		if (borderValue == null){
			borderValue = new Scalar(-1);
		}
		Imgproc.dilate(src, dst, kernel, anchor, (int)iterations, borderType, borderValue);
	}

	/**
	 * Sets the values of pixels in a binary image to their distance to the nearest black pixel.
	 * @param input The image on which to perform the Distance Transform.
	 * @param type The Transform.
	 * @param maskSize the size of the mask.
	 * @param output The image in which to store the output.
	 */
	private void findContours(Mat input, boolean externalOnly,
		List<MatOfPoint> contours) {
		Mat hierarchy = new Mat();
		contours.clear();
		int mode;
		if (externalOnly) {
			mode = Imgproc.RETR_EXTERNAL;
		}
		else {
			mode = Imgproc.RETR_LIST;
		}
		int method = Imgproc.CHAIN_APPROX_SIMPLE;
		Imgproc.findContours(input, contours, hierarchy, mode, method);
	}

//create a reference to the Network Tables entry
public CellPipeline(NetworkTableEntry entryin) {
	super();
	runnerEntry = entryin;
}

}

