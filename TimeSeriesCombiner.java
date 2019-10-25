package gb.esac.timeseries;

import org.apache.log4j.Logger;


public final class TimeSeriesCombiner {

    private static Logger logger  = Logger.getLogger(TimeSeriesCombiner.class);

    public void subtract(TimeSeries lc, double constant) {

	logger.info("Subtracting a constant ("+constant+")");

	double[] newRates = new double[lc.nBins()];
	double[] r = lc.getRates();
	for ( int i=0; i < lc.nBins(); i++ ) {
	    
	    newRates[i] = r[i] - constant;
	}

	lc.setRates(newRates);
    }


    public void subtract(TimeSeries lc1, TimeSeries lc2) throws TimingException, BinningException  {


	logger.info("Subtracting a TimeSeries");
	
	// // TIMESERIES.REBIN METHOD DOESN'T EXIST; PERHAPS IT'S IN A DIFFERENT REPOSITORY
	// //  Check input array size and rebin if necessary
	// if ( lc1.nBins() != lc2.nBins() ) {

	//     logger.warn("TimeSeries have different number of bins");

	//     if ( lc1.nBins() > lc2.nBins() ) {
	// 	lc1.rebin(lc2.nBins());
	//     }
	//     else {
	// 	lc2.rebin(lc1.nBins());
	//     }
	// }


	//  Subtract lc.rates from rates
	double[] newRates = new double[lc1.nBins()];
	for ( int i=0; i < lc1.nBins(); i++ ) {
	    
	    newRates[i] = lc1.getRates()[i] - lc2.getRates()[i];
	}
	lc1.setRates(newRates);


	//  Combine errors
	double[] e1 = lc1.getErrorsOnRates();
	double[] e2 = lc2.getErrorsOnRates();
	double[] newErrors = new double[lc1.nBins()];
	for ( int i=0; i < lc1.nBins(); i++ ) {
	    newErrors[i] = Math.sqrt( Math.pow(e1[i], 2) + Math.pow(e2[i], 2) );
	}
	lc1.setErrorsOnRates(newErrors);
    }


    public void add(TimeSeries lc, double constant) {

	logger.info("Adding a constant ("+constant+")");

	double[] newRates = new double[lc.nBins()];
	double[] r = lc.getRates();
	for ( int i=0; i < lc.nBins(); i++ ) {
	    newRates[i] = r[i] + constant;
	}

	lc.setRates(newRates);
    }


    public void add(TimeSeries lc1, TimeSeries lc2) throws TimingException, BinningException  {

	logger.info("Adding a TimeSeries");

	// // rebin method doesn't exist
	// //  Check input array size and rebin if necessary
	// if ( lc1.nBins() != lc2.nBins() ) {

	//     logger.warn("TimeSeries have different number of bins");

	//     if ( lc1.nBins() > lc2.nBins() ) {

	// 	lc1.rebin(lc2.nBins());
	//     }
	//     else {
	// 	lc2.rebin(lc1.nBins());
	//     }

	// }

	//  Add lc.rates to rates
	double[] newRates = new double[lc1.nBins()];
	for ( int i=0; i < lc1.nBins(); i++ ) {
	    
	    newRates[i] = lc1.rates[i] + lc2.rates[i];
	}
	lc1.setRates(newRates);
	
	//  Combine errors
	double[] e1 = lc1.getErrorsOnRates();
	double[] e2 = lc2.getErrorsOnRates();
	double[] newErrors = new double[lc1.nBins()];
	for ( int i=0; i < lc1.nBins(); i++ ) {
	    newErrors[i] = Math.sqrt( Math.pow(e1[i], 2) + Math.pow(e2[i], 2) );
	}
	lc2.setErrorsOnRates(newErrors);
    }

   
    public void scale(TimeSeries lc, double scalingFactor) {

	logger.info("Scaling TimeSeries by "+scalingFactor+"");

	double[] newRates = new double[lc.nBins()];
	double[] newErrors = new double[lc.nBins()];
	double[] r = lc.getRates();
	double[] e = lc.getErrorsOnRates();
	for ( int i=0; i < lc.nBins(); i++ ) {
	    
	    newRates[i] = r[i]*scalingFactor;
	    newErrors[i] = e[i]*scalingFactor;
	}

	lc.setRates(newRates);
	lc.setErrorsOnRates(newErrors);
    }


    public double[][] combineRatesAndErrors(double[] rates1, double[] errors1, double[] rates2, double[] errors2) {


	int nCommonBins = rates1.length;
	double[] combinedRates = new double[nCommonBins];
	double[] combinedErrors = new double[nCommonBins];
	double weight1 = 0;
	double weight2 = 0;
	double sumOfWeights = 0;
	double weightedSum = 0;
	for ( int i=0; i < nCommonBins; i++ ) {
	    weight1 = 1/Math.pow(errors1[i], 2);
	    weight2 = 1/Math.pow(errors2[i], 2);
	    sumOfWeights = weight1 + weight2;
	    weightedSum = rates1[i]*weight1 + rates2[i]*weight2;

	    combinedRates[i] = weightedSum/sumOfWeights;
	    combinedErrors[i] = 1/Math.sqrt(sumOfWeights);
	}

	return new double[][] {combinedRates, combinedErrors};

    }


}
