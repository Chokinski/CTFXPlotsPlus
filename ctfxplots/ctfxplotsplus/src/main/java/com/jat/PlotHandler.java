package com.jat;

import java.time.Instant;
import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;

public class PlotHandler {
    private OHLCChart chart;
    
    @FXML
    private ScrollPane parent;
    public PlotHandler() {


    }

    public void showOHLCChart(ScrollPane parent, AnchorPane pane, boolean resizable, int pageSize, int numPoints) {
        // Create axes for the chart
        DateAxis xAxis = new DateAxis(); // Assuming you have a LocalDateAxis class
        CurrencyAxis yAxis = new CurrencyAxis();
        numPoints = 24;
        
        // Create an OHLCChart
        this.chart = new OHLCChart(xAxis, yAxis);
    
        // Generate initial data for the chart
        LocalDate initialLowerBoundDate = LocalDate.now().minusDays(numPoints); // Start from numPoints days ago
        loadChartData(initialLowerBoundDate, pageSize);
    
        // Add the chart to the pane, maintain its size
        displayChart(pane);
        // Bind the chart's size to the pane's size
        chart.prefWidthProperty().bind(pane.widthProperty());
        chart.prefHeightProperty().bind(pane.heightProperty());
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    
        // Add listeners to the chart's visible range
        xAxis.lowerBoundProperty().addListener((obs, oldVal, newVal) -> {
            loadChartData(newVal, pageSize);
        });
    
        xAxis.upperBoundProperty().addListener((obs, oldVal, newVal) -> {
            loadChartData(newVal, pageSize);
        });
    
        // Display the chart
        chart.addZoomListener();
        chart.addPanListener();
        setParent(parent);
        setResizable(resizable);
    }
    
    public void displayChart(AnchorPane pane) {
        if (!pane.getChildren().contains(chart)) {
            pane.getChildren().add(chart);
        }
    }
    
    private void loadChartData(LocalDate lowerBound, int pageSize) {
        // Calculate the start and end dates of the data to load
        LocalDate startDate = lowerBound.minusDays(pageSize / 2);
        LocalDate endDate = startDate.plusDays(pageSize);
    
        // Load the data
        chart.mockData(startDate, endDate);
    }


    public void setResizable(boolean resizable) {
        this.parent.setFitToHeight(resizable);
        this.parent.setFitToWidth(resizable);
    }

    public void setParent(ScrollPane parent) {
        this.parent = parent;
    }
    public OHLCChart getOHLCChart() {
        return this.chart;
    }
    
}
/*
 * public class Controller {
 * 
 * @FXML
 * private AnchorPane anchorForChart;
 * 
 * public void displayChart() {
 * // Create a PlotHandler
 * PlotHandler plotHandler = new PlotHandler();
 * 
 * // Create axes for the chart
 * NumberAxis xAxis = new NumberAxis();
 * NumberAxis yAxis = new NumberAxis();
 * 
 * // Create an OHLCChart
 * OHLCChart chart = new OHLCChart(xAxis, yAxis);
 * 
 * // Create data for the chart
 * ObservableList<XYChart.Data<Number, Number>> data =
 * FXCollections.observableArrayList();
 * // Add data to the list...
 * // For example:
 * data.add(new XYChart.Data<>(1, 100, new OHLCData(95, 110, 90, 100)));
 * data.add(new XYChart.Data<>(2, 105, new OHLCData(100, 110, 95, 105)));
 * // ...
 * 
 * // Add the data to the chart
 * chart.setData(FXCollections.observableArrayList(new XYChart.Series<>(data)));
 * 
 * // Display the chart in the anchorForChart pane
 * plotHandler.showOHLCChart(chart, anchorForChart);
 * }
 * }
 */
