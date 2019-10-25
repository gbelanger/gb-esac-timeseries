package gb.esac.timeseries;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import gb.esac.binner.BinningException;


public class TimeSeriesFileReader {

    private static Logger logger  = Logger.getLogger(TimeSeriesFileReader.class);
    private static ITimeSeriesFileReader[] formats = {new FitsTimeSeriesFileReader(), new QDPTimeSeriesFileReader()};  // Fits reader must come first

    static ITimeSeries readTimeSeriesFile(String filename) throws TimeSeriesFileException { 
	logger.info("Reading file "+(new File(filename)).getPath());
	Exception e = new Exception();
	for ( ITimeSeriesFileReader reader : formats ) {
	    try {
		return reader.readTimeSeriesFile(filename);
	    }
	    catch ( TimeSeriesFileException e1 ) { e = e1; }
	    catch ( TimeSeriesException e2 ) { e = e2; }
	    catch ( BinningException e3 ) { e = e3; }
	    catch ( IOException e4 ) { e = e4;}
	}
	throw new TimeSeriesFileException("Unrecognised format: not FITS or ASCII.", e);
    }
}
