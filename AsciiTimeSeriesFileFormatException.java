package gb.esac.timeseries;


public class AsciiTimeSeriesFileFormatException extends TimeSeriesFileFormatException {

    public AsciiTimeSeriesFileFormatException() {
        super();
    }

    public AsciiTimeSeriesFileFormatException (String msg) {
        super(msg);
    }

    public AsciiTimeSeriesFileFormatException (String msg, Exception e) {
        super(msg, e);
    }

}
