package com.jat;

import javafx.scene.chart.XYChart;
import java.io.Serializable;
import java.time.LocalDate;

import java.util.Objects;

public class OHLCData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDate timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    private final XYChart.Data<LocalDate, Number> data;


    public OHLCData(LocalDate timestamp, double open, double high, double low, double close, double vol) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = vol;
        this.data = new XYChart.Data<>(timestamp, getMid()); // y value is the midpoint
    }

    // getters for open, high, low, close, data

    public LocalDate getDate() { return timestamp; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public double getVolume() { return volume; }
    public double getMid() { return (open + close) / 2; }
    public double getDelta() { return close - open; }
    public double getChangeInPips() { return (close - open) * 10000; }
    public double getRange() { return high - low; }
    public boolean isBullish() { return close > open; }
    public boolean isBearish() { return close < open; }
    public String getPriceMovement() { return isBullish() ? "Bullish" : isBearish() ? "Bearish" : "Neutral"; }
    public double getDeltaPercent() { return ((close - open) / open) * 100; }
    public double getVolumeInThousands() { return volume / 1000; }
    public double getVolumeInMillions() { return volume / 1_000_000; }

    public String getOHLC() {
        return "O:" + open + " H:" + high + " L:" + low + " C:" + close;
    }

    public XYChart.Data<LocalDate, Number> getData() { return data; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OHLCData ohlcData = (OHLCData) obj;
        return timestamp.equals(ohlcData.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public String toString() {
        return "OHLCData{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}