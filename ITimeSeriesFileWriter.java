package gb.esac.timeseries;

import java.io.IOException;
import java.text.DecimalFormat;


public interface ITimeSeriesFileWriter {

    int bufferSize = 256000;
    DecimalFormat noDigits = new DecimalFormat("0");
    DecimalFormat oneDigit = new DecimalFormat("0.0");
    DecimalFormat twoDigits = new DecimalFormat("0.00");
    DecimalFormat threeDigits = new DecimalFormat("0.000");
    DecimalFormat stats = new DecimalFormat("0.00E00");
    DecimalFormat number = new DecimalFormat("0.000");
    DecimalFormat timeFormat = new DecimalFormat("0.000E0");

    static void writeCounts(ITimeSeries ts, String filename) throws Exception {};
    static void writeCounts(ITimeSeries ts, double[] function, String filename) throws Exception {};
    static void writeCountsAndSampling(ITimeSeries ts, String filename) throws TimeSeriesFileException, IOException {};
    //static void writeCountsAndSampling(ITimeSeries ts, double[] function, String filename) throws IOException, TimeSeriesFileFormatException {};

    static void writeRates(ITimeSeries ts, String filename) throws TimeSeriesFileException, IOException {};
    static void writeRates(ITimeSeries ts, double[] function, String filename) throws TimeSeriesFileException, IOException {};
    static void writeRatesAndSampling(ITimeSeries ts, String filename) throws TimeSeriesFileException, IOException {};
    //static void writeRatesAndSampling(ITimeSeries ts, double[] function, String filename) throws TimeSeriesFileException, IOException {};
            
}
