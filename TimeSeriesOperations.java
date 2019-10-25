package gb.esac.timeseries;


import java.util.Arrays;

import cern.colt.list.DoubleArrayList;
import gb.esac.tools.DataUtils;
import gb.esac.tools.MinMax;
import org.apache.log4j.Logger;


public final class TimeSeriesOperations {

    private static Logger logger  = Logger.getLogger(TimeSeriesOperations.class);

    /**
     * Describe <code>getSegment</code> method here.
     *
     * @param lc a <code>TimeSeries</code> value
     * @param time1 a <code>double</code> value with respect to the start. Thus time1=0 is the start of the TimeSeries
     * @param time2 a <code>double</code> value with respect to the start. Thus time1=0 is the start of the TimeSeries
     * @return a <code>TimeSeries</code> value. If there are on data between time1 and time2, returns null;
     * @exception TimeSeriesException if an error occurs
     */
    public static TimeSeries getSegment(TimeSeries lc, double time1, double time2) throws TimeSeriesException {
	logger.info("Extracting segment from time "+time1+" to "+time2+" seconds.");
	//  Run some validity checks
	double duration = lc.duration();
	if ( time1 > time2 ) {
	    throw new TimeSeriesException("Segment requested ends before it begins (time1 > time2)");
	}
	if ( time1 > duration ) {
	    throw new TimeSeriesException("Segment requested starts later then the end of the TimeSeries");
	}
	if ( time2 > duration ) {
	    throw new TimeSeriesException("Segment requested ends after the end of the TimeSeries. Use getSegment(ts, startTime) instead.");
	}
	//  Get the segment
	int nBins = lc.nBins();
	double[] binCentres = lc.getBinCentres();
	double[] binWidths = lc.getBinWidths();
	double[] binEdges = lc.getBinEdges();
	DoubleArrayList binEdgesList = new DoubleArrayList();
	double[] binHeights = lc.getBinHeights();
	double[] rates = lc.getRates();
	double[] errors = lc.getErrorsOnRates();
	DoubleArrayList binHeightsList = new DoubleArrayList();
	DoubleArrayList ratesList = new DoubleArrayList();
	DoubleArrayList errorsList = new DoubleArrayList();	    
	int i=0;
	while ( time1 > binEdges[2*i+1] ) {
	    i++;
	}
	while ( i < nBins && binEdges[2*i+1] <= time2 ) {
	    binEdgesList.add(binEdges[2*i]);
	    binEdgesList.add(binEdges[2*i+1]);
	    binHeightsList.add(binHeights[i]);
	    ratesList.add(rates[i]);
	    errorsList.add(errors[i]);
	    i++;
	}
	binEdgesList.trimToSize();
	ratesList.trimToSize();
	errorsList.trimToSize();
	binHeightsList.trimToSize();
	if ( binEdgesList.size() > 2 ) {
	
	    if ( lc.errorsAreSet() ) {
		return new TimeSeries(lc.tStart()+time1, binEdgesList.elements(), ratesList.elements(), errorsList.elements());
	    }
	    else {
		return new TimeSeries(lc.tStart()+time1, binEdgesList.elements(), binHeightsList.elements());
	    }
	}
	else { return null; }
    }

    public static TimeSeries getSegment(TimeSeries ts, double tStart) throws TimeSeriesException {
	logger.info("Extracting segment beginning at time "+tStart+" seconds");
	double tEnd = ts.tStop();
	logger.debug(tEnd);
	return getSegment(ts, tStart, tEnd);
    }

    public static TimeSeries getSegment(TimeSeries ts, int firstBinIndex, int lastBinIndex) throws TimeSeriesException {
	logger.info("Extracting segment from bin index "+firstBinIndex+" to "+lastBinIndex);
	double[] binEdges = ts.getBinEdges();
	double time1 = binEdges[2*firstBinIndex];
	double time2 = binEdges[2*lastBinIndex+1];
	return getSegment(ts, time1, time2);
    }

    public static TimeSeries foldForward(TimeSeries ts, int nBinsForward) throws TimeSeriesException {
	logger.info("Folding forward by "+nBinsForward+" bins");
	double[] binEdges = ts.getBinEdges();
	double[] binWidths = ts.getBinWidths();
	double[] binHeights = ts.getBinHeights();
	double[] newBinEdges = new double[binEdges.length];
	double[] newBinHeights = new double[binHeights.length];
	double totalTimeForward = 0;
	int i=0;
	int k=nBinsForward+i;
	while ( k < ts.nBins() ) {
	    double binWidth = binWidths[k];
	    totalTimeForward += binWidth;
	    newBinEdges[2*i] = binEdges[2*k];
	    newBinEdges[2*i+1] = binEdges[2*k+1];
	    newBinHeights[i] = binHeights[k];
	    i++;
	    k=nBinsForward+i;
	}
	k=0;
	while ( i < ts.nBins() ) {
	    double binWidth = binWidths[k];
	    totalTimeForward += binWidth;
	    newBinEdges[2*i] = binEdges[2*k] + ts.duration();
	    newBinEdges[2*i+1] = binEdges[2*k+1] + ts.duration();
	    newBinHeights[i] = binHeights[k];
	    i++;
	    k++;
	}
	double newTStart = ts.tStart() + totalTimeForward;
	return new TimeSeries(newTStart, newBinEdges, newBinHeights);
    }

