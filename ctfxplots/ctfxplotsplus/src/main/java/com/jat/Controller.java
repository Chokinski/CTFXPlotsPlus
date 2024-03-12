package com.jat;

import java.util.Date;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Controller {

    @FXML
    private Font x1;

    @FXML
    private Color x2;

    @FXML
    private Font x3;

    @FXML
    private Color x4;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private AnchorPane anchorForChart;

    @FXML
    private Slider sliderSpacing;
    @FXML
    private Slider sliderWidth;
    


    @FXML
    void initialize() {
        PlotHandler plotHandler = new PlotHandler();
        plotHandler.showOHLCChart(scrollPane, anchorForChart,true, 2000, 250);
        OHLCChart chart = plotHandler.getOHLCChart();
        chart.addScaleControl(sliderWidth);
        chart.addBarSpacingControl(sliderSpacing);}

}
