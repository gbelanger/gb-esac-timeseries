package gb.esac.timeseries;


public class AsciiTimeSeriesFileException extends TimeSeriesFileException {

    public AsciiTimeSeriesFileException() {
        super();
    }

    public AsciiTimeSeriesFileException (String msg) {
        super(msg);
    }

    public AsciiTimeSeriesFileException (String msg, Exception e) {
        super(msg, e);
    }

}