    public static TimeSeries foldForward(TimeSeries ts, double deltaT) throws TimeSeriesException {
	logger.info("Folding forward by "+deltaT+" seconds");	
	double[] binCentres = ts.getBinCentres();
	int i=0;
	double diff = binCentres[i] - binCentres[0];
	while ( diff < deltaT ) {
	    i++;
	    diff = binCentres[i] - binCentres[0];
	}
	int nBins = i-1;
	return foldForward(ts, nBins);
    }

    public static TimeSeries shiftTimeAxis(TimeSeries lc, double deltaT) {
	logger.info("Shifting time axis by "+deltaT+" seconds");
	double newTStart = lc.tStart() + deltaT;
	if ( lc.errorsAreSet() ) {
	    return new TimeSeries(newTStart, lc.getBinEdges(), lc.getRates(), lc.getErrorsOnRates());
	}
	else {
	    return new TimeSeries(newTStart, lc.getBinEdges(), lc.getBinHeights());
	}
    }

    public static TimeSeries shiftTimeAxisToZero(TimeSeries lc) {
	logger.info("Shifting time axis to zero");
	double newTStart = lc.getBinCentres()[0];
	if ( lc.errorsAreSet() ) {
	    return new TimeSeries(newTStart, lc.getBinEdges(), lc.getRates(), lc.getErrorsOnRates());
	}
	else {
	    return new TimeSeries(newTStart, lc.getBinEdges(), lc.getBinHeights());
	}
    }

    private static boolean seriesAreOrdered(ITimeSeries[] timeSeries) {
	double[] tStarts = new double[timeSeries.length];
	for ( int i=0; i < timeSeries.length; i++ ) {
	    tStarts[i] = timeSeries[i].tStart();
	}
	double[] sortedTStarts = Arrays.copyOf(tStarts, tStarts.length);
	Arrays.sort(sortedTStarts);
	boolean seriesAreOrdered = true;
	for ( int i=0; i < timeSeries.length; i++ ) {
	    if ( tStarts[i] != sortedTStarts[i] ) seriesAreOrdered = false;
	}
	if ( seriesAreOrdered ) {
	    logger.info("TimeSeries are ordered");
	}
	else {
	    logger.warn("TimeSeries are not ordered");
	}
	return seriesAreOrdered;
    }

    private static TimeSeries[] sort(TimeSeries[] timeSeries) {
	logger.info("Sorting TimeSeries in chronological order");
	double[] tStarts = new double[timeSeries.length];
	for ( int i=0; i < timeSeries.length; i++ ) {
	    tStarts[i] = timeSeries[i].tStart();
	}
	double[] sortedTStarts = Arrays.copyOf(tStarts, tStarts.length);
	Arrays.sort(sortedTStarts);
	logger.info("Building new array of sorted TimeSeries");
	TimeSeries[] orderedTimeSeries = new TimeSeries[timeSeries.length];
	for ( int i=0; i < timeSeries.length; i++ ) {
	    int index = DataUtils.getIndex(sortedTStarts[i], tStarts);
	    ITimeSeries ts = timeSeries[index];
	    if ( ts.errorsAreSet() ) {
		orderedTimeSeries[i] = new TimeSeries(ts.tStart(), ts.getBinEdges(), ts.getBinHeights());
	    }
	    else {
		orderedTimeSeries[i] = new TimeSeries(ts.tStart(), ts.getBinEdges(), ts.getRates(), ts.getErrorsOnRates());
	    }
	}
	logger.info("TStarts of ordered TimeSeries are:");
	for ( int i=0; i < timeSeries.length; i++ ) {
	    logger.info("   "+orderedTimeSeries[i].tStart());
	}
	return orderedTimeSeries;
    }

