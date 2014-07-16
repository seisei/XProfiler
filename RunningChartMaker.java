package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class RunningChartMaker extends Maker {
	private Log log;
	private NumberAxis xAxis;
    private CategoryAxis yAxis;
    private Map<String, List<Sync>> syncMap;
    private List<XYChart.Series<Number,String>> syncSeries;
    private List<XYChart.Series<Number,String>> finishSeries;
    LineChart<Number,String> lc;
    int[] waitSize;
    int[] syncSize;
    private Label label1;
    private Label label2;
    private Pane eventLabelPane;
    VBox group;
    Pane labelPane;
    
    RunningChartMaker(Log log, Pane labelPane) {
    	this.log = log;
    	this.labelPane = labelPane;
    	syncMap = new HashMap<String, List<Sync>>();
    	
    	for (Activity act : log.activity) {
    		List<Sync> waitList = new ArrayList<Sync>();
    		if (!(act.sync_wait.isEmpty())) { //if not empty   			
    			for (Sync sync : act.sync_wait) {
    				waitList.add(sync);
    			}
    		}
    		
    		if (!(act.sync_finish.isEmpty())) {
    			for (Sync finish : act.sync_finish) {
    				waitList.add(finish);
    			}
    		}
    		Collections.sort(waitList, new SyncComparator());
    		syncMap.put(act.activityNumber, waitList);
    	}
    	    	
		final long end = log.activity.get(0).endTime - log.activity.get(0).startTime;
        int ketaEnd = String.valueOf(end).length();
        int msdEnd = Integer.parseInt(String.valueOf(String.valueOf(end).charAt(0)));
		xAxis = new NumberAxis("Running Time (millis second)", 0, Math.pow(10, ketaEnd-1)*(msdEnd+1), (Math.pow(10, ketaEnd-1)*(msdEnd+1))/10);
		xAxis.setMinorTickCount(1);
		xAxis.setMinorTickLength(5.0);
		xAxis.setTickLabelFont(Font.font("", 15));
		
		ObservableList<String> list = FXCollections.observableArrayList();
		for (Activity act : log.activity) {
			list.add(act.activityNumber);
		}
		yAxis= new CategoryAxis(list);
		yAxis.setLabel("Activity Number");
		yAxis.setTickLabelFont(Font.font("", 15));
		
		waitSize = new int[log.activity.size()];
		for (int i = 0; i < waitSize.length; i++) {
			if (log.activity.get(i).sync_wait != null) {
				waitSize[i] = log.activity.get(i).sync_wait.size();
			} else {
				waitSize[i] = 0;
			}
		}
		
		syncSize = new int[log.activity.size()];
		for (int i = 0; i < syncSize.length; i++) {
			if (log.activity.get(i).sync_finish != null) {
				syncSize[i] = log.activity.get(i).sync_finish.size();
			} else {
				syncSize[i] = 0;
			}
		}
		
		syncSeries = new ArrayList<XYChart.Series<Number,String>>();
		finishSeries = new ArrayList<XYChart.Series<Number,String>>();
		
		XYChart.Series<Number,String> series1;
		for (int i = 0; i < log.activity.size(); i++) {
			series1 = new XYChart.Series<Number,String>();
			//series1.getData().add(new XYChart.Data<Number,String>(0,log.activity.get(i).activityNumber));
			series1.setName(log.activity.get(i).activityNumber);
			syncSeries.add(series1);
			/*
			series2 = new XYChart.Series<Number,String>();
			series2.getData().add(new XYChart.Data<Number,String>(-20,log.activity.get(i).activityNumber));
			series2.setName(log.activity.get(i).activityNumber);
			finishSeries.add(series2);
			*/
		}
		//setFinishSeriesClone(new ArrayList<XYChart.Series<Number,String>>(finishSeries));
	}
	
	
	public NumberAxis getxAxis() {
		return xAxis;
	}
	public void setxAxis(NumberAxis xAxis) {
		this.xAxis = xAxis;
	}
	
	@Override
	protected Pane createChart() {
		group = new VBox();
    	lc = new LineChart<Number,String>(xAxis,yAxis);
        lc.setAnimated(false);
        lc.setTitle("Running Time of Activities");
        lc.setLegendVisible(false);
       
        lc.setMinSize(1200, 900);
        //lc.setMaxSize(600, 500);
        lc.getStyleClass().add("seisei");
        for (XYChart.Series<Number,String> series : syncSeries) {
        	//lc.getData().add(series);
        }
        
        for (XYChart.Series<Number,String> series : finishSeries) {
        	//lc.getData().add(series);
        }
        
        lc.setLayoutX(0);
        lc.setLayoutY(0);

        group.getChildren().add(lc);
        return group;
    }
	
	public void setAnimation(Timeline animation, final long start) {
		animation.getKeyFrames().add(new KeyFrame(Duration.millis(900), new EventHandler<ActionEvent>() {
        	int count = 0;
            @Override public void handle(ActionEvent actionEvent) {
            	display(log, count, start);
            	count++;
            }
        }));
        animation.setCycleCount(getCycle(log));
	}
	
	
	private int getCycle(Log log) {
		List<Integer> list = new ArrayList<Integer>();
		for (Activity act : log.activity) {
			if (!(syncMap.get(act.activityNumber).isEmpty())) list.add(syncMap.get(act.activityNumber).size());
		}
		Collections.sort(list);
		if (list.isEmpty()) {
			return 0;
		} else {
			return list.get(list.size()-1);
		}
	}
	
	public void display(Log log, int count, long start) {
		for (int i = 0; i < log.activity.size(); i++) {
			final XYChart.Series<Number,String> series = new XYChart.Series<Number,String>();
			lc.getData().add(series);
			series.getNode().setStyle("-fx-stroke-width: 6px; -fx-stroke:Crimson;");

			if (!(syncMap.get(log.activity.get(i).activityNumber).isEmpty())) {
				if (count == 0) {
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(log.activity.get(i).startTime, log.activity.get(i).activityNumber);
        			series.getData().add(data);
        			data.getNode().setVisible(false);
					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count).startTime, log.activity.get(i).activityNumber);
        			series.getData().add(data2);
        			data2.getNode().setVisible(false);
        			setMouseEvent(series, log.activity.get(i));        			
        			continue;
				}
				
				if (count < syncMap.get(log.activity.get(i).activityNumber).size()) { //before last cycle
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count-1).endTime, log.activity.get(i).activityNumber);
					series.getData().add(data);
					data.getNode().setVisible(false);
					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count).startTime, log.activity.get(i).activityNumber);
	            	series.getData().add(data2);
	            	data2.getNode().setVisible(false);
	            	setMouseEvent(series, log.activity.get(i));
				} else if (count == syncMap.get(log.activity.get(i).activityNumber).size()) { //last cycle
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count-1).endTime, log.activity.get(i).activityNumber);
					series.getData().add(data);
					data.getNode().setVisible(false);
					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(log.activity.get(i).endTime, log.activity.get(i).activityNumber);
	    			series.getData().add(data2);
	    			data2.getNode().setVisible(false);
	    			setMouseEvent(series, log.activity.get(i));
				} else {
					continue;
				}
			} else {
				if (count == 0) {
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(log.activity.get(i).startTime, log.activity.get(i).activityNumber);
					series.getData().add(data);
					data.getNode().setVisible(false);
					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(log.activity.get(i).endTime, log.activity.get(i).activityNumber);
        			series.getData().add(data2);
        			data2.getNode().setVisible(false);
        			setMouseEvent(series, log.activity.get(i));
				}
			}
		}
	}
	
	public void setMouseEvent(final XYChart.Series<Number,String> series, final Activity activity) {
		series.getNode().setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
            	eventLabelPane = new HBox();
                label1 = LabelBuilder.create().id(series.getName()).font(Font.font("",FontWeight.BOLD, 24)).build();
                label1.setText(" Activity Number: \n Generated Place: \n Start Time: \n End Time: \n Class: \n Method: \n Start Line: \n End Line:");
                label2 = LabelBuilder.create().id(series.getName()).font(Font.font("", 24)).build();
                label2.setText(activity.activityNumber + "\n" + activity.generatedPlace + "\n" + activity.startTime + "\n" + activity.endTime + "\n" + activity.generatedClass + "\n" + activity.generatedMethod + "\n" + activity.generatedPosition + "\n" + activity.endPosition);
                eventLabelPane.getChildren().add(label1);
                eventLabelPane.getChildren().add(label2);
                labelPane.getChildren().add(eventLabelPane);
                VBox.setMargin(eventLabelPane, new Insets(50, 0, 0, 0));
            }
        });
    	
    	series.getNode().setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                for (int i = labelPane.getChildren().size()-1; i >= 0; i--) {
                	if (labelPane.getChildren().get(i).toString().equals(eventLabelPane.toString())) {
                		labelPane.getChildren().remove(i);
                	}
                }
            }
        });
	}
}

