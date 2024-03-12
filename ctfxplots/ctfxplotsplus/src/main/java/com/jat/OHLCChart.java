package com.jat;

import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.DoubleSummaryStatistics;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class OHLCChart extends XYChart<LocalDate, Number> {
    private Canvas buffer;
    private PauseTransition pause = new PauseTransition(Duration.millis(100));
    private double barSpacing;
    private double scale;
    private double maxYValue;
    private int count = 0;
    private double mouseX;
    private double mouseY;
    private DateAxis xAxis; // Assuming DateAxis is a custom axis that extends from ValueAxis<LocalDate>
    private CurrencyAxis yAxis;
    private ObservableList<OHLCData> ohlcDataList;
    private Series<LocalDate, Number> series;
    private List<OHLCData> allData = new ArrayList<>();

    public OHLCChart(DateAxis xAxis, CurrencyAxis yAxis) {
        super(xAxis, yAxis);
        this.setData(FXCollections.observableArrayList());
        this.getStyleClass().add("ohlc-chart");
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.series = new XYChart.Series<>();
        // Set the axis labels
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        // Set the chart title
        setTitle("OHLC Chart");
        setChartCache();
        addZoomListener();
        addPanListener();
    }

    // Provide some mockdata to display on the chart

    public void mockData(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }
    
        ObservableList<OHLCData> ohlcDataList = FXCollections.observableArrayList();
        Random random = new Random();
        double lastClose = 100; // Start with a close price of 100
    
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            double open = lastClose + random.nextGaussian(); // Open is last close plus a random Gaussian noise
            double high = open + random.nextDouble() * 5; // Random value above open
            double low = open - random.nextDouble() * 5; // Random value below open
            double close = low + random.nextDouble() * (high - low); // Random value between low and high
            double volume = 1000 + random.nextDouble() * 500; // Random volume between 1000 and 1500
    
            lastClose = close; // Update lastClose for the next iteration
    
            OHLCData ohlcData = new OHLCData(date, open, high, low, close, volume);
            ohlcDataList.add(ohlcData);
        }
        setSeries(ohlcDataList);
        // Debug statement to print the size of ohlcDataList
        System.out.println("Size of ohlcDataList: " + ohlcDataList.size());
    }

    // Method to set the series of the chart

    public void setSeries(ObservableList<OHLCData> ohlcDataList) {
        this.ohlcDataList = ohlcDataList;
        series.getData().clear(); // Clear existing data
        addDataToSeries(series);
        updateChart(series, getMinMaxDates(), getMinMaxValues());
        requestLayout();
    }

private void addDataToSeries(Series<LocalDate, Number> series) {
    if (ohlcDataList == null) {
        throw new IllegalStateException("ohlcDataList cannot be null");
    }

    List<XYChart.Data<LocalDate, Number>> dataToAdd = ohlcDataList.stream()
        .map(this::toChartData)
        .collect(Collectors.toList());

    Platform.runLater(() -> {
        series.getData().addAll(dataToAdd);
        for (XYChart.Data<LocalDate, Number> data : dataToAdd) {
            addTooltipToData(series, (OHLCData) data.getExtraValue());
        }
    });
}

    // Method to add data to the chart

    public synchronized void addData(OHLCData ohlcData) {
        if (ohlcData == null) {
            throw new IllegalArgumentException("ohlcData cannot be null");
        }
        // Convert OHLCData to chart data
        XYChart.Data<LocalDate, Number> data = toChartData(ohlcData);

        // Add to series
        this.series.getData().add(data);

        // Add to allDataList
        allData.add(ohlcData);

        // Print out debug information
        System.out.println("Added data: " + ohlcData);
        System.out.println("Lower bound: " + xAxis.getLowerBound());
        System.out.println("Upper bound: " + xAxis.getUpperBound());
    }

    private LocalDate[] getMinMaxDates() {
        LocalDate minDate = LocalDate.MAX;
        LocalDate maxDate = LocalDate.MIN;

        for (OHLCData ohlcData : ohlcDataList) {
            LocalDate date = ohlcData.getDate();
            minDate = date.isBefore(minDate) ? date : minDate;
            maxDate = date.isAfter(maxDate) ? date : maxDate;
        }

        return new LocalDate[] { minDate, maxDate };
    }

