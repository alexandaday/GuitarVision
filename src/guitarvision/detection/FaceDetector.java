package guitarvision.detection;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {
	public Mat getFaces(Mat image)
	{
		CascadeClassifier classifier = new CascadeClassifier(getClass().getResource("haarcascade_frontalface_alt.xml").getPath());
		
		MatOfRect faces = new MatOfRect();
		
		classifier.detectMultiScale(image, faces);
		
		Mat result = image.clone();
		
		for(Rect face: faces.toArray())
		{
			Imgproc.rectangle(image, face.tl(), face.br(), new Scalar(255,255,255));
		}
		
		return result;
	}
}
