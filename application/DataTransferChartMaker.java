package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.util.Duration;

public class DataTransferChartMaker extends Maker {
	
	final private Log log;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private List<XYChart.Series<Number,Number>> activitySeries;
    private Map<Integer, Integer> yAxisMap; //MouseEvent of yAxis
    private Label label1;
    private Label label2;
    private Pane eventLabelPane;
    Node chart;
    VBox group;
    Pane labelPane;
    private long maxSize = 16;
    
	DataTransferChartMaker(final Log log, Pane labelPane) {
		this.log = log;
		this.labelPane = labelPane;
		final long end = log.activity.get(0).endTime - log.activity.get(0).startTime;
        int ketaEnd = String.valueOf(end).length();
        int msdEnd = Integer.parseInt(String.valueOf(String.valueOf(end).charAt(0)));
		xAxis = new NumberAxis("Time (millis second)", 0, Math.pow(10, ketaEnd-1)*(msdEnd+1), (Math.pow(10, ketaEnd-1)*(msdEnd+1))/10);
		xAxis.setMinorTickCount(1);
		xAxis.setMinorTickLength(5.0);
		xAxis.setTickLabelFont(Font.font("", 15));
		
		yAxis= new NumberAxis("Byte", -1, maxSize, 1);
        yAxis.setMinorTickCount(1);
        yAxis.setTickLabelFont(Font.font("", 15));

		activitySeries = new ArrayList<XYChart.Series<Number,Number>>();
		for (int i = 0, j = 0; i < log.activity.size(); i++) {
			if (!(log.activity.get(i).move.isEmpty())) {
				XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
				series.getData().add(new XYChart.Data<Number,Number>(log.activity.get(i).startTime, 0));
				activitySeries.add(series);
				activitySeries.get(j).setName(log.activity.get(i).activityNumber);
				j++;
			}
		}
	}
	
	public NumberAxis getxAxis() {
		return xAxis;
	}
	public void setxAxis(NumberAxis xAxis) {
		this.xAxis = xAxis;
	}
	
	public List<XYChart.Series<Number,Number>> getActivityMoveSeries() {
		return activitySeries;
	}
	
	@Override
	protected Pane createChart() {
		group = new VBox();
    	LineChart<Number,Number> lc = new LineChart<Number,Number>(xAxis,yAxis);
        lc.setCreateSymbols(false);
        lc.setAnimated(false);
        lc.setTitle("Data Transfer of Activities");
        lc.setPrefSize(Screen.getPrimary().getVisualBounds().getWidth()*2/3, Screen.getPrimary().getVisualBounds().getHeight()*2/3);
        //lc.setMaxSize(650, 300);
        
        for (XYChart.Series<Number,Number> series : activitySeries) {
        	lc.getData().add(series);
        }
        lc.setLayoutX(0);
        lc.setLayoutY(0);
        
        yAxisMap = new HashMap<Integer, Integer>();
        for (int i = (int) yAxis.getLowerBound(), j = (int) yAxis.getUpperBound(); i <= yAxis.getUpperBound() && j >= yAxis.getLowerBound(); i++, j--) {
        	yAxisMap.put(i, (int) lc.getLayoutY() + (int) ((lc.getMinHeight()-lc.getLayoutY())*j)/(int) yAxis.getUpperBound());
        }
        
        yAxis.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
            	for (int i = yAxisMap.size()-1; i >= 0; i--) {
            		if (me.getSceneY() <= yAxisMap.get(i)) {
            			PlaceViewer viewer = new PlaceViewer(log, i);
            			chart = viewer.createMoveChart();
            			//label1 = LabelBuilder.create().text("seisei" + i).id("seisei").font(Font.font("MS UI Gothic", 30)).layoutX(me.getSceneX()).layoutY(me.getSceneY()).build();
            			group.getChildren().add(chart);
            			Timeline animation = new Timeline();
            			viewer.setAnimation(animation, log, group, i);
            			animation.play();
            			break;
            		}
            	}
            }
        });
        
        lc.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
            	if (!(group.getChildren().isEmpty()) && chart != null) {
            		for (int i = group.getChildren().size()-1; i >= 0; i--) {
                		if (group.getChildren().get(i).toString().equals(chart.toString())) {
                     		group.getChildren().remove(i);
                     	}
                    }
            	}
            }
        });
        
        group.getChildren().add(lc);
        return group;
    }
	@Override
	public void setAnimation(Timeline animation, final long start) {
		animation.getKeyFrames().add(new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {
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
			if (!(act.move.isEmpty())) list.add(act.move.size());
		}
		Collections.sort(list);
		if (list.isEmpty()) {
			return 0;
		} else {
			return list.get(list.size()-1);
		}
	}
	
	public void display(Log log, int count, long start) {
		
		for (int i = 0, j = 0; i < log.activity.size(); i++) {
			if (!(log.activity.get(i).move.isEmpty()) || j < activitySeries.size()) {
				if (count < log.activity.get(i).move.size()) {
	    			if (count == 0) {
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, 0));
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).move.get(count).transferredDataSize));
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, 0));
	    				setMouseEvent(activitySeries.get(j), log.activity.get(i));
	    			} else {
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, 0));
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).move.get(count).transferredDataSize));
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, 0));
	    				setMouseEvent(activitySeries.get(j), log.activity.get(i));
	    			}
	    			
	    			if (count == log.activity.get(i).move.size()-1) {
	    				activitySeries.get(j).getData().add(new XYChart.Data<Number, Number>(log.activity.get(i).endTime - start, 0));
	    				setMouseEvent(activitySeries.get(j), log.activity.get(i));
	        		}
	    			j++;
	    		}
			}
		}	
	}
	
	public void setMouseEvent(final XYChart.Series<Number,Number> series, final Activity activity) {
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
                VBox.setMargin(eventLabelPane, new Insets(50,0,0,0));
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
