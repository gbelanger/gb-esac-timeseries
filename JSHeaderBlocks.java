package gb.esac.timeseries;

public final class JSHeaderBlocks {

    //  Static Strings
    static final String COUNTS = "\"Counts\"";
    static final String BIN_CENTRES = "'BinCentres'";    
    static final String BIN_HEIGHTS = "'BinHeights'";
    static final String RATES = "\"Rates\"";
    static final String ERRORS_ON_RATES = "'ErrorsOnRates'";
    static final String X_ERRORS = "'BinWidth'";
    static final String SCATTER_PLOT = "'scatter'";
    static final String MARGINS = "{ l: 80, b: 80, r: 80, t: 80 },";
    // Step can occur either:
    //// before (vh),
    //// after (hv), or
    //// between points (hvh)
    static final String LINE_STEP = "\"hvh\"";
    static final String LINE_NORMAL = "\"linear\"";
    static final String RGB_BLACK = "\"rgb(0,0,0)\"";
    static final String RGB_WHITE = "\"rgb(250,250,250)\"";
    static final String RGB_GREY = "\"rgb(204, 204, 204)\"";
    static final String RGB_BLUE = "\"rgb(255,0,0)\"";

    
    //  Trace block

    ////  Counts
    static String[] getCountsTraceBlock(String filename_tsv) {
	String variableName = COUNTS;
	String xValuesName = BIN_CENTRES;
	String yValuesName = BIN_HEIGHTS;
	String yErrorsName = "";
	String xErrorStart = "/**";  //  using this to comment out that part of the HTML block
	String xErrorEnd = "*/";     //  using this to comment out that part of the HTML block
	String yErrorStart = "/**";  //  using this to comment out that part of the HTML block
	String yErrorEnd = "*/";     //  using this to comment out that part of the HTML block
	return getTraceBlock(variableName, xValuesName, yValuesName, yErrorsName, xErrorStart, xErrorEnd, yErrorStart, yErrorEnd, filename_tsv);
    }

    ////  Rates
    static String[] getRatesTraceBlock(String filename_tsv) {
	String variableName = RATES;
	String xValuesName = BIN_CENTRES;
	String yValuesName = RATES;
	String yErrorsName = ERRORS_ON_RATES;
	String xErrorStart = "/**";  //  using this to comment out that part of the HTML block
	String xErrorEnd = "*/";     //  using this to comment out that part of the HTML block
	String yErrorStart = "";
	String yErrorEnd = "";
	return getTraceBlock(variableName, xValuesName, yValuesName, yErrorsName, xErrorStart, xErrorEnd, yErrorStart, yErrorEnd, filename_tsv);
    }

    ////  Generic (private)
    private static String[] getTraceBlock(String variableName, String xValuesName, String yValuesName, String yErrorsName, String xErrorStart, String xErrorEnd, String yErrorStart, String yErrorEnd, String filename_tsv) {
	String xErrorsName = X_ERRORS;
	String plotType = SCATTER_PLOT;
	String shape = LINE_STEP;
	String lineColor = "\"default\"";   // Makes the line blue
	String errorlineColor = "\"default\"";
	// Define block	
	String[] traceBlock = new String[] {
	    "Plotly.d3.tsv('"+filename_tsv+"', function(rows){",
            "  var trace = {",
            "    name: "+variableName+",",
            "    x: rows.map(function(row){    // set the x-data",
            "      return row["+xValuesName+"];",
            "    }),",
            "    y: rows.map(function(row){    // set the y-data",
            "      return row["+yValuesName+"];",
            "    }),",
            "  type: "+plotType+",           // set the chart type",
            "  mode: 'lines',                // connect points with lines",
            "  marker: {",
            "    opacity: 0,",
            "  },",
            "  line: {                       // set the width and color of the line.",
            "    shape: "+shape+",             // step occurs - before (vh), after (hv), inbetween points (hvh); simple line (linear)",
            "    width: 1,",
            "    color: "+lineColor+",",
            "  },",
            "",
            xErrorStart,
            "  error_x: {",
            "    array: rows.map(function(row){ // set the height of the error bars",
            "      return row["+xErrorsName+"];",
            "    }),",
            "    thickness: 0.5,                // set the thickness of the error bars",
            "    width: 0,",
            "    color: "+errorlineColor,
            "  }",
            xErrorEnd,
            yErrorStart,
            "  error_y: {",
            "    array: rows.map(function(row){ // set the height of the error bars",
            "      return row["+yErrorsName+"];",
            "    }),",
            "    thickness: 0.5,                // set the thickness of the error bars",
            "    width: 0,",
            "    color: "+errorlineColor+",",
            "  }",
            yErrorEnd,
            "};",
	    ""
	};
	return traceBlock;
    }

