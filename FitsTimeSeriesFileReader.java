package gb.esac.timeseries;


import gb.esac.binner.BinningUtils;
import gb.esac.eventlist.EventList;
import gb.esac.eventlist.EventListException;
import gb.esac.eventlist.FitsEventFileReader;
import gb.esac.tools.Converter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.BufferedDataInputStream;
import org.apache.log4j.Logger;

/**
 * Class <code>FitsTimeSeriesFileReader</code> reads a light curve file in FITS format.
 * The input file must contain an HDU names RATE or EVENTS.
 *
 * If the input file contains an HDU named EVENTS, then it is treated as an event file.
 * In this case, the TIME column is interpreted as arrival times.
 * The TimeSeries will have adjacent bins of equal widths.
 *
 * If the input file contains an HDU named RATE, then it is treated as a time series.
 * In this case, the TIME column is interpreted as bin centres. The method looks for a column named RATE,
 * to define the rates, a column named ERROR to define the errors on the rates, and for a TIMEDEL keyword 
 * in the header. If there is a TIMEDEL keyword, then the binEdges are defined. 
 * If there is no such keyword, the method looks for a column named TIMEDEL to define the binwidths. 
 * If there is no TIMEDEL column either, then the method throws a TimeSeriesException 
 * because there is no binning information.
 *
 * The method tries to read all FITS data columns (TIME, RATE, ERROR, and TIMEDEL) first as double[],
 * and if this fails (ClassCastException) then in float[]. If this also fails, then it throws a TimeSeriesException.
 *
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @version 1.0 (June 2010, ESAC)
 */
public class FitsTimeSeriesFileReader implements ITimeSeriesFileReader {

    private static Logger logger  = Logger.getLogger(FitsTimeSeriesFileReader.class);
    private static double zero = 1e-13;

    public TimeSeries readTimeSeriesFile(String filename) throws  TimeSeriesFileException, TimeSeriesException, IOException {
	//  Open the FITS file and retrieve all the HDUs
	BasicHDU[] hdus = getAllHDUs(filename);
	//  Read the data
	BinaryTableHDU hdu = null;
	try {
	    String hduName = "EVENTS";
	    hdu = getBinaryTableHDU(hdus, hduName);
	    logger.info("There is an EVENTS HDU: file is an event file");
	    try {
		EventList evlist = (new FitsEventFileReader()).readEventFile(filename);
		return TimeSeriesMaker.makeTimeSeries(evlist);
	    }
	    catch ( Exception e ) {
		throw new FitsTimeSeriesFileException("Problem reading event list", e);
	    }
	}
	catch ( NullPointerException e ) {
	    hdu = findTimeSeriesHDU(hdus);
	    String[] colNames = getRateAndErrorColNames(hdu);
	    double[] rates = getDoubleDataCol(hdu, colNames[0]);
	    double[] errorsOnRates = getDoubleDataCol(hdu, colNames[1]);
	    double[] halfBinWidths = getHalfBinWidths(hdu);
	    double[] binCentres = getBinCentres(getTimeCol(hdu), halfBinWidths);
	    return TimeSeriesMaker.makeTimeSeries(binCentres, halfBinWidths, rates, errorsOnRates);
	}
    }


    /**
     * This method tries to create a FITS object from a file and read its HDUs.
     *
     * @param filename a <code>String</code> value
     * @return a <code>BasicHDU[]</code> value
     * @exception IOException is thrown if the file does not exist or something like that.
     * @exception FitsTimeSeriesFileFormatException is thrown if the file is not in FITS format 
     * @exception FitsTimeSeriesFileException is thrown if the file is FITS but there is a problem reading
     * @exception TimeSeriesFileException is thrown if the file is empty or corrupted
     */

    private BasicHDU[] getAllHDUs(String filename) throws IOException, TimeSeriesFileException {
	Fits fitsFile = null;
	try {
	    File file = new File(filename);
	    boolean isGzipped = isGzipped(file);
	    BufferedDataInputStream dis = new BufferedDataInputStream(new FileInputStream(file));
	    fitsFile = new Fits(dis, isGzipped);
	    BasicHDU[] hdus = fitsFile.read();
	    return hdus;
	}
	catch ( TruncatedFileException e ) {
	    throw new TimeSeriesFileException("File is either empty or corrupted", e);
	}
	catch ( FitsException e ) {
	    if ( e.getMessage().contains("Not FITS format") ) {
		throw new FitsTimeSeriesFileFormatException("File format is not FITS");
	    }
	    else {
		throw new FitsTimeSeriesFileException("File format is FITS, but there was a problem reading the file", e);
	    }
	}
	finally {
	    fitsFile.getStream().close();
	}
    }

