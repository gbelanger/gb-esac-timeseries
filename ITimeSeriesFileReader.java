package gb.esac.timeseries;

import gb.esac.binner.BinningException;
import java.io.IOException;


public interface ITimeSeriesFileReader {

    ITimeSeries readTimeSeriesFile(String filename) throws TimeSeriesFileException, TimeSeriesException, BinningException, IOException ;

}
