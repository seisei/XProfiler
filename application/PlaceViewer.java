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
import javafx.scene.Group;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.Duration;

// The times that the activity exists at the target place
class Oetime {
	long startTime;
	long endTime;
	
	Oetime(){
		startTime = 0l;
		endTime = 0l;
	}
	
	Oetime(long start, long end) {
		this.startTime = start;
		this.endTime = end;
	}
}

public class PlaceViewer {
	private NumberAxis xAxis;
    private CategoryAxis yAxis;
    List<Activity> originalList; // The activities which were born in the target place
    Map<String, List<Oetime>> oetimeMap;
    List<Oetime> oetimeList; //originally existing time
    List<Activity> foreignList; // The activities which were not born in the target place, and which have moved to the target place
    private Map<String, List<Sync>> osyncMap; // original sync map
    private Map<String, List<Move>> omoveMap; // original move map
    private Map<String, List<Move>> oleaveMap; // original leave map
    private Map<String, List<Sync>> fsyncMap; // foreign sync map
    private Map<String, List<Move>> fmoveMap; // foreign move map
    private Map<String, List<Move>> fleaveMap; // foreign leave map
    private List<XYChart.Series<Number, String>> syncSeries;
    LineChart<Number, String> lc;
    
    PlaceViewer(Log log, int placeNumber) {
    	
    	fmoveMap = new HashMap<String, List<Move>>();
    	fleaveMap = new HashMap<String, List<Move>>();
    	originalList = new ArrayList<Activity>(); //specified place list
    	for (Activity act : log.activity) {
    		if (act.generatedPlace == placeNumber) {
    			originalList.add(act); //add if the generated place is the target place
    		} else {
    			foreignList = new ArrayList<Activity>();
    			List<Move> moveList = new ArrayList<Move>();
    			List<Move> leaveList = new ArrayList<Move>();
    			int count = 0;
    			boolean isAdded = false; //to distinguish whether there is a need to add the leave time of the activity
    			for (Move move : act.move) { //add if move contains the target place
    				if (isAdded) {
    					leaveList.add(move);
    					isAdded = false;
    				}
    				if (placeNumber == move.target) {
    					if (count == 0) {
    						foreignList.add(act);
        					moveList.add(move);
    					} else {
    						moveList.add(move);
    					}
    					isAdded = true;
    				}
    			}
    			fmoveMap.put(act.activityNumber, moveList);
    			fleaveMap.put(act.activityNumber, leaveList);
    		}
    	}
    	
    	oetimeMap = new HashMap<String, List<Oetime>>();
    	for (Activity act : originalList) {
    		oetimeList = new ArrayList<Oetime>();
    		boolean isMoved = false; // to detect the double movement
    		int i;
    		
    		Oetime oetime = new Oetime();
    		for (i = 0; i < act.move.size(); i++) {
    			if (isMoved) {
    				oetime = new Oetime();
    				if (i == 0) oetime.startTime = act.startTime; // first of all, add start time
    			}
        		 
    			if (act.move.get(i).target != placeNumber) {
    				if (isMoved) continue; // if isMoved is true, it means double movement happened
    				oetime.endTime = act.move.get(i).movedTime; // add the end time
    				isMoved = true; // activity moved!
    				oetimeList.add(oetime);
    			} else {
    				oetime.startTime = act.move.get(i).movedTime;
    				isMoved = false; // activity backed!
    				
    				if (i == act.move.size()-1) { // at last. last move event absolutely comes back to the original place(equals to the target place)
        				oetime.endTime = act.endTime;
        				oetimeList.add(oetime);
        			}
    			}
    		}
    		oetimeMap.put(act.activityNumber, oetimeList);
    		
    		if (i == 0 && act.move.isEmpty()) { // if there is no move event in this activity
    			oetimeList.add(new Oetime(act.startTime, act.endTime));
    		}
    	}
    	
    	
    	
    	fsyncMap = new HashMap<String, List<Sync>>();	
    	for (Activity act : foreignList) {
    		List<Sync> waitList = new ArrayList<Sync>();
    		
    		if (act.generatedPlace == placeNumber) {
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
    		} else { // if this activity is not created at the target place which the user wants to profile
    			List<Move> moveTmp = fmoveMap.get(act.activityNumber);
    			List<Move> leaveTmp = fleaveMap.get(act.activityNumber);
    			if (!(act.sync_wait.isEmpty())) {
    				for (Sync sync : act.sync_wait) {
    					for (int i = 0; i < moveTmp.size(); i++) {
    						if (sync.startTime >= moveTmp.get(i).movedTime && sync.endTime <= leaveTmp.get(i).movedTime) {
    							waitList.add(sync);
    						}
    					}
    				}
    			}
    			
    			if (!(act.sync_finish.isEmpty())) {
    				for (Sync sync : act.sync_finish) {
    					for (int i = 0; i < moveTmp.size(); i++) {
    						if (sync.startTime >= moveTmp.get(i).movedTime && sync.endTime <= leaveTmp.get(i).movedTime) {
    							waitList.add(sync);
    						}
    					}
    				}
    			}
    		}
    		
    		Collections.sort(waitList, new SyncComparator());
    		fsyncMap.put(act.activityNumber, waitList);
    	}
    	    	
		final long end = log.activity.get(0).endTime - log.activity.get(0).startTime;
        int ketaEnd = String.valueOf(end).length();
        int msdEnd = Integer.parseInt(String.valueOf(String.valueOf(end).charAt(0)));
		xAxis = new NumberAxis("Running Time", 0, Math.pow(10, ketaEnd-1)*(msdEnd+1), (Math.pow(10, ketaEnd-1)*(msdEnd+1))/10);
		xAxis.setMinorTickCount(1);
		xAxis.setMinorTickLength(5.0);
		xAxis.setTickLabelFont(Font.font("", 15));
		
		ObservableList<String> list = FXCollections.observableArrayList();
		for (Activity act : originalList) {
			list.add(act.activityNumber);
			//System.out.println(act.activityNumber);
		}
		for (Activity act : foreignList) {
			list.add(act.activityNumber);
		}
		yAxis= new CategoryAxis(list);
		yAxis.setLabel("Activity Number");
		yAxis.setTickLabelFont(Font.font("", 15));
		
		
		
	}
	
