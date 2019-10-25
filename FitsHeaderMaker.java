package gb.esac.timeseries;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.Header;


/**
 * Class <code>FitsHeaderMaker</code> constructs headers for <code>FitsTimeSeriesFileWriter</code>
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>
 * @author Harry Holt
 * @version 1.0, 2016 August, ESAC
 */


final class FitsHeaderMaker {
    
    private static Logger logger = Logger.getLogger(FitsHeaderMaker.class);
    private static String classname = (FitsHeaderMaker.class).getName();
    private static String sdf_format = "yyyy-MM-dd'T'hh:mm:ss";
    private static SimpleDateFormat sdf = new SimpleDateFormat(sdf_format);
    
    static Header getCountsHeader(ITimeSeries ts, BinaryTable binTable, String producedBy) throws IOException, FitsException {
	logger.info("Making header (counts)");
	Header hdr = BinaryTableHDU.manufactureHeader(binTable);
        hdr.addValue("TFORM1", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT1", "s", "physical unit of the field");
        hdr.addValue("TTYPE1", "TIME", "label for field 1");
        hdr.addValue("TFORM2", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT2", "s", "physical unit of the field");
        hdr.addValue("TTYPE2", "TIMEDEL", "label for field 2");
	hdr.addValue("TFORM3", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT3", "counts per bin", "physical unit of the field");
	hdr.addValue("TTYPE3", "COUNTS", "label for field 3");
	if ( ts instanceof CodedMaskTimeSeries ) {	
            hdr.addValue("TFORM4", "1D", "data format of field: 8-byte DOUBLE");
            hdr.addValue("TUNIT4", "degree", "physical unit of the field"); // Confirm units
            hdr.addValue("TTYPE4", "DISTTOAXIS", "label for field 4");  // Decide Name
	}	
        String extName = "COUNTS";
        hdr.addValue("EXTNAME", extName, "name of this binary extension table");
	hdr = addCommonHeaderInfo(ts, hdr);
        hdr.addValue("AUTHOR", producedBy, "Program name that produced this file");
        return hdr;
    }

    static Header getRatesHeader(ITimeSeries ts, BinaryTable binTable, String producedBy) throws IOException, FitsException {
	logger.info("Making header (rates)");
        Header hdr = BinaryTableHDU.manufactureHeader(binTable);        
        hdr.addValue("TFORM1", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT1", "s", "physical unit of the field");
        hdr.addValue("TTYPE1", "TIME", "label for field 1");
        hdr.addValue("TFORM2", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT2", "s", "physical unit of the field");
        hdr.addValue("TTYPE2", "TIMEDEL", "label for field 2");
	hdr.addValue("TFORM3", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT3", "counts/s", "physical unit of the field");
	hdr.addValue("TTYPE3", "RATES", "label for field 3");
	hdr.addValue("TFORM4", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT4", "counts/s", "physical unit of the field");
	hdr.addValue("TTYPE4", "ERRORS", "label for field 4");
	if ( ts instanceof CodedMaskTimeSeries ) {	
            hdr.addValue("TFORM5", "1D", "data format of field: 8-byte DOUBLE");
            hdr.addValue("TUNIT5", "degree", "physical unit of the field"); // Confirm units
            hdr.addValue("TTYPE5", "DISTTOAXIS", "label for field 5");  // Decide Name
	}
        String extName = "RATES";
        hdr.addValue("EXTNAME", extName, "name of this binary extension table");
	hdr = addCommonHeaderInfo(ts, hdr);
        hdr.addValue("AUTHOR", producedBy, "Program name that produced this file");
        return hdr;
    }

    static Header getSamplingHeader(ITimeSeries ts, BinaryTable samplingTable, String producedBy) throws IOException, FitsException { 
	logger.info("Making header (sampling function)");
        Header hdr = BinaryTableHDU.manufactureHeader(samplingTable);
        hdr.addValue("TFORM1", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT1", "s", "physical unit of the field");
        hdr.addValue("TTYPE1", "TIME", "label for field 1");
        hdr.addValue("TFORM2", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT2", "s", "physical unit of the field");
        hdr.addValue("TTYPE2", "TIMEDEL", "label for field 2");
        hdr.addValue("TFORM3", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT3", "n/a", "physical unit of the field");
        hdr.addValue("TTYPE3", "FUNCTION", "label for field 3");
	String extName = "SAMPLING";
        hdr.addValue("EXTNAME", extName, "name of this binary extension table");
	hdr = addCommonHeaderInfo(ts, hdr);
        hdr.addValue("AUTHOR", producedBy, "Program name that produced this file");
        return hdr;
    }

    static Header getPointingsHeader(CodedMaskTimeSeries ts, BinaryTable pointingsTable, String producedBy) throws IOException, FitsException {
	logger.info("Making header (pointings)");
        Header hdr = BinaryTableHDU.manufactureHeader(pointingsTable);        
        hdr.addValue("TFORM1", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT1", "degrees", "physical unit of the field");
	hdr.addValue("TTYPE1", "RA", "label for field 1"); // rasOfPointings
        hdr.addValue("TFORM2", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT2", "degrees", "physical unit of the field");
        hdr.addValue("TTYPE2", "DEC", "label for field 2"); // decsOfPointings
        hdr.addValue("TFORM3", "1D", "data format of field: 8-byte DOUBLE");
        hdr.addValue("TUNIT3", "s", "physical unit of the field");
        hdr.addValue("TTYPE3", "EXPOSURE", "label for field 3"); // exposureOnTarget
        String extName = "POINTINGS";        
        hdr.addValue("EXTNAME", extName, "name of this binary extension table");
	hdr = addCommonHeaderInfo(ts, hdr);
        hdr.addValue("AUTHOR", producedBy, "Program name that produced this file");
        return hdr;
    }
    
    static Header getGTIHeader(CodedMaskTimeSeries ts, BinaryTable gtiTable, String producedBy) throws IOException, FitsException {
	logger.info("Making header (GTI)");
	Header hdr = BinaryTableHDU.manufactureHeader(gtiTable);        
	hdr.addValue("TFORM1", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT1", "s", "physical unit of the field");
	hdr.addValue("TTYPE1", "ONTIME", "label for field 1"); // binWidths
	hdr.addValue("TFORM2", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT2", "n/a", "physical unit of the field");
	hdr.addValue("TTYPE2", "ONTIMEFRAC", "label for field 2"); // liveTimeFractions
	hdr.addValue("TFORM3", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT3", "s", "physical unit of the field");
	hdr.addValue("TTYPE3", "LIVETIME", "label for field 3"); // effectivePointingsDurations
	hdr.addValue("TFORM4", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT4", "n/a", "physical unit of the field");
	hdr.addValue("TTYPE4", "DEADTFRAC", "label for field 3"); // deadTimeFractions	
	hdr.addValue("TFORM5", "1D", "data format of field: 8-byte DOUBLE");
	hdr.addValue("TUNIT5", "s", "physical unit of the field");
	hdr.addValue("TTYPE5", "DEADTIME", "label for field 3"); // deadTimeDurations	
	String extName = "GTI";
	hdr.addValue("EXTNAME", extName, "name of this binary extension table");
	hdr = addCommonHeaderInfo(ts, hdr);
        hdr.addValue("AUTHOR", producedBy, "Program name that produced this file");
        return hdr;
    }

    
    //  Private 
    private static Header addCommonHeaderInfo(ITimeSeries ts, Header hdr) throws HeaderCardException {
        hdr.addValue("TIMVERS", "OGIP/93-003", "OGIP memo number for file format");
        hdr.addValue("CONTENT", "Time Series", "file contains time series data");
        hdr.addValue("ORIGIN", "ESA, ESAC", "origin of the file");
        hdr.addValue("DATE", sdf.format(new Date()), "file creation date ("+sdf_format+")");
        hdr.addValue("TELESCOP", ts.telescope(), "telescope (mission) name");
        hdr.addValue("INSTRUME", ts.instrument(), "instrument used for observation");
        hdr.addValue("E_MIN", ts.energyRangeMin(), "low energy for channel keV");
        hdr.addValue("E_MAX", ts.energyRangeMax(), "high energy for channel keV");
        hdr.addValue("EUNIT", "keV", "energy unit");
        hdr.addValue("MJDREF", "", "MJD for reference file");
        hdr.addValue("TIMESYS", "MJD", "The time system is MJD");
        hdr.addValue("TIMEUNIT", ts.timeUnit(), "unit for TSTARTI/F and TSTOPI?F");
        hdr.addValue("EQUINOX", "2.0000E+03", "equinox of celestial coord. system");
        hdr.addValue("RADECSYS", "FK5", "FK5 coordinate system used");
        hdr.addValue("OBJECT", ts.targetName(),"common object name ");
        hdr.addValue("RA", ts.targetRA(), "target RA in degrees");
        hdr.addValue("DEC", ts.targetDec(), "target Dec in degrees");
        hdr.addValue("DATE-OBS", ts.dateObs(), "date of first obsvn (yyyy-MM-dd)");
        hdr.addValue("TIME-OBS", ts.timeObs(), "time of first obsvn (hh:mm:ss)");
        hdr.addValue("DATE-END", ts.dateEnd(), "date of last obsvn (yyyy-MM-dd)");
        hdr.addValue("TIME-END", ts.timeEnd(), "date of first obsvn (hh:mm:ss)");
        hdr.addValue("TSTART", ts.tStart(), "obeservation start time");
        hdr.addValue("TSTOP", ts.tStop(), "observation stop time");
        hdr.addValue("TIMEZERO", ts.tStart(), "zerotime to calculate t(n) event or bin");
        hdr.addValue("TIERRELA", ts.relTimeError(), "relative time error");
        hdr.addValue("TIERABSO", ts.absTimeError(), "absolute time error");
        hdr.addValue("CLOCKCOR", "NO", "if time corrected to UT");
        hdr.addValue("TIMEREF", "F", "barycentric correction applied to times");
        hdr.addValue("TASSIGN", "F", "time is assigned");
	hdr.addValue("ONTIME", ts.sumOfBinWidths(), "sum of pointing durations");
	hdr.addValue("LIVETIME", ts.livetime(), "deadtime-corrected sum of pointing durations");
	hdr.addValue("EXPOSURE", ts.exposureOnTarget(), "effective exposure on target");
        hdr.addValue("BACKAPP", "F", "background subtracted");
        hdr.addValue("DEADAPP", "F", "deadtime applied");
        hdr.addValue("VIGNAPP", "F", "vignetting or collimator applied");
	return hdr;
    }

}
