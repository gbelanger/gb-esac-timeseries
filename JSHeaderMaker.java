package gb.esac.timeseries;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;

import gb.esac.binner.BinningUtils;
import gb.esac.io.AsciiDataFileWriter;
import gb.esac.tools.BasicStats;
import gb.esac.tools.MinMax;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

/**
 * 
 * Class <code>JSTimeSeriesFileWriter</code> writes <code>ITimeSeries</code> objects in FITS format.
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @author Harry Holt
 * @version 1.0, 2017 February, ESAC
 *
 * Mar 2017 - GB completed rewriting and restructuring of this class (and JSTimeSeriesFileWriter)
 * Dec 2016 - GB started working on rewriting to conform to the interfaces in this package.
 * August 2016 Harry Holt created the first version of this class
 *
 * This code creates the combined JavaScript and HTML file that can read the data produced by JSTimeSeriesFileWriter.
 * It uses both HTML and JavaScript to produce graphs that can be viewed in a browser using plotly.
 * The data currently needs to be written in tsv format but this could be changed.
 * This code is called by JSTimeSeriesFileWriter.class to actually print the file.
 * 
 */

final class JSHeaderMaker {

    private static String classname = (JSHeaderMaker.class).getCanonicalName();    
    private static Logger logger = Logger.getLogger(JSHeaderMaker.class);
    private static int bufferSize = 256000;
    private static DecimalFormat decimals = new DecimalFormat("0.00");
    private static final String Y_AXIS_TITLE_RATES = "\"Intensity (s-1)\"";

    private static String getXAxisTitle(ITimeSeries ts) {
        return "\"Time (s) since "+ts.tStart()+"\"";
    }

    private static String getCountsYAxisTitle(ITimeSeries ts) {
	String yAxisTitle = "\"Intensity (cts per bin)\"";
        double binWidth = 0;
        try { binWidth = ts.binWidth(); }
        catch ( TimeSeriesException e ) { }
        if ( binWidth != 0 ) {
            yAxisTitle = "\"Intensity (cts per "+decimals.format(binWidth)+" s)\"";
        }
	return yAxisTitle;
    }

