package gb.esac.timeseries;

import cern.colt.list.DoubleArrayList;
import gb.esac.binner.BinningException;
import gb.esac.binner.BinningUtils;
import gb.esac.periodogram.WindowFunction;
import gb.esac.periodogram.WindowFunctionException;
import gb.esac.tools.Converter;
import gb.esac.tools.DataSmoother;
import gb.esac.tools.DataUtils;
import gb.esac.tools.DistributionFunc;
import hep.aida.ref.histogram.Histogram1D;
import java.util.Arrays;
import org.apache.log4j.Logger;


public final class TimeSeriesUtils {

    private static Logger logger  = Logger.getLogger(TimeSeriesUtils.class);

    // public static TimeSeries applySamplingFunction(TimeSeries ts, double[] samplingBinEdges, double[] samplingValues){
    // 	// Assumption: the sampling has exactly the same duration as the ts
    // 	// Initial checks on coverage of sampling edges and ts
    // 	if (samplingBinEdges[0] != binEdges[0] || samplingBinEdges[samplingBinEdges.length-1] != binEdges[binEdges.length-1]) {
    // 		throw new BinningException("Cannot apply sampling: Bin edges of time series and sampling function do not match.");
    // 	}
    // 	// Create list to contain results of sampling
    // 	DoubleArrayList newBinEdgesList = new DoubleArrayList();
    // 	DoubleArrayList newBinHeightsList = new DoubleArrayList();
    // 	// Go through ts and apply sampling pattern
    // 	double[] binEdges = ts.getBinEdges();
    // 	double[] intensities = ts.getBinHeights();
    // 	int nbins = ts.nbins();
    // 	int i = 0;
    // 	int k = 0;
  		// while (i < nbins && k < samplingBinEdges.length) {
    // 		while (binEdges[2*i+1] < samplingBinEdges[2*k+1]) {
    // 			newBinEdgesList.add(binEdges[2*i]);
				// newBinEdgesList.add(binEdges[2*i+1]);
				// if (samplingValues[k] == 1) {
				// 	newBinHeightsList.add(intensities[i]);
				// }
				// else {
				// 	newBinHeightsList.add(0.0);
				// }
				// i++;
    // 		}
    // 		// Here we need to split
    // 		k++;
    // 	}



    // 	// Case where sampling starts with 0's
    // 	int k = 0;
    // 	if (samplingValues[k] == 0) {
    // 		while (k < samplingValues.length && samplingValues[k] == 0) {k++;}
    // 	}
    // 	// Case where sampling starts with 1's
    // 	else {

    // 	}
    // 	int j = 0;
    // 	if (samplingBinEdges[k] > binEdges[j])


    // 	for (int i = 0; i < nbins; i++) {
    // 		AbsoluteQuantityBin = new AbsoluteQuantityBin(binEdges[2*i], binEdges[2*i+1], intensities[i]);
    // 	}
    // }

    public static double[] getRandomArrivalTimes(TimeSeries ts, int nEvents) {
		double tzero = ts.tStart();
		double binTime = ts.duration()/ts.nBins();
		Histogram1D lcHisto = Converter.array2histo("light curve", tzero, binTime, ts.getRates());
		Histogram1D cdfHisto = DistributionFunc.getCDFHisto(lcHisto);
		double[] times = DistributionFunc.getRandom(cdfHisto, nEvents);
		Arrays.sort(times);
		return times;
    }

