package com.jat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Calendar;
import java.util.stream.IntStream;

public class MandelbrotGenerator {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    // Method to generate the Mandelbrot set

    private int mandelbrotIterations(double real, double imaginary, int maxIterations) {
        double zReal = 0;
        double zImaginary = 0;
        int iterations = 0;

        while (zReal * zReal + zImaginary * zImaginary <= 4 && iterations < maxIterations) {
            double temp = zReal * zReal - zImaginary * zImaginary + real;
            zImaginary = 2 * zReal * zImaginary + imaginary;
            zReal = temp;
            iterations++;
        }

        return iterations;
    }

    public int[][] generateMandelbrotSet(double minX, double maxX, double minY, double maxY, int maxIterations) {
        int[][] mandelbrotSet = new int[WIDTH][HEIGHT];

        IntStream.range(0, WIDTH).parallel().forEach(x -> {
            for (int y = 0; y < HEIGHT; y++) {
                double real = minX + x * (maxX - minX) / (WIDTH - 1);
                double imaginary = minY + y * (maxY - minY) / (HEIGHT - 1);
                mandelbrotSet[x][y] = mandelbrotIterations(real, imaginary, maxIterations);
            }
        });

        return mandelbrotSet;
    }

    public ObservableList<OHLCData> generateOHLCData(int[][] mandelbrotSet, int maxPoints) {
        ObservableList<OHLCData> ohlcDataList = FXCollections.observableArrayList();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.JANUARY, 1, 0, 0, 0);

        int points = 0;
        outerLoop: for (int x = 0; x < WIDTH - 1; x++) {
            for (int y = 0; y < HEIGHT - 1; y++) {
                double open = mandelbrotSet[x][y] / 10.0;
                double close = mandelbrotSet[x + 1][y + 1] / 10.0;
                double high = Math.max(open, close);
                double low = Math.min(open, close);
                double volume = 1;

                //OHLCData data = new OHLCData(calendar.getTime(), open, high, low, close, volume);
                //ohlcDataList.add(data);

                points++;
                if (points >= maxPoints) {
                    break outerLoop;
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return ohlcDataList;
    }

    /*public void visualizeMandelbrotSet(int maxIterations, int maxPoints) {
        double minX = -0.75 - 0.25; // Center around -0.75 on the x-axis
        double maxX = -0.75 + 0.25;
        double minY = -0.25; // Center around 0 on the y-axis
        double maxY = 0.25;
        int datacount = 0;

        // Generate the Mandelbrot set
        int[][] mandelbrotSet = generateMandelbrotSet(minX, maxX, minY, maxY, maxIterations);

        // Create a new ObservableList for the OHLC data
        ObservableList<OHLCData> ohlcDataList = FXCollections.observableArrayList();

        // Initialize the calendar at the start of your data generation method
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.JANUARY, 1, 0, 0, 0);

        // For each point in the Mandelbrot set, create a new data object and add it to
        // the list
        int points = 0;
        outerLoop: for (int x = 0; x < WIDTH - 1; x++) {
            for (int y = 0; y < HEIGHT - 1; y++) {
                // Use the number of iterations as the open and close prices, divide by a
                // constant factor to make the bodies shorter
                double open = mandelbrotSet[x][y] / 10.0;
                double close = mandelbrotSet[x + 1][y + 1] / 10.0;
                double high = Math.max(open, close);
                double low = Math.min(open, close);
                double volume = 1;

                // Create a new OHLCData object with the current date
                OHLCData data = new OHLCData(calendar.getTime(), open, high, low, close, volume);
                datacount = datacount + 1;

                // Print all relevant data of the item
                System.out.println(String.format("Data item %s", datacount));

                // Add the data object to the list
                ohlcDataList.add(data);

                points++;
                if (points >= maxPoints) {
                    break outerLoop;
                }

                // Move to the next day
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

        }

        // Use setSeries to update the chart data
        setSeries(ohlcDataList);

        // Set the lower and upper bounds of the X-axis to the first and last dates in
        // your data
        DateAxis xAxis = (DateAxis) this.getXAxis();
        xAxis.setLowerBound(ohlcDataList.get(0).getDate());
        xAxis.setUpperBound(ohlcDataList.get(ohlcDataList.size() - 1).getDate());

        // Set the lower and upper bounds of the Y-axis to the minimum and maximum
        // prices in your data
        NumberAxis yAxis = (NumberAxis) this.getYAxis();
        double minYPrice = ohlcDataList.stream().mapToDouble(OHLCData::getLow).min().orElse(0);
        double maxYPrice = ohlcDataList.stream().mapToDouble(OHLCData::getHigh).max().orElse(0);
        yAxis.setLowerBound(minYPrice);
        yAxis.setUpperBound(maxYPrice);

        // Disable auto-ranging
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
        layoutPlotChildren();
    }
*/
}
