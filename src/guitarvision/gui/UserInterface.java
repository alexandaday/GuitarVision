package guitarvision.gui;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import guitarvision.Engine;
import guitarvision.PerformanceTest;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert.AlertType;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class UserInterface extends Application{
	//Dialog box
	private Alert alert = new Alert(AlertType.INFORMATION);
	
	private Slider sliderCanny;
	private Slider sliderHough;
	private CheckBox checkBoxEdges;
	
	private ImageView imageView;
	
	public static void launchInterface(String[] args){
		launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		//****Testing window****
		
		stage.setTitle("GuitarVision Testing");
		
		StackPane root = new StackPane();
		root.setAlignment(Pos.TOP_CENTER);
		
		VBox verticalLayout = new VBox();
		verticalLayout.setAlignment(Pos.TOP_CENTER);
		
		root.getChildren().add(verticalLayout);
		
		imageView = new ImageView();
		imageView.setFitHeight(500);
		imageView.setPreserveRatio(true);
		
		Label labelCanny = new Label();
		labelCanny.setText("Canny Upper Threshold");
		Label labelHough = new Label();
		labelHough.setText("Hough Threshold");
		
		Label valueCanny = new Label();
		valueCanny.setText("");
		Label valueHough = new Label();
		valueHough.setText("");
		
		Button buttonTest = new Button();
		buttonTest.setText("Performance Test");
		buttonTest.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				Thread processingThread = new Thread(new Runnable() {
					public void run()
					{
						PerformanceTest performanceTest = new PerformanceTest();
						
						performanceTest.compareToManualTranscriptions();
					}
				});
				
				processingThread.start();
			}
		});
		
		sliderCanny = new Slider(0,1,0.5);
		sliderCanny.setOnDragDetected(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				int intValue = processSliderValue(sliderCanny.getValue());
				int intValue2 = processSliderValue(sliderHough.getValue());
				valueCanny.setText(Integer.toString(intValue));
				updateImageWithNewParameters(intValue, intValue2, checkBoxEdges.isSelected());
			}
		});
		
		sliderHough = new Slider(0,1,0.5);
		sliderHough.setOnDragDetected(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				int intValue = processSliderValue(sliderCanny.getValue());
				int intValue2 = processSliderValue(sliderHough.getValue());
				valueHough.setText(Integer.toString(intValue2));
				updateImageWithNewParameters(intValue, intValue2, checkBoxEdges.isSelected());
			}
		});
		
		checkBoxEdges = new javafx.scene.control.CheckBox();
		checkBoxEdges.setText("Show Edges");
		checkBoxEdges.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				int intValue = processSliderValue(sliderCanny.getValue());
				int intValue2 = processSliderValue(sliderHough.getValue());
				updateImageWithNewParameters(intValue, intValue2, checkBoxEdges.isSelected());
			}
		});

		HBox horizontalLayout = new HBox();
		HBox horizontalLayout2 = new HBox();
		horizontalLayout.setAlignment(Pos.CENTER);
		horizontalLayout2.setAlignment(Pos.CENTER);
		
		horizontalLayout.getChildren().add(labelCanny);
		horizontalLayout.getChildren().add(sliderCanny);
		horizontalLayout.getChildren().add(valueCanny);
		
		horizontalLayout2.getChildren().add(labelHough);
		horizontalLayout2.getChildren().add(sliderHough);
		horizontalLayout2.getChildren().add(valueHough);
		
		verticalLayout.getChildren().add(buttonTest);
		verticalLayout.getChildren().add(checkBoxEdges);
		verticalLayout.getChildren().add(horizontalLayout);
		verticalLayout.getChildren().add(horizontalLayout2);
		verticalLayout.getChildren().add(imageView);
		
		//Initialise the window
		double initialCannySliderValue = 0.072;
		double initialHoughSliderValue = 0.470;
		
		int intValue1 = processSliderValue(initialCannySliderValue);
		int intValue2 = processSliderValue(initialHoughSliderValue);
		
		valueCanny.setText(Integer.toString(intValue1));
		valueHough.setText(Integer.toString(intValue2));
		
		updateImageWithNewParameters(intValue1, intValue2, false);
		
		sliderCanny.setValue(initialCannySliderValue);
		sliderHough.setValue(initialHoughSliderValue);
		
		//Display the window
		stage.setScene(new Scene(root, 1000, 600));
		stage.show();
		
		//****Simple user window****
		
		Stage simpleWindow = new Stage();
		
		StackPane simpleRoot = new StackPane();
		
		//Video Picker
		FileChooser filePicker = new FileChooser();
		filePicker.setTitle("Open Guitar Video");
		filePicker.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP4", "*.mp4"),
                new FileChooser.ExtensionFilter("M4V", "*.m4v"),
                new FileChooser.ExtensionFilter("MOV", "*.mov")
        );
		
		//Load Video Button
		Button buttonLoadVideo = new Button();
		buttonLoadVideo.setText("Load Video");
		buttonLoadVideo.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				File file = filePicker.showOpenDialog(simpleWindow);

				if (file != null)
				{
					Thread processingThread = new Thread(new Runnable() {
						public void run()
						{
							Engine.getInstance().transcribeFromVideo(file, null, null, true);
						}
					});
					
					processingThread.start();
				}
			}
		});
		
		simpleRoot.getChildren().add(buttonLoadVideo);
		simpleRoot.setPadding(new Insets(50,50,50,50));
		
		simpleWindow.setTitle("GuitarVision");
		simpleWindow.setScene(new Scene(simpleRoot));
		simpleWindow.show();
	}
	
	public void displayDialogMessage(String text, String title)
	{
		if (title == null)
		{
			title = "Information";
		}
		
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(text);
		
		alert.showAndWait();
	}
	
	public int processSliderValue(double value)
	{
		//Scale the 0-1 slider value for use as Canny/Hough parameter values
		return (int) (value * 1000);
	}
	
	public void updateImageWithNewParameters(int argumentCanny, int argumentHough, boolean showEdges)
	{		
		Mat image = Engine.getInstance().getProcessedImage(argumentCanny, argumentHough, showEdges);
		
		MatOfByte imageBuffer = new MatOfByte();
		
		Imgcodecs.imencode(".png", image, imageBuffer);
		
		imageView.setImage(new Image(new ByteArrayInputStream(imageBuffer.toArray())));
	}
}