    private boolean isGzipped(File file) throws IOException {
	InputStream in = new FileInputStream(file);
	int magic1 = in.read();
	int magic2 = in.read();
	in.close();
	return (magic1 == 0037 && magic2 == 0213);
    }

    private double[] getDoubleDataCol(BinaryTableHDU hdu, String colName) throws FitsTimeSeriesFileException, TimeSeriesFileException {
	try {
	    double[] col = null;
	    try {
		col = (double[]) hdu.getColumn(colName); 
		logger.info("Returning double data column "+colName);
		return col;
	    }
	    catch ( ClassCastException e ) {
		try {
		    float[] fltData = (float[]) hdu.getColumn(colName);
		    logger.info("Returning double (converted from float) data column "+colName);		    
		    col = Converter.float2double(fltData);
		    return col;
		}
		catch ( ClassCastException e2 ) {
		    try {
			int[] intData = (int[]) hdu.getColumn(colName);
			logger.info("Returning double (converted from int) data column "+colName);
			col = Converter.int2double(intData);
			return col; 
		    }
		    catch ( ClassCastException e3 ) {
			throw new ClassCastException(colName+" format not double[], float[], or int[].\n"+e3);
		    }
		}
	    }
	}
	catch ( FitsException e ) {
	    throw new TimeSeriesFileException("Problem in getDoubleDataCol", e);
	}
    }
    
    private String[] getRateAndErrorColNames(BinaryTableHDU hdu) throws NullPointerException {
	String timeColName = null;
	String rateColName = null;
	String errorColName = null;
	int nCols = hdu.getNCols();
	for ( int j=0; j < nCols; j++ ) {
	    String colName = hdu.getColumnName(j); 
	    if ( colName.equals("TIME") ) {
		timeColName = colName;
	    }
	    else if ( colName.startsWith("RATE") ) {
		rateColName = colName;
	    }
	    else if ( colName.startsWith("ERROR") ) {
		errorColName = colName;
	    }
	    else {}
	}	
	if ( timeColName == null || rateColName == null || errorColName == null ) {
	    throw new NullPointerException("Cannot find columns named TIME, RATE* and ERROR*");
	}
	else {
	    logger.info("Rate and Error column names are: "+rateColName+" and "+errorColName);
	    return new String[] {rateColName, errorColName};
	}
    }
    
    private BinaryTableHDU getBinaryTableHDU(BasicHDU[] hdus, String hduName) throws NullPointerException {
	for ( BasicHDU h : hdus ) {
	    try { 
		BinaryTableHDU hdu = (BinaryTableHDU) h;
		String extname = h.getHeader().getStringValue("EXTNAME");
		if ( extname.equals(hduName) ) {
		    return hdu;
		}
	    }
	    catch ( ClassCastException e ) {}  //  The HDU is not a BinaryTableHDU
	}
	throw new NullPointerException("HDU array does not contain and HDU with name = "+hduName);
    }

    private double[] getBinCentres(double[] times, double[] halfBinWidths) throws TimeSeriesFileException {
	//  Since the times in FITS time series files are not the bin centre but the left edge of the bin
	//  We need to shift the time in order to get the bin centre.
	if ( times.length != halfBinWidths.length ) {
	    throw new TimeSeriesFileException("Array lengths are different (times.length != halfBinWidths.length)");
	}
	int nBins = times.length;
	double[] binCentres = new double[times.length];
	for ( int i=0; i < nBins; i++ ) {
	    binCentres[i] = times[i] + halfBinWidths[i];
	}
	logger.info("Returning bin centres");
	return binCentres;
    }

