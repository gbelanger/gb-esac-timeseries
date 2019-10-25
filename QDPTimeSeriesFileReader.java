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
import java.util.ArrayList;
import gb.esac.tools.StringUtils;

/**
 * Class <code>QDPTimeSeriesFileReader</code> reads a times series or Coded Mask Time Series file in ASCII-QDP format.
 * The Time Series reader is very simple an only additionally reads the tStart header in the label.
 * The Coded Mask Reader requires 5 columns to read the file, setting the columns to the arrays binCentres, dtOver2, rates, errorOnRates and distToPointingAxis.
 *
 * The TimeSeries will have bins defined by the binCentres and corresponding half-widths.
 *
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @version August 2016 (last modified)
 * @coauthor Harry Holt
 */

public class QDPTimeSeriesFileReader extends AsciiTimeSeriesFileReader {
    
    private static Logger logger = Logger.getLogger(QDPTimeSeriesFileReader.class);

    // Internal variables
    private String[] commentBlock;
    private String[] qdpHeaderBlock;

    // Time series attributes
    private String targetName;
    private double targetRA;
    private double targetDec;
    private String instrument;
    private double maxDistForFullCoding;
    private double energyRangeMin;
    private double energyRangeMax;
    private double exposureOnTarget;
    
    public ITimeSeries readTimeSeriesFile(String filename) throws TimeSeriesFileException, TimeSeriesException, BinningException, IOException  {
        AsciiDataFileReader dataFile = null;
	try {
	    dataFile = new AsciiDataFileReader(filename);
	}
	catch ( AsciiDataFileFormatException e ) {
	    throw new AsciiTimeSeriesFileException("Problem reading ASCII data file", e);
	}
	int ncols = dataFile.getNDataCols();
	if ( ncols >= 5 ) {  //   Assume it is a CodedMaskTimeSeries
	    parseHeader(dataFile.getHeader());
	    //  These columns must be defined
	    double[] binCentres = dataFile.getDblCol(0);
	    double[] dtOver2 = dataFile.getDblCol(1);
	    double[] rates = dataFile.getDblCol(2);
	    double[] errors = dataFile.getDblCol(3);
	    double[] distToPointingAxis = dataFile.getDblCol(4);    // Constructed from rasOfPointing and decsOfPointing
	    //  construct the bin edges
	    double[] binEdges = BinningUtils.getBinEdgesFromBinCentresAndHalfWidths(binCentres, dtOver2);
	    try {
		double[] rasOfPointings = dataFile.getDblCol(5);
		double[] decsOfPointings = dataFile.getDblCol(6);
		double[] exposuresOnTarget = dataFile.getDblCol(7);
		double[] effectivePointingDurations = dataFile.getDblCol(8);
		return TimeSeriesMaker.makeCodedMaskTimeSeries(targetName, targetRA, targetDec, energyRangeMin, energyRangeMax, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, rasOfPointings, decsOfPointings, exposuresOnTarget);
	    }
	    catch ( ArrayIndexOutOfBoundsException e ) {
		double[] effectivePointingDurations = new double[dtOver2.length];
		for ( int i=0; i < dtOver2.length; i++ ) {
		    effectivePointingDurations[i] = 2*dtOver2[i];
		}
		CodedMaskTimeSeries ts = TimeSeriesMaker.makeCodedMaskTimeSeries(targetName, targetRA, targetDec, energyRangeMin, energyRangeMax, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, distToPointingAxis);
		ts.setExposureOnTarget(this.exposureOnTarget);
		return ts;
	    }
	}
	else {
            throw new AsciiTimeSeriesFileException("Not an ASCII CodedMaskTimeSeries file\n CodedMaskTimeSeries requires at least 5 columns: binCentres, halfBinWidths, rates, errors, distToPointingAxis");
        }
    }

    private void parseHeader(String[] header) {
	//  Define the comment block and the header block
	ArrayList<String> commentLines = new ArrayList<String>();
	ArrayList<String> qdpHeaderLines = new ArrayList<String>();	
	for ( int i=0; i < header.length; i++ ) {
	    if ( header[i].startsWith("!") ) {
		commentLines.add(header[i]);
	    }
	    else {
		qdpHeaderLines.add(header[i]);
	    }
	}
	commentLines.trimToSize();
	qdpHeaderLines.trimToSize();

	//  Define the values of variables from the contents of the comment block
	commentBlock = new String[commentLines.size()];
	qdpHeaderBlock = new String[qdpHeaderLines.size()];
	for ( int i=0; i < commentBlock.length; i++ ) {
	    commentBlock[i] = (String) commentLines.get(i);
	}
	for ( int i=0; i < qdpHeaderBlock.length; i++ ) {
	    qdpHeaderBlock[i] = (String) qdpHeaderLines.get(i);
	}
	String[] stringsToFind = new String[] {"Target Name", "Target RA", "Target Dec", "Instrument", "Max distance for full coding", "Energy range min", "Energy range max", "Exposure on target"};
	int[] indexes = new int[stringsToFind.length];
	for ( int i=0; i < stringsToFind.length; i++ ) {
	    indexes[i] = StringUtils.findStringIndex(stringsToFind[i], commentBlock);
	}
	int k=0;
	int index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.targetName = commentBlock[index].substring(from);
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.targetRA = (new Double(commentBlock[index].substring(from))).doubleValue();
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.targetDec = (new Double(commentBlock[index].substring(from))).doubleValue();
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.instrument = commentBlock[index].substring(from);
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.maxDistForFullCoding = (new Double(commentBlock[index].substring(from))).doubleValue();
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.energyRangeMin = (new Double(commentBlock[index].substring(from))).doubleValue();
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    this.energyRangeMax = (new Double(commentBlock[index].substring(from))).doubleValue();
	}
	k++;
	index = indexes[k];
	if ( index != -1 ) {
	    int from = commentBlock[index].indexOf(": ") + 2;
	    int to = commentBlock[index].indexOf(" s");
	    this.exposureOnTarget = (new Double(commentBlock[index].substring(from, to))).doubleValue();
	}

    }	

}
