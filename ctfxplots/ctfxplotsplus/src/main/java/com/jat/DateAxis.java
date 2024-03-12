package com.jat;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.chart.Axis;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DateAxis extends Axis<LocalDate> {

    private static final ThreadLocal<SimpleDateFormat> format = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private LocalDate lowerBound = LocalDate.now();
    private LocalDate upperBound = LocalDate.now().plusDays(1);
    private double length = 100;
    private Range rangeCache;
    private double barSpacing = 0;
    private double tickUnit = 1.0;
    private LinkedHashMap<Long, LocalDate> cachedTickValues = null;
    private final ObjectProperty<LocalDate> lowerBoundProperty = new SimpleObjectProperty<>(this, "lowerBound", LocalDate.now());
    private final ObjectProperty<LocalDate> upperBoundProperty = new SimpleObjectProperty<>(this, "upperBound", LocalDate.now());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    

    protected List<LocalDate> calculateMajorTickMarks() {
        long lowerBound = getLowerBound().toEpochDay();
        long upperBound = getUpperBound().toEpochDay();
        long tickUnit = (upperBound - lowerBound) / 10L;
        int size = (int) ((upperBound - lowerBound) / tickUnit) + 1;
        List<LocalDate> ticks = new ArrayList<>(size);
        for (long value = lowerBound; value <= upperBound; value += tickUnit) {
            ticks.add(cachedTickValues.computeIfAbsent(value, LocalDate::ofEpochDay));
        }
        return ticks;
    }

    protected List<LocalDate> calculateMinorTickMarks() {
        List<LocalDate> majorTicks = getTickValues();
        List<LocalDate> minorTicks = new ArrayList<>(majorTicks.size() - 1);
        for (int i = 0; i < majorTicks.size() - 1; i++) {
            long minorTick = (majorTicks.get(i).toEpochDay() + majorTicks.get(i + 1).toEpochDay()) / 2;
            minorTicks.add(cachedTickValues.computeIfAbsent(minorTick, LocalDate::ofEpochDay));
        }
        return minorTicks;
    }

    @Override
    protected void setRange(Object range, boolean animate) {
        if (range instanceof Range) {
            Range r = (Range) range;
            lowerBoundProperty().set(r.lowerBound);
            upperBoundProperty().set(r.upperBound);
            setTickUnit((r.upperBound.toEpochDay() - r.lowerBound.toEpochDay()) / 10.0);
        } else {
            throw new IllegalArgumentException("Expected range of type Range");
        }
    }

    public void setTickUnit(double tickUnit) {
        this.tickUnit = tickUnit;
        List<LocalDate> range = new ArrayList<>();
        range.add(lowerBound);
        range.add(upperBound);
        invalidateRange(range);
    }

    public double getTickUnit() {
        return tickUnit;
    }

    @Override
    protected Range getRange() {
        return new Range(lowerBound, upperBound);
    }

    @Override
    public String getTickMarkLabel(LocalDate date) {
        return formatter.format(date);
    }

    public void setDateRange(LocalDate lowerBound, LocalDate upperBound) {
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
        requestAxisLayout();
    }

    @Override
    public LocalDate getValueForDisplay(double displayPosition) {
        long delta = ChronoUnit.DAYS.between(getLowerBound(), getUpperBound());
        double scale = length / delta;
        return getLowerBound().plusDays((long) ((displayPosition - getZeroPosition()) / scale));
    }

    public void toggleAutoRanging() {
        setAutoRanging(!isAutoRanging());
    }

    public List<LocalDate> getTickValues() {
        if (cachedTickValues == null) {
            cachedTickValues = new LinkedHashMap<>();
            calculateMajorTickMarks().forEach(date -> cachedTickValues.put(date.toEpochDay(), date));
        }
        return new ArrayList<>(cachedTickValues.values());
    }

    private int getTickMarkPosition(LocalDate date) {
        List<LocalDate> tickValues = getTickValues();
        for (int i = 0; i < tickValues.size(); i++) {
            if (Objects.equals(date, tickValues.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public double getDisplayPosition(LocalDate date) {
        double length = getLength();
        double lowerBoundTime = getLowerBound().toEpochDay();
        double upperBoundTime = getUpperBound().toEpochDay();
        double dateValue = date.toEpochDay();
        double position;

        if (isVertical()) {
            position = length - ((dateValue - lowerBoundTime) / (upperBoundTime - lowerBoundTime) * length);
        } else {
            position = ((dateValue - lowerBoundTime) / (upperBoundTime - lowerBoundTime) * length);
        }

        if (position < 0) {
            position = 0;
        } else if (position > length) {
            position = length;
        }

        return position;
    }

    public double scaleValue(double value) {
        long delta = ChronoUnit.DAYS.between(getLowerBound(), getUpperBound());
        double scale = getLength() / delta;
        double displayPosition = getDisplayPosition(LocalDate.ofEpochDay((long) value));
        return displayPosition / scale;
    }

    public int getPosition(LocalDate date) {
        int position = getTickMarkPosition(date);
        if (position == -1) {
            LocalDate lowerBound = getLowerBound();
            LocalDate upperBound = getUpperBound();
            long range = ChronoUnit.DAYS.between(upperBound, lowerBound);
            long offset = ChronoUnit.DAYS.between(date, lowerBound);
            position = (int) ((double) offset / range * getTickValues().size());
        }
        return position;
    }

    public boolean isVertical() {
        Side side = getSide();
        return Side.LEFT.equals(side) || Side.RIGHT.equals(side);
    }

    public double getLength() {
        return isVertical() ? getHeight() : getWidth();
    }

    @Override
    public boolean isValueOnAxis(LocalDate date) {
        long time = date.toEpochDay();
        return time >= getLowerBound().toEpochDay() && time <= getUpperBound().toEpochDay();
    }

    @Override
    public double toNumericValue(LocalDate date) {
        return date.toEpochDay();
    }

    @Override
    public LocalDate toRealValue(double v) {
        return LocalDate.ofEpochDay((long) v);
    }

    static class Range {
        LocalDate lowerBound;
        LocalDate upperBound;

        Range(LocalDate lowerBound, LocalDate upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        public boolean isValid(LocalDate lower, LocalDate upper) {
            return lowerBound.isEqual(lower) && upperBound.isEqual(upper);
        }
    }

    public void setLowerBound(LocalDate lowerBound) {
        this.lowerBound = lowerBound;
        rangeCache = null;
    }

    public void setUpperBound(LocalDate upperBound) {
        this.upperBound = upperBound;
        rangeCache = null;
    }

    public LocalDate getUpperBound() {
        return upperBound;
    }

    public LocalDate getLowerBound() {
        return lowerBound;
    }

    public final ObjectProperty<LocalDate> lowerBoundProperty() {
        return lowerBoundProperty;
    }

    public final ObjectProperty<LocalDate> upperBoundProperty() {
        return upperBoundProperty;
    }

    public void setBarSpacing(double barSpacing) {
        this.barSpacing = barSpacing;
        rangeCache = null;
        invalidateRange(Arrays.asList(lowerBound, upperBound));
        requestAxisLayout();
        adjustBarSpacing();
    }

    @Override
    protected Object autoRange(double length) {
        this.length = length;
        if (rangeCache == null || !rangeCache.isValid(getLowerBound(), getUpperBound())) {
            rangeCache = new Range(getLowerBound(), getUpperBound());
        }
        return rangeCache;
    }

    @Override
    public double getZeroPosition() {
        return getDisplayPosition(LocalDate.ofEpochDay(0));
    }

    @Override
    protected List<LocalDate> calculateTickValues(double length, Object range) {
        List<LocalDate> originalTickValues = calculateMajorTickMarks();
        return IntStream.range(0, originalTickValues.size())
                .mapToObj(
                        i -> getLowerBound().plusDays(
                                (long) (i * (originalTickValues.size() - 1) / (double) originalTickValues.size())
                        ))
                .collect(Collectors.toList());
    }

    public void zoom(double zoomFactor, LocalDate zoomPoint) {
        long range = ChronoUnit.DAYS.between(getUpperBound(), getLowerBound());
        long newRange = (long) (range * zoomFactor);
        long midPoint = zoomPoint.toEpochDay();
    
        long newLowerBoundTime = midPoint - newRange / 2;
        long newUpperBoundTime = midPoint + newRange / 2;
    
        newLowerBoundTime = Math.max(newLowerBoundTime, LocalDate.MIN.toEpochDay());
        newUpperBoundTime = Math.min(newUpperBoundTime, LocalDate.MAX.toEpochDay());
    
        setLowerBound(LocalDate.ofEpochDay(newLowerBoundTime));
        setUpperBound(LocalDate.ofEpochDay(newUpperBoundTime));
        rangeCache = null;
    }
    
    public void zoomIn(LocalDate zoomPoint) {
        zoom(0.9, zoomPoint);
        adjustBarSpacing();
    }
    
    public void zoomOut(LocalDate zoomPoint) {
        zoom(1.1, zoomPoint);
        adjustBarSpacing();
    }


    
    private void adjustPan(double panPercentage, boolean isPanRight) {
        long range = ChronoUnit.DAYS.between(getUpperBound(), getLowerBound());

        long shift = (long) (range * panPercentage);

        long newLowerBoundTime = isPanRight ? getLowerBound().toEpochDay() + shift : getLowerBound().toEpochDay() - shift;
        long newUpperBoundTime = isPanRight ? getUpperBound().toEpochDay() + shift : getUpperBound().toEpochDay() - shift;

        newLowerBoundTime = Math.max(newLowerBoundTime, LocalDate.MIN.toEpochDay());
        newUpperBoundTime = Math.min(newUpperBoundTime, LocalDate.MAX.toEpochDay());

        setLowerBound(LocalDate.ofEpochDay(newLowerBoundTime));
        setUpperBound(LocalDate.ofEpochDay(newUpperBoundTime));
        rangeCache = null;
    }

    public void panLeft(double panPercentage) {
        adjustPan(panPercentage, false);
        adjustBarSpacing();
    }

    public void panRight(double panPercentage) {
        adjustPan(panPercentage, true);
        adjustBarSpacing();
    }

    public void adjustBarSpacing() {
        List<LocalDate> tickValues = getTickValues();
        int numberOfBars = tickValues.size();

        if (numberOfBars > 0) {
            double barSpacing = ChronoUnit.DAYS.between(getUpperBound(), getLowerBound()) / (double) numberOfBars;

            for (int i = 0; i < numberOfBars; i++) {
                LocalDate tickDate = tickValues.get(i);
                double newPosition = getDisplayPosition(tickDate);
                getTickMarks().get(i).setPosition(newPosition);
            }
        }
    }

	public double getDisplayPosition(String dateAsString) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getDisplayPosition'");
	}
}