package gb.esac.timeseries;

import gb.esac.binner.BinningUtils;
import gb.esac.io.AsciiDataFileWriter;
import gb.esac.tools.BasicStats;
import gb.esac.tools.MinMax;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import org.apache.log4j.Logger;
import java.util.Date;
import java.lang.*;
import cern.colt.list.*;
import nom.tam.util.ArrayFuncs;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Class <code>JSTimeSeriesFileWriter</code> writes <code>ITimeSeries</code> objects in FITS format.
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @author Harry Holt
 * @version 1.0, 2017 February, ESAC
 *
 * Mar 2017 - GB completed rewriting and restructuring of this class (and JSHeaderMaker)
 * Dec 2016 - GB started working on rewriting to conform to the interfaces in this package.
 * July 2016 - Harry Holt created this file. 
 *
 * It uses both HTML and JavaScript to produce graphs that can be viewed in a browser using plotly.
 * The data currently needs to be written in tsv format but this could be changed.
 * Note the difference between the sampling output files for JS and QDP: 
 * - For the QDP writer, the sampling is produced after a NO NO NO line in the data file. 
 * - For the JS writer, however, the sampling function is just added as additional columns with different headers.
 */
final class JSTimeSeriesFileWriter implements ITimeSeriesFileWriter {
    
    private static Logger logger = Logger.getLogger(JSTimeSeriesFileWriter.class);
    private static String classname = (JSTimeSeriesFileWriter.class).getCanonicalName();
    private static int bufferSize = 256000;

    // Column names
    private static final String COL_NAMES_COUNTS = "BinCentres \t HalfBinWidths \t BinHeights";
    private static final String COL_NAMES_COUNTS_FUNC = "BinCentres \t HalfBinWidths \t BinHeights \t FuncCentres \t Function";
    private static final String COL_NAMES_COUNTS_SAMP = "BinCentres \t HalfBinWidths \t BinHeights \t SampCentres \t SampHalfBinWidths \t SampFunc";

    private static final String COL_NAMES_RATES = "BinCentres \t HalfBinWidths \t Rates \t ErrorsOnRates";
    private static final String COL_NAMES_RATES_FUN = "BinCentres \t HalfBinWidths \t Rates \t ErrorsOnRates";
    private static final String COL_NAMES_RATES_SAMP = "BinCentres \t HalfBinWidths \t Rates \t ErrorsOnRates";

    private static final String COL_NAMES_COUNTS_CODED_MASK = "BinCentres \t HalfBinWidths \t BinHeights \t DistToPointingAxis";
    private static final String COL_NAMES_RATES_CODED_MASK = "BinCentres \t HalfBinWidths \t Rates \t ErrorsOnRates \t DistToPointingAxis";
    private static final String COL_NAMES_ALLDATA_CODED_MASK = "BinCentres \t HalfBinWidths \t Rates \t ErrorsOnRates \t DistToPointingAxis \t RasOfPointing \t DecsOfPointing \t ExposuresOnTarget \t EffectivePointingDurations";    

//"BinCentres" +"\t"+ "HalfBinWidths" +"\t"+ "Rates" +"\t"+ "ErrorsOnRates" +"\t"+ "DistToAxis" +"\t"+ "UnModBinCentres" +"\t"+ "UnModHalfBinWidths" +"\t"+ "UnModRates" +"\t"+ "UnModErrorsOnRates" +"\t"+ "UnModDistToAxis" +"\t"+ "RA" +"\t"+ "DEC" +"\t"+ "ExposureOnTarget" +"\t"+ "EffectivePointingDur" +"\t"+ "ONTIMEFRAC" +"\t"+ "DEADTIME" +"\t"+ "DEADTFRAC"
    
