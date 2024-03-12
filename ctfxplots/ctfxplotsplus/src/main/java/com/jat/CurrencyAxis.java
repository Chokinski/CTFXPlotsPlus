package com.jat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.chart.ValueAxis;

public class CurrencyAxis extends ValueAxis<Number> {
    private double lowerBound = 0.0;
    private double upperBound = 100.0; // Adjust as per your needs
    private double tickUnit = 1.00; // Adjust as per your needs
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

public static class Range {
    public final double lowerBound;
    public final double upperBound;

    public Range(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
}

@Override
protected void setRange(Object range, boolean animate) {
    if (range instanceof Range) {
        Range r = (Range) range;
        setBounds(r.lowerBound, r.upperBound);
    }
}

public void setBounds(double lowerBound, double upperBound) {
    if (this.lowerBound != lowerBound || this.upperBound != upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        invalidateRange(Arrays.asList(lowerBound, upperBound));
    }
}

    @Override
    protected Range getRange() {
        return new Range(lowerBound, upperBound);
    }

    @Override
    public String getTickMarkLabel(Number number) {
        return currencyFormat.format(number);
    }

    @Override
    public Number getValueForDisplay(double displayPosition) {
        double delta = upperBound - lowerBound;
        if (delta == 0) {
            return 0;
        }
        double length = getSide().isHorizontal() ? getWidth() : getHeight();
        return lowerBound + ((displayPosition * delta) / length);
    }

    @Override
    public double getDisplayPosition(Number number) {
        double delta = upperBound - lowerBound;
        if (delta == 0) {
            return 0;
        }
        double length = getSide().isHorizontal() ? getWidth() : getHeight();
        double position = ((length * ((number.doubleValue() - lowerBound) / delta)));
        return getSide().isHorizontal() ? position : length - position; // Invert the position for vertical axes
    }
    @Override
    public boolean isValueOnAxis(Number number) {
        double value = number.doubleValue();
        return value >= lowerBound && value <= upperBound;
    }

    @Override
    public double toNumericValue(Number number) {
        return number.doubleValue();
    }

    @Override
    public Number toRealValue(double v) {
        return v;
    }

    public void setTickUnit(double tickUnit) {
        this.tickUnit = tickUnit;
        invalidateRange(Arrays.asList(lowerBound, upperBound));
    }


    @Override
    protected List<Number> calculateMinorTickMarks() {
        return new ArrayList<>();
    }

@Override
protected List<Number> calculateTickValues(double length, Object range) {
    if (range == null) {
        return new ArrayList<>();
    }

    Range r = (Range) range;
    double tickUnit = (r.upperBound - r.lowerBound) / 10.0; // Divide range into 10 ticks
    int size = (int) ((r.upperBound - r.lowerBound) / tickUnit) + 1;
    List<Number> tickValues = new ArrayList<>(size);

    for (double value = r.lowerBound; value <= r.upperBound; value += tickUnit) {
        tickValues.add(value);
    }

    return tickValues;
}
    
    public void zoomIn() {
    double zoomFactor = 0.9; // Zoom in by 10%
    setBoundsWithShift(calculateShift(zoomFactor), true);
}

public void zoomOut() {
    double zoomFactor = 1.1; // Zoom out by 10%
    setBoundsWithShift(calculateShift(zoomFactor), false);
}

public void panUp(double panPercentage) {
    double panFactor = Math.max(0.01, (upperBound - lowerBound) / 100); // Dynamic pan distance
    double shift = panFactor * panPercentage;
    setBounds(lowerBound + shift, upperBound + shift);}


public void panDown(double panPercentage) {
    double panFactor = Math.max(0.01, (upperBound - lowerBound) / 100); // Dynamic pan distance
    double shift = panFactor * panPercentage;
    setBounds(lowerBound - shift, upperBound - shift);
}
    
private void setBoundsWithShift(double shift, boolean isZoomIn) {
    double range = upperBound - lowerBound;
    double mid = lowerBound + range / 2.0;
    if (isZoomIn) {
        lowerBound = mid - range * shift / 2.0;
        upperBound = mid + range * shift / 2.0;
    } else {
        lowerBound = mid - range / shift / 2.0;
        upperBound = mid + range / shift / 2.0;
    }
    setBounds(Math.max(0, lowerBound), Math.min(Double.MAX_VALUE, upperBound));
}
    private double calculateShift(double factor) {
        double newRange = (upperBound - lowerBound) * factor;
        double midPoint = (upperBound + lowerBound) / 2;
        return midPoint - newRange / 2 - lowerBound;
    }

    @Override
    protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        double padding = (maxValue - minValue) * 0.1; // Add 10% padding
        double lowerBound = minValue - padding;
        double upperBound = maxValue + padding;
        return new Range(lowerBound, upperBound);
    }

    @Override
    public double getZeroPosition() {
        if (lowerBound <= 0 && upperBound >= 0) {
            return 0;
        } else {
            return -1; // Return -1 if zero is not in the range
        }
    }
}