    public static TimeSeries dropLeadingAndTrailingNaNs(TimeSeries ts) {
		logger.warn("Dropping leading and trailing NaNs");
		TimeSeries newTimeSeries = new TimeSeries(ts);

		int nLeadingNaNs = countLeadingNaNs(ts);
		int nTrailingNaNs = countTrailingNaNs(ts);
		if ( nLeadingNaNs != 0 && nTrailingNaNs == 0 ) {
		    newTimeSeries = dropLeadingBins(ts, nLeadingNaNs);
		}
		else if ( nLeadingNaNs == 0 && nTrailingNaNs != 0 ) {
		    newTimeSeries =  dropTrailingBins(ts, nTrailingNaNs);
		}
		else if ( nLeadingNaNs != 0 && nTrailingNaNs != 0 ) {
		    TimeSeries tmp = dropLeadingBins(ts, nLeadingNaNs);
		    newTimeSeries = dropTrailingBins(tmp, nTrailingNaNs);
		}
		return newTimeSeries;
    }

    public static TimeSeries dropLeadingBins(TimeSeries ts, int nBinsToDrop) {
		logger.info("Dropping the first "+nBinsToDrop+" bins");
		double[] binEdges = ts.getBinEdges();

		//  Define new tStart to correspond to the start of the first non-NaN bin
		double leftEdgeOfFirstGoodBin = binEdges[2*nBinsToDrop];
		double newTStart = ts.tStart() + leftEdgeOfFirstGoodBin;

		//  Define new binEdges
		double[] shiftedBinEdges = DataUtils.shift(binEdges, -leftEdgeOfFirstGoodBin);
		double[] newBinEdges = new double[binEdges.length - 2*nBinsToDrop];
		for ( int i=0; i < newBinEdges.length; i++ ) {
		    newBinEdges[i] = shiftedBinEdges[i+2*nBinsToDrop];
		}
		
		//  Define new binHeights and construct the new TimeSeries
		if ( ts.errorsAreSet() ) {
		    double[] rates = ts.getRates();
		    double[] errors = ts.getErrorsOnRates();
		    double[] newRates = new double[rates.length - nBinsToDrop];
		    double[] newErrors = new double[newRates.length];
		    for ( int i=0; i < newRates.length; i++ ) {
			newRates[i] = rates[i+nBinsToDrop];
			newErrors[i] = errors[i+nBinsToDrop];
		    }
		    return new TimeSeries(newTStart, newBinEdges, newRates, newErrors);
		}
		else {
		    double[] binHeights = ts.getBinHeights();
		    double[] newBinHeights = new double[binHeights.length - nBinsToDrop];
		    for ( int i=0; i < newBinHeights.length; i++ ) {
			newBinHeights[i] = binHeights[i+nBinsToDrop];
		    }
		    return new TimeSeries(newTStart, newBinEdges, newBinHeights);
		}
    }

    public static TimeSeries dropTrailingBins(TimeSeries ts, int nBinsToDrop) {
		logger.info("Dropping the last "+nBinsToDrop+" bins");
		double[] binEdges = ts.getBinEdges();

		//  Define new binEdges
		double[] newBinEdges = new double[binEdges.length - 2*nBinsToDrop];
		for ( int i=0; i < newBinEdges.length; i++ ) {
		    newBinEdges[i] = binEdges[i];
		}

		//  Define new binHeights and construct the new TimeSeries
		if ( ts.errorsAreSet() ) {
		    double[] rates = ts.getRates();
		    double[] errors = ts.getErrorsOnRates();
		    double[] newRates = new double[rates.length - nBinsToDrop];
		    double[] newErrors = new double[newRates.length];
		    for ( int i=0; i < newRates.length; i++ ) {
			newRates[i] = rates[i];
			newErrors[i] = errors[i];
		    }
		    return new TimeSeries(ts.tStart(), newBinEdges, newRates, newErrors);
		}
		else {
		    double[] binHeights = ts.getBinHeights();
		    double[] newBinHeights = new double[binHeights.length - nBinsToDrop];
		    for ( int i=0; i < newBinHeights.length; i++ ) {
			newBinHeights[i] = binHeights[i];
		    }
		    return new TimeSeries(ts.tStart(), newBinEdges, newBinHeights);
		}
    }