    //// Counts
    public static void writeCounts(ITimeSeries ts, String filename) throws IOException {
	String name = filename.substring(0, filename.lastIndexOf("."));
	String filename_tsv = name+".tsv";
	String filename_html = name+".html";
	// Write the data file in TSV format
	double[][] dataTable = constructCountsDataTable(ts);
	writeDataFile(dataTable, COL_NAMES_COUNTS, filename_tsv);
	// Get the HTML/JS headers and footers
        String[] headerHTML = JSHeaderMaker.getHTMLHeader();
        String[] headerJS = JSHeaderMaker.getCountsHeader(ts, filename_tsv);
        String[] footerHTML = JSHeaderMaker.getHTMLFooter();
	String[] html = (String[]) ArrayUtils.addAll(headerHTML, headerJS, footerHTML);
	// Print all HTML to file
        int bufferSize =256000;
        PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename_html), bufferSize));
        for ( int i=0; i < html.length; i++ )  printWriter.println(headerHTML[i]);
        printWriter.close();
	logger.info(ts.getClass().getCanonicalName()+" in counts written to "+filename);
    }

    
    // public static void writeCounts(ITimeSeries ts, double[] function, String filename) throws IOException {
    // 	if ( ts.nBins() != function.length ) {
    // 	    throw new TimeSeriesFileException("Array lengths are different: ts.nBins != function.length");
    // 	}
    // 	double[][] dataTable = constructCountsDataTable(ts, function);
    // 	writeJSCountsWithFunction(dataTable, COL_NAMES_COUNTS_FUNC, filename);
    //     logger.info(ts.getClass().getCanonicalName()+" in counts (with function) written to "+filename);
    // }
    
    // public static void writeCountsAndSampling(ITimeSeries ts, String filename) throws IOException {
    // 	double[][] dataTable = constructCountsAndSamplingDataTable(ts);
    // 	writeJSCountsWithSampling(dataTable, COL_NAMES_COUNTS_SAMP, filename);
    //     logger.info(ts.getClass().getCanonicalName()+" in counts and sampling function written to "+filename);
        
    // }
    
    //// Rates
    // public static void writeRates ( ITimeSeries ts, String filename ) throws IOException {
    // 	double[][] dataTable = constructRatesDataTable(ts);
    // 	writeJSRates(dataTable, filename);
    //     logger.info(ts.getClass().getCanonicalName()+" in rates written to "+filename);
    // }
    
    // public static void writeRates ( ITimeSeries ts, double[] function, String filename ) throws IOException {
    // 	double[][] dataTable = constructRatesDataTable(ts, function);
    // 	writeJSRatesWithFunction(dataTable, filename);
    //     logger.info(ts.getClass().getCanonicalName()+" in rates (with function) written to "+filename);
    // }
    
    // public static void writeRatesAndSampling ( ITimeSeries ts, String filename ) throws IOException {
    // 	double[][] dataTable = constructRatesAndSamplingDataTable(ts);
    // 	writeJSRatesWithSampling(dataTable, filename);	
    //     logger.info(ts.getClass().getCanonicalName()+" in rates and sampling function written to "+filename);
    // }
    
    // For CodedMaskTimeSeries
    // public static void writeAllData ( CodedMaskTimeSeries ts, String filename ) throws IOException {
    // 	String tsClassName = ts.getClass().getCanonicalName();
    // 	String Path = filename.substring(0, filename.lastIndexOf("/")+1);
    // 	String Name = filename.substring(filename.lastIndexOf("/")+1, filename.lastIndexOf(".")) ;
    // 	String filename_counts = Path + Name +"_counts.tsv";
    // 	String filename_rates = Path + Name +"_rates.tsv";
	
    // 	String[] headerCounts = JSHeaderMaker.getAllHeader(ts, "counts");
    // 	String[] headerRates = JSHeaderMaker.getAllHeader(ts, "rates");
    // 	writeCountsAllData(ts, headerCounts, filename_counts);
    // 	writeRatesAllData(ts, headerRates, filename_rates);
    // 	writeJSCodedMaskFile(ts, "counts_cm", filename_counts);
    // 	writeJSCodedMaskFile(ts, "rates_cm", filename_rates);
    // 	logger.info(tsClassName+" in counts (with all data) written to "+filename_counts);
    // 	logger.info(tsClassName+" in rates (with all data) written to "+filename_counts);
    // }
    

    /////  Private  methods

    //  Construct data table
    private static double[][] constructCountsDataTable(ITimeSeries ts) {
	double[][] modBins = getModifiedBinning(ts);
	double[] modBinCentres = modBins[0];
	double[] modHalfBinWidths = modBins[1];
	double[] modBinHeights = getModifiedIntensities(ts, ts.getBinHeights());
	return new double[][] {modBinCentres, modHalfBinWidths, modBinHeights};
    }

    private static double[][] constructCountsDataTable(ITimeSeries ts, double[] function) {
	double[][] modBins = getModifiedBinning(ts);
	double[] modBinCentres = modBins[0];
	double[] modHalfBinWidths = modBins[1];
	double[] modBinHeights = getModifiedIntensities(ts, ts.getBinHeights());
	double[] modFunction = getModifiedIntensities(ts, function);
	return new double[][] {modBinCentres, modHalfBinWidths, modBinHeights, modFunction};
    }

    // private static double[][] constructCountsAndSamplingDataTable(ITimeSeries ts) {
    // 	double[][] modBins = getModifiedBinning(ts);
    // 	double[] modBinCentres = modBins[0];
    // 	double[] modHalfBinWidths = modBins[1];
    // 	double[] modBinHeights = getModifiedIntensities(ts, ts.getBinHeights());
    // 	double[] modSampling = getModifiedSampling(ts);
    // 	return new double[][] {modBinCentres, modHalfBinWidths, modBinHeights, modSampling};
    // }

    // Write
    private static void writeDataFile(double[][] dataTable, String colNames, String filename) throws IOException {
	PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename), bufferSize));
	pw.println(colNames);
	// Get dimensions
	int[] dims = ArrayFuncs.getDimensions(dataTable);
	int nCols = dims[0];
	int nRows = dims[1];
	// Print to file
	for ( int row=0; row < nRows; row++ ) {
	    for ( int col=0; col < nCols; col++ ) {
		if ( col < (nCols-1) ) {
		    //System.out.println("row="+row+" col="+col);
		    pw.print(dataTable[col][row]+"\t");
		} 
		else {
		    pw.println(dataTable[col][row]);
		}
	    }
	}
        pw.close();
    }

    
    