    public static TimeSeries combine(TimeSeries[] timeSeries) {
	logger.info("Combining array of "+timeSeries.length+" TimeSeries");
	DoubleArrayList newBinEdgesList = new DoubleArrayList();
	DoubleArrayList newRatesList = new DoubleArrayList();
	DoubleArrayList newErrorsList = new DoubleArrayList();
	//  Check if sorted in chronological order. If not then sort.
	TimeSeries[] lcArray;
	if ( ! seriesAreOrdered(timeSeries) ) {
	    lcArray = sort(timeSeries);
	}
	else {
	    lcArray = timeSeries;
	}
	// Define tStart, tStop and duration
	int nLCs = lcArray.length;
	double tstart = lcArray[0].tStart();
	double tstop = lcArray[nLCs-1].tStop();
	double duration = tstop - tstart;
	logger.info("TStart = "+tstart);
	logger.info("TStop = "+tstop);
	logger.info("Duration = "+duration);
	// Define minBinWidth
	double minBinWidth = Double.MAX_VALUE;
	for ( int i=0; i < nLCs; i++ ) {
	    try {
		minBinWidth = Math.min(minBinWidth, lcArray[i].binWidth());
	    }
	    catch ( TimeSeriesException e ) {
		double[] binWidths = lcArray[i].getBinWidths();
		minBinWidth = Math.min(minBinWidth, MinMax.getMin(binWidths));
	    }
	}
	logger.info("Bin width = "+minBinWidth);
	//   Define the edges of the first new bin
	double leftEdgeOfNewBin = tstart;
	double rightEdgeOfNewBin = tstart + minBinWidth;
	newBinEdgesList.add(leftEdgeOfNewBin);
	newBinEdgesList.add(rightEdgeOfNewBin);
	//   Initialize variables for combining the TimeSeries
	double time = rightEdgeOfNewBin;
	int i=0;
	TimeSeries lc = lcArray[i];
	double[] binEdges = lc.getBinEdges();
	double[] shiftedBinEdges = DataUtils.shift(binEdges, lc.tStart());	
	while ( time < tstop ) {
	    //   Sum the contribution from each LCs to this newBin
	    double weightedSum = 0;
	    double sumOfWeights = 0;
	    for ( int j=0; j < nLCs; j++ ) {
		TimeSeries thisLC = lcArray[j];
		double[] rateAndError = TimeSeriesResampler.getRateFromTo(thisLC, leftEdgeOfNewBin, rightEdgeOfNewBin);
		double rate = rateAndError[0];
		double error = rateAndError[1];
		double weight = 1/Math.pow(error, 2);
		weightedSum += weight*rate;
		sumOfWeights += weight;
	    }
	    double newRate = weightedSum/sumOfWeights;
	    double newError = 1/Math.sqrt(sumOfWeights);
	    newRatesList.add(newRate);
	    newErrorsList.add(newError);
	    //   Define LEFT edge of the next new bin
	    logger.debug("time = "+time);
	    lc = lcArray[0];
	    binEdges = lc.getBinEdges();
	    shiftedBinEdges = DataUtils.shift(binEdges, lc.tStart());	
	    double nextEdge = DataUtils.findClosestValueLargerThan(time, shiftedBinEdges);
	    logger.debug("nextEdge = "+nextEdge);
	    double nextEdgeIndex = DataUtils.getIndex(nextEdge, shiftedBinEdges);
	    logger.debug(nextEdgeIndex);
	    boolean nextEdgeIndexIsEven = nextEdgeIndex%2 == 0;
	    logger.debug(nextEdgeIndexIsEven);
	    double rightEdgeOfPreviousBin = rightEdgeOfNewBin;
	    if ( nextEdgeIndexIsEven ) {
		//  We are in a gap
		logger.debug("We are in gap");
		leftEdgeOfNewBin = nextEdge;
		try {
		    while ( nextEdgeIndexIsEven ) {
			leftEdgeOfNewBin = Math.min(leftEdgeOfNewBin, nextEdge);
			//  Check the next TimeSeries
			i++;
			TimeSeries thisLC = lcArray[i];
			binEdges = thisLC.getBinEdges();
			shiftedBinEdges = DataUtils.shift(binEdges, thisLC.tStart());
			nextEdge = DataUtils.findClosestValueLargerThan(time, shiftedBinEdges);
			nextEdgeIndex = DataUtils.getIndex(nextEdge, shiftedBinEdges);
			nextEdgeIndexIsEven = nextEdgeIndex%2 == 0;
			logger.debug("leftEdgeOfNewBin = "+leftEdgeOfNewBin);
		    }
		    //leftEdgeOfNewBin = rightEdgeOfPreviousBin;
		}
		catch ( ArrayIndexOutOfBoundsException e ) {
		    //  If we get here, all LCs have this gap in common
		}
	    }
	    else {
		//  We are inside a bin
		leftEdgeOfNewBin = rightEdgeOfPreviousBin;
	    }
	    //   Define RIGHT edge of the next new bin
	    rightEdgeOfNewBin = Double.MAX_VALUE;
	    for ( int j=0; j < nLCs; j++ ) {
		lc = lcArray[j];
		binEdges = lc.getBinEdges();
		shiftedBinEdges = DataUtils.shift(binEdges, lc.tStart());
		nextEdge = DataUtils.findClosestValueLargerThan(leftEdgeOfNewBin, shiftedBinEdges);
		rightEdgeOfNewBin = Math.min(rightEdgeOfNewBin, nextEdge);
		logger.debug("nextEdge = "+nextEdge+"\t rightEdgeOfNewBin = "+rightEdgeOfNewBin);
	    }
	    //   Increment time to the right edge of the next new bin
	    if ( rightEdgeOfNewBin == tstop ) {
		time = tstop;
	    }
	    else {
		time = rightEdgeOfNewBin;
	    }
	    logger.debug("time at the bottom of while = "+time);
	}
	newBinEdgesList.trimToSize();
	double[] finalEdges = DataUtils.shift(newBinEdgesList.elements(), -newBinEdgesList.get(0));
	newRatesList.trimToSize();
	newErrorsList.trimToSize();
	return new TimeSeries(tstart, finalEdges, newRatesList.elements(), newErrorsList.elements());
    }


}
