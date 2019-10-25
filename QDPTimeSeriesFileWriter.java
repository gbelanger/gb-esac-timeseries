package gb.esac.timeseries;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import gb.esac.binner.BinningUtils;
import gb.esac.io.AsciiDataFileWriter;
import org.apache.log4j.Logger;

/**
 * Class <code>QDPTimeSeriesFileWriter</code> writes <code>AbstractTimeSeries</code> objects.
 * The headers are made using the factory class <code>QDPHeaderMaker</code>.
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @author Harry Holt
 * @version 1.0, 2017 April, ESAC (last modified)
 */


final class QDPTimeSeriesFileWriter implements ITimeSeriesFileWriter {
    
    private static Logger logger = Logger.getLogger(QDPTimeSeriesFileWriter.class);
    private static String classname = (QDPTimeSeriesFileWriter.class).getCanonicalName();

    ////
    //    Counts
    ////
    public static void writeCounts(ITimeSeries ts, String filename) throws IOException {
        AsciiDataFileWriter writer = new AsciiDataFileWriter(filename);
	String[] header = QDPHeaderMaker.getCountsHeader(ts, filename, classname);
	if ( ts instanceof CodedMaskTimeSeries ) {
	    writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getBinHeights(), ((CodedMaskTimeSeries)ts).getDistToPointingAxis());
	}
	else {
	    writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getBinHeights());
	}
        logger.info(ts.getClass().getCanonicalName()+" in counts written to "+filename);
    }

    public static void writeCounts(ITimeSeries ts, double[] function, String filename) throws IOException, TimeSeriesFileFormatException {
        AsciiDataFileWriter writer = new AsciiDataFileWriter(filename);
	String[] header = QDPHeaderMaker.getCountsHeader(ts, filename, classname);
	try {
	    writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getBinHeights(), function);
	}
	catch ( ArrayIndexOutOfBoundsException e ) {
	    throw new TimeSeriesFileFormatException("Error writing TimeSeriesFile: ts.nBins() != function.length");	    
	}
        logger.info(ts.getClass().getCanonicalName()+" in counts (with function) written to "+filename);	
    }
    
    public static void writeCountsAndSampling(ITimeSeries ts, String filename) throws IOException, TimeSeriesFileFormatException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename), bufferSize));
	String[] header = QDPHeaderMaker.getCountsHeader(ts, filename, classname);
	double[][] data = null;
	if ( ts instanceof CodedMaskTimeSeries ) {
	    data = new double[][] {ts.getBinCentres(), ts.getHalfBinWidths(), ts.getBinHeights(), ((CodedMaskTimeSeries)ts).getDistToPointingAxis()};
	}
	else {
	    data = new double[][] {ts.getBinCentres(), ts.getHalfBinWidths(), ts.getBinHeights()};
	}
	double[][] sampling = getSamplingFunction(ts);
	printToFile(pw, header, data, sampling);
	String tsClassName = ts.getClass().getCanonicalName();
        logger.info(tsClassName+" in counts and sampling function written to "+filename);
    }

    ////
    //   Rates
    ////
    public static void writeAllData(CodedMaskTimeSeries ts, String filename) throws IOException {
        String[] header = QDPHeaderMaker.getAllDataHeader(ts, filename, classname);
        AsciiDataFileWriter writer = new AsciiDataFileWriter(filename);
	writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates(), ts.getDistToPointingAxis(), ts.getRasOfPointings(), ts.getDecsOfPointings(), ts.getExposuresOnTarget(), ts.getEffectivePointingDurations());
        logger.info(ts.getClass().getCanonicalName()+" in rates (and all data) written to "+filename);
    }

    public static void writeRates(ITimeSeries ts, String filename) throws IOException {
	String[] header = QDPHeaderMaker.getRatesHeader(ts, filename, classname);
        AsciiDataFileWriter writer = new AsciiDataFileWriter(filename);
	if ( ts instanceof CodedMaskTimeSeries ) {
	    writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates(), ((CodedMaskTimeSeries)ts).getDistToPointingAxis());
	}
	else {
	    writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates());    
	}
        logger.info(ts.getClass().getCanonicalName()+" in rates written to "+filename);		
    }
    
    public static void writeRates(ITimeSeries ts, double[] function, String filename) throws IOException, TimeSeriesFileFormatException {
        AsciiDataFileWriter writer = new AsciiDataFileWriter(filename);
	String[] header = QDPHeaderMaker.getRatesHeader(ts, filename, classname);	
	try {
	    if ( ts instanceof CodedMaskTimeSeries ) {
		writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates(), ((CodedMaskTimeSeries)ts).getDistToPointingAxis(), function);    
	    }
	    else {
		writer.writeData(header, ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates(), function);
	    }
	}
	catch ( ArrayIndexOutOfBoundsException e ) {
	    throw new TimeSeriesFileFormatException("Error writing TimeSeriesFile: ts.nBins() != function.length");	    
	}
	logger.info(ts.getClass().getCanonicalName()+" in rates (with function) written to "+filename);		
    }
    
    public static void writeRatesAndSampling(ITimeSeries ts, String filename) throws IOException, TimeSeriesFileFormatException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename), bufferSize));
	String[] header = QDPHeaderMaker.getRatesHeader(ts, filename, classname);
	double[][] data = null;
	if ( ts instanceof CodedMaskTimeSeries ) {
	    data = new double[][] {ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates(), ((CodedMaskTimeSeries)ts).getDistToPointingAxis()};
	}
	else {
	    data = new double[][] {ts.getBinCentres(), ts.getHalfBinWidths(), ts.getRates(), ts.getErrorsOnRates()};
	}
	double[][] sampling = getSamplingFunction(ts);
	printToFile(pw, header, data, sampling);
	String tsClassName = ts.getClass().getCanonicalName();
        logger.info(tsClassName+" in rates and sampling function written to "+filename);

    }

    ////
    //  Private methods
    ////
    
    private static double[][] getSamplingFunction(ITimeSeries ts) {
        double[] edges = ts.getSamplingFunctionBinEdges();
        double[] centres = BinningUtils.getBinCentresFromBinEdges(edges);
        double[] halfWidths = BinningUtils.getHalfBinWidthsFromBinEdges(edges);
        double[] func = ts.getSamplingFunctionValues();
	return new double[][] {centres, halfWidths, func};
    }

    private static void printToFile(PrintWriter pw, String[] header, double[][] data, double[][] sampling) throws TimeSeriesFileFormatException {
	//  Write the header
        for ( int i=0; i < header.length; i++ ) { pw.println(header[i]); }
	//  Write the time series data
	try {
	    for ( int i=0; i < data[0].length; i++ ) {
		pw.print(data[0][i]);
		int k=1;
		while ( k < data.length ) {
		    pw.print("\t"+data[k][i]);
		    k++;
		}
		pw.println();
	    }
	}
	catch ( ArrayIndexOutOfBoundsException e ) {
	    throw new TimeSeriesFileFormatException("Error writing TimeSeriesFile: ts.nBins() != function.length");
	}
        pw.println("NO NO NO NO NO");
        //  Write the sampling function
        for ( int i=0; i < sampling[0].length; i++ ) {
            pw.println(sampling[0][i] +"\t"+ sampling[1][i] +"\t"+ (sampling[2][i]) +"\t 0.0 \t" + (sampling[2][i]));
        }
        pw.close();
    }

    
}
