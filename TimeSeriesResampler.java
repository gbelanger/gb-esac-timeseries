package gb.esac.timeseries;

import gb.esac.binner.BinningException;
import gb.esac.binner.BinningUtils;
import gb.esac.binner.Rebinner;
import gb.esac.binner.Resampler;
import gb.esac.binner.IntensityBinResampler;
import gb.esac.tools.DataUtils;
import gb.esac.tools.MinMax;
import org.apache.log4j.Logger;
import gb.esac.binner.DensityBin;
import gb.esac.binner.Bin;

public final class TimeSeriesResampler {

    //  IMPORTANT:
    //  There is a problem when the start time is not 0
    //  Must fix this!!!

    private static Logger logger  = Logger.getLogger(TimeSeriesResampler.class);

    public static TimeSeries rebinToMinSignif(TimeSeries lc, double minSignif) {
	logger.info("Rebinning TimeSeries to minSignif = "+minSignif);
	double[][] binEdgesRatesAndErrors = Rebinner.rebinToMinSignif(lc.getRates(), lc.getErrorsOnRates(), lc.getBinEdges(), minSignif);
	double[] newBinEdges = binEdgesRatesAndErrors[0];
	double[] rebRates = binEdgesRatesAndErrors[1];
	double[] rebErrors = binEdgesRatesAndErrors[2];
	return new TimeSeries(lc.tStart(), newBinEdges, rebRates, rebErrors);
    }

    public static TimeSeries resampleToClosestPowerOfTwo(TimeSeries lc) throws BinningException {
	int nLCBins = lc.nBins();
	double n = Math.log(nLCBins)/Math.log(2);
	double diff = n - Math.floor(n);
	TimeSeries ts = null;
	if ( diff == 0 ) { 
	    if ( lc.binWidthIsConstant() ) {
		logger.info("Number of bins already is a power of 2: Returning copy of TimeSeries");
		ts = new TimeSeries(lc);
	    }
	    else {
		logger.info("Resampling TimeSeries to closest power of 2");
		int nBins = (int) Math.pow(2, n);
		ts = resample(lc, nBins);
	    }
	}
	else {
	    logger.info("Resampling TimeSeries to closest power of 2");
	    double powerOfTwo = Math.ceil(n);
	    powerOfTwo = Math.floor(n);
	    int nBins = (int) Math.pow(2, powerOfTwo);
	    ts = resample(lc, nBins);
	}
	return ts;
    }
    
    public static TimeSeries resample(TimeSeries lc, int nNewBins) throws BinningException {
	logger.info("Resampling TimeSeries to nNewBins = "+nNewBins);
	double newBinWidth = lc.duration()/nNewBins;
	return resample(lc, newBinWidth);
    }

    public static TimeSeries resample(TimeSeries lc, double newBinWidth) throws BinningException {
	logger.info("Resampling TimeSeries to newBinWidth = "+newBinWidth);
	double[] newBinEdges = BinningUtils.getBinEdges(0, lc.duration(), newBinWidth);
	return resample(lc, newBinEdges);
    }

    public static TimeSeries resample(TimeSeries lc, double[] newBinEdges) throws BinningException {
	logger.info("Resampling TimeSeries using defined binEdges");

	//  This is the old version of the code using Resampler.java
	//
	//double[][] ratesAndErrors =
	// Resampler.resample(lc.getRates(), lc.getErrorsOnRates(), lc.getBinEdges(), newBinEdges);
	//double[] rebRates = ratesAndErrors[0];
	//double[] rebErrors = ratesAndErrors[1];
	//
	//  Up to here

	//  This is the new version of the code using BinResampler.java
	//
	//  Construct Bin[] for the new bins
	int nNewBins = newBinEdges.length/2;
	Bin[] newBins = new Bin[nNewBins];
	for (int i = 0; i < nNewBins; i++ ) {
	    double leftEdge = newBinEdges[2*i];
	    double rightEdge = newBinEdges[2*i+1];
	    newBins[i] = new Bin(leftEdge, rightEdge);
	}
	
	//  Construct IntensityBin[] for the old bins
	int nOldBins = lc.nBins();
	double[] leftEdges = lc.getLeftBinEdges();
	double[] rightEdges = lc.getRightBinEdges();
	double[] rates = lc.getRates();
	double[] errors = lc.getErrorsOnRates();
	DensityBin[] oldIntensityBins = new DensityBin[nOldBins];
	for (int i = 0; i < nOldBins; i++) {
	    oldIntensityBins[i] = new DensityBin(leftEdges[i],rightEdges[i],rates[i],errors[i]);
	}
	
	// Resample and define return values
	DensityBin[] newIntensityBins = IntensityBinResampler.resample(oldIntensityBins, newBins);
	double[] rebRates = new double[nNewBins];
	double[] rebErrors = new double[nNewBins];
	for (int i = 0; i < nNewBins; i++) {
	    rebRates[i] = newIntensityBins[i].getValue();
	    rebErrors[i] = newIntensityBins[i].getError();
	}
	
	return new TimeSeries(lc.tStart(), newBinEdges, rebRates, rebErrors);
    }