//     //// All Data (only for CodedMaskTimeSeries)
//     public static void writeCountsAllData(ITimeSeries ts, String[] header, String filename) throws IOException {
//         PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename), bufferSize));
//         for ( int i=0; i < header.length; i++ )  pw.println(header[i]);
        
//         // Getting the modified arrays
//         double[][] modArrays = getModifiedArrays(ts, "counts");
//         double[] modBinCentres =  modArrays[0];
//         double[] modHalfBinWidths = modArrays[1];
//         double[] modBinHeights = modArrays[2];
//         double[] modDistToAxis = modArrays[4];      // Index 3 is for errors on rates (which isn't needed here)
//         // Additional information we need to store in the file but won't be plotted
//         double[] c1 = ts.getBinCentres();
//         double[] c2 = ts.getHalfBinWidths();
//         double[] c3 = ts.getBinHeights();
//         double[] c4 = ts.getRates();
//         double[] c5 = ts.getErrorsOnRates();
//         double[] c6 = ((CodedMaskTimeSeries)ts).getDistToPointingAxis();
//         double[] c7 = ((CodedMaskTimeSeries)ts).getRasOfPointings();
//         double[] c8 = ((CodedMaskTimeSeries)ts).getDecsOfPointings();
//         double[] c9 = ((CodedMaskTimeSeries)ts).getExposuresOnTarget();
//         double[] c10 = ((CodedMaskTimeSeries)ts).getEffectivePointingDurations();
//         double[] c11 = ((CodedMaskTimeSeries)ts).getLiveTimeFractions();
//         double[] c12 = ((CodedMaskTimeSeries)ts).getDeadTimeDurations();
//         double[] c13 = ((CodedMaskTimeSeries)ts).getDeadTimeFractions();
//         int rewriteBins = getRewriteBins(ts, "counts");
//         int[] lengthsMod = new int[] {modBinCentres.length, modHalfBinWidths.length, modBinHeights.length};
//         int[] lengths = new int[] {c1.length, c2.length, c3.length, c4.length, c5.length, c6.length, c7.length, c8.length, c9.length, c10.length, c11.length, c12.length, c13.length};
        