    public static int countLeadingNaNs(TimeSeries ts) {
		double[] binHeights = ts.getBinHeights();
		int nLeadingNaNs = 0;
		int k=0;
		while ( Double.isNaN(binHeights[k]) ) {
		    nLeadingNaNs++;
		    k++;
		}
		if ( nLeadingNaNs > 0 ) {
		    logger.warn("There are "+nLeadingNaNs+" leading NaNs");
		}
		else {
		    logger.info("There are no leading NaNs");
		}
		return nLeadingNaNs;
    }

    public static int countTrailingNaNs(TimeSeries ts) {
		double[] binHeights = ts.getBinHeights();
		int nTrailingNaNs = 0;
		int k=binHeights.length-1;
		while ( Double.isNaN(binHeights[k]) ) {
		    nTrailingNaNs++;
		    k--;
		}
		if ( nTrailingNaNs > 0 ) {
		    logger.warn("There are "+nTrailingNaNs+" trailing NaNs");
		}
		else {
		    logger.info("There are no trailing NaNs");
		}
		return nTrailingNaNs;
    }


    public static TimeSeries removeGaps(TimeSeries ts) throws BinningException {
		logger.info("Removing data gaps");
		double[] binCentres = ts.getBinCentres();
		double[] binEdges = ts.getBinEdges();
		double[] binWidths = ts.getBinWidths();
		double[] gapEdges = ts.getGapEdges();

		DoubleArrayList newRatesList = new DoubleArrayList();
		DoubleArrayList newErrorsList = new DoubleArrayList();
		DoubleArrayList newBinWidthsList = new DoubleArrayList();
		DoubleArrayList newBinHeightsList = new DoubleArrayList();

		if ( ts.errorsAreSet() ) {
		    double[] rates = ts.getRates();
		    double[] errors = ts.getErrorsOnRates();
		    int k=0;
		    int i=0;
		    while ( i < ts.nBins() ) {
			if ( binCentres[i] >= gapEdges[2*k] && binCentres[i] <= gapEdges[2*k+1] ) {
			    while ( binCentres[i] >= gapEdges[2*k] && binCentres[i] <= gapEdges[2*k+1] && i < ts.nBins() ) {
				i++;
			    }
			    k++;
			}
			else {
			    newRatesList.add(rates[i]);
			    newErrorsList.add(errors[i]);
			    newBinWidthsList.add(binWidths[i]);
			    i++;
			}
		    }
		    newRatesList.trimToSize();
		    newErrorsList.trimToSize();
		    newBinWidthsList.trimToSize();
		    double[] newRates = newRatesList.elements();
		    double[] newErrors = newErrorsList.elements();
		    double[] newBinWidths = newBinWidthsList.elements();
		    double[] newBinEdges = BinningUtils.getBinEdges(0, newBinWidths);
		    return new TimeSeries(ts.tStart(), newBinEdges, newRates, newErrors);
		}
		else {
		    double[] binHeights = ts.getBinHeights();
		    int k=0;
		    int i=0;
		    while ( i < ts.nBins() ) {
			if ( binCentres[i] >= gapEdges[2*k] && binCentres[i] <= gapEdges[2*k+1] ) {
			    while ( binCentres[i] >= gapEdges[2*k] && binCentres[i] <= gapEdges[2*k+1] && i < ts.nBins() ) {
				i++;
			    }
			    k++;
			}
			else {
			    newBinHeightsList.add(binHeights[i]);
			    newBinWidthsList.add(binWidths[i]);
			    i++;
			}
		    }
		    newBinHeightsList.trimToSize();
		    newBinWidthsList.trimToSize();
		    double[] newBinHeights = newBinHeightsList.elements();
		    double[] newBinWidths = newBinWidthsList.elements();
		    double[] newBinEdges = BinningUtils.getBinEdges(0, newBinWidths);
		    return new TimeSeries(ts.tStart(), newBinEdges, newBinHeights);
		}
    }