    private double[] getTimeCol(BinaryTableHDU hdu) throws TimeSeriesFileException {
	Header header = hdu.getHeader();
	double tStart = header.getDoubleValue("TSTART");
	if ( tStart == 0.0 ) {
	    tStart = header.getIntValue("TSTARTI") + header.getDoubleValue("TSTARTF");
	    if ( tStart == 0.0 ) {
		throw new FitsTimeSeriesFileFormatException("Not a FITS time series file. There is no TSTART, nor TSTARTI and TSTARTF");
	    }
	    String timeunit = header.getStringValue("TIMEUNIT");  // The TIMEUNIT keyword applies to header values
	    if ( timeunit.equalsIgnoreCase("d") ) { 	    //  Convert to seconds if necessary
		tStart *= 86400;
	    }
	}
	double[] times = getDoubleDataCol(hdu, "TIME");
	int timeColNumber = hdu.findColumn("TIME")+1;
	String timeColUnits = header.getStringValue("TUNIT"+timeColNumber);
	if ( timeColUnits.equalsIgnoreCase("d") ) {
	    for ( int i=0; i < times.length; i++ ) {
		times[i] *= 86400;
	    }
	}
	if ( times[0] == 0.0 ) {
	    for ( int i=0; i < times.length; i++ ) {
		times[i] += tStart;
	    }
	}
	return times;
    }

    private double[] getHalfBinWidths(BinaryTableHDU hdu) throws TimeSeriesFileException {
	//  Check for TIMEDEL keyword or column
	Header header = hdu.getHeader();
	double timedel = header.getDoubleValue("TIMEDEL");
	double[] dt = null;
	double[] halfBinWidths = null;
	if ( timedel == 0.0 ) {
	    int colNumber = hdu.findColumn("TIMEDEL");
	    if ( colNumber == -1 ) {
		throw new FitsTimeSeriesFileException("There is no TIMEDEL keyword in header and no TIMEDEL column in file: No binning information");
	    }
	    else {
		logger.info("Using TIMEDEL column");
		dt = getDoubleDataCol(hdu, "TIMEDEL");
		halfBinWidths = new double[dt.length];
		int timedelColNumber = hdu.findColumn("TIMEDEL")+1;
		String timedelColUnits = header.getStringValue("TUNIT"+timedelColNumber);
		if ( timedelColUnits.equalsIgnoreCase("d") ) {
		    for ( int i=0; i < dt.length; i++ ) {
			dt[i] *= 86400;
			halfBinWidths[i] = dt[i]/2;
		    }
		}
	    }
	}
	else {
	    String timeunit = header.getStringValue("TIMEUNIT");  // The TIMEUNIT keyword applies to header values
	    if ( timeunit == null ) {
		HeaderCard card = header.findCard("TIMEDEL");
		String comment = card.getComment();
		if ( comment.contains("[d]") ) {
		    timedel *= 86400;
		}
	    }
	    else {
		if ( timeunit.equalsIgnoreCase("d") ) {
		    timedel *= 86400;
		}
	    }
	    logger.info("TIMEDEL (binwidth) = "+timedel);
	    int nRows = hdu.getNRows();
	    halfBinWidths = new double[nRows];
	    for ( int i=0; i < nRows; i++ ) {
		halfBinWidths[i] = timedel/2;
	    }
	}
	logger.info("Returning half bin widths");
	return halfBinWidths;
    }

    private BinaryTableHDU findTimeSeriesHDU(BasicHDU[] hdus) throws FitsTimeSeriesFileException {
	int k=1;
	try {
	    while ( true ) {
		try {
		    BinaryTableHDU hdu = (BinaryTableHDU) hdus[k];
		    String extname = hdu.getHeader().getStringValue("EXTNAME");
		    if ( extname.equals("RATE") ) return hdu;
		    String hduclas1 = hdu.getHeader().getStringValue("HDUCLAS1");
		    if ( hduclas1 != null && hduclas1.equals("LIGHTCURVE") )  return hdu;
		    else {
			try {
			    String[] rateAndErrorColNames = getRateAndErrorColNames(hdu);
			    return hdu;
			}
			catch ( NullPointerException e ) {}  //  The HDU does not contain the three cols TIME, RATE* and ERROR*
		    }
		}
		catch ( ClassCastException e ) {}  //  the HDU is not a BinaryTableHDU
		k++;  //  Check the next HDU
	    }
	}
	catch ( ArrayIndexOutOfBoundsException e ) {
	    throw new FitsTimeSeriesFileException("Not a FITS time series file. There is no HDU that contains TIME, RATE* and ERROR* columns");
	}	    
    }

}