private double[] getMinMaxValues() {
    DoubleSummaryStatistics stats = ohlcDataList.stream()
        .flatMapToDouble(ohlcData -> DoubleStream.of(ohlcData.getLow(), ohlcData.getHigh()))
        .summaryStatistics();

    return new double[] { stats.getMin(), stats.getMax() };
}

private void addTooltipToData(Series<LocalDate, Number> series, OHLCData ohlcData) {
    Node node = series.getData().get(series.getData().size() - 1).getNode();
    if (node != null) {
        Tooltip tooltip = new Tooltip("Open: " + ohlcData.getOpen() + "\nHigh: " + ohlcData.getHigh() +
                "\nLow: " + ohlcData.getLow() + "\nClose: " + ohlcData.getClose());
        Tooltip.install(node, tooltip);
        node.setVisible(true);
    }
}

private void updateChart(Series<LocalDate, Number> series, LocalDate[] minMaxDates, double[] minMaxValues) {
    getData().setAll(series);

    double range = minMaxValues[1] - minMaxValues[0];
    double lowerBound = minMaxValues[0] - range * 0.1;
    double upperBound = minMaxValues[1] + range * 0.1;

    xAxis.setAutoRanging(false);
    xAxis.setLowerBound(minMaxDates[0]);
    xAxis.setUpperBound(minMaxDates[1]);

    yAxis.setAutoRanging(false);
    yAxis.setLowerBound(lowerBound);
    yAxis.setUpperBound(upperBound);

    Platform.runLater(() -> {
        addBoundsChangeListener(series);
        System.out.println("Min date: " + minMaxDates[0]);
        System.out.println("Max date: " + minMaxDates[1]);
        System.out.println("Min value: " + lowerBound);
        System.out.println("Max value: " + upperBound);
    });
}

private XYChart.Data<LocalDate, Number> toChartData(OHLCData ohlcData) {
    return new XYChart.Data<>(ohlcData.getDate(), ohlcData.getClose(), ohlcData);
}

public void addDataChangeListener(Series<LocalDate, Number> series, ObservableList<OHLCData> ohlcDataList) {
    series.getData().addListener((ListChangeListener.Change<? extends XYChart.Data<LocalDate, Number>> c) -> {
        while (c.next()) {
            if (c.wasAdded()) {
                c.getAddedSubList().stream()
                        .map(data -> (OHLCData) data.getExtraValue())
                        .forEach(allData::add);
            }
            if (c.wasRemoved()) {
                c.getRemoved().stream()
                        .map(data -> (OHLCData) data.getExtraValue())
                        .forEach(allData::remove);
            }
        }
    });

    //addBoundsChangeListener(series);
}