	public NumberAxis getxAxis() {
		return xAxis;
	}
	public void setxAxis(NumberAxis xAxis) {
		this.xAxis = xAxis;
	}
	
	public List<XYChart.Series<Number,String>> getActivitySyncSeries() {
		return syncSeries;
	}
	
	protected LineChart<Number, String> createMoveChart() {
    	lc = new LineChart<Number, String>(xAxis,yAxis);
        lc.setAnimated(false);
        lc.setTitle("Running Time of Activities");
        lc.setLegendVisible(false);
        lc.setMinSize(220, 220);
        
        lc.setLayoutX(500);
        lc.setLayoutY(500);
        
		for (Activity act : originalList) {
			XYChart.Series<Number,String> series = new XYChart.Series<Number,String>();
			series.getData().add(new XYChart.Data<Number,String>(-20, act.activityNumber));
			series.setName(act.activityNumber);
			lc.getData().add(series);
		}
        
        return lc;
    }
	
	
	public void setAnimation(Timeline animation, final Log log, final Pane group, final int placeNumber) {
		animation.getKeyFrames().add(new KeyFrame(Duration.millis(900), new EventHandler<ActionEvent>() {
			int count = 0;
            @Override public void handle(ActionEvent actionEvent) {
            	display(log, count, placeNumber);
            	count++;
            	
            }
        }));
		//System.out.println(originalList.size());
        animation.setCycleCount(getCycle(log));
	}
	
	private int getCycle(Log log) {
		if (originalList.size() >= foreignList.size()) {
			return originalList.size();
		} else {
			return foreignList.size();
		}
	}
	
	public void display(Log log, int count, int placeNumber) {

		if (!(originalList.isEmpty())) {
			Activity act;
			if (count < originalList.size()) {
				act = originalList.get(count);
				List<Oetime> oetime = oetimeMap.get(act.activityNumber);
				
				for (Oetime time : oetime) {
					final XYChart.Series<Number, String> tmp = new XYChart.Series<Number, String>();
					if (count == 0) {
						System.out.println(time.startTime + " " + time.endTime);
					}
					tmp.getData().add(new XYChart.Data<Number, String>(time.startTime, act.activityNumber));
					tmp.getData().add(new XYChart.Data<Number, String>(time.endTime, act.activityNumber));
					lc.getData().add(tmp);
				}
			}
		}
		
		if (!(foreignList.isEmpty())) {
			Activity act;
			if (count < foreignList.size()) {
				act = foreignList.get(count);
				List<Sync> syncList = new ArrayList<Sync>();
				List<Move> moveList = new ArrayList<Move>();
				List<Move> leaveList = new ArrayList<Move>();
				if (!(fsyncMap.get(act.activityNumber).isEmpty())) syncList = fsyncMap.get(act.activityNumber);
				if (!(fmoveMap.get(act.activityNumber).isEmpty())) moveList = fmoveMap.get(act.activityNumber);
				if (!(fleaveMap.get(act.activityNumber).isEmpty())) leaveList = fleaveMap.get(act.activityNumber);
			}
		}
		
		/*
		final XYChart.Series<Number, Number> tmp = new XYChart.Series<Number, Number>();
		
		if (act.generatedPlace == placeNumber) {
    		tmp.getData().add(new XYChart.Data<Number, Number>(act.startTime, act.generatedPlace));
    		
		} else {
			List<Move> listM = fmoveMap.get(act.activityNumber);
			List<Move> listL = fleaveMap.get(act.activityNumber);
			List<Sync> listS = fsyncMap.get(act.activityNumber);
			
			for (Move move : listM) {
				if () {
					
				}
			}
			tmp.getData().add(new XYChart.Data<Number, Number>(, act.generatedPlace));
			
		}
		*/
		
	}
}