    public static TimeSeries fillGapsWithZeros(TimeSeries ts) {
		int nDataBins = ts.nBins();
		int nnewBins = ts.nSamplingFunctionBins();
		//System.out.println("nDataBins = "+nDataBins);
		//System.out.println("nSamplingFunctionBins = "+nnewBins);
		double[] samplingFuncBinEdges = ts.getSamplingFunctionBinEdges();
		double[] samplingFuncValues = ts.getSamplingFunctionValues();
		int nZeros = 0;
		int nOnes = 0;
		for ( int i=0; i < samplingFuncValues.length; i++ ) {
		    if ( samplingFuncValues[i] == 1 ) nOnes++;
		    else nZeros++;
		}
		//System.out.println("nZeros = "+nZeros);
		//System.out.println("nOnes = "+nOnes);
		//System.out.println("nDataBins+nZeros = "+(nDataBins+nZeros)+" = nnewBins = "+nnewBins);
		if ( ts.errorsAreSet() ) {
		    double[] rates = ts.getRates();
		    double[] errors = ts.getErrorsOnRates();
		    double[] newRates = new double[nnewBins];
		    double[] newErrors = new double[nnewBins];
		    int k=0;
		    for ( int i=0; i < samplingFuncValues.length; i++ ) {
			if ( samplingFuncValues[i] == 1 ) {
			    newRates[i] = rates[k];
			    newErrors[i] = errors[k];
			    k++;
			}
			else {
			    newRates[i] = 0;
			    newErrors[i] = 0;
			}
		    }
		    return new TimeSeries(ts.tStart(), samplingFuncBinEdges, newRates, newErrors);
		}
		else {
		    double[] binHeights = ts.getBinHeights();
		    double[] newBinHeights = new double[nnewBins];
		    int k=0;
		    for ( int i=0; i < samplingFuncValues.length; i++ ) {
			if ( samplingFuncValues[i] == 1 ) {
			    newBinHeights[i] = binHeights[k];
			    k++;
			}
			else {
			    newBinHeights[i] = 0;
			}
		    }
		    return new TimeSeries(ts.tStart(), samplingFuncBinEdges, newBinHeights);
		}
    }


