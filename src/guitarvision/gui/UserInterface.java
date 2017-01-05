package guitarvision.gui;

import java.io.ByteArrayInputStream;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import guitarvision.Engine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class UserInterface extends Application{
	public static void launchInterface(String[] args){
		launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		stage.setTitle("GuitarVision Development");
		
		StackPane root = new StackPane();
		
		VBox verticalLayout = new VBox();
		
		root.getChildren().add(verticalLayout);
		
		//ImageView
		
		ImageView imageView = new ImageView();
		
		imageView.setFitHeight(500);
		
		imageView.setPreserveRatio(true);
		
		//Button
		
		Button button = new Button();
		
		button.setText("Display Original Image");
		
		button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				Mat image = Engine.getInstance().getOriginalImage();
				
				MatOfByte imageBuffer = new MatOfByte();
				
				Imgcodecs.imencode(".png", image, imageBuffer);
				
				imageView.setImage(new Image(new ByteArrayInputStream(imageBuffer.toArray())));
			}
		});
		
		//Slider
		
		Slider slider = new Slider(0,1,0.5);
		
		slider.setOnDragDetected(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				int argument = (int) (slider.getValue() * 1000);
				
				System.out.println(argument);
				
				Mat image = Engine.getInstance().getProcessedImage(argument);
				
				MatOfByte imageBuffer = new MatOfByte();
				
				Imgcodecs.imencode(".png", image, imageBuffer);
				
				imageView.setImage(new Image(new ByteArrayInputStream(imageBuffer.toArray())));
			}
		});
		
		verticalLayout.getChildren().add(button);
		verticalLayout.getChildren().add(slider);
		verticalLayout.getChildren().add(imageView);
		
		stage.setScene(new Scene(root, 1000, 600));
		stage.show();
		
		//Second window
		
		Stage userWindow = new Stage();
		
		StackPane userRoot = new StackPane();
		
		userWindow.setTitle("GuitarVision");
		userWindow.setScene(new Scene(userRoot, 400, 400));
		userWindow.show();
	}
}