    //  Layout block

    static String[] getLayoutBlock(String xAxisTitle, String yAxisTitle, String trace) {
        String font = "'Times New Roman, Times, serif'";
        ////  Font Options:
	// 'Arial, sans-serif'
	// 'Balto, sans-serif'
	// 'Courier New, monospace'
	// 'Droid Sans, sans-serif'
	// 'Droid Serif, serif'
	// 'Droid Sans Mono, sans-serif'
	// 'Georgia, serif'
	// 'Gravitas One, cursive'
	// 'Old Standard TT, serif'
	// 'Open Sans, sans-serif'
	// ('')
	// 'PT Sans Narrow, sans-serif'
	// 'Raleway, sans-serif'
	// 'Times New Roman, Times, serif'
        //
	String mirror = "'all'";                // makes a box with ticks around the graph
        String axisColor = RGB_BLACK;
        String zerolinecolor = RGB_GREY;
        String ticks = "'inside'";              // draws ticks inside axis line",
        String tickmode = "\"auto\"";           // intervals ticks occur
        String plot_bgcolor = RGB_WHITE;
        String titleColor = RGB_BLACK;
        String paper_bgcolor = RGB_WHITE;       // sets color of paper where its drawn",
	String margins = MARGINS;
	String title = "";
	String id = "myPlot";
	String[] layoutBlock = new String[] {
	    "var layout = {",
            "  font: {family: "+font+"},",
            "  yaxis: {",
            "    title: "+yAxisTitle+",",
            "    showgrid: true,",
            "    showline: true,",
            "    zeroline: true,",
            "    zerolinewidth: 0.5,",
            "    zerolinecolor: "+zerolinecolor+",",
            "    mirror: "+mirror+",",
            "    color: "+axisColor+",",
            "    ticks: "+ticks+",",
            "    tickmode:"+tickmode+",",
            "  },",
            "  xaxis: {",
            "    title: "+xAxisTitle+",",
            "    showgrid: false,",
            "    showline: true,",
            "    zeroline: true,",
            "    zerolinewidth: 0.5,",
            "    zerolinecolor: "+zerolinecolor+",",
            "    mirror: "+mirror+",",
            "    color: "+axisColor+",",
            "    ticks: "+ticks+",",
            "    tickmode: "+tickmode+",",
            "  },",
            "  margin: "+margins+",",
	    "  plot_bgcolor: "+plot_bgcolor+",",
	    "  title: "+title+",",
	    "  titlefont: { color: "+titleColor+" },",
	    "  autosize: true,",
	    "  paper_bgcolor: "+paper_bgcolor+",",
	    "",
	    "};",
	    "",
	    "Plotly.plot("+id+", ["+trace+"], layout, {scrollZoom: true});",
	    "});",
	    "window.onresize = function(){Plotly.Plots.resize(document.getElementById("+id+"));};"
	};
	return layoutBlock;
    }

    public static String[] getFunctionBlock(String prefix) {
	String plotType = SCATTER_PLOT;
	String shape = LINE_STEP;
	String functionLineColor = RGB_BLUE;
        String xValuesName = "'"+prefix+"Centres'";	
        String yValuesName = "'"+prefix+"'";	
	String[] functionBlock = new String[] {
	    "var "+prefix+" = {",
            "  name: \""+prefix+"\",",
            "  x: rows.map(function(row){          // set the x-data",
            "    return row["+xValuesName+"];",     // Changed to read sampling column
            "  }),",
            "  y: rows.map(function(row){          // set the y-data",
            "    return row["+yValuesName+"];",     // Changed to read sampling column
            "  }),",
            "  type: "+plotType+",",
            "  mode: 'lines',",
            "  marker: { opacity: 0 },",
            "  line: {",
            "    shape: "+shape+",",
            "    width: 1,",
            "    color: "+functionLineColor,
            "  },",
            "",
            "};",
            ""
	};
	return functionBlock;
    }
    
    

}
