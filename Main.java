package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

/**
 * ﾂ� A chart that fills in the area between a line of data points and the axes.
 * ﾂ� Good for comparing accumulated totals over time. ﾂ� ﾂ� @see
 * javafx.scene.chart.Chart ﾂ� @see javafx.scene.chart.Axis ﾂ� @see
 * javafx.scene.chart.NumberAxis ﾂ� @related charts/line/LineChart ﾂ� @related
 * charts/scatter/ScatterChart ﾂ� */
public class Main extends Application {
	
    private Timeline[] animation;
    private Log log;
    private Label titleLabel;
    private Label moveLabel;
    private Label syncLabel;
    private Label runningLabel;
    private Label dataLabel;
    private Label runningSyncLabel;
    private Label moveDataLabel;
    private BorderPane root;
    private VBox chartPane;
    private HBox menuPane;
    private VBox entirePane;
    private Maker[] chart;
    private Pane currentChart;
    private int[] count;
    
    private Map<String, Integer> chartMap;

	private void init(Stage primaryStage) {
		FileChooser fc = new FileChooser();
		fc.setTitle("select file");
		File file = fc.showOpenDialog(primaryStage);
		
		if (file != null) {
			
			Gson gson = new Gson();
			JsonReader reader = null;
			try {
				reader = new JsonReader(new BufferedReader(new FileReader(file)));
			} catch (FileNotFoundException e) {
				System.exit(0);
			}
			
			log = gson.fromJson(reader, Log.class);
		} else {
			System.exit(0);
		}
		
		titleLabel = LabelBuilder.create().text(log.targetSource).font(Font.font("", 30)).prefWidth(Screen.getPrimary().getVisualBounds().getWidth()).alignment(Pos.CENTER).build();
		
		chartMap = new HashMap<String, Integer>();
		chartMap.put("move", 0);
		chartMap.put("sync", 1);
		chartMap.put("running", 2);
		chartMap.put("data", 3);
		chartMap.put("runningSync", 4);
		chartMap.put("moveData", 5);
		
		root = new BorderPane();
		root.setPrefSize(Screen.getPrimary().getVisualBounds().getWidth(), Screen.getPrimary().getVisualBounds().getHeight());
		chartPane = new VBox();
		menuPane = new HBox();
		entirePane = new VBox();
		entirePane.getChildren().add(menuPane);
		menuPane.getChildren().add(chartPane);
		chartPane.setPrefWidth(Screen.getPrimary().getVisualBounds().getWidth()*2/3);
		root.setTop(titleLabel);
		VBox labelPane = new VBox();
		chart = new Maker[6];
		chart[0] = new MoveChartMaker(log, labelPane);
		chart[1] = new SyncChartMaker(log, labelPane);
		chart[2] = new RunningChartMaker(log, labelPane);
		chart[3] = new DataTransferChartMaker(log, labelPane);
		chart[4] = new RunningAndSyncChartMaker(log, labelPane);
		chart[5] = new MoveAndDataTransferChartMaker(log, entirePane, chartPane, labelPane);
		
		count = new int[6];
				
		final long start = log.activity.get(0).startTime;
		
		moveLabel = LabelBuilder.create().id("move").text("Display movement chart").font(Font.font("", 25)).build();
		setMouseEvent(moveLabel, chart, start);
		syncLabel = LabelBuilder.create().id("sync").text("Display synchronization chart").font(Font.font("", 25)).build();
		setMouseEvent(syncLabel, chart, start);
		runningLabel = LabelBuilder.create().id("running").text("Display running time chart").font(Font.font("", 25)).build();
		setMouseEvent(runningLabel, chart, start);
		dataLabel = LabelBuilder.create().id("data").text("Display data transfer chart").font(Font.font("", 25)).build();
		setMouseEvent(dataLabel, chart, start);
		runningSyncLabel = LabelBuilder.create().id("runningSync").text("Display running and sync chart").font(Font.font("", 25)).build();
		setMouseEvent(runningSyncLabel, chart, start);
		moveDataLabel = LabelBuilder.create().id("moveData").text("Display movement and data transfer chart").font(Font.font("", 25)).build();
		setMouseEvent(moveDataLabel, chart, start);
		
		labelPane.setPrefSize(Screen.getPrimary().getVisualBounds().getWidth()/3, Screen.getPrimary().getVisualBounds().getHeight()/3);
        
		labelPane.getChildren().add(moveLabel);
		labelPane.getChildren().add(syncLabel);
		labelPane.getChildren().add(runningLabel);
		labelPane.getChildren().add(dataLabel);
		labelPane.getChildren().add(runningSyncLabel);
		labelPane.getChildren().add(moveDataLabel);
		menuPane.getChildren().add(labelPane);
		
		root.setRight(entirePane);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(this.getClass().getResource("application.css").toExternalForm());
        primaryStage.setScene(scene);
        List<Node> list = new ArrayList<Node>();
        //list.add(chart[0].createMoveChart(root));
        //list.add(chart[1].createSyncChart());
        //list.add(chart[2].createSyncChart());
        //list.add(chart[3].createMoveChart(root));
        
        animation = new Timeline[6];
        for (int i = 0; i < animation.length; i++) {
        	animation[i] = new Timeline();
        }
        
        //chart[0].setAnimation(animation[0], log, root, start);
        //chart[1].setAnimation(animation[1], log, root, start);
        //chart[2].setAnimation(animation[2], log, root, start);
        //chart[3].setAnimation(animation[3], log, root, start);
        
        
    }
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		init(primaryStage);
		primaryStage.setFullScreen(true);
		primaryStage.show();
		//play();
	}
	
	public void play() {
        animation[0].play();        
        animation[1].play();
        animation[2].play();
        animation[3].play();
    }
 
    @Override public void stop() {
        //animation.pause();
    }
    
    public void setMouseEvent(final Label label, final Maker[] chart, final long start) {
		label.setOnMousePressed(new EventHandler<MouseEvent>() {
			Pane createdChart;
            public void handle(MouseEvent me) {
            	if (currentChart != null) {
            		chartPane.getChildren().remove(currentChart);
            	}
            	if (count[chartMap.get(label.getId())]++ == 0) {
            		currentChart = createdChart = chart[chartMap.get(label.getId())].createChart();
                	chartPane.getChildren().add(createdChart);
                	chart[chartMap.get(label.getId())].setAnimation(animation[chartMap.get(label.getId())], start);
                	animation[chartMap.get(label.getId())].play();
            	} else {
            		chartPane.getChildren().add(createdChart);
            		currentChart = createdChart;
            	}
            	
            }
        });
    	
    	label.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                for (int i = chartPane.getChildren().size()-1; i >= 0; i--) {
                	if (chartPane.getChildren().get(i).toString().equals(label.toString())) {
                		//root.getChildren().remove(i);
                	}
                }
            }
        });
	}

	public static void main(String[] args) {
		launch(args);
	}
}

class TransferredData {
	String varName;
	String typeName;
	long size;
}

class Move {
	long movedTime;
	int target;
	long startPosition;
	long endPosition;
	String className;
	String methodName;
	long transferredDataSize;
	List<TransferredData> transferredData;
}

abstract class Sync {
	long startTime;
	long endTime;
	long startPosition;
	long endPosition;
	String className;
	String methodName;
}

class Finish extends Sync {
}

class Wait extends Sync {
}

class Activity {
	String activityNumber;
	int generatedPlace;
	long startTime;
	String generatedClass;
	String generatedMethod;
	long generatedPosition;
	long endPosition;
	List<Move> move;
	List<Finish> sync_finish;
	String syncHost;
	List<Wait> sync_wait;
	long endTime;
}

class Log {
	String targetSource;
	int numPlaces;
	List<Activity> activity;
}