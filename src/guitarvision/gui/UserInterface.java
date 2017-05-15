package guitarvision.gui;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import guitarvision.Engine;
import guitarvision.PerformanceTest;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
		
		Slider sliderCanny = new Slider();
		sliderCanny.setMin(0);
		sliderCanny.setMax(1000);
		sliderCanny.valueProperty().addListener(new ChangeListener<Number>(){
			public void changed(ObservableValue<? extends Number> values, Number oldValue, Number newValue)
			{
				int sliderValue = (int) newValue.doubleValue();
				valueCanny.setText(Integer.toString(sliderValue));
				updateImageWithCannyUpper(sliderValue);
			}
		});

		Slider sliderHough = new Slider(0,1,0.5);
		sliderHough.setMin(0);
		sliderHough.setMax(1000);
		sliderHough.valueProperty().addListener(new ChangeListener<Number>(){
			public void changed(ObservableValue<? extends Number> values, Number oldValue, Number newValue)
			{
				int sliderValue = (int) newValue.doubleValue();
				valueHough.setText(Integer.toString(sliderValue));
				updateImageWithHoughUpper(sliderValue);
			}
		});
		
		CheckBox autoLevelsCheckBox = new javafx.scene.control.CheckBox();
		autoLevelsCheckBox.setText("Auto");
		autoLevelsCheckBox.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				boolean autoEnabled = autoLevelsCheckBox.isSelected();
				sliderCanny.setDisable(autoEnabled);
				sliderHough.setDisable(autoEnabled);
				updateImageWhetherAuto(autoEnabled);
			}
		});
		
		CheckBox checkBoxEdges = new javafx.scene.control.CheckBox();
		checkBoxEdges.setText("Show Edges");
		checkBoxEdges.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				updateImageWhetherEdges(checkBoxEdges.isSelected());
			}
		});
		
		ToggleGroup imageToggle = new ToggleGroup();
		
		RadioButton image1 = new RadioButton("Scene 1");
		image1.setToggleGroup(imageToggle);
		image1.setSelected(true);
		RadioButton image2 = new RadioButton("Scene 2");
		image2.setToggleGroup(imageToggle);
		RadioButton image3 = new RadioButton("Scene 3");
		image3.setToggleGroup(imageToggle);
		RadioButton image4 = new RadioButton("Scene Angle");
		image4.setToggleGroup(imageToggle);
		RadioButton image5 = new RadioButton("Scene Different Guitar");
		image5.setToggleGroup(imageToggle);
		RadioButton image6 = new RadioButton("Scene Poor Lighting");
		image6.setToggleGroup(imageToggle);
		RadioButton image7 = new RadioButton("Load Image");
		image7.setToggleGroup(imageToggle);
		
		//Image Picker
		FileChooser imagePicker = new FileChooser();
		imagePicker.setTitle("Open Guitar Video");
		imagePicker.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("JPG", "*.jpg"),
				new FileChooser.ExtensionFilter("PNG", "*.png")
				);

		imageToggle.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> value, Toggle prevTogle, Toggle newTogle)
			{
				if (imageToggle.getSelectedToggle() != null)
				{
					Toggle selectedToggle = imageToggle.getSelectedToggle();
					String filePath = "resources/images/guitar1.png";
					if(selectedToggle == image1)
					{
						filePath = "resources/images/guitar1.png";
					}
					else if(selectedToggle == image2)
					{
						filePath = "resources/images/guitar2.png";
					}
					else if(selectedToggle == image3)
					{
						filePath = "resources/images/guitar3.png";
					}
					else if(selectedToggle == image4)
					{
						filePath = "resources/images/guitar4.png";
					}
					else if(selectedToggle == image5)
					{
						filePath = "resources/images/guitar5.png";
					}
					else if(selectedToggle == image6)
					{
						filePath = "resources/images/guitar6.png";
					}
					else if(selectedToggle == image7)
					{
						File file = imagePicker.showOpenDialog(stage);

						if (file != null)
						{
							filePath = file.getPath();
						}
					}
					updateImageWithNewPath(filePath);
				}
			}
		});
		

		HBox horizontalLayout = new HBox();
		HBox horizontalLayout2 = new HBox();
		HBox horizontalLayout3 = new HBox();
		horizontalLayout.setAlignment(Pos.CENTER);
		horizontalLayout2.setAlignment(Pos.CENTER);
		horizontalLayout3.setAlignment(Pos.CENTER);
		
		horizontalLayout.getChildren().add(labelCanny);
		horizontalLayout.getChildren().add(sliderCanny);
		horizontalLayout.getChildren().add(valueCanny);
		
		horizontalLayout2.getChildren().add(labelHough);
		horizontalLayout2.getChildren().add(sliderHough);
		horizontalLayout2.getChildren().add(valueHough);
		//horizontalLayout2.getChildren().add(autoLevelsCheckBox);
		
		horizontalLayout3.getChildren().add(image1);
		horizontalLayout3.getChildren().add(image2);
		horizontalLayout3.getChildren().add(image3);
		horizontalLayout3.getChildren().add(image4);
		horizontalLayout3.getChildren().add(image5);
		horizontalLayout3.getChildren().add(image6);
		horizontalLayout3.getChildren().add(image7);
		
		verticalLayout.getChildren().add(buttonTest);
		verticalLayout.getChildren().add(checkBoxEdges);
		verticalLayout.getChildren().add(horizontalLayout);
		verticalLayout.getChildren().add(horizontalLayout2);
		verticalLayout.getChildren().add(horizontalLayout3);
		verticalLayout.getChildren().add(imageView);
		
		//Initialise the window
		sliderCanny.setValue(70);
		sliderHough.setValue(300);
		autoLevelsCheckBox.setSelected(false);
		
		updateImage();
		
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

		Label noteLabel = new Label();
		noteLabel.setText("Length of first note:");
		
		Label lightingLabel = new Label();
		lightingLabel.setText("Poor lighting:");
		
		CheckBox checkBoxLighting = new javafx.scene.control.CheckBox();
		
		String note1 = "Semibreve/Whole Note";
		String note2 = "Minim/Half Note";
		String note3 = "Crotchet/Quarter Note";
		String note4 = "Quaver/Eighth Note";
		String note5 = "Semiquaver/Sixteenth Note";
		
		
		
		ChoiceBox<String> noteLengthSelect = new ChoiceBox<String>(
			FXCollections.observableArrayList(
					note1,
					note2,
					note3,
					note4,
					note5
			)					
		);
		
		//Load Video Button
		Button buttonLoadVideo = new Button();
		buttonLoadVideo.setText("Process Video");
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
							String noteLength = noteLengthSelect.getValue();
							if (noteLength == null)
							{
								noteLength = "";
							}
							int noteTicks = 4;
							if (noteLength.equals(note1))
							{
								noteTicks = 16; 
							}
							else if (noteLength.equals(note2))
							{
								noteTicks = 8; 
							}
							else if (noteLength.equals(note3))
							{
								noteTicks = 4; 
							}
							else if (noteLength.equals(note4))
							{
								noteTicks = 2; 
							}
							else if (noteLength.equals(note5))
							{
								noteTicks = 1; 
							}
							Engine.getInstance().transcribeFromVideo(file, null, null, noteTicks, checkBoxLighting.isSelected(), true);
						}
					});
					
					processingThread.start();
				}
			}
		});

		HBox horizontalLayoutUser = new HBox();
		horizontalLayoutUser.setAlignment(Pos.CENTER);
		horizontalLayoutUser.setSpacing(5);
		horizontalLayoutUser.getChildren().add(noteLabel);
		horizontalLayoutUser.getChildren().add(noteLengthSelect);
		horizontalLayoutUser.getChildren().add(lightingLabel);
		horizontalLayoutUser.getChildren().add(checkBoxLighting);
		horizontalLayoutUser.getChildren().add(buttonLoadVideo);
		
		simpleRoot.getChildren().add(horizontalLayoutUser);
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
	
	boolean autoCanny = false;
	int cannyUpper = 0;
	boolean autoLevels = false;
	int houghUpper = 0;
	boolean showEdges = false;
	String imagePath = "resources/images/guitar1.png";
	
	public void updateImageWithCannyUpper(int val)
	{
		cannyUpper = val;
		updateImage();
	}
	
	public void updateImageWhetherAuto(boolean val)
	{
		autoLevels = val;
		updateImage();
	}
	
	public void updateImageWithHoughUpper(int val)
	{
		houghUpper = val;
		updateImage();
	}
	
	public void updateImageWhetherEdges(boolean val)
	{
		showEdges = val;
		updateImage();
	}
	
	public void updateImageWithNewPath(String val)
	{
		imagePath = val;
		updateImage();
	}
	
	public void updateImage()
	{		
		Mat image = Engine.getInstance().getProcessedImage(cannyUpper, houghUpper, showEdges, imagePath, autoLevels);
		
		MatOfByte imageBuffer = new MatOfByte();
		
		Imgcodecs.imencode(".png", image, imageBuffer);
		
		imageView.setImage(new Image(new ByteArrayInputStream(imageBuffer.toArray())));
	}
}