private void addBoundsChangeListener(Series<LocalDate, Number> series) {
    series.getNode().boundsInParentProperty().addListener((observable, oldBounds, newBounds) -> {
        series.getData().stream()
                .map(XYChart.Data::getNode)
                .forEach(node -> {
                    if (node != null) {
                        node.setClip(new Rectangle(newBounds.getMinX(), newBounds.getMinY(),
                                newBounds.getWidth(), newBounds.getHeight()));
                    }
                });
    });
}
    
    public void reloadData(XYChart.Series<LocalDate, Number> series) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    Bounds bounds = calculateBounds();
                    ObservableList<XYChart.Data<LocalDate, Number>> data = series.getData();
    
                    // Clear existing data
                    data.clear();
    
                    // Add new data
                    for (OHLCData ohlcData : allData) {
                        LocalDate date = ohlcData.getDate();
                        double value = ohlcData.getClose();
                        data.add(new XYChart.Data<>(date, value, ohlcData));
                    }
    
                    Platform.runLater(() -> {
                        // Update the bounds of the axes
                        xAxis.setAutoRanging(false);
                        xAxis.setLowerBound(bounds.minX);
                        xAxis.setUpperBound(bounds.maxX);
    
                        yAxis.setAutoRanging(false);
                        yAxis.setLowerBound(bounds.minY);
                        yAxis.setUpperBound(bounds.maxY);
                    });
                } catch (Exception e) {
                    // Log the exception
                    System.err.println("An error occurred in the reloadData method: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    private Bounds calculateBounds() {
        Bounds bounds = new Bounds();
        bounds.minX = null;
        bounds.maxX = null;
        bounds.minY = Double.MAX_VALUE;
        bounds.maxY = Double.MIN_VALUE;
    
        for (OHLCData ohlcData : allData) {
            LocalDate date = ohlcData.getDate();
            double value = ohlcData.getClose();
    
            // Update min and max x values
            if (bounds.minX == null || date.isBefore(bounds.minX))
                bounds.minX = date;
            if (bounds.maxX == null || date.isAfter(bounds.maxX))
                bounds.maxX = date;
    
            // Update min and max y values
            if (value < bounds.minY)
                bounds.minY = value;
            if (value > bounds.maxY)
                bounds.maxY = value;
        }
    
        return bounds;
    }


    public void setChartCache() {
        setCache(true);
        setCacheHint(CacheHint.QUALITY);
    }

    public void addZoomListener() {
        final Node chartArea = this.lookup(".chart-plot-background");
        chartArea.setOnScroll(new EventHandler<ScrollEvent>() {
            public void handle(ScrollEvent event) {
                if (event.getDeltaY() == 0) {
                    return;
                }
                double scaleFactor = event.getDeltaY() > 0 ? 0.9 : 1.1;
                LocalDate zoomPoint = xAxis.getValueForDisplay(isVertical() ? event.getY() : event.getX());
                xAxis.zoom(scaleFactor, zoomPoint);
            }
    
            private boolean isVertical() {
                Side side = xAxis.getSide();
                return Side.LEFT.equals(side) || Side.RIGHT.equals(side);
            }
        });
    }

    public void adjustBarWidth(double factor) {
        for (Series<LocalDate, Number> series : getData()) {
            for (XYChart.Data<LocalDate, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node instanceof Region) {
                    Region region = (Region) node;
                    double width = region.getWidth() * factor;
                    region.setPrefWidth(width);
                    region.setMaxWidth(width);
                    region.setMinWidth(width);
                }
            }
        }
    }

    public void addBarSpacingControl(Slider slider) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Adjust the bar spacing based on the slider value
            setBarSpacing(newVal.doubleValue());
        });
    }

    public void addScaleControl(Slider slider) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Adjust the scale of the chart data based on the slider value
            setScale(newVal.doubleValue());
        });

    }

    public void setBarSpacing(double spacing) {
        this.barSpacing = spacing;
        layoutPlotChildren();
    }

    public void setScale(double scale) {
        this.scale = scale;
        // Assuming the scale affects the y-axis
        yAxis.setTickUnit(this.scale);
    }

    public void addPanListener() {
        final Node chartArea = this.lookup(".chart-plot-background");
        final Region plotArea = (Region) this.lookup(".chart-plot-background");
        final DoubleProperty shiftAmount = plotArea.translateXProperty();

        chartArea.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pause.setOnFinished(e -> setCursor(javafx.scene.Cursor.DEFAULT));
                pause.playFromStart();
            }
        });

        chartArea.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                setCursor(javafx.scene.Cursor.CLOSED_HAND);
                shiftAmount.set(shiftAmount.get() + event.getX() - (getWidth() / 2));
                event.consume();
            }
        });
    }