    public static double[] getRateFromTo(TimeSeries lc, double t1, double t2) {
	//  Get data from TimeSeries
	double[] edges = lc.getBinEdges();
	double[] oldBinEdges = DataUtils.shift(edges, lc.tStart());
	int nOldBins = lc.nBins();
	double tstop = oldBinEdges[oldBinEdges.length-1];
	double[] rates = lc.getRates();
	double[] errors = lc.getErrorsOnRates();
	//   Initialize variables
	double counts = 0;
	double errorCounts = 0;
	double exposure = 0;
	double effNewBinTime = 0;
	//   Determine where we are in the TimeSeries
	//  There's a bug here somewhere with the identification of the bin index. No time to look into it now.
	//System.out.println(t1);
	int binEdgeIndex = DataUtils.getClosestIndexInSortedData(t1, oldBinEdges);   
	int k = (int) Math.floor(binEdgeIndex/2);  // k is the index of the old bins
	double leftEdge = oldBinEdges[2*k];
	double rightEdge = oldBinEdges[2*k+1];
	boolean binEdgeIndexIsEven = binEdgeIndex%2 == 0;
	if ( binEdgeIndexIsEven ) {	    
	    leftEdge = t1;
	}
	//   Sum the counts of the old bins while within the new bin
	double rebinnedRate = 0;
	double rebinnedError = 0;
	while ( k < nOldBins-1 && rightEdge <= t2 ) {
	    exposure = (rightEdge - leftEdge);
	    effNewBinTime += exposure;
	    counts += exposure*rates[k];
	    errorCounts += Math.pow(exposure*errors[k], 2);
	    //   Move to the next old bin and define its edges
	    k++; 
	    if ( k < nOldBins ) {
		leftEdge = oldBinEdges[2*k];
		rightEdge = oldBinEdges[2*k+1];
	    }
	}
	//   At this point, the next old bin is not fully contained within the new bin
	//   If there is a gap in the old bins, and therefore, the new bin ends before or at the start 
	//   of the next old bin, write out the final rate for the new bin and reset counts to 0
	if ( t2 <= leftEdge ) {
	    rebinnedRate = counts/effNewBinTime;
	    rebinnedError = Math.sqrt(errorCounts)/effNewBinTime;
	    //logger.debug("effNewBinTime="+effNewBinTime);
	    //logger.debug("r="+rebinnedRates[0][i]+"\t\t e="+rebinnedRates[1][i]);
	    effNewBinTime = 0;
	    exposure = 0;
	    counts = 0;
	    errorCounts = 0;
	}
	//   If the new bin ends inside the next old bin, add the counts corresponding to the 
	//   fraction of the old bin, and write out the final rate for the new bin. 
	//   Here we reset the counts to the other fraction of the old bin. 
	else {
	    if ( k == nOldBins-1 ) {
		t2 = Math.min(t2, tstop);
		//   Add last bit of counts from the first part of the old bin
		exposure = (t2 - leftEdge);
		counts += rates[k]*exposure;
		errorCounts += Math.pow(errors[k]*exposure, 2);
		effNewBinTime += exposure;
		rebinnedRate = counts/effNewBinTime;
		rebinnedError = Math.sqrt(errorCounts)/effNewBinTime;
		//logger.debug("effNewBinTime="+effNewBinTime);
		//logger.debug("r="+rebinnedRates[0][i]+"\t\t e="+rebinnedRates[1][i]);
	    }
	    else {
		//   Add last bit of counts from the first part of the old bin
		exposure = (t2 - leftEdge);
		counts += rates[k]*exposure;
		errorCounts += Math.pow(errors[k]*exposure, 2);
		effNewBinTime += exposure;
		rebinnedRate = counts/effNewBinTime;
		rebinnedError = Math.sqrt(errorCounts)/effNewBinTime;
		//logger.debug("effNewBinTime="+effNewBinTime);
		//logger.debug("r="+rebinnedRates[0][i]+"\t\t e="+rebinnedRates[1][i]);
		//   Reset to take into account the second piece of the old bin
		exposure = t2 - leftEdge;
		counts = exposure*rates[k];
		errorCounts = Math.pow(errors[k]*exposure, 2);
		effNewBinTime = exposure;
		//   Move to the next old bin and define its edges
		k++;
		if ( k < nOldBins ) {
		    leftEdge = oldBinEdges[2*k];
		    rightEdge = oldBinEdges[2*k+1];
		}
	    }
	}
	return new double[] {rebinnedRate, rebinnedError};
    }
}