    public static TimeSeries fillGaps(TimeSeries ts) {
		/** There is a bug here:
		     We take out the NaNs from the rates but keep the original times.
		     This needs fixing.
		**/
		logger.info("Filling data gaps");
		if ( ts.errorsAreSet() ) {
		    double[] newRates = DataUtils.fillDataGaps(ts.getRates());
		    double[] newErrors = DataUtils.fillDataGaps(ts.getErrorsOnRates());
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, newErrors);
		}
		else {
		    double[] newBinHeights = DataUtils.fillDataGaps(ts.getBinHeights());
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
		}
    }

    public static TimeSeries scale(TimeSeries ts, double scalingFactor) {
		logger.info("Scaling TimeSeries by a factor of "+scalingFactor);
		if ( ts.errorsAreSet() ) {
		    double[] newRates = ts.getRates();
		    double[] newErrors = ts.getErrorsOnRates();
		    for ( int i=0; i < ts.nBins(); i++ ) {
			newRates[i] *= scalingFactor;
			newErrors[i] *= scalingFactor;
		    }
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, newErrors);
		}
		else {
		    double[] newBinHeights = ts.getBinHeights();
		    for ( int i=0; i < ts.nBins(); i++ ) {
			newBinHeights[i] *= scalingFactor;
		    }
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
		}
    }

    public static TimeSeries addOffset(TimeSeries ts, double offset) {
		logger.info("Adding offset of "+offset+" to TimeSeries");
		if ( ts.errorsAreSet() ) {
		    double[] newRates = ts.getRates();
		    double[] newErrors = ts.getErrorsOnRates();
		    for ( int i=0; i < ts.nBins(); i++ ) {
				newRates[i] += offset;
				newErrors[i] += offset;
		    }
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, newErrors);
		}
		else {
		    double[] newBinHeights = ts.getBinHeights();
		    for ( int i=0; i < ts.nBins(); i++ ) {
				newBinHeights[i] += offset;
		    }
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
		}
    }

    public static TimeSeries detrend(TimeSeries ts) {
	logger.info("Detrending TimeSeries");
	if ( ts.errorsAreSet() ) {
	    double[] newRates = DataSmoother.detrend(ts.getBinCentres(), ts.getRates());
	    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, ts.getErrorsOnRates());
	}
	else {
	    double[] newBinHeights = DataSmoother.detrend(ts.getBinCentres(), ts.getBinHeights());
	    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
	}
    }

    public static TimeSeries kalmanFilter(TimeSeries lc, double processRMS) {
	logger.info("Kalman filtering TimeSeries");
		double[] kalmanRates = DataSmoother.kalmanFilter(lc.getRates(), lc.getErrorsOnRates(), processRMS);
		return new TimeSeries(lc.tStart(), lc.getBinEdges(), kalmanRates, lc.getErrorsOnRates());
    }

    public static TimeSeries smooth(TimeSeries ts, int nBins) throws BinningException {
		if ( nBins > ts.nBins() ) {
		    throw new BinningException("Smoothing window size is too large. nBins must be less than bins in TimeSeries");
		}
		logger.info("Smoothing TimeSeries");

		if ( ts.errorsAreSet() ) {
		    double[] newRates = DataSmoother.smooth(ts.getRates(), nBins);
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, ts.getErrorsOnRates());
		}
		else {
		    double[] newBinHeights = DataSmoother.smooth(ts.getBinHeights(), nBins);
		    return new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
		}
    }

    public static TimeSeries applyWindowFunction(TimeSeries ts, String windowName) throws WindowFunctionException {
		logger.info("Applying window function to intensities (binheights or rates)");
		WindowFunction window = new WindowFunction(windowName);
		double[] binCentres = ts.getBinCentres();
		double duration = ts.duration();
		double integralBefore = ts.sumOfBinHeights();
		double integralAfter = 0;
		TimeSeries ts_windowed;
		if ( ts.errorsAreSet() ) {
		    double[] newRates = window.apply(ts.getRates(), binCentres, duration);
		    ts_windowed = new TimeSeries(ts.tStart(), ts.getBinEdges(), newRates, ts.getErrorsOnRates());
		    integralAfter = ts_windowed.sumOfBinHeights();
		}
		else {
		    double[] newBinHeights = window.apply(ts.getBinHeights(), binCentres, duration);
		    ts_windowed = new TimeSeries(ts.tStart(), ts.getBinEdges(), newBinHeights);
		    integralAfter = ts_windowed.sumOfBinHeights();
		}	
		double areaScalingFactor = integralBefore/integralAfter;
		areaScalingFactor = 1;
		return TimeSeriesUtils.scale(ts_windowed, areaScalingFactor);
	}



//     /**  This was written for testing and proves useless in Fourier analysis  **/
//     public static TimeSeries padWithZeros(TimeSeries ts, int nZeros) throws TimeSeriesException, BinningException {

// 	logger.info("Padding with "+nZeros+" zeros in the central part of the TimeSeries");