    //  Counts
    static String[] getCountsHeader(ITimeSeries ts, String filename_tsv) {
	String trace = "trace";	
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);
	String[] traceBlock = JSHeaderBlocks.getCountsTraceBlock(filename_tsv);	
	return (String[]) ArrayUtils.addAll(traceBlock, layoutBlock);	
    }
    
    static String[] getCountsAndFunctionHeader(ITimeSeries ts, String filename_tsv) throws IOException {
	String prefix = "Function";
	String trace = "trace, "+prefix;	
	String[] functionBlock = JSHeaderBlocks.getFunctionBlock(prefix);
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);	
	String[] traceBlock = JSHeaderBlocks.getCountsTraceBlock(filename_tsv);
	return (String[]) ArrayUtils.addAll(traceBlock, functionBlock, layoutBlock);
    }
    
    static String[] getCountsAndSamplingHeader(ITimeSeries ts, String filename_tsv) throws IOException {
	String prefix = "Sampling";
	String trace = "trace, "+prefix;
	String[] functionBlock = JSHeaderBlocks.getFunctionBlock(prefix);
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);
	String[] traceBlock = JSHeaderBlocks.getCountsTraceBlock(filename_tsv);
	return (String[]) ArrayUtils.addAll(traceBlock, functionBlock, layoutBlock);
    }

    
    //  Rates
    static String[] getRatesHeader(ITimeSeries ts, String filename_tsv) throws IOException {
	String trace = "trace";	
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);
	String[] traceBlock = JSHeaderBlocks.getRatesTraceBlock(filename_tsv);	
	return (String[]) ArrayUtils.addAll(traceBlock, layoutBlock);	
    }
    
    static String[] getRatesHeader(ITimeSeries ts, double[] function, String filename_tsv) throws IOException {
	String prefix = "Function";
	String trace = "trace, "+prefix;	
	String[] functionBlock = JSHeaderBlocks.getFunctionBlock(prefix);
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);	
	String[] traceBlock = JSHeaderBlocks.getRatesTraceBlock(filename_tsv);
	return (String[]) ArrayUtils.addAll(traceBlock, functionBlock, layoutBlock);
    }
     
    static String[] getRatesAndSamplingHeader(ITimeSeries ts, String filename_tsv) throws IOException {
	String prefix = "Sampling";
	String trace = "trace, "+prefix;
	String[] functionBlock = JSHeaderBlocks.getFunctionBlock(prefix);
	String[] layoutBlock = JSHeaderBlocks.getLayoutBlock(getXAxisTitle(ts), getCountsYAxisTitle(ts), trace);
	String[] traceBlock = JSHeaderBlocks.getRatesTraceBlock(filename_tsv);
	return (String[]) ArrayUtils.addAll(traceBlock, functionBlock, layoutBlock);
    }
    
    // static String[] getAllDataHeader (ITimeSeries ts, String type ) throws IOException {

    // 	return header
    // }


    // HTML
    
    static String[] getHTMLHeader() throws IOException {
        String scriptType = "\"text/javascript\"";
        String plotlyPath = "\"file:///Users/gbelanger/dev/js/plotly.js-master/dist/plotly.js\"";
        String chartset = "\"utf-8\"";
        String id = "\"plotly-div\"";
        String classType = "\"graph\"";
        String[] headerHTML = new String[] {
            "<!-- Produced by: "+ classname +" -->",
            "<!-- Date: "+new Date() +" -->",
            "<!-- Author: G. Belanger - ESA/ESAC" +" -->",
            "",
            "<!DOCTYPE html>",
            "<head>",
            "<script type="+scriptType+" src="+plotlyPath+" charset="+chartset+"></script>",
            "</head>",
            "<body>",
            "<!-- Plotly chart will be drawn inside this div -->",
            "<div class="+classType+" id="+id+"></div>",
            "<script>",
        };
        return headerHTML;
    }

    static String[] getHTMLFooter() throws IOException {
        String[] footerHTML = new String[] {
            "</script>",
            "</body>",
            "</html>",
        };
        return footerHTML;
    }

    // JavaScript
    
    // NEED TO HAVE TWO PANELS TO DISPLAY RATES/COUNTS IN ONE AND DIST TO POINTING AXIS IN THE OTHER
    // DON'T KNOW HOW TO DO THIS YET
    // CAN'T TEST THE FILE BECAUSE THE OTHER OBJECTS AREN'T SET UP TO READ CODEDMASK YET
    // NEED TO WAIT FOR NEW STRUCTURE TO COME INTO PLAY
    
    static String[] makeJSCodedMaskHeader(ITimeSeries ts, String type, String filename, String outputDataFilename) throws IOException {
        
        String Name = outputDataFilename.substring(outputDataFilename.lastIndexOf("/")+1, outputDataFilename.lastIndexOf(".")) ;
        String id = "\"plotly-div\"";
        double start = ts.tStart()-0.5;
        double stop = ts.tStop()+0.5;
        String plotType = "'scatter'";
        String var = "\"Counts\"";
        String title = "\""+Name+"\""; //+filename+"\"";
        String shape = "\"hvh\"";       // hvh for stepped line, linear for normal line
        String lineColor = "\"default\"";       // Makes the line blue
        String sampLineColor = "\"rgb(150,150,150)\"";
        String errorlineColor = "\"default\"";
        String yAxisTitle = "\"Intensity (cts)\"";
        double binWidth = 0;
        try { binWidth = ts.binWidth(); }
        catch ( TimeSeriesException e ) { }
        if ( binWidth != 0 ) {
            yAxisTitle = "\"Intensity (cts per "+decimals.format(binWidth)+" s)\"";
        }
        String xAxisTitle = getXAxisTitle(ts);
	String xValuesName = JSHeaderBlocks.BIN_CENTRES;
	String yValuesName = JSHeaderBlocks.BIN_HEIGHTS;
        String xAngleValuesName = JSHeaderBlocks.BIN_CENTRES;
        String yAngleValuesName = "'DistToAxis'";
        String xerror_start = "/**";
        String xerror_end = "*/";
        String yerror_start = "/**";
        String yerror_end = "*/";
        String xerrors = "'xerrors'";              // NEED TO LOOK AT WHAT THESE ARE
        String yerrors = "'yerrors'";     // NEED TO LOOK AT WHAT THESE ARE
        
        // Default is counts but here are some changes for rates
        if (type.equals("rates_cm")||type.equals("rates_cm_all")){
            plotType = JSHeaderBlocks.SCATTER_PLOT;
            var = JSHeaderBlocks.RATES;
            shape = JSHeaderBlocks.LINE_STEP;
            yAxisTitle =  Y_AXIS_TITLE_RATES;
            yValuesName = JSHeaderBlocks.RATES;
            xerror_start = "/**";
            xerror_end = "*/";
            yerror_start = "";
            yerror_end = "";
            yerrors = JSHeaderBlocks.ERRORS_ON_RATES;
        }
        String font = "'Times New Roman, Times, serif'";
        // Font Options: 'Arial, sans-serif' | 'Balto, sans-serif' | 'Courier New, monospace' | 'Droid Sans, sans-serif' | 'Droid Serif, serif' | 'Droid Sans Mono, sans-serif' | 'Georgia, serif' | 'Gravitas One, cursive' | 'Old Standard TT, serif' | 'Open Sans, sans-serif' or ('') | 'PT Sans Narrow, sans-serif' | 'Raleway, sans-serif' | 'Times New Roman, Times, serif'
	String mirror = "'all'";                // makes a box with ticks around the graph
        String axisColor = JSHeaderBlocks.RGB_BLACK;
        String zerolinecolor = JSHeaderBlocks.RGB_GREY;
        String ticks = "'inside'";              // draws ticks inside axis line",
        String tickmode = "\"auto\"";           // intervals ticks occur
        String plot_bgcolor = JSHeaderBlocks.RGB_WHITE;
        String titleColor = JSHeaderBlocks.RGB_BLACK;
        String paper_bgcolor = JSHeaderBlocks.RGB_WHITE;       // sets color of paper where its drawn",
	String margins = JSHeaderBlocks.MARGINS;
        String[] headerJS = new String[] {
            "Plotly.d3.tsv('"+outputDataFilename+"', function(rows){",
            " var trace = {",
            " name: "+var+",",
            " x: rows.map(function(row){          // set the x-data",
            "  return row["+xValuesName+"];",
            " }),",
            " y: rows.map(function(row){          // set the y-data",
            "  return row["+yValuesName+"];",
            " }),",
            " xaxis: \'x1\',",
            " yaxis: \'y1\',",
            " type: "+plotType+",                    // set the chart type",
            " mode: 'lines',                      // connect points with lines",
            " marker: {",
            "  opacity: 0,",
            " },",
            " line: {                             // set the width and color of the line.",
            "  shape: "+shape+",  // This determines at what point the step occurs - before the point (vh), after the point (hv), inbetween (hvh), simple line (linear)",
            "  width: 1,",
            "  color: "+lineColor+",",
            " },",
            "",
	    
            xerror_start+",",
            " error_x: {",
            "  array: rows.map(function(row){    // set the height of the error bars",
            "   return row["+xerrors+"];",
            "  }),",
            "  thickness: 0.5,               // set the thickness of the error bars",
            "  width: 0,",
            "  color: "+errorlineColor,
            " }",
            xerror_end+",",
            yerror_start+",",
            " error_y: {",
            "  array: rows.map(function(row){    // set the height of the error bars",
            "   return row["+yerrors+"];",
            "  }),",
            "  thickness: 0.5,                // set the thickness of the error bars",
            "  width: 0,",
            "  color: "+errorlineColor+",",
            " }",
            yerror_end+",",           
            "};",
            "",
	    
            "var angles = {",
            " name: \"Distance To Pointing Axis\",",
            " x: rows.map(function(row){          // set the x-data",
            "  return row["+xAngleValuesName+"];",
            " }),",
            " y: rows.map(function(row){          // set the y-data",
            "  return row["+yAngleValuesName+"];",
            " }),",
            " xaxis: \'x1\',",
            " yaxis: \'y2\',",
            " type: "+plotType+",                    // set the chart type",
            " mode: 'lines',                      // connect points with lines",
            " marker: {",
            "  opacity: 0,",
            " },",
            " line: {                             // set the width and color of the line.",
            "  shape: "+shape+",  // This determines at what point the step occurs - before the point (vh), after the point (hv), inbetween (hvh), simple line (linear)",
            "  width: 1,",
            "  color: "+sampLineColor+",",
            " },",
            "};",
            "",
            
            "var layout = {",
            " font: {family: "+font+"},",
            " yaxis: {",
            "  title: "+yAxisTitle+",       // set the y axis title",
            "  showgrid: true,",
            "  showline: true,",
            "  zeroline: true,",
            "  zerolinewidth: 0.5,",
            "  zerolinecolor: "+zerolinecolor+",",
            "  mirror: "+mirror+",",
            "  color: "+axisColor+",",
            "  ticks: "+ticks+",                  // draws ticks inside axis line",
            "  tickmode:"+tickmode+",",
            "  domain: [0.35, 1],",
            " },",
            " xaxis: {",
            "  title: "+xAxisTitle+",",
            "  domain: ["+start+","+stop+"],",
            "  showgrid: false,                 // remove the x-axis grid lines",
            "  showline: true,",
            "  zeroline: true,",
            "  zerolinewidth: 0.5,",
            "  zerolinecolor: "+zerolinecolor+",",
            "  mirror: "+mirror+",",
            "  color: "+axisColor+",",
            "  ticks: "+ticks+",                  // draws ticks inside axis line",
            "  tickmode: "+tickmode+",",
            "  domain: [0, 1],",
            "  anchor: 'y2',",
            " },",
            " margin: "+JSHeaderBlocks.MARGINS+",",
            " plot_bgcolor: "+plot_bgcolor+",",
            " title: "+title+",          // Sets the title text",
            " titlefont: { color: "+titleColor+" },  // Sets the title color",
            " autosize: true,",
            " paper_bgcolor: "+paper_bgcolor+",  // sets color of paper where its drawn",
            "",
            
            " yaxis2: {domain: [0, 0.30],",
            "  showgrid: true,                 // remove the x-axis grid lines",
            "  showline: true,",
            "  zeroline: true,",
            "  zerolinewidth: 0.5,",
            "  zerolinecolor: "+JSHeaderBlocks.RGB_GREY+",",
            "  mirror: "+mirror+",",
            "  color: "+JSHeaderBlocks.RGB_BLACK+",",
            "  ticks: "+ticks+",                  // draws ticks inside axis line",
            " },",
            "};",

            "",
            "window.onresize = function(){Plotly.Plots.resize(document.getElementById("+id+"));};",
            "Plotly.plot("+id+", [trace, angles], layout, {scrollZoom: true});",
            "});",
        };
        return headerJS;
    }


    
}
