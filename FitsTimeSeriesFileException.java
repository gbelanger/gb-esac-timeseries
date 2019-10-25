package gb.esac.timeseries;


public class FitsTimeSeriesFileException extends TimeSeriesFileException {

    public FitsTimeSeriesFileException() {
        super();
    }

    public FitsTimeSeriesFileException (String msg) {
        super(msg);
    }

    public FitsTimeSeriesFileException (String msg, Exception e) {
        super(msg, e);
    }

}
