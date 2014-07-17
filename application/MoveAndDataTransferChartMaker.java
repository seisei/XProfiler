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
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.util.Duration;

public class MoveAndDataTransferChartMaker extends Maker {
	
	final private Log log;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private List<XYChart.Series<Number,Number>> activitySeries;
    private List<XYChart.Series<Number,Number>> activitySeriesNotMove;
    private Map<Integer, Integer> yAxisMap; //MouseEvent of yAxis
    private Map<String, Map<String, String>> transferredVarMap;
    private Label label1;
    private Label label2;
    private Label routeLabel1;
    private Label routeLabel2;
    private Pane eventLabelPane;
    private Pane routePane;
    Node chart;
    CheckBox cb;
    VBox group;
    Pane entirePane;
    Pane chartPane;
    Pane labelPane;
    
    MoveAndDataTransferChartMaker(final Log log, Pane entirePane, Pane chartPane, Pane labelPane) {
		this.log = log;
		this.entirePane = entirePane;
		this.chartPane = chartPane;
		this.labelPane = labelPane;
		final long end = log.activity.get(0).endTime - log.activity.get(0).startTime;
        int ketaEnd = String.valueOf(end).length();
        int msdEnd = Integer.parseInt(String.valueOf(String.valueOf(end).charAt(0)));
		xAxis = new NumberAxis("Time (millis second)", 0, Math.pow(10, ketaEnd-1)*(msdEnd+1), (Math.pow(10, ketaEnd-1)*(msdEnd+1))/10);
		xAxis.setMinorTickCount(1);
		xAxis.setMinorTickLength(5.0);
		xAxis.setTickLabelFont(Font.font("", 15));
		
		yAxis= new NumberAxis("Place Number", -1, log.numPlaces, 1);
        yAxis.setMinorTickCount(1);
        yAxis.setTickLabelFont(Font.font("", 15));
        
        transferredVarMap = new HashMap<String, Map<String, String>>();
        setTransferredVarMap();
        
		activitySeries = new ArrayList<XYChart.Series<Number,Number>>();
		activitySeriesNotMove = new ArrayList<XYChart.Series<Number,Number>>();

		for (int i = 0, j = 0, k = 0; i < log.activity.size(); i++) {
			if (!(log.activity.get(i).move.isEmpty())) {
				XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
				XYChart.Data<Number, Number> data = new XYChart.Data<Number,Number>(log.activity.get(i).startTime,log.activity.get(i).generatedPlace);
				series.getData().add(data);
				activitySeries.add(series);
				activitySeries.get(j).setName(log.activity.get(i).activityNumber);
				j++;
			} else {
				XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
				XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(log.activity.get(i).startTime,log.activity.get(i).generatedPlace);
				series.getData().add(data);
				XYChart.Data<Number,Number> data2 = new XYChart.Data<Number,Number>(log.activity.get(i).endTime,log.activity.get(i).generatedPlace);
				series.getData().add(data2);
				activitySeriesNotMove.add(series);
				activitySeriesNotMove.get(k).setName(log.activity.get(i).activityNumber);
				k++;
			}
		}
	}
	
	
	public NumberAxis getxAxis() {
		return xAxis;
	}
	
	public List<XYChart.Series<Number,Number>> getActivityMoveSeries() {
		return activitySeries;
	}
	
