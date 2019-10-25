package gb.esac.timeseries;

public final class JSHeaderBlocks {


    
    public String[] getTraceBlock(String variableName, String xValuesName, String yValuesName, String filename_tsv, ) {

	String plotType = "'scatter'";
	String shape = "\"hvh\"";       // hvh for stepped line, linear for normal line    
	String lineColor = "\"default\"";       // Makes the line blue
	String errorlineColor = "\"default\"";

	String xerror_start = "/**";
	String xerror_end = "*/";
	String yerror_start = "/**";
	String yerror_end = "*/";
	String xerrors = "'xerrors'";     // NEED TO LOOK AT WHAT THESE ARE
	String yerrors = "'yerrors'";     // NEED TO LOOK AT WHAT THESE ARE
    
	String[] traceBlock = new String[] {
	    "Plotly.d3.tsv('"+filename_tsv+"', function(rows){",
            "var trace = {",
            "name: "+variableName+",",
            "x: rows.map(function(row){          // set the x-data",
            "return row["+xValuesName+"];",
            "}),",
            "y: rows.map(function(row){          // set the y-data",
            "return row["+yValuesName+"];",
            "}),",
            "type: "+plotType+",                    // set the chart type",
            "mode: 'lines',                      // connect points with lines",
            "marker: {",
            "opacity: 0,",
            "},",
            "line: {                             // set the width and color of the line.",
            "shape: "+shape+",  // This determines at what point the step occurs - before the point (vh), after the point (hv), inbetween (hvh), simple line (linear)",
            "width: 1,",
            "color: "+lineColor,
            "},",
            "",
            xerror_start,
            "error_x: {",
            "array: rows.map(function(row){    // set the height of the error bars",
            "return row["+xerrors+"];",
            "}),",
            "thickness: 0.5,               // set the thickness of the error bars",
            "width: 0,",
            "color: "+errorlineColor,
            "}",
            xerror_end,
            yerror_start,
            "error_y: {",
            "array: rows.map(function(row){    // set the height of the error bars",
            "return row["+yerrors+"];",
            "}),",
            "thickness: 0.5,                // set the thickness of the error bars",
            "width: 0,",
            "color: "+errorlineColor,
            "}",
            yerror_end,
            "};",
	    ""
	};
	return traceBlock;
    }
    
    public static String[] getFunctionBlock(String prefix) {
	String plotType = "'scatter'";
	String shape = "\"hvh\"";       // hvh for stepped line, linear for normal line    	
	String functionLineColor = "\"rgb(255,0,0)\"";    	
        String xValuesName = "'"+prefix+"Centres'";	
        String yValuesName = "'"+prefix"+'";	
	String[] functionBlock = new String[] {
	    "var "+prefix+" = {",
            "name: \""+prefix+"\",",
            "x: rows.map(function(row){          // set the x-data",
            "return row["+xValuesName+"];",     // Changed to read sampling column
            "}),",
            "y: rows.map(function(row){          // set the y-data",
            "return row["+yValuesName+"];",     // Changed to read sampling column
            "}),",
            "type: "+plotType+",                    // set the chart type",
            "mode: 'lines',                      // connect points with lines",
            "marker: {",
            "opacity: 0,",
            "},",
            "line: {                             // set the width and color of the line.",
            "shape: "+shape+",  // This determines at what point the step occurs - before the point (vh), after the point (hv), inbetween (hvh), simple line (linear)",
            "width: 1,",
            "color: "+functionLineColor,
            "},",
            "",
            "};",
            ""
	};
	return functionBlock;
    }

    static String[] getLayoutBlock(String xAxisTitle, String yAxisTitle, String trace) {
        String font = "'Times New Roman, Times, serif'";
        // Font Options: 'Arial, sans-serif' | 'Balto, sans-serif' | 'Courier New, monospace' | 'Droid Sans, sans-serif' | 'Droid Serif, serif' | 'Droid Sans Mono, sans-serif' | 'Georgia, serif' | 'Gravitas One, cursive' | 'Old Standard TT, serif' | 'Open Sans, sans-serif' or ('') | 'PT Sans Narrow, sans-serif' | 'Raleway, sans-serif' | 'Times New Roman, Times, serif'
        String mirror = "'all'";            // makes a box with ticks around the graph
        String axisColor = "\"rgb(0,0,0)\"";
        String zerolinecolor = "\"rgb(204, 204, 204)\"";    // makes the zeroline grey
        String ticks = "'inside'";
        String tickmode = "\"auto\"";           // intervals ticks occur
        String plot_bgcolor = "\"rgb(250,250,250)\"";
        String titleColor = "\"rgb(0,0,0)\"";
        String paper_bgcolor = "\"rgb(250,250,250)\"";
	
	String[] layoutBlock = new String[] {
	    "var layout = {",
            "font: {family: "+font+"},",
            "yaxis: {",
            "title: "+yAxisTitle+",       // set the y axis title",
            "showgrid: true,",
            "showline: true,",
            "zeroline: true,",
            "zerolinewidth: 0.5,",
            "zerolinecolor: "+zerolinecolor+",",
            "mirror: "+mirror+",",
            "color: "+axisColor+",",
            "ticks: "+ticks+",                  // draws ticks inside axis line",
            "tickmode:"+tickmode+",",
            "},",
            "xaxis: {",
            "title: "+xAxisTitle+",",
            "showgrid: false,                 // remove the x-axis grid lines",
            "showline: true,",
            "zeroline: true,",
            "zerolinewidth: 0.5,",
            "zerolinecolor: "+zerolinecolor+",",
            "mirror: "+mirror+",",
            "color: "+axisColor+",",
            "ticks: "+ticks+",                  // draws ticks inside axis line",
            "tickmode: "+tickmode+",",
            "},",
            "margin: {                  // update the left, bottom, right, top margin",
            "l: 80, b: 80, r: 80, t: 80",
            "},",
            "plot_bgcolor: "+plot_bgcolor+",",
            "title: "+title+",          // Sets the title text",
            "titlefont: { color: "+titleColor+" },  // Sets the title color",
            "autosize: true,",
            "paper_bgcolor: "+paper_bgcolor+",  // sets color of paper where its drawn",
            "",
            "};",
            "",
            "Plotly.plot("+id+", ["+trace+"], layout, {scrollZoom: true});",
            "});",
            "window.onresize = function(){Plotly.Plots.resize(document.getElementById("+id+"));};"
	};
	return layoutBlock;
    }

}
