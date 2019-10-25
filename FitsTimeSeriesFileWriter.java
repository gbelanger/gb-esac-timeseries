package gb.esac.timeseries;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import gb.esac.binner.BinningUtils;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.util.BufferedDataOutputStream;
import org.apache.log4j.Logger;


/**
 * Class <code>FitsTimeSeriesFileWriter</code> writes <code>ITimeSeries</code> objects in FITS format.
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @author with Harry Holt
 * @version 1.0, 2016 December, ESAC
 */

final class FitsTimeSeriesFileWriter {
    
    private static Logger logger  = Logger.getLogger(FitsTimeSeriesFileWriter.class);
    private static String classname = FitsTimeSeriesFileWriter.class.getSimpleName();
    private PrintWriter printWriter;

    //// Counts
    public static void writeCounts(ITimeSeries ts, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructCountsTable(ts);
        Header head = FitsHeaderMaker.getCountsHeader(ts, binTable, classname);
        BinaryTableHDU hdu = new BinaryTableHDU(head, binTable);
        BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
        Fits f = new Fits();
        f.addHDU(hdu);
        f.write(dos);
        logger.info(ts.getClass().getName()+" in counts written to "+filename);
    }
    
    public static void writeCounts(ITimeSeries ts, double[] function, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructCountsTable(ts);
	binTable.addColumn(function);
	Header hdr = FitsHeaderMaker.getCountsHeader(ts, binTable, classname);
	hdr.addValue("TTYPE4", "FUNCTION", "label for field 4");
	hdr.addValue("TFORM4", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT4", "counts per bin", "physical unit of the field");
        BinaryTableHDU hdu = new BinaryTableHDU(hdr, binTable);
        BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
        Fits f = new Fits();
        f.addHDU(hdu);
        f.write(dos);
        logger.info(ts.getClass().getName()+" in counts (with function) written to "+filename);
    }	
    
    public static void writeCountsAndSampling(ITimeSeries ts, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructCountsTable(ts);
	Header hdr = FitsHeaderMaker.getCountsHeader(ts, binTable, classname);
        BinaryTableHDU countsHDU = new BinaryTableHDU(hdr, binTable);
	BinaryTable samplingTable = constructSamplingTable(ts);
	Header samplingHdr = FitsHeaderMaker.getSamplingHeader(ts, samplingTable, classname);
        BinaryTableHDU samplingHDU = new BinaryTableHDU(samplingHdr, samplingTable);
	BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
	Fits f = new Fits();
	f.addHDU(countsHDU);
	f.addHDU(samplingHDU);
	f.write(dos);
	logger.info(ts.getClass().getName()+" in counts and sampling function written to "+filename);
    }

    //// Rates
    public static void writeRates(ITimeSeries ts, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructRatesTable(ts);
        Header head = FitsHeaderMaker.getRatesHeader(ts, binTable, classname);
        BinaryTableHDU hdu = new BinaryTableHDU(head, binTable);
        BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
        Fits f = new Fits();
        f.addHDU(hdu);
        f.write(dos);
        logger.info(ts.getClass().getName()+" in rates written to "+filename);	
    }

    public static void writeRates(ITimeSeries ts, double[] function, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructRatesTable(ts);
	binTable.addColumn(function);
	Header hdr = FitsHeaderMaker.getRatesHeader(ts, binTable, classname);
	hdr.addValue("TTYPE5", "FUNCTION", "label for field 4");
	hdr.addValue("TFORM5", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT5", "counts/s", "physical unit of the field");
        BinaryTableHDU hdu = new BinaryTableHDU(hdr, binTable);
        BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
        Fits f = new Fits();
        f.addHDU(hdu);
        f.write(dos);
        logger.info(ts.getClass().getName()+" in rates (with function) written to "+filename);
    }	

    public static void writeRatesAndSampling(ITimeSeries ts, String filename) throws IOException, FitsException {
	BinaryTable binTable = constructRatesTable(ts);
	Header hdr = FitsHeaderMaker.getRatesHeader(ts, binTable, classname);
        BinaryTableHDU hdu = new BinaryTableHDU(hdr, binTable);
	BinaryTable samplingTable = constructSamplingTable(ts);
	Header samplingHdr = FitsHeaderMaker.getSamplingHeader(ts, samplingTable, classname);
        BinaryTableHDU samplingHDU = new BinaryTableHDU(samplingHdr, samplingTable);
	BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
	Fits f = new Fits();
	f.addHDU(hdu);
	f.addHDU(samplingHDU);
	f.write(dos);
	logger.info(ts.getClass().getName()+" in rates and sampling function written to "+filename);
    }


    // For CodedMaskTimeSeries
    public static void writeAllData(CodedMaskTimeSeries ts, String filename) throws IOException, FitsException {
        FitsFactory.setUseAsciiTables(false);
	// Time series
	BinaryTable binTable = constructRatesTable(ts);
        Header head = FitsHeaderMaker.getRatesHeader(ts, binTable, classname);
        BinaryTableHDU hdu = new BinaryTableHDU(head, binTable);
	// Pointings
        BinaryTable pointingsTable = new BinaryTable();
	pointingsTable.addColumn(ts.getRasOfPointings());
        pointingsTable.addColumn(ts.getDecsOfPointings());
        pointingsTable.addColumn(ts.getExposuresOnTarget());
	Header pointingsHdr = FitsHeaderMaker.getPointingsHeader(ts, pointingsTable, classname);
        BinaryTableHDU pointingsHDU = new BinaryTableHDU(pointingsHdr, pointingsTable);
	//  GTI
        BinaryTable gtiTable = new BinaryTable();
	gtiTable.addColumn(ts.getBinWidths());
        gtiTable.addColumn(ts.getLiveTimeFractions());
        gtiTable.addColumn(ts.getEffectivePointingDurations());
        gtiTable.addColumn(ts.getDeadTimeFractions());
        gtiTable.addColumn(ts.getDeadTimeDurations());
        Header gtiHdr = FitsHeaderMaker.getGTIHeader(ts, gtiTable, classname);
        BinaryTableHDU gtiHDU = new BinaryTableHDU(gtiHdr, gtiTable);
	// Write
        BufferedDataOutputStream dos = new BufferedDataOutputStream(new FileOutputStream(filename));
        Fits f = new Fits();
        f.addHDU(hdu);
        f.addHDU(pointingsHDU);
	f.addHDU(gtiHDU);
        f.write(dos);
	logger.info(ts.getClass().getName()+" written to "+filename);	
    }


    /////  Private  methods
    private static BinaryTable constructCountsTable(ITimeSeries ts) throws FitsException {
        FitsFactory.setUseAsciiTables(false);
	BinaryTable binTable = new BinaryTable();
	if ( ts instanceof CodedMaskTimeSeries ) {
	    binTable.addColumn(ts.getBinCentres());
	    binTable.addColumn(ts.getBinWidths());
	    binTable.addColumn(ts.getBinHeights());
	    binTable.addColumn(((CodedMaskTimeSeries)ts).getDistToPointingAxis());
	}
	else {
	    binTable.addColumn(ts.getBinCentres());
	    binTable.addColumn(ts.getBinWidths());
	    binTable.addColumn(ts.getBinHeights());
	}
	return binTable;
    }

    private static BinaryTable constructRatesTable(ITimeSeries ts) throws FitsException {
        FitsFactory.setUseAsciiTables(false);
	BinaryTable binTable = new BinaryTable();
	if ( ts instanceof CodedMaskTimeSeries ) {
	    binTable.addColumn(ts.getBinCentres());
	    binTable.addColumn(ts.getBinWidths());
	    binTable.addColumn(ts.getRates());
	    binTable.addColumn(ts.getErrorsOnRates());
	    binTable.addColumn(((CodedMaskTimeSeries)ts).getDistToPointingAxis());	    
	}
	else {
	    binTable.addColumn(ts.getBinCentres());
	    binTable.addColumn(ts.getBinWidths());
	    binTable.addColumn(ts.getRates());
	    binTable.addColumn(ts.getErrorsOnRates());
	}
	return binTable;
    }

    private static BinaryTable constructSamplingTable(ITimeSeries ts) throws FitsException {
        FitsFactory.setUseAsciiTables(false);
        BinaryTable samplingTable = new BinaryTable();
        double[] edges = ts.getSamplingFunctionBinEdges();
        double[] centres = BinningUtils.getBinCentresFromBinEdges(edges);
        double[] binWidths = BinningUtils.getBinWidthsFromBinEdges(edges);
	double[] function = ts.getSamplingFunctionValues();
        samplingTable.addColumn(centres);
        samplingTable.addColumn(binWidths);
        samplingTable.addColumn(function);
	return samplingTable;
    }


}





















