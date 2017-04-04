package guitarvision.detection;

import java.net.URL;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {
	public Mat getFaces(Mat image)
	{
		URL classifierData = getClass().getResource("haarcascade_frontalface_alt.xml");
		
		Mat result = image.clone();
		
		if (classifierData != null)
		{
			CascadeClassifier classifier = new CascadeClassifier(classifierData.getPath());

			MatOfRect faces = new MatOfRect();

			classifier.detectMultiScale(image, faces);

			for(Rect face: faces.toArray())
			{
				Imgproc.rectangle(image, face.tl(), face.br(), new Scalar(255,255,255));
			}
		}
		
		return result;
	}
}