	@Override
	protected Pane createChart() {
		group = new VBox();
		final LineChart<Number,Number> lc = new LineChart<Number,Number>(xAxis,yAxis);
        //lc.setCreateSymbols(false);
        lc.setAnimated(false);
        lc.setTitle("Movement and Data Transfer of Activities");
        lc.setPrefSize(Screen.getPrimary().getVisualBounds().getWidth()*2/3, Screen.getPrimary().getVisualBounds().getHeight()*2/3);
        //lc.setMaxSize(650, 300);
        
        final List<XYChart.Series<Number,Number>> seriesList = new ArrayList<XYChart.Series<Number,Number>>(activitySeries);
        seriesList.addAll(activitySeriesNotMove);
        Collections.sort(seriesList, new SeriesComparator());
        for (XYChart.Series<Number,Number> series : seriesList) {
        	lc.getData().add(series);
        	for (Activity activity : log.activity) {
        		if (series.getName().equals(activity.activityNumber)) setMouseEvent(series, activity);
        	}
        	for (XYChart.Data<Number, Number> data : series.getData()) {
        		data.getNode().setVisible(false);
        	}
        }

        
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
        
        cb = new CheckBox("Display only the activities that have moved");
        cb.setFont(Font.font("", 25));
        //cb.setLayoutX(1250);
        //cb.setLayoutY(500);
        cb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	if (cb.isSelected()) {
                	lc.getData().removeAll(activitySeriesNotMove);
                } else {
                	lc.getData().removeAll(activitySeries);
                	Collections.sort(seriesList, new SeriesComparator());
                	lc.getData().addAll(seriesList);
                }
            }
        });
        
        group.getChildren().add(lc);
        group.getChildren().add(cb);
        group.setAlignment(Pos.CENTER_RIGHT);

        return group;
    }
	@Override
	public void setAnimation(Timeline animation, final long start) {
		animation.getKeyFrames().add(new KeyFrame(Duration.millis(300), new EventHandler<ActionEvent>() {
        	int count = 0;
        	public void handle(ActionEvent actionEvent) {
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
			final XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			//lc.getData().add(series);
			
			if (!(log.activity.get(i).move.isEmpty()) || j < activitySeries.size()) {
				if (count < log.activity.get(i).move.size()) {
	    			if (count == 0) {
	    				XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).generatedPlace);
	    				activitySeries.get(j).getData().add(data);
	    				long size = log.activity.get(i).move.get(count).transferredDataSize;
	    				data.getNode().setScaleX((size/8)+1);
	    				data.getNode().setScaleY((size/8)+1);
	    				if (size > 0) data.getNode().setStyle("-fx-background-color: Black; -fx-border-color: Red;");
	    				XYChart.Data<Number, Number> data2 = new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).move.get(count).target);
	    				activitySeries.get(j).getData().add(data2);
	    				data.getNode().setVisible(true);
	    				data2.getNode().setVisible(false);
	    				setMouseEvent(data, log.activity.get(i), log.activity.get(i).move.get(count));
	    				setMouseEvent(activitySeries.get(j), log.activity.get(i));
	    			} else {
	    				XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).move.get(count-1).target);
	    				activitySeries.get(j).getData().add(data);
	    				data.getNode().setVisible(true);
	    				long size = log.activity.get(i).move.get(count).transferredDataSize;
	    				data.getNode().setScaleX((size/8)+1);
	    				data.getNode().setScaleY((size/8)+1);
	    				if (size > 0) data.getNode().setStyle("-fx-background-color: Black; -fx-border-color: Red;");
	    				XYChart.Data<Number, Number> data2 = new XYChart.Data<Number, Number>(log.activity.get(i).move.get(count).movedTime - start, log.activity.get(i).move.get(count).target);
	    				activitySeries.get(j).getData().add(data2);
	    				data2.getNode().setVisible(false);
	    				setMouseEvent(data, log.activity.get(i), log.activity.get(i).move.get(count));
	    			}
	    			
	    			if (count == log.activity.get(i).move.size()-1) {
	    				XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(log.activity.get(i).endTime - start, log.activity.get(i).move.get(count).target);
	    				activitySeries.get(j).getData().add(data);
	    				data.getNode().setVisible(false);
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
	
	public void setMouseEvent(final XYChart.Data<Number,Number> data, final Activity activity, final Move move) {
		data.getNode().setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
            	eventLabelPane = new HBox();
            	routePane = new HBox();
                label1 = LabelBuilder.create().font(Font.font("",FontWeight.BOLD, 24)).build();
                label1.setText(" Event: \n Activity Number: \n Target Place: \n Moved Time: \n Class: \n Method: \n Start Line: \n End Line: \n Transferred Data: ");
                label2 = LabelBuilder.create().font(Font.font("", 24)).build();
                String str = "movement by \"at\" \n"+activity.activityNumber + " \n" + move.target + " \n" + move.movedTime + " \n" + move.className + " \n" + move.methodName + " \n" + move.startPosition + " \n" + move.endPosition + " \n" + move.transferredDataSize + " bytes";
                StringBuilder routestr = new StringBuilder();
                for (TransferredData data : move.transferredData) {
                	if (transferredVarMap.get(activity.activityNumber) != null) {
                		if (transferredVarMap.get(activity.activityNumber).get(data.varName) != null) {
                        	routestr.append(data.varName + ": " + transferredVarMap.get(activity.activityNumber).get(data.varName));
                		}
                	}
                }
                label2.setText(str);
                routeLabel1 = LabelBuilder.create().font(Font.font("", FontWeight.BOLD, 24)).build();
                routeLabel1.setText("Data Transfer Route: ");
                routeLabel2 = LabelBuilder.create().font(Font.font("", 24)).build();
                routeLabel2.setText(routestr.toString());
                eventLabelPane.getChildren().add(label1);
                eventLabelPane.getChildren().add(label2);
                labelPane.getChildren().add(eventLabelPane);
                VBox.setMargin(eventLabelPane, new Insets(50, 0, 0, 0));
                
                routePane.getChildren().add(routeLabel1);
                routePane.getChildren().add(routeLabel2);
                entirePane.getChildren().add(routePane);
            }
        });
    	
    	data.getNode().setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                for (int i = labelPane.getChildren().size()-1; i >= 0; i--) {
                	if (labelPane.getChildren().get(i).toString().equals(eventLabelPane.toString())) {
                		labelPane.getChildren().remove(i);
                	}
                }
                
                for (int i = entirePane.getChildren().size()-1; i >= 0; i--) {
                	if (entirePane.getChildren().get(i).toString().equals(routePane.toString())) {
                		entirePane.getChildren().remove(i);
                	}
                }
            }
        });
	}
	
	private void setTransferredVarMap() {
		for (Activity activity : log.activity) {
			for (Move move : activity.move) {
				for (TransferredData data : move.transferredData) {
					if (transferredVarMap.get(activity.activityNumber) != null) {
						String str = transferredVarMap.get(activity.activityNumber).get(data.varName);
						str += " -> " + move.target;
						transferredVarMap.get(activity.activityNumber).put(data.varName, str);
					} else {
						transferredVarMap.put(activity.activityNumber, new HashMap<String, String>());
						transferredVarMap.get(activity.activityNumber).put(data.varName, activity.generatedPlace+" -> "+move.target);
					}
				}
			}
		}
	}
}

