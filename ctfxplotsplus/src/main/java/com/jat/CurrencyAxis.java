package com.jat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.chart.ValueAxis;

import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CurrencyAxis extends ValueAxis<Double> {

    private OHLCChart chart;
    private Range range;
    private int MAX_TICK_COUNT = 7; // Adjust as needed

    public CurrencyAxis(double lb, double ub, OHLCChart c) {
        
        setAutoRanging(false);
        setSide(Side.RIGHT);
        setTickLabelsVisible(true);
        setTickLabelGap(5);
        setAnimated(false);
        this.chart = c; // Store reference to the chart
        this.range = new Range(lb, ub);
        

        // Set the formatter for tick labels
        setTickLabelFormatter(new StringConverter<Double>() {
            
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }

                double val = value;
                double range = ub - lb;
                DecimalFormat df;

                if (range > 1_000_000) {
                    df = new DecimalFormat("$0.##M");
                } else if (range > 1000) {
                    df = new DecimalFormat("$0.##K");
                } else {
                    df = new DecimalFormat("$0.##");
                }

                return df.format(val / (range > 1_000_000 ? 1_000_000 : (range > 1000 ? 1000 : 1)));
            }

            @Override
            public Double fromString(String string) {
                return null; // Not needed for formatting
            }
        });
        this.getStyleClass().add("axis-currency");
    }




    public static class Range {
        double lowerBound;
        double upperBound;

        Range(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }

    public void setBounds(double lowerBound, double upperBound) {
        this.range = new Range(lowerBound, upperBound);
        requestAxisLayout();

    }

    @Override
    protected Range getRange() {
        return range;
    }

    

    @Override
    protected List<Double> calculateTickValues(double length, Object range) {
        if (range == null || !(range instanceof Range)) {
            System.out.println("Currency Range is not of type Range or etc...");
            return FXCollections.observableArrayList();
        }
    
        Range r = (Range) range;
        double rangeInValue = r.upperBound - r.lowerBound;
        double tickInterval = Math.max(1, rangeInValue / (MAX_TICK_COUNT - 1));
    
        return calculateTickPositions(r.lowerBound, r.upperBound, tickInterval);
    }

    private List<Double> calculateTickPositions(double lower, double upper, double tickInterval) {
        List<Double> tickValues = new ArrayList<>();
        double tickValue = lower;
    
        // Loop to calculate tick positions with careful handling of floating-point precision
        while (tickValue <= upper + 1e-9) { // Adding a small epsilon to ensure inclusion of upper bound
            tickValues.add(tickValue);
            tickValue = Math.round((tickValue + tickInterval) * 1e9) / 1e9; // Rounding to avoid floating-point inaccuracies
        }
    
        return tickValues;
    }

    @Override
    public String getTickMarkLabel(Double number) {
        if (number == null) {
            ////System.out.println("Number is null, returning empty label");
            return "";
        }

        double range = this.range.upperBound - this.range.lowerBound;
        DecimalFormat df;

        if (range > 1_000_000) {
            df = new DecimalFormat("$0.##M");
        } else if (range > 1000) {
            df = new DecimalFormat("$0.##K");
        } else {
            df = new DecimalFormat("$0.##");
        }

        String label = df.format(number / (range > 1_000_000 ? 1_000_000 : (range > 1000 ? 1000 : 1)));
        //System.out.println("Formatted label for " + number + ": " + label);
        return label;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //System.out.println("Layout children called");

        // Fetch the tick marks
        ObservableList<TickMark<Double>> tickMarks = getTickMarks();
        //System.out.println("Tick marks: " + tickMarks);

        // No need to loop through tick marks and set labels anymore
        // Labels will be automatically managed by the formatter set above
    }

@Override
public Double getValueForDisplay(double displayPosition) {
    double range = this.range.upperBound - this.range.lowerBound;
    if (range == 0) {
        return this.range.lowerBound;
    }

    double axisLength = getSide().isHorizontal() ? getWidth() : getHeight();

    // Invert the position for vertical axis to make larger values go up
    if (getSide().isVertical()) {
        displayPosition = axisLength - displayPosition;
    }

    return this.range.lowerBound + (displayPosition / axisLength) * range;
}

@Override
public double getDisplayPosition(Double value) {
    double range = this.range.upperBound - this.range.lowerBound;
    if (range == 0) {
        return 0;
    }

    double axisLength = getSide().isHorizontal() ? getWidth() : getHeight();
    double position = (value - this.range.lowerBound) / range * axisLength;

    // Invert the position for vertical axis to make larger values go up
    if (getSide().isVertical()) {
        position = axisLength - position;
    }

    return position;
}



    @Override
    protected void setRange(Object range, boolean animate) {
        if (range instanceof Range) {
            Range r = (Range) range;
            setBounds(r.lowerBound, r.upperBound);
        } else {
            throw new IllegalArgumentException("Unsupported range object type: " + (range != null ? range.getClass().getName() : "null"));
        }
    }

    @Override
    protected List<Double> calculateMinorTickMarks() {
        List<Double> minorTickMarks = new ArrayList<>();

        // Ensure we have a valid range
        if (getRange() == null || !(getRange() instanceof Range)) {
            //System.out.println("Invalid range object type: " + (getRange() != null ? getRange().getClass().getName() : "null"));
            return minorTickMarks; // Return empty list or handle as needed
        }

        Range range = (Range) getRange();
        double lowerBound = range.lowerBound;
        double upperBound = range.upperBound;

        // Calculate minor tick interval (e.g., 10 minor ticks between major ticks)
        int minorTickCount = 10; // Adjust as needed
        double majorTickInterval = (upperBound - lowerBound) / (getTickMarks().size() - 1); // Calculate major tick interval
        double minorTickInterval = majorTickInterval / (minorTickCount + 1); // Calculate minor tick interval

        // Generate minor tick marks
        double minorTick = lowerBound + minorTickInterval;
        while (minorTick < upperBound) {
            minorTickMarks.add(minorTick);
            minorTick += minorTickInterval;
        }

        return minorTickMarks;
    }

    public void invalidateRangeInternal(Double[] l) {
        
        double minCurrency = Double.MAX_VALUE;
        double maxCurrency = Double.MIN_VALUE;
    
        List<Series<LocalDateTime, Double>> data = chart.getChartData();
    

                    if (l[0] < minCurrency) {
                        minCurrency = l[0];
                    }
                    if (l[1]> maxCurrency) {
                        maxCurrency = l[1];
                    }
                
        
    
        // Only update bounds if they have changed significantly
        if (minCurrency != range.lowerBound || maxCurrency != range.upperBound) {
            setBounds(minCurrency, maxCurrency);
            range.lowerBound = minCurrency;
            range.upperBound = maxCurrency;
            calculateTickValues(maxCurrency, range);
            requestAxisLayout();
        }
        
    }
    public void giveChart(OHLCChart chart) {
        this.chart = chart;
    }
}