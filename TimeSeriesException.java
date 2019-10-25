package gb.esac.timeseries;


public class TimeSeriesException extends Exception {

    public TimeSeriesException () {
        super();
    }

    public TimeSeriesException (String msg) {
        super(msg);
    }

    public TimeSeriesException (String msg, Exception e) {
        super(msg+"\n", e);
    }
}