//         if ( ts instanceof CodedMaskTimeSeries ) {
//             lengthsMod = new int[] {modBinCentres.length, modHalfBinWidths.length, modBinHeights.length, modDistToAxis.length};
            
//         }
//         //int rewriteBins = (new Double(MinMax.getMin(lengths))).intValue();
//         double var = BasicStats.getVariance(lengthsMod);
//         if ( var != 0 ) {
//             logger.warn("input column data of different lengths. Using min.");
//         }
//         double varAll = BasicStats.getVariance(lengths);
//         if ( var != 0 ) {
//             logger.warn("input column data of different lengths. Using min.");
//         }
//         int totalBins = (new Double(MinMax.getMin(lengthsMod))).intValue();
//         int totalBinsAll = (new Double(MinMax.getMin(lengths))).intValue();
//         int[] lengthsBins = new int[] {totalBins, totalBinsAll};
//         int printBins = (new Double(MinMax.getMin(lengthsBins))).intValue();
        
//         for ( int i=0; i < printBins; i++ ) {
//             pw.println(modBinCentres[i] +"\t"+ modHalfBinWidths[i] +"\t"+ modBinHeights[i] +"\t"+ modDistToAxis[i] +"\t"+c1[i] +"\t"+ c2[i] +"\t"+ c3[i] +"\t"+ c4[i] +"\t"+ c5[i] +"\t"+ c6[i] +"\t"+ c7[i] +"\t"+ c8[i] +"\t"+ c9[i] +"\t"+ c10[i] +"\t"+ c11[i] +"\t"+ c12[i] +"\t"+ c13[i]);
//         }
//         int extraBins = (new Double(MinMax.getMax(lengths))).intValue();
//         for ( int i=printBins; i < extraBins; i++ ) {
//             if (totalBins > totalBinsAll) {
//                 pw.println(modBinCentres[i] +"\t"+ modHalfBinWidths[i] +"\t"+  modBinHeights[i] +"\t"+ modDistToAxis[i]  +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN"+"\t"+ "NaN" );
//             } else if ( totalBins < totalBinsAll) {
//                 pw.println("NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ c1[i] +"\t"+ c2[i] +"\t"+ c3[i] +"\t"+ c4[i] +"\t"+ c5[i] +"\t"+ c6[i] +"\t"+ c7[i] +"\t"+ c8[i] +"\t"+ c9[i] +"\t"+ c10[i] +"\t"+ c11[i] +"\t"+ c12[i] +"\t"+ c13[i]);
//             }
//         }
//         pw.close();
//     }

    
//     public static void writeRatesAllData(ITimeSeries ts, String[] header, String filename) throws IOException {
//         PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename), bufferSize));
//         for ( int i=0; i < header.length; i++ )  pw.println(header[i]);
        
//         // Getting the modified arrays
//         double[][] modArrays = getModifiedArrays(ts, "rates");
//         double[] modBinCentres =  modArrays[0];
//         double[] modHalfBinWidths = modArrays[1];
//         double[] modRates = modArrays[2];
//         double[] modErrorsOnRates = modArrays[3];
//         double[] modDistToAxis = modArrays[4];      // Index 3 is for errors on rates (which isn't needed here)
//         // Additional information we need to store in the file but won't be plotted
//         double[] c1 = ts.getBinCentres();
//         double[] c2 = ts.getHalfBinWidths();
//         double[] c3 = ts.getBinHeights();
//         double[] c4 = ts.getRates();
//         double[] c5 = ts.getErrorsOnRates();
//         double[] c6 = ((CodedMaskTimeSeries)ts).getDistToPointingAxis();
//         double[] c7 = ((CodedMaskTimeSeries)ts).getRasOfPointings();
//         double[] c8 = ((CodedMaskTimeSeries)ts).getDecsOfPointings();
//         double[] c9 = ((CodedMaskTimeSeries)ts).getExposuresOnTarget();
//         double[] c10 = ((CodedMaskTimeSeries)ts).getEffectivePointingDurations();
//         double[] c11 = ((CodedMaskTimeSeries)ts).getLiveTimeFractions();
//         double[] c12 = ((CodedMaskTimeSeries)ts).getDeadTimeDurations();
//         double[] c13 = ((CodedMaskTimeSeries)ts).getDeadTimeFractions();
//         int rewriteBins = getRewriteBins(ts, "rates");
//         int[] lengthsMod = new int[] {modBinCentres.length, modHalfBinWidths.length, modRates.length, modErrorsOnRates.length};
//         int[] lengths = new int[] {c1.length, c2.length, c3.length, c4.length, c5.length, c6.length, c7.length, c8.length, c9.length, c10.length, c11.length, c12.length, c13.length};
        
