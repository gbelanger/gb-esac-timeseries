package gb.esac.timeseries;

import java.lang.*;
import nom.tam.fits.Fits;
import org.apache.log4j.Logger;

/**
 * July 2016 created by Harry Holt
 * Test the function of the JSTimeSeriesFileWriter
 
 filename = file that is used to contruct TimeSeries object
 outputDataFilename = file where the data is stored so it can be read by the JS and HTML file
 outputScriptFilename = file which contains the JS and HTML code to produce the Plotly graph
 */

public class TestJSTimeSeriesFileWriter {
    private static Logger logger  = Logger.getLogger(TestJSTimeSeriesFileWriter.class);
    
    public static void main(String[] args) throws Exception  {
        
        
        String type = "rates";
        
        String filename = "/Users/harryholt/esa/java_previous/gb/esac/timeseries/saxj1808_lightcurve_16msec.lc";  // Data used to produce initial TimeSeries object
        
        // WARNING - CAN'T TEST CODEDMASK AT THE MOMENT
        if (type.equals("rates_cm")||type.equals("counts_cm")){
            filename = "/Users/harryholt/esa/java_previous/gb/esac/timeseries/ts_isgri_83.633_22.01_20-35keV.qdp";
        }
        
        //filename = "saxj1808_lightcurve_16msec.lc";
        
        String Path = filename.substring(0, filename.lastIndexOf("/")+1);
        String Name = filename.substring(filename.lastIndexOf("/")+1, filename.lastIndexOf(".")) ;
        String outputFilename = Path + "ts.qdp";
        
        
        TimeSeries lc = TimeSeriesMaker.makeTimeSeries(filename);
        
        double tStart = 0;
        double tStop = 10;
        TimeSeries seg = TimeSeriesOperations.getSegment(lc, tStart, tStop);
        
        // If you want to test the function[] then you need to add the array and change the input arguements for writeCountsAsJS and writeRatesAsJS
        
        if (type.equals("counts")) {
            seg.writeCountsAsJS(outputFilename);
        } else if (type.equals("counts_samp")){
            seg.writeCountsAndSamplingAsJS(outputFilename);
        } else if (type.equals("rates")){
            seg.writeRatesAsJS(outputFilename);
        } else if (type.equals("rates_samp")){
            seg.writeRatesAndSamplingAsJS(outputFilename);
        } else if (type.equals("rates_cm")){
            seg.writeCodeMaskRatesAsJS(outputFilename);
        }
        
        // Testing the normal TimeSeriesWriter
            
        //seg.writeCountsAsQDP("seg_counts.qdp");
        //seg.writeRatesAsQDP("seg_rates.qdp");


    }
}