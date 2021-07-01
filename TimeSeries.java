package gb.esac.timeseries;

import java.io.IOException;
import java.util.Arrays;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import org.apache.log4j.Logger;
import gb.esac.tools.BasicStats;
import gb.esac.tools.DataUtils;

/**
 *
 * <code>TimeSeries</code> is a representation of a basic time series object. 
 * The implemantion of all methods except <code>ITimeSeries.livetime()</code> and <code>ITimeSeries.exposureOnTarget()</code>
 * are found in <code>AbstractTimeSeries</code>.
 *
 * @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>, ESA/ESAC, SRE-O, Villanueva de la Canada (Madrid), Spain
 * @version 1.0, 2016 August
 * @see AbstractTimeSeries
 * 
 */

public class TimeSeries extends AbstractTimeSeries implements ITimeSeries {
    
    private static Logger logger = Logger.getLogger(TimeSeries.class);
    
    TimeSeries() {
    	super();
    }

    TimeSeries(TimeSeries ts) {
    	super(ts);
    }
    
    TimeSeries(double tStart, double[] binEdges, double[] counts) {
    	super(tStart, binEdges, counts);
    }
    
    TimeSeries(double tStart, double[] binEdges, double[] rates, double[] errorsOnRates) {
        super(tStart, binEdges, rates, errorsOnRates);
    }

    //  Abstract methods in AbstractTimeSeries that requires implementation in each sub-class
    public double livetime() {
    	return this.sumOfBinWidths();
    }
    public double exposureOnTarget() {
    	return this.sumOfBinWidths();
    }
    
}
