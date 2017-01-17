package guitarvision.gui;

import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import guitarvision.Engine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class UserInterface extends Application{
	private Slider slider;
	private Slider slider2;
	private CheckBox checkBox;
	
	private ImageView imageView;
	
	public static void launchInterface(String[] args){
		launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		////Development window
		stage.setTitle("GuitarVision Development");
		
		StackPane root = new StackPane();
		
		VBox verticalLayout = new VBox();
		
		root.getChildren().add(verticalLayout);
		
		//ImageView
		
		imageView = new ImageView();
		
		imageView.setFitHeight(500);
		
		imageView.setPreserveRatio(true);
		
		//Labels
		
		Label label1 = new Label();
		label1.setText("Canny Upper Threshold");
		Label label2 = new Label();
		label2.setText("Hough Threshold");
		
		Label value1 = new Label();
		value1.setText("");
		Label value2 = new Label();
		value2.setText("");
		
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
				int intValue = processSliderValue(slider.getValue());
				int intValue2 = processSliderValue(slider2.getValue());
				value1.setText(Integer.toString(intValue));
				updateTestingImage(intValue, intValue2, checkBox.isSelected());
			}
		});
		
		//Slider2
		slider2 = new Slider(0,1,0.5);
		
		slider2.setOnDragDetected(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				int intValue = processSliderValue(slider.getValue());
				int intValue2 = processSliderValue(slider2.getValue());
				value2.setText(Integer.toString(intValue2));
				updateTestingImage(intValue, intValue2, checkBox.isSelected());
			}
		});
		
		checkBox = new javafx.scene.control.CheckBox();
		
		checkBox.setText("Show Edges");
		
		checkBox.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				int intValue = processSliderValue(slider.getValue());
				int intValue2 = processSliderValue(slider2.getValue());
				updateTestingImage(intValue, intValue2, checkBox.isSelected());
			}
		});
		
		HBox horizontalLayout = new HBox();
		HBox horizontalLayout2 = new HBox();
		
		horizontalLayout.getChildren().add(label1);
		horizontalLayout.getChildren().add(slider);
		horizontalLayout.getChildren().add(value1);
		
		horizontalLayout2.getChildren().add(label2);
		horizontalLayout2.getChildren().add(slider2);
		horizontalLayout2.getChildren().add(value2);
		
		verticalLayout.getChildren().add(button);
		verticalLayout.getChildren().add(checkBox);
		verticalLayout.getChildren().add(horizontalLayout);
		verticalLayout.getChildren().add(horizontalLayout2);
		verticalLayout.getChildren().add(imageView);
		
		stage.setScene(new Scene(root, 1000, 600));
		stage.show();
		
		////Second window
		
		Stage userWindow = new Stage();
		
		StackPane userRoot = new StackPane();
		
		//Video Picker
		
		FileChooser filePicker = new FileChooser();
		filePicker.setTitle("Open Guitar Video");
		filePicker.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP4", "*.mp4"),
                new FileChooser.ExtensionFilter("M4V", "*.m4v")
        );
		
		//Load Video Button
		
		Button buttonLoadVid = new Button();
				
		buttonLoadVid.setText("Load Video");
		
		buttonLoadVid.setOnAction(new EventHandler<ActionEvent>()
				{
					@Override
					public void handle(ActionEvent event)
					{
						File file = filePicker.showOpenDialog(userWindow);
						
						if (file != null)
						{
							Engine.getInstance().loadVideo(file);
						}
					}
				});
		
		userRoot.getChildren().add(buttonLoadVid);
		
		userWindow.setTitle("GuitarVision");
		userWindow.setScene(new Scene(userRoot, 400, 400));
		userWindow.show();
	}
	
	public int processSliderValue(double value)
	{
		return (int) (value * 1000);
	}
	
	public void updateTestingImage(int argument, int argument2, boolean showEdges)
	{		
		Mat image = Engine.getInstance().getProcessedImage(argument, argument2, showEdges);
		
		MatOfByte imageBuffer = new MatOfByte();
		
		Imgcodecs.imencode(".png", image, imageBuffer);
		
		imageView.setImage(new Image(new ByteArrayInputStream(imageBuffer.toArray())));
	}
}