// 	int nBins = ts.nBins();
// 	int nNewBins = nBins+nZeros;
// 	int nBinsBeforePadding = (int) Math.ceil(nBins/2);
// 	int nBinsAfterPadding = (int) Math.floor(nBins/2);
// 	double[] paddedCounts = new double[nNewBins];
// 	double[] paddedRates = new double[nNewBins];
// 	double[] paddedErrors = new double[nNewBins];
// 	double[] counts = ts.getBinHeights();
// 	double[] rates = ts.getRates();
// 	double[] errors = ts.getErrorsOnRates();
// 	for ( int i=0; i < nBinsBeforePadding; i++ ) {
// 	    paddedCounts[i] = counts[i];
// 	    paddedRates[i] = rates[i];
// 	    paddedErrors[i] = errors[i];
// 	}
// 	int offset = nBinsBeforePadding;
// 	for ( int i=0; i < nZeros; i++ ) {
// 	    paddedCounts[i+offset] = 0.0;
// 	    paddedRates[i+offset] = 0.0;
// 	    paddedErrors[i+offset] = 0.0;
// 	}
// 	offset += nZeros;
// 	for ( int i=0; i < nBinsAfterPadding; i++ ) {
// 	    paddedCounts[i+offset] = counts[i+nBinsBeforePadding];
// 	    paddedRates[i+offset] = rates[i+nBinsBeforePadding];
// 	    paddedErrors[i+offset] = errors[i+nBinsBeforePadding];
// 	}

// 	double binWidth = ts.getBinWidth();
// 	double addedTime = nZeros*binWidth;
// 	double[] binEdges = ts.getBinEdges();
// 	double[] newBinEdges = BinningUtils.getBinEdges(binEdges[0], binEdges[binEdges.length-1]+addedTime, nNewBins);

// 	if ( ts.errorsAreSet() ) {
// 	    return new TimeSeries(ts.tStart(), newBinEdges, paddedRates, paddedErrors);
// 	}
// 	else {
// 	    return new TimeSeries(ts.tStart(), newBinEdges, paddedCounts);
// 	}

//     }


//     public double estimatePowerSpectralIndex(TimeSeries lc) throws TimingException, BinningException  {

// 	logger.info("Estimating power spectral index");


// 	//  Get lc data
// 	double duration = lc.getDuration();
// 	double[] times = lc.getTimes();
// 	double[] rate = lc.getRates();
// 	double[] error = lc.getErrors();
// 	double[] binEdges = lc.getBinEdges();
// 	double binWidth = 0;
// 	try { binWidth = lc.getBinWidth(); }
// 	catch ( TimingException e ) {
// 	    binWidth = Stats.getMax(lc.getBinWidths());
// 	}
// 	double oldBinTime = binWidth;
// 	double binTimeMax = duration/64;
// 	double binTimeMin = Math.max(5, oldBinTime);
// 	double nuMax = 1/(2*binTimeMin);
// 	double nuMin = 1/duration;


// 	//  Define range of binTimes
// 	Vector binTimesVec = new Vector();
// 	Vector nBinsVec = new Vector();
// 	double binTime = binTimeMax;
// 	while ( binTime > binTimeMin ) {
// 	    double binsDbl = (new Double(Math.ceil(duration/binTime))).intValue();
// 	    double n = Math.round(Math.log10(binsDbl)/Math.log10(2));
// 	    int binsInt = (new Double(Math.pow(2, n))).intValue();
// 	    nBinsVec.add(binsInt);
// 	    binTime = duration/binsInt;
// 	    binTimesVec.add(binTime);
// 	    binTime /= 2;
// 	}
// 	binTimesVec.trimToSize();
// 	nBinsVec.trimToSize();
// 	double[] binTimes = new double[binTimesVec.size()];
// 	int[] nBins = new int[nBinsVec.size()];
// 	logger.info("Bin times used between "+binTimeMin+" and "+binTimeMax+" s are: ");
// 	for ( int i=nBins.length-1; i >= 0; i-- ) {
// 	    binTimes[i] = ((Double) binTimesVec.elementAt(i)).doubleValue();
// 	    nBins[i] = ((Integer) nBinsVec.elementAt(i)).intValue();
// 	    logger.info(num.format(binTimes[i])+" s  ("+nBins[i]+" bins)");
// 	}


// 	//  Determine equivalent mean rate based on signal to noise
// 	double meanRate = lc.getMean();
// 	int nevents = (new Double(meanRate*duration)).intValue();
// 	double meanIndivError = Stats.getMean(error);
// 	double meanSignalToNoise = meanRate/meanIndivError;
// 	double equivMeanRate = Math.pow(meanSignalToNoise, 2)/oldBinTime;