// Modify these methods
/*    private double calculateScale(DateAxis axis) {
        double visibleRange = axis.getUpperBound().toEpochDay() - axis.getLowerBound().toEpochDay();
        return visibleRange / axis.getWidth();
    }

    private void updateAxisBounds() {
        LocalDate minX = LocalDate.MAX;
        LocalDate maxX = LocalDate.MIN;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Series<LocalDate, Number> series : getData()) {
            for (Data<LocalDate, Number> item : series.getData()) {
                OHLCData ohlcItem = (OHLCData) item.getExtraValue();
                LocalDate date = ohlcItem.getDate();
                double low = ohlcItem.getLow();
                double high = ohlcItem.getHigh();

                // Update minX, maxX, minY, maxY
                minX = date.isBefore(minX) ? date : minX;
                maxX = date.isAfter(maxX) ? date : maxX;
                minY = Math.min(minY, low);
                maxY = Math.max(maxY, high);
            }
        }

        // Update axis bounds
        xAxis.setLowerBound(minX);
        xAxis.setUpperBound(maxX);
        yAxis.setLowerBound(minY);
        yAxis.setUpperBound(maxY);
    }

    private double calculateScale(CurrencyAxis axis) {
        return (axis.getUpperBound() - axis.getLowerBound()) / axis.getWidth();
    }*/

    public double getMaxYValue() {
        this.maxYValue = Double.MIN_VALUE;
        for (Series<LocalDate, Number> series : getData()) {
            for (Data<LocalDate, Number> item : series.getData()) {
                Number yValue = item.getYValue();
                if (yValue.doubleValue() > maxYValue) {
                    maxYValue = yValue.doubleValue();
                }
            }
        }
        return maxYValue;
    }

    // Modify this method to use LocalDate instead of LocalDate
    @Override
    protected void dataItemAdded(Series<LocalDate, Number> series, int itemIndex, Data<LocalDate, Number> item) {
        OHLCData ohlcItem = (OHLCData) item.getExtraValue();

        while (ohlcItem != null) {
            count = count + 1;
            Node candle = createCandleStick(ohlcItem);
            item.setNode(candle);
            getPlotChildren().add(candle);

            // Test for when data items are added
            System.out.println(String.format("Data item |" + count +
                    " |: %s, %s, %s, %s, %s, %s",
                    ohlcItem.getDate(), ohlcItem.getOpen(), ohlcItem.getHigh(),
                    ohlcItem.getLow(),
                    ohlcItem.getClose(), ohlcItem.getVolume()));
            if (ohlcItem.getDate().isBefore(xAxis.getLowerBound())) {
                xAxis.setLowerBound(ohlcItem.getDate());
            }
            if (ohlcItem.getDate().isAfter(xAxis.getUpperBound())) {
                xAxis.setUpperBound(ohlcItem.getDate());
            }
            if (ohlcItem.getLow() < yAxis.getLowerBound()) {
                yAxis.setLowerBound(ohlcItem.getLow());
            }
            if (ohlcItem.getHigh() > yAxis.getUpperBound()) {
                yAxis.setUpperBound(ohlcItem.getHigh());
            }

            break;
        }
    }
/*
@Override
protected double getCurrentDisplayedXValue(Data<LocalDate, Number> item) {
    return xAxis.getDisplayPosition(item.getXValue());}


@Override
protected double getCurrentDisplayedYValue(Data<LocalDate, Number> item) {
    OHLCData ohlcData = (OHLCData) item.getExtraValue();
    return yAxis.getDisplayPosition(ohlcData.getClose().doubleValue());}

*/
    // Modify this method to use LocalDate instead of Date
    @Override
    protected void dataItemRemoved(Data<LocalDate, Number> item, Series<LocalDate, Number> series) {
        final Node candle = item.getNode();
        getPlotChildren().remove(candle);
    }

    // Modify this method to use LocalDate instead of Date
    @Override
    protected void dataItemChanged(Data<LocalDate, Number> item) {
        final Node candle = item.getNode();
        OHLCData ohlcData = (OHLCData) item.getExtraValue();
        candle.setLayoutX(getXAxis().getDisplayPosition(item.getXValue()));
        candle.setLayoutY(getYAxis().getDisplayPosition(ohlcData.getHigh()));
    }

    @Override
    protected void seriesAdded(Series<LocalDate, Number> series, int seriesIndex) {
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        LocalDate minX = LocalDate.ofEpochDay(Long.MAX_VALUE);
        LocalDate maxX = LocalDate.ofEpochDay(Long.MIN_VALUE);
    
        for (Data<LocalDate, Number> item : series.getData()) {
            dataItemAdded(series, seriesIndex, item);
    
            // Update min and max values for Y
            OHLCData ohlcData = (OHLCData) item.getExtraValue();
            if (ohlcData.getLow() < minY) {
                minY = ohlcData.getLow();
            }
            if (ohlcData.getHigh() > maxY) {
                maxY = ohlcData.getHigh();
            }
    
            // Update min and max values for X
            LocalDate xValue = item.getXValue();
            if (xValue.isBefore(minX)) {
                minX = xValue;
            }
            if (xValue.isAfter(maxX)) {
                maxX = xValue;
            }
        }
    
        // Set the lower and upper bounds for Y
        xAxis.setLowerBound(minX);
        xAxis.setUpperBound(maxX);
        yAxis.setLowerBound(minY);
        yAxis.setUpperBound(maxY);
        yAxis.setAutoRanging(true);
        xAxis.setAutoRanging(true);
    }

    @Override
    protected void seriesRemoved(Series<LocalDate, Number> series) {
        for (Data<LocalDate, Number> item : series.getData()) {
            dataItemRemoved(item, series);
        }
    }

    private void ensureDataIsVisible() {
        // Get the current bounds
        LocalDate lowerX = xAxis.getLowerBound();
        LocalDate upperX = xAxis.getUpperBound();
        double lowerY = yAxis.getLowerBound();
        double upperY = yAxis.getUpperBound();
    
        // For each data item in each series
        for (Series<LocalDate, Number> series : getData()) {
            for (Data<LocalDate, Number> item : series.getData()) {
                OHLCData ohlcData = (OHLCData) item.getExtraValue();
    
                // If the data item is outside the bounds, update the bounds
                if (ohlcData.getLow() < lowerY) {
                    lowerY = ohlcData.getLow();
                }
                if (ohlcData.getHigh() > upperY) {
                    upperY = ohlcData.getHigh();
                }
                LocalDate xValue = item.getXValue();
                if (xValue.isBefore(lowerX)) {
                    lowerX = xValue;
                }
                if (xValue.isAfter(upperX)) {
                    upperX = xValue;
                }
            }
        }
    
        // Set the new bounds
        xAxis.setLowerBound(lowerX);
        xAxis.setUpperBound(upperX);
        yAxis.setLowerBound(lowerY);
        yAxis.setUpperBound(upperY);
    }

