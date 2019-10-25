package gb.esac.timeseries;

public class TimeSeriesFileException extends TimeSeriesException {

    public TimeSeriesFileException() {
        super();
    }

    public TimeSeriesFileException (String msg) {
        super(msg);
    }

    public TimeSeriesFileException (String msg, Exception e) {
        super(msg, e);
    }

}
