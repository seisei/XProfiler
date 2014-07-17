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
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.util.Duration;

public class RunningAndSyncChartMaker extends Maker {
	private Log log;
	private NumberAxis xAxis;
    private CategoryAxis yAxis;
    private Map<String, List<Sync>> syncMap;
    LineChart<Number,String> lc;
    List<Activity> syncFinishList;
    List<XYChart.Series<Number,String>> syncRelationSeries;
    int[] waitSize;
    int[] syncSize;
    private Label label1;
    private Label label2;
    private Pane eventLabelPane;
    VBox group;
    Pane labelPane;
    CheckBox cb;
    
    RunningAndSyncChartMaker(Log log, Pane labelPane) {
    	this.log = log;
    	this.labelPane = labelPane;
    	syncMap = new HashMap<String, List<Sync>>();
    	syncFinishList = new ArrayList<Activity>();
    	
    	for (Activity act : log.activity) {
    		List<Sync> syncList = new ArrayList<Sync>();
    		if (!(act.sync_wait.isEmpty())) { //if not empty   			
    			for (Sync sync : act.sync_wait) {
    				syncList.add(sync);
    			}
    		}
    		
    		if (!(act.sync_finish.isEmpty())) {
    			for (Sync finish : act.sync_finish) {
    				syncList.add(finish);
    				syncFinishList.add(act);
    			}
    		}
    		Collections.sort(syncList, new SyncComparator());
    		syncMap.put(act.activityNumber, syncList);
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
        lc.setTitle("Running and Synchronization Time of Activities");
        lc.setLegendVisible(false);
       
        //lc.setMinSize(1200, 900);
        //lc.setMaxSize(600, 500);
        lc.setPrefSize(Screen.getPrimary().getVisualBounds().getWidth()*2/3, Screen.getPrimary().getVisualBounds().getHeight()*2/3);
        lc.getStyleClass().add("seisei");
        
        lc.getXAxis().layout();
        
        syncRelationSeries = new ArrayList<XYChart.Series<Number,String>>();
        for (Activity activity1 : syncFinishList) {
        	for (Activity activity2 : log.activity) {
        		if (activity2.syncHost != null && activity2.syncHost.equals(activity1.activityNumber)) {
        			for (Sync sync : activity1.sync_finish) {
        				if (activity2.startTime >= sync.startTime && activity2.startTime <= sync.endTime) {
        					XYChart.Series<Number,String> series = new XYChart.Series<Number,String>();
        					lc.getData().add(series);
        					syncRelationSeries.add(series);
        					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(sync.startTime, activity1.activityNumber);
        					series.getData().add(data);
        					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(activity2.startTime, activity2.activityNumber);
        					series.getData().add(data2);
        					data.getNode().setVisible(false);
        					data2.getNode().setVisible(false);
        					series.getNode().setStyle("-fx-stroke-width: 1px; -fx-stroke:Gray;");
        				}
        			}
        		}
        	}
        }
        lc.getData().removeAll(syncRelationSeries);
        
        cb = new CheckBox("Display the sync relation among activities");
        cb.setFont(Font.font("", 25));
        cb.setTextAlignment(TextAlignment.RIGHT);

        cb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (cb.isSelected()) {
                	for (XYChart.Series<Number,String> series : syncRelationSeries) {
                		lc.getData().add(series);
                		series.getNode().setStyle("-fx-stroke-width: 0.5px; -fx-stroke:Gray;");
                	}
                } else {
                	lc.getData().removeAll(syncRelationSeries);
                }
            }
        });

        group.getChildren().add(lc);
        group.getChildren().add(cb);
        group.setAlignment(Pos.TOP_RIGHT);
        return group;
    }
	@Override
	public void setAnimation(Timeline animation, final long start) {
		animation.getKeyFrames().add(new KeyFrame(Duration.millis(400), new EventHandler<ActionEvent>() {
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
		return list.get(list.size()-1)+1;
	}
	
	public void display(Log log, int count, long start) {
		for (int i = 0; i < log.activity.size(); i++) {
			final XYChart.Series<Number,String> series = new XYChart.Series<Number,String>();
			final XYChart.Series<Number,String> series2 = new XYChart.Series<Number,String>();
			
			lc.getData().add(series);
			lc.getData().add(series2);
			series.getNode().setStyle("-fx-stroke-width: 6px; -fx-stroke:Crimson;");
			series2.getNode().setStyle("-fx-stroke-width: 6px; -fx-stroke:Crimson;");
			series2.getNode().setOpacity(0.4);
			if (!(syncMap.get(log.activity.get(i).activityNumber).isEmpty())) {
				Sync currentSync;
				if (count == 0) {
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(log.activity.get(i).startTime, log.activity.get(i).activityNumber);
        			series.getData().add(data);
        			data.getNode().setVisible(false);
        			//data.getNode().setVisible(false);
        			XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count).startTime, log.activity.get(i).activityNumber);
        			series.getData().add(data2);
        			data2.getNode().setVisible(false);
        			//data3.getNode().setVisible(false);
        			setMouseEvent(series, group, log.activity.get(i));

        			Sync sync = syncMap.get(log.activity.get(i).activityNumber).get(count);
	            	series2.getData().add(new XYChart.Data<Number, String>(sync.startTime, log.activity.get(i).activityNumber));
        			series2.getData().add(new XYChart.Data<Number, String>(sync.endTime, log.activity.get(i).activityNumber));
        			if (sync instanceof Finish) {
        				setMouseEvent(series2, group, "finish", sync);
        			} else if (sync instanceof Wait) {
        				setMouseEvent(series2, group, "Clock.advanceAll()", sync);
        			}
        			
        			continue;
				}
				
				if (count < syncMap.get(log.activity.get(i).activityNumber).size()) { //before last cycle
	        		series.getData().add(new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count-1).endTime, log.activity.get(i).activityNumber));
	            	series.getData().add(new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count).startTime, log.activity.get(i).activityNumber));
	            	setMouseEvent(series, group, log.activity.get(i));
	            	
	            	Sync sync = syncMap.get(log.activity.get(i).activityNumber).get(count);
	            	series2.getData().add(new XYChart.Data<Number, String>(sync.startTime, log.activity.get(i).activityNumber));
        			series2.getData().add(new XYChart.Data<Number, String>(sync.endTime, log.activity.get(i).activityNumber));
        			if (sync instanceof Finish) {
        				setMouseEvent(series2, group, "finish", sync);
        			} else if (sync instanceof Wait) {
        				setMouseEvent(series2, group, "Clock.advanceAll()", sync);
        			}
	            	
				} else if (count == syncMap.get(log.activity.get(i).activityNumber).size()) { //last cycle
					series.getData().add(new XYChart.Data<Number, String>(syncMap.get(log.activity.get(i).activityNumber).get(count-1).endTime, log.activity.get(i).activityNumber));
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(log.activity.get(i).endTime, log.activity.get(i).activityNumber);
					series.getData().add(data);
	    			data.getNode().setVisible(false);
	    			setMouseEvent(series, group, log.activity.get(i));
				} else {
					continue;
				}
			} else {
				if (count == 0) {
					XYChart.Data<Number, String> data = new XYChart.Data<Number, String>(log.activity.get(i).startTime, log.activity.get(i).activityNumber);
					series.getData().add(data);
					XYChart.Data<Number, String> data2 = new XYChart.Data<Number, String>(log.activity.get(i).endTime, log.activity.get(i).activityNumber);
        			series.getData().add(data2);
        			data.getNode().setVisible(false);
        			data2.getNode().setVisible(false);
        			setMouseEvent(series, group, log.activity.get(i));
				}
			}
			if (log.activity.get(i).syncHost != null) {
				for (XYChart.Data<Number, String> data : series.getData()) {
					data.getNode().setStyle("-fx-background-color: Pink;");
				}
			}
		}
	}
	
	public void setMouseEvent(final XYChart.Series<Number,String> series, final VBox root, final Activity activity) {
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
	
	private void setMouseEvent(final XYChart.Series<Number,String> series, final VBox root, final String event, final Sync sync) {
		series.getNode().setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
            	eventLabelPane = new HBox();
            	label1 = LabelBuilder.create().id(series.getName()).font(Font.font("",FontWeight.BOLD, 24)).build();
            	label1.setText(" Event: \n Start Time: \n End Time: \n Class: \n Method: \n Start Line: \n End Line:");
            	label2 = LabelBuilder.create().id(series.getName()).font(Font.font("", 24)).build();
            	label2.setText("synchronization by \""+event + "\" \n" + sync.startTime + "\n" + sync.endTime + "\n" + sync.className + "\n" + sync.methodName + "\n" + sync.startPosition + "\n" + sync.endPosition);
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