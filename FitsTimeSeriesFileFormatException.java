package gb.esac.timeseries;


public class FitsTimeSeriesFileFormatException extends TimeSeriesFileFormatException {

    public FitsTimeSeriesFileFormatException() {
        super();
    }

    public FitsTimeSeriesFileFormatException (String msg) {
        super(msg);
    }

    public FitsTimeSeriesFileFormatException (String msg, Exception e) {
        super(msg, e);
    }

}
