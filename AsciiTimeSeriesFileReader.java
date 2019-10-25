package gb.esac.timeseries;

import gb.esac.binner.BinningException;
import gb.esac.binner.BinningUtils;
import gb.esac.eventlist.AsciiEventFileException;
import gb.esac.eventlist.AsciiEventFileReader;
import gb.esac.eventlist.EventList;
import gb.esac.eventlist.EventListException;
import gb.esac.io.AsciiDataFileFormatException;
import gb.esac.io.AsciiDataFileReader;
import java.io.IOException;
import java.io.*;
import org.apache.log4j.Logger;

/**
 * Class <code>AsciiTimeSeriesFileReader</code> reads a times series file in ASCII format.
 *
 * If it contains only 1 column, then it is treated as an event file
 . * The TimeSeries will have adjacent bins of equal widths.
 *
 * If it contains 2 columns, then 
 * the first column are the binCentres and the second are the counts in each bin.
 * The TimeSeries will have adjacent bins with widths determined from the binCentres.
 *
 * If it contains 3 columns, then 
 * the first column are the binCentres, the second are the rates, and the third are the errors.
 * The TimeSeries will have adjacent bins with widths determined from the binCentres.
 *
 * If it contains 4 columns (or more), then 
 * the first column are the binCentres, the second are the halfBinWidths, the third are the rates, the third are the errors.
 * The TimeSeries will have bins defined by the binCentres and corresponding widths.
 *
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @version 1.0 (June 2010, ESAC)
 * 
 * 28 Oct 2014
 * - relaxed the constrain on the number of cols to be able to read a file with more than 4 cols but use only the data in the first 4.
 *
 */
class AsciiTimeSeriesFileReader implements ITimeSeriesFileReader {

    private static Logger logger  = Logger.getLogger(AsciiTimeSeriesFileReader.class);
    
    public ITimeSeries readTimeSeriesFile(String filename) throws TimeSeriesFileException, TimeSeriesException, BinningException, IOException  {
	AsciiDataFileReader dataFile = null;
	try {
	    dataFile = new AsciiDataFileReader(filename);
	}
	catch ( AsciiDataFileFormatException e ) {
	    throw new AsciiTimeSeriesFileException("Problem reading ASCII data file", e);
	}	
        // Read the tStart time from the label so the information is't lost when we read and re-read the files
	String[] header = dataFile.getHeader();
	String stringsToFind = "LAB X Time (s) since MJD ";
	int j=0;
	boolean found = header[j].contains(stringsToFind);
	while ( ! found ) {
	    j++;
	    found = header[j].contains(stringsToFind);
	}
	int tStartRow = j;
	String tStartStr = header[tStartRow].substring((header[tStartRow].indexOf(stringsToFind))+stringsToFind.length());;
	double tStart = (new Double(tStartStr)).doubleValue();
    
	// NOT USING TSTART YET

	// Back to reading the file
	int ncols = dataFile.getNDataCols();
	if ( ncols == 1 ) {
	    //  If there is only 1 column, I assume that it is an event list
	    try {
		return TimeSeriesMaker.makeTimeSeries(new AsciiEventFileReader().readEventFile(filename));
	    }
	    catch ( AsciiEventFileException e ) {
		throw new AsciiTimeSeriesFileException("Problem reading ASCII data file", e);
	    }
	    catch ( EventListException e ) {
		throw new AsciiTimeSeriesFileException("Problem reading ASCII event file", e);
	    }
	}
	else if ( ncols == 2 ) {
	    double[] binCentres = dataFile.getDblCol(0);
	    double[] counts = dataFile.getDblCol(1);
	    double[] binEdges = null;
	    try {
		binEdges = BinningUtils.getBinEdgesFromBinCentres(binCentres);
	    }
	    catch ( BinningException e ) {
		throw new TimeSeriesFileException("Cannot construct bin edges", e);
	    }
	    return TimeSeriesMaker.makeTimeSeries(binEdges, counts);
	}
	else if ( ncols == 3 ) {
	    double[] binCentres = dataFile.getDblCol(0);
	    double[] rates = dataFile.getDblCol(1);
	    double[] errorsOnRates = dataFile.getDblCol(2);
	    return TimeSeriesMaker.makeTimeSeries(binCentres, rates, errorsOnRates);
	}
	else if ( ncols == 4 ) {
	    double[] binCentres = dataFile.getDblCol(0);
	    double[] dtOver2 = dataFile.getDblCol(1);
	    double[] rates = dataFile.getDblCol(2);
	    double[] errorsOnRates = dataFile.getDblCol(3);
	    return TimeSeriesMaker.makeTimeSeries(binCentres, dtOver2, rates, errorsOnRates);
	}
	else {
	    throw new AsciiTimeSeriesFileException("Not an ASCII time series file.\n"
						   +"Format can be: \n   1 col = arrival times;\n   2 cols = binCentres and counts;\n   3 cols = binCentres, rates and errors;\n   4 cols (or more) = binCentres, halfBinWidths, rates, errors");
	}
	
    }
    
}