@Override
protected void layoutPlotChildren() {
    if (buffer == null) {
        buffer = new Canvas();
        buffer.widthProperty().bind(widthProperty());
        buffer.heightProperty().bind(heightProperty());
        getPlotChildren().add(buffer);
    }

    GraphicsContext gc = buffer.getGraphicsContext2D();

    // Clear the canvas
    gc.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());

    for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
        Series<LocalDate, Number> series = getData().get(seriesIndex);
        for (int dataIndex = 0; dataIndex < series.getData().size(); dataIndex++) {
            Data<LocalDate, Number> item = series.getData().get(dataIndex);
            Node itemNode = item.getNode();
            if (itemNode != null) {
                double x = getXAxis().getDisplayPosition(getCurrentDisplayedXValue(item));
                double y = getYAxis().getDisplayPosition(getCurrentDisplayedYValue(item));

                // Draw the item to the buffer instead of setting its layout
                gc.setFill(Color.BLACK);
                gc.fillRect(x, y, 1, 1);
            }
        }
    }
}

private Group createCandleStick(OHLCData ohlcData) {
    // Create candle container
    final Group candleStick = new Group();

    // Extract OHLC data
    double open = ohlcData.getOpen();
    double close = ohlcData.getClose();
    double high = ohlcData.getHigh();
    double low = ohlcData.getLow();

    // Calculate body height and top position
    double bodyHeight = yAxis.getDisplayPosition(Math.max(open, close)) - yAxis.getDisplayPosition(Math.min(open, close));
    double bodyTop = Math.min(yAxis.getDisplayPosition(open), yAxis.getDisplayPosition(close));

    // Create body rectangle
    Rectangle body = new Rectangle();
    body.setY(bodyTop);
    body.setHeight(bodyHeight);
    body.setWidth(5); // Set the width of the body
    body.setFill(close > open ? Color.GREEN : Color.RED); // Determine body color

    // Create upper wick
    Line upperWick = new Line(2.5, yAxis.getDisplayPosition(high), 2.5, bodyTop);
    
    // Create lower wick
    Line lowerWick = new Line(2.5, yAxis.getDisplayPosition(low), 2.5, bodyTop + bodyHeight);

    // Add elements to candlestick group
    candleStick.getChildren().addAll(upperWick, lowerWick, body);

    return candleStick;
}



    public class Bounds {
        LocalDate minX, maxX;
        double minY, maxY;
    }
}
