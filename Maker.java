package application;

import java.util.List;

import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public abstract class Maker {

	abstract protected Pane createChart();
	public List<XYChart.Series<Number,Number>> getActivityMoveSeries() { return null; }
	public List<XYChart.Series<Number,String>> getActivityWaitSeries() { return null; }
	public List<XYChart.Series<Number,String>> getActivityFinishSeries() { return null; }
	public List<XYChart.Series<Number,String>> getActivitySyncSeries() { return null; }
	public List<XYChart.Series<Number,String>> getWaitSeriesClone() { return null; }
	public List<XYChart.Series<Number,String>> getFinishSeriesClone() { return null; }
	abstract public void setAnimation(Timeline animation, final long start);
	abstract public void display(Log log, int count, long start);
}