//         if ( ts instanceof CodedMaskTimeSeries ) {
//             lengthsMod = new int[] {modBinCentres.length, modHalfBinWidths.length, modRates.length, modErrorsOnRates.length, modDistToAxis.length};
            
//         }
//         //int rewriteBins = (new Double(MinMax.getMin(lengths))).intValue();
//         double var = BasicStats.getVariance(lengthsMod);
//         if ( var != 0 ) {
//             logger.warn("input column data of different lengths. Using min.");
//         }
//         double varAll = BasicStats.getVariance(lengths);
//         if ( var != 0 ) {
//             logger.warn("input column data of different lengths. Using min.");
//         }
//         int totalBins = (new Double(MinMax.getMin(lengthsMod))).intValue();
//         int totalBinsAll = (new Double(MinMax.getMin(lengths))).intValue();
//         int[] lengthsBins = new int[] {totalBins, totalBinsAll};
//         int printBins = (new Double(MinMax.getMin(lengthsBins))).intValue();
        
//         for ( int i=0; i < printBins; i++ ) {
//             pw.println(modBinCentres[i] +"\t"+ modHalfBinWidths[i] +"\t"+ modRates[i] +"\t"+ modErrorsOnRates[i] +"\t"+ modDistToAxis[i] +"\t"+c1[i] +"\t"+ c2[i] +"\t"+ c3[i] +"\t"+ c4[i] +"\t"+ c5[i] +"\t"+ c6[i] +"\t"+ c7[i] +"\t"+ c8[i] +"\t"+ c9[i] +"\t"+ c10[i] +"\t"+ c11[i] +"\t"+ c12[i] +"\t"+ c13[i]);
//         }
//         int extraBins = (new Double(MinMax.getMax(lengths))).intValue();
//         for ( int i=printBins; i < extraBins; i++ ) {
//             if (totalBins > totalBinsAll) {
//                 pw.println(modBinCentres[i] +"\t"+ modHalfBinWidths[i] +"\t"+  modRates[i] +"\t"+ modErrorsOnRates[i]  +"\t"+ modDistToAxis[i]  +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN"+"\t"+ "NaN" );
//             } else if ( totalBins < totalBinsAll) {
//                 pw.println("NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN" +"\t"+ "NaN"+"\t"+ c1[i] +"\t"+ c2[i] +"\t"+ c3[i] +"\t"+ c4[i] +"\t"+ c5[i] +"\t"+ c6[i] +"\t"+ c7[i] +"\t"+ c8[i] +"\t"+ c9[i] +"\t"+ c10[i] +"\t"+ c11[i] +"\t"+ c12[i] +"\t"+ c13[i]);
//             }
//         }
//         pw.close();
//     }


    private static double[][] getModifiedBinning(ITimeSeries ts) {
	double[] binCentres = ts.getBinCentres();
	double[] halfBinWidths = ts.getHalfBinWidths();
	DoubleArrayList modBinCentresList = new DoubleArrayList();
	DoubleArrayList modHalfBinWidthsList = new DoubleArrayList();
	modBinCentresList.add(binCentres[0]);
	for ( int i=1; i < ts.nBins()-1; i++ ) {
	    int k = 0;
	    modBinCentresList.add(binCentres[i]);
	    modHalfBinWidthsList.add(halfBinWidths[i]);
            double gap = ( binCentres[i+1] - binCentres[i] ) - ( halfBinWidths[i+1] + halfBinWidths[i] );	    
            if (gap != 0) {
                modBinCentresList.add(binCentres[i-k] + 2*halfBinWidths[i-k]);
                modHalfBinWidthsList.add(halfBinWidths[i-k]);
                modBinCentresList.add(binCentres[i+1-k] - 2*halfBinWidths[i+1-k]);
                modHalfBinWidthsList.add(halfBinWidths[i+1-k]);
	    }
	    k++;
	}
	modBinCentresList.add(binCentres[ts.nBins()-1]);
	modHalfBinWidthsList.add(halfBinWidths[ts.nBins()-1]);
	modBinCentresList.trimToSize();
	modHalfBinWidthsList.trimToSize();
	return new double[][] {modBinCentresList.elements(), modHalfBinWidthsList.elements()};
    }

    private static double[] getModifiedIntensities(ITimeSeries ts, double[] data) {
	double[] binCentres = ts.getBinCentres();
	double[] halfBinWidths = ts.getHalfBinWidths();
	DoubleArrayList modData = new DoubleArrayList();
	modData.add(data[0]);
	for ( int i=1; i < data.length-1; i++ ) {
	    int k = 0;
	    modData.add(data[i]);
            double gap = ( binCentres[i+1] - binCentres[i] ) - ( halfBinWidths[i+1] + halfBinWidths[i] );	    
            if (gap != 0) {
		modData.add(0.0);
		modData.add(0.0);
	    }
	    k++;
	}
	modData.add(data[data.length-1]);
	modData.trimToSize();
	return modData.elements();
    }
    

    
