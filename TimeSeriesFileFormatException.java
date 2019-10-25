package gb.esac.timeseries;


public class TimeSeriesFileFormatException extends TimeSeriesFileException {

    public TimeSeriesFileFormatException() {
        super();
    }

    public TimeSeriesFileFormatException (String msg) {
        super(msg);
    }

    public TimeSeriesFileFormatException (String msg, Exception e) {
        super(msg, e);
    }

}