// 	//  Loop on bin times to estimate the spectral index
// 	int nSims = 20;
// 	FFTPeriodogram psd;
// 	double[][] simPSD = null;
// 	double[] simFreq = null;
// 	double[] simPow = null;
// 	double slope = 0;
// 	double slopeErr = 0;
// 	double[] lsFitResults = new double[4];
// 	double[] simSlopes = new double[nSims];
// 	double[] simSlopeErrs = new double[nSims];

// 	PeriodogramMaker psdMaker = new PeriodogramMaker();
// 	TimeSeriesMaker lcMaker = new TimeSeriesMaker();

//  	for ( int i=nBins.length-1; i >= 0; i-- ) {
	    
//  	    //  Make the PSD
//  	    TimeSeries lcCopy = lcMaker.makeTimeSeries(lc);
//  	    if ( lcCopy.thereAreGapsInRates )
//  		lcCopy.fillGapsInRates("average");
//  	    lcCopy.resample(nBins[i]);
//  	    psd = psdMaker.makeFFTPeriodogram(lcCopy, "leahy");

//  	    //  Fit  the slope
//  	    double[] slopeAndErr = psd.fitPowerLawInLogSpace();
//  	    slope = -slopeAndErr[0];
//  	    slopeErr = slopeAndErr[1];
// //  	    pw.print(slope+"\t"+slopeErr+"\t");

// // 	    System.out.println("Log  :   Comparing with simulations:");
// // 	    for ( int k=0; k < alpha.length; k++ ) {

// // 		System.out.print("Log  :    index = "+alpha[k]+", simulating "+nSims+" event lists ... ");
// // 		simPow = new double[freq.length];
// // 		double[] sumOfPows = new double[freq.length];

// // 		for ( int j=0; j < nSims; j++ ) {

// // 		    double[] t = ArrivalTimes.generateRedArrivalTimes(equivMeanRate, duration, alpha[k]);
// // 		    simPSD = Periodograms.makePSD_FFT(t, nBins[i], "leahy");
// // 		    simFreq = simPSD[0];
// // 		    simPow = simPSD[1];
// // 		    x = new double[simFreq.length];
// // 		    y = new double[simPow.length];
// // 		    for ( int m=0; m < simPow.length; m++ ) {
// // 			x[m] = Math.log10(simFreq[m]);
// // 			y[m] = Math.log10(simPow[m]);
// // 			sumOfPows[m] += simPow[m];
// // 		    }
// // // 		    lsFitResults = Stats.leastSquaresFitLine(x, y);
// // // 		    simSlopes[j] = -lsFitResults[1];
// // // 		    simSlopeErrs[j] = lsFitResults[3];
// // 		}
// // // 		double ave = Stats.getWMean(simSlopes, simSlopeErrs);
// // // 		double sig = Math.sqrt(Stats.getWVariance(simSlopes, simSlopeErrs)/nSims);

// // 		double[] avePow = new double[freq.length];
// // 		for ( int m=0; m < freq.length; m++ ) {
// // 		    avePow[m] = Math.log10(sumOfPows[m]/nSims);
// // 		}
// // 		lsFitResults = Stats.leastSquaresFitLine(x, avePow);
// // 		double ave = -lsFitResults[1];
// // 		double sig = lsFitResults[3];
// // 		System.out.println("index = "+num.format(ave)+" +/- "+ num.format(sig));
// // 		pw.print(num.format(ave)+"\t"+num.format(sig)+"\t");
// // 	    }
// // 	    pw.println();
// // 	    pw.flush();
//  	}
// // 	pw.close();
// // 	System.out.println("Log  : Result written to "+outName);



// 	double estimatedIndex = 0;

// 	return estimatedIndex;

//     }



}