// //// Write the HTML and JS File
    
//     private static void writeJSSamplingFile (ITimeSeries ts, String type, String filename) throws IOException {
//         String outputScriptFilename = getOutputScriptFilename (filename);
//         String[] headerHTML = JSHeaderMaker.getHTMLHeader();
//         String[] headerJS = JSHeaderMaker.getJSSamplingHeader(ts, type, filename, filename);
//         String[] footerHTML = JSHeaderMaker.getHTMLFooter();
//         int bufferSize =256000;
//         PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputScriptFilename), bufferSize));
//         for ( int i=0; i < headerHTML.length; i++ )  printWriter.println(headerHTML[i]);
//         for ( int i=0; i < headerJS.length; i++ )  printWriter.println(headerJS[i]);
//         for ( int i=0; i < footerHTML.length; i++ )  printWriter.println(footerHTML[i]);
//         printWriter.close();
//     }
    
//     private static void writeJSFunctionFile (ITimeSeries ts, String type, String filename) throws IOException {
//         String outputScriptFilename = getOutputScriptFilename (filename);
//         String[] headerHTML = JSHeaderMaker.getHTMLHeader();
//         String[] headerJS = JSHeaderMaker.getJSFunctionHeader(ts, type, filename, filename);
//         String[] footerHTML = JSHeaderMaker.getHTMLFooter();
//         int bufferSize =256000;
//         PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputScriptFilename), bufferSize));
//         for ( int i=0; i < headerHTML.length; i++ )  printWriter.println(headerHTML[i]);
//         for ( int i=0; i < headerJS.length; i++ )  printWriter.println(headerJS[i]);
//         for ( int i=0; i < footerHTML.length; i++ )  printWriter.println(footerHTML[i]);
//         printWriter.close();
//     }
    
//     private static void writeJSCodedMaskFile (ITimeSeries ts, String type, String filename) throws IOException {
//         String outputScriptFilename = getOutputScriptFilename (filename);
//         String[] headerHTML = JSHeaderMaker.getHTMLHeader();
//         String[] headerJS = JSHeaderMaker.getJSCodedMaskHeader(ts, type, filename, filename);
//         String[] footerHTML = JSHeaderMaker.getHTMLFooter();
//         int bufferSize =256000;
//         PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputScriptFilename), bufferSize));
//         for ( int i=0; i < headerHTML.length; i++ )  printWriter.println(headerHTML[i]);
//         for ( int i=0; i < headerJS.length; i++ )  printWriter.println(headerJS[i]);
//         for ( int i=0; i < footerHTML.length; i++ )  printWriter.println(footerHTML[i]);
//         printWriter.close();
//     }
    
    
    
}
    
    
