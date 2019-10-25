package gb.esac.timeseries;

import java.io.IOException;
import java.util.Arrays;
import java.io.FileWriter;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import org.apache.log4j.Logger;

import gb.esac.tools.BasicStats;
import gb.esac.tools.DataUtils;
import java.awt.geom.Point2D;
import nom.tam.fits.FitsException;


/**
 
 The abstract class <code>AbstractTimeSeries</code> is an object used to represent binned data ordered in time.
 It should be viewed as a histogram whose x-axis is in units of time (seconds).
 
 The time axis is discretized into bins defined by two edges per bin; we consider that the bin width
 is constant if the variance of the bin widths is smaller than 1e-6. Gaps between bins are also defined
 by two gap edges; we consider that there is a gap in the time series if there is at least one gap that
 is longer than 1e-6 s.
 
 The Y-axis or the height of each bin shows the total number of counts per bin, and there are therefore
 no errors on the bin heights. The rates are defined as the bin height divided by the bin width.
 Upon constructing the time series, if the errors on the rates are set, then the time series is
 considered as non-Poissonian, and the errors are used to derive weights in all statistical operations.
 
 All TimeSeries instances are immutable. The constructors are package-private and used by the public classes
 TimeSeriesMaker and TimeSeriesFileReader. All the setters are private and are therefore used internally to
 define all the fields and properties of a TimeSeries instance. The getters are public and return copies of the
 internal objects like binCentres, binWidths, and binHeights for example. There are no static class variables
 other than the logger, and so all are instance variables.
 
 In June 2016
 - Added new "instnace variables" and the corresponding methods to access these values
 String instrument (which is null by default, and the only variable with a public setter method
 double errorOnMeanRate
 double[] weightsOnRates
 double weightedMeanRate
 double errorOnWeightedMean
 
  @modifier Harry Holt
In July 2016
 - Converted the file from TimeSeries to AbstractTimeSeries
 - Including remove all private access modifiers to properties 

In August 2016
 - Added constructors here to call super from TimeSeries 
 - Internal setter methods have been made package-private to allow access from CodedMaskTimeSeries
 - Modified to reflect the creation of an interface ITimeSeries
 - Implemented all write methods for output in different formats

 @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>, ESA/ESAC, SRE-O, Villanueva de la Canada (Madrid), Spain
 @version August 2016

**/

public abstract class AbstractTimeSeries implements ITimeSeries {

    private static Logger logger = Logger.getLogger(AbstractTimeSeries.class);

    private String timeUnit = "s";
    
    // Attributes
    private String telescope = "";
    private boolean telescopeIsSet = false;
    private String instrument = "";
    private boolean instrumentIsSet = false;
    private double mjdref = Double.NaN;
    private boolean mjdrefIsSet = false;
    private double targetRA = Double.NaN;
    private double targetDec = Double.NaN;
    private Point2D.Double targetRaDec = null;
    private boolean targetRaDecAreSet = false;
    private String targetName = "";
    private boolean targetNameIsSet = false;
    private double energyRangeMin = Double.NaN;
    private double energyRangeMax = Double.NaN;
    private Point2D.Double energyRange = null;    
    private boolean energyRangeIsSet = false;
    private String dateObs = "";
    private String dateEnd = "";
    private boolean dateObsEndAreSet = false;
    private String timeObs = "";
    private String timeEnd = "";
    private boolean timeObsEndAreSet = false;
    private double relTimeError = Double.NaN;
    private double absTimeError = Double.NaN;
    private boolean relTimeErrorIsSet = false;
    private boolean absTimeErrorIsSet = false;    
    
    // bins
    private double[] binEdges;
    private double[] leftBinEdges;
    private double[] rightBinEdges;
    private int nBins;
    private double tStart;
    private double tStop;
    private double tMid;
    private double duration;
    private double exposureOnTarget;    
    private double ontime;
    private double sumOfBinWidths;
    private double[] binCentres;
    private double[] binWidths;
    private double[] halfBinWidths;
    private boolean binWidthIsConstant = false;
    private double minBinWidth;
    private double maxBinWidth;
    private double avgBinWidth;
    private double binCentreAtMinBinHeight;
    private double binCentreAtMaxBinHeight;
    // gaps
    private double[] gapEdges;
    private double[] gapLengths;
    private int nGaps;
    private int nNaNs;
    private int nNonNaNs;
    private double minGap;
    private double maxGap;
    private double meanGap;
    private double sumOfGaps;
    private boolean thereAreGaps = false;
    private boolean thereAreNaNs = false;
    // sampling function
    private int nSamplingFunctionBins;
    private double[] samplingFunctionValues;
    private double[] samplingFunctionBinEdges;
    // bin heights
    private double[] binHeights;
    private double minBinHeight;
    private double maxBinHeight;
    private double meanBinHeight;
    private double sumOfBinHeights;
    private double sumOfSquaredBinHeights;
    private double varianceInBinHeights;
    private double meanDeviationInBinHeights;
    private double skewnessInBinHeights;
    private double kurtosisInBinHeights;
    // rates
    private double[] rates;
    private double[] errorsOnRates; 
    private double[] weightsOnRates;
    private boolean errorsAreSet = false; 
    private double minRate;
    private double maxRate;
    private double meanRate;
    private double weightedMeanRate;
    private double errorOnMeanRate;
    private double errorOnWeightedMeanRate;
    private double sumOfRates;
    private double sumOfSquaredRates;
    private double sumOfWeightsOnRates;
    private double varianceInRates;
    private double meanDeviationInRates;
    private double skewnessInRates;
    private double kurtosisInRates;
    private double skewnessStandardError;
    private double kurtosisStandardError;
    
    //  Constructors are Package-private 
    AbstractTimeSeries() {}

    AbstractTimeSeries(TimeSeries ts) {
        if ( ts.errorsAreSet() ) {
            setBinEdges(ts.tStart(), ts.getBinEdges());
            setRates(ts.getRates());
            setErrorsOnRates(ts.getErrorsOnRates());
        }
        else {
            setBinEdges(ts.tStart(), ts.getBinEdges());
            setCounts(ts.getBinHeights());
        }
        printRateInfo();
    }
    
    AbstractTimeSeries(double tStart, double[] binEdges, double[] counts) {
        setBinEdges(tStart, binEdges);
        setCounts(counts);
        printRateInfo();
    }
    
    AbstractTimeSeries(double tStart, double[] binEdges, double[] rates, double[] errorsOnRates) {
        setBinEdges(tStart, binEdges);
        setRates(rates);
        setErrorsOnRates(errorsOnRates);
        printRateInfo();
    }
     
    // info-printing
    void printRateInfo() {   // HH used in TimeSeries so can't be private
        logger.info("Intensities are defined");
        logger.info("  Sum of binHeights = "+this.sumOfBinHeights);
        logger.info("  Mean rate = "+this.meanRate);
        logger.info("  Min rate = "+this.minRate);
        logger.info("  Max rate = "+this.maxRate);
        logger.info("  Variance in rates = "+this.varianceInRates);
    }
    
    //  Setters
    void setBinEdges(double tStart, double[] binEdges) {
	// binEdges are defined wrt tStart
	binEdges = DataUtils.resetToZero(binEdges);
	//
        this.binEdges = new double[binEdges.length];
        this.leftBinEdges = new double[binEdges.length/2];
        this.rightBinEdges = new double[binEdges.length/2];
        this.nBins = binEdges.length/2;
        for ( int i=0; i < this.nBins; i++ ) {
            this.binEdges[2*i] = binEdges[2*i];
            this.binEdges[2*i+1] = binEdges[2*i+1];
            this.leftBinEdges[i] = binEdges[2*i];
            this.rightBinEdges[i] = binEdges[2*i+1];
        }
        this.tStart = tStart;
        this.duration = this.binEdges[this.binEdges.length-1] - this.binEdges[0];
        this.tStop = this.tStart + this.duration;
        this.tMid = (this.tStart + this.tStop)/2;
        logger.info("TimeSeries has "+this.nBins+" bins");
        logger.info("  TStart = "+this.tStart);
        logger.info("  TStop = "+this.tStop);
        //logger.info("  TMid = "+this.tMid);
        logger.info("  Duration = "+this.duration);
        this.binCentres = new double[this.nBins];
        this.binWidths = new double[this.nBins];
        this.halfBinWidths = new double[this.nBins];
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        for ( int i=0; i < this.nBins; i++ ) {
            this.binCentres[i] = (this.binEdges[2*i] + this.binEdges[2*i+1])/2;
            this.binWidths[i] = this.binEdges[2*i+1] - this.binEdges[2*i];
            this.halfBinWidths[i] = this.binWidths[i]/2.0;
            min = Math.min(min, binWidths[i]);
            max = Math.max(max, binWidths[i]);
            sum += binWidths[i];
        }
        this.minBinWidth = min;
        this.maxBinWidth = max;
	this.sumOfBinWidths = sum;
	logger.info("  Sum of bin widths = "+this.sumOfBinWidths);
	this.avgBinWidth = sum/this.nBins;
        //  Check if bin width is constant, excluding the last bin
        double[] widths = getBinWidths();
        double[] w = new double[widths.length-1];
        for ( int i=0; i < widths.length-1; i++ ) {
            w[i] = widths[i];
        }
        double var = BasicStats.getVariance(w);
        if ( var < 1e-10 || Double.isNaN(var) ) {
            this.binWidthIsConstant = true;
            logger.info("  Bin width is constant = "+this.binWidths[0]);
        }
        else {
            this.binWidthIsConstant = false;
            logger.warn("  Bin width is not constant");
            logger.info("  Min bin width = "+this.minBinWidth);
            logger.info("  Max bin width = "+this.maxBinWidth);
            logger.info("  Average bin width = "+this.avgBinWidth);
        }
        // Define gapEdges and sampling function
        this.gapEdges = new double[2*(this.nBins-1)];
        this.gapLengths = new double[this.nBins-1];
        DoubleArrayList samplingFuncValuesList = new DoubleArrayList();
        DoubleArrayList samplingFuncEdgesList = new DoubleArrayList();
        samplingFuncEdgesList.add(this.binEdges[0]);
        samplingFuncEdgesList.add(this.binEdges[1]);
        samplingFuncValuesList.add(1); // time series never starts with a gap, because if there is one, we take it out
        double minGap = Double.MAX_VALUE;
        double maxGap = -Double.MAX_VALUE;
        int nGaps = 0;
        double sumOfGaps = 0;
        for ( int i=1; i < this.nBins; i++ ) {
            double gap = this.binEdges[2*i] - this.binEdges[2*i-1];
            if ( gap > Math.ulp(2*this.binEdges[2*i]) ) {
                nGaps++;
                sumOfGaps += gap;
                samplingFuncEdgesList.add(this.binEdges[2*i-1]);
                samplingFuncEdgesList.add(this.binEdges[2*i]);
                samplingFuncValuesList.add(0);
            }
            samplingFuncEdgesList.add(this.binEdges[2*i]);
            samplingFuncEdgesList.add(this.binEdges[2*i+1]);
            samplingFuncValuesList.add(1);
            minGap = Math.min(minGap, gap);
            maxGap = Math.max(maxGap, gap);
            this.gapLengths[i-1] = gap;
            this.gapEdges[2*(i-1)] = this.binEdges[2*i-1];
            this.gapEdges[2*(i-1)+1] = this.binEdges[2*i];
        }
        if ( maxGap > Math.ulp(2*this.binEdges[binEdges.length-1]) ) {
            this.thereAreGaps = true;
            this.nGaps = nGaps;
            this.sumOfGaps = sumOfGaps;
            this.meanGap = sumOfGaps/nGaps;
            this.maxGap = maxGap;
            this.minGap = minGap;
            logger.warn("There are "+nGaps+" gaps in timeline");
            logger.info("  Total gap time = "+sumOfGaps);
            logger.info("  Gap fraction wrt duration = "+(sumOfGaps/this.duration));
            logger.info("  Mean gap = "+meanGap);
            logger.info("  Max gap = "+maxGap);
        }
        else {
            this.thereAreGaps = false;
            this.nGaps = 0;
            this.sumOfGaps = 0;
            this.meanGap = 0;
            this.maxGap = 0;
            this.minGap = 0;
            logger.info("No gaps in timeline");
        }
        samplingFuncValuesList.trimToSize();
        samplingFuncEdgesList.trimToSize();
        this.samplingFunctionValues = samplingFuncValuesList.elements();
        this.samplingFunctionBinEdges = samplingFuncEdgesList.elements();
        this.nSamplingFunctionBins = (this.samplingFunctionValues).length;
        logger.info("Sampling function is defined");
        logger.info("  nZeros = "+this.nGaps);
        logger.info("  nOnes = "+this.nBins);
    }
    
    void setCounts(double[] counts) {
        this.binHeights = new double[this.nBins];
        this.rates = new double[this.nBins];
        double minBinHeight = Double.MAX_VALUE;
        double maxBinHeight = -Double.MAX_VALUE;
        double sumOfBinHeights = 0;
        double sumOfSquaredBinHeights = 0;
        double sumOfRates = 0;
        double sumOfSquaredRates = 0;
        double minRate = Double.MAX_VALUE;
        double maxRate = -Double.MAX_VALUE;
        int nNaNs = 0;
        int nNonNaNs = 0;
        for ( int i=0; i < this.nBins; i++ ) {
            this.binHeights[i] = counts[i];
            this.rates[i] = this.binHeights[i]/this.binWidths[i];
            if ( Double.isNaN(this.binHeights[i]) ) {
                //logger.warn("NaN encountered in binHeights: index "+i+". Excluding from calculations");
                nNaNs++;
		this.thereAreNaNs = true;
                this.thereAreGaps = true;
            }
            else {
                minBinHeight = Math.min(minBinHeight, this.binHeights[i]);
                maxBinHeight = Math.max(maxBinHeight, this.binHeights[i]);
                sumOfBinHeights += this.binHeights[i];
                sumOfSquaredBinHeights += this.binHeights[i]*this.binHeights[i];
                minRate = Math.min(minRate, this.rates[i]);
                maxRate = Math.max(maxRate, this.rates[i]);
                sumOfRates += this.rates[i];
                sumOfSquaredRates += this.rates[i]*this.rates[i];
                nNonNaNs++;
            }
        }
        this.nNonNaNs = nNonNaNs;
	this.nNaNs = nNaNs;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.minBinHeight = minBinHeight;
        this.maxBinHeight = maxBinHeight;
        this.sumOfBinHeights = sumOfBinHeights;
        this.sumOfSquaredBinHeights = sumOfSquaredBinHeights;
        this.sumOfSquaredRates = sumOfSquaredRates;
        setStatsOnIntensities();
    }
    
    void setRates(double[] r) {
        this.rates = new double[this.nBins];
        this.binHeights = new double[this.nBins];
        double minRate = Double.MAX_VALUE;
        double maxRate = -Double.MAX_VALUE;
        double sumOfRates = 0;
        double sumOfSquaredRates = 0;
        double minBinHeight = Double.MAX_VALUE;
        double maxBinHeight = -Double.MAX_VALUE;
        double sumOfBinHeights = 0;
        double sumOfSquaredBinHeights = 0;
        int nNaNs = 0;
        int nNonNaNs = 0;
        for ( int i=0; i < this.nBins; i++ ) {
            //  Rate
            double rate = r[i];
            this.rates[i] = rate;
            double counts = this.rates[i]*this.binWidths[i];
            this.binHeights[i] = counts;
            if ( Double.isNaN(rate) ) {
                nNaNs++;
		thereAreNaNs = true;
                thereAreGaps = true;
            }
            else {
                minRate = Math.min(minRate, rate);
                maxRate = Math.max(maxRate, rate);
                sumOfRates += rate;
                sumOfSquaredRates += rate*rate;
                minBinHeight = Math.min(minBinHeight, this.binHeights[i]);
                maxBinHeight = Math.max(maxBinHeight, this.binHeights[i]);
                sumOfBinHeights += counts;
                sumOfSquaredBinHeights += counts*counts;
                nNonNaNs++;
            }
        }
        if ( thereAreNaNs ) {
            logger.warn("There are "+nNaNs+" NaN values in the RATE column");
        }
        this.nNonNaNs = nNonNaNs;
	this.nNaNs = nNaNs;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.minBinHeight = minBinHeight;
        this.maxBinHeight = maxBinHeight;
        this.sumOfRates = sumOfRates;
        this.sumOfBinHeights = sumOfBinHeights;
        this.sumOfSquaredBinHeights = sumOfSquaredBinHeights;
        this.sumOfSquaredRates = sumOfSquaredRates;
    }
    
    void setErrorsOnRates(double[] errors) {
        this.errorsOnRates = new double[this.nBins];
        this.weightsOnRates = new double[this.nBins];
        double sum = 0;
        for ( int i=0; i < this.nBins; i++ ) {
            if ( Double.isNaN(errors[i]) ) {
                if ( ! Double.isNaN(this.rates[i]) ) {
                    logger.warn("There is a NaN value in errors whose corresponding rate is not NaN. Setting error from mean counts per bin");
                    double uncertainty = Math.sqrt(this.meanBinHeight);
                    this.errorsOnRates[i] = uncertainty/this.binWidths[i];
                }
            }
            else {
                this.errorsOnRates[i] = errors[i];
            }
            this.weightsOnRates[i] = 1./Math.pow(this.errorsOnRates[i], 2);
            sum += this.weightsOnRates[i];
        }
        this.sumOfWeightsOnRates = sum;
        this.errorsAreSet = true;
        setStatsOnIntensities();
    }
    
    private void setStatsOnIntensities() {
        this.binCentreAtMinBinHeight = this.binCentres[DataUtils.getIndex(this.minBinHeight, this.binHeights)];
        this.binCentreAtMaxBinHeight = this.binCentres[DataUtils.getIndex(this.maxBinHeight, this.binHeights)];
        this.meanBinHeight = this.sumOfBinHeights/this.nNonNaNs;
	if ( this.errorsAreSet ) {
	    this.meanRate = this.sumOfRates/this.nNonNaNs;
	    this.weightedMeanRate = Descriptive.weightedMean(new DoubleArrayList(this.rates), new DoubleArrayList(this.weightsOnRates));
	}
	else {
	    this.meanRate = this.sumOfBinHeights/this.sumOfBinWidths;
	    this.weightedMeanRate = this.meanRate;	    
	}

        this.varianceInBinHeights = Descriptive.sampleVariance(this.nNonNaNs, this.sumOfBinHeights, this.sumOfSquaredBinHeights);
        this.varianceInRates = Descriptive.sampleVariance(this.nNonNaNs, this.sumOfRates, this.sumOfSquaredRates);
        this.errorOnMeanRate = Math.sqrt(this.varianceInRates/this.nNonNaNs);
        this.errorOnWeightedMeanRate = 1./Math.sqrt(this.sumOfWeightsOnRates);
        this.meanDeviationInBinHeights = Descriptive.meanDeviation(new DoubleArrayList(this.binHeights), this.meanBinHeight);
        this.meanDeviationInRates = Descriptive.meanDeviation(new DoubleArrayList(this.rates), this.meanRate);
        this.skewnessInBinHeights = Descriptive.sampleSkew(new DoubleArrayList(this.binHeights), this.meanBinHeight, this.varianceInBinHeights);
        this.skewnessInRates = Descriptive.sampleSkew(new DoubleArrayList(this.rates), this.meanBinHeight, this.varianceInBinHeights);
        this.skewnessStandardError = Descriptive.sampleSkewStandardError(this.nBins);
        this.kurtosisInBinHeights = Descriptive.sampleKurtosis(new DoubleArrayList(this.binHeights), this.meanBinHeight, this.varianceInBinHeights);
        this.kurtosisInRates = Descriptive.sampleKurtosis(new DoubleArrayList(this.rates), this.meanBinHeight, this.varianceInBinHeights);
        this.kurtosisStandardError =  Descriptive.sampleKurtosisStandardError(this.nBins);
    }

    //  Public methods

    // Time series attributes
    
    //// Target Name
    public void setTargetName(String targetName) {
        this.targetName = targetName;
	this.targetNameIsSet = true;
    }
    public String targetName() {
	if ( !this.targetNameIsSet ) {
	    logger.warn("Target name is not defined: Returning empty string");
	}
	return new String(this.targetName);
    }
    public boolean targetNameIsSet() {
	return this.targetNameIsSet;
    }

    ////  Target RA, Dec
    public void setTargetRaDec(double ra, double dec) {
	this.targetRA = ra;
	this.targetDec = dec;
	this.targetRaDec = new Point2D.Double(ra, dec);
	this.targetRaDecAreSet = true;
    }
    public void setTargetRaDec(Point2D.Double raDec) {
	setTargetRaDec(raDec.getX(), raDec.getY());
    }
    public void setTargetRaDec(double[] raDec) {
	setTargetRaDec(raDec[0], raDec[1]);
    }
    public double targetRA() {
	if ( !this.targetRaDecAreSet ) {
	    logger.warn("Target RA, Dec are not defined: Returning Double.NaN");
	}
	return this.targetRA;
    }
    public double targetDec() {
	if ( !this.targetRaDecAreSet ) {
	    logger.warn("Target RA, Dec are not defined: Returning Double.NaN");
	}
	return this.targetDec;
    }
    public Point2D.Double targetRaDec() {
	if ( !this.targetRaDecAreSet ) {
	    logger.warn("Target RA, Dec are not defined: Returning null object");
	}
	return this.targetRaDec;
    }
    public boolean targetRaDecAreSet() {
	return this.targetRaDecAreSet;
    }

    ////  Energy range
    public void setEnergyRange(double eMin, double eMax) {
	this.energyRangeMin = eMin;
	this.energyRangeMax = eMax;
	this.energyRange = new Point2D.Double(eMin, eMax);
	this.energyRangeIsSet = true;
    }
    public void setEnergyRange(Point2D.Double eMinMax) {
	setEnergyRange(eMinMax.getX(), eMinMax.getY());
    }
    public void setEnergyRange(double[] eMinMax) {
	setEnergyRange(eMinMax[0], eMinMax[1]);
    }
    public double energyRangeMin() {
	if ( !this.energyRangeIsSet ) {
	    logger.warn("Energy range is not defined: Returning Double.NaN");
	}
	return this.energyRangeMin;
    }
    public double energyRangeMax() {
	if ( !this.energyRangeIsSet ) {
	    logger.warn("Energy range is not defined: Returning Double.NaN");
	}
	return this.energyRangeMax;
    }
    public Point2D.Double energyRange() {
	if ( !this.energyRangeIsSet ) {
	    logger.warn("Energy range is not defined: Returning Double.NaN");
	}
	return (Point2D.Double)energyRange.clone();
    }

    ////  Time unit
    public String timeUnit() {
	return new String(this.timeUnit);
    }

    ////  Telescope
    public void setTelescope(String telescope) {
        this.telescope = telescope;
	this.telescopeIsSet = true;
    }
    public String telescope() {
	if ( !this.telescopeIsSet ) {
	    logger.warn("Telescope is not defined: Returning empty string");
	}
	return new String(this.telescope);
    }
    public boolean telescopeIsSet() {
	return this.telescopeIsSet;
    }

    ////  Instrument
    public void setInstrument(String instrument) {
        this.instrument = instrument;
	this.instrumentIsSet = true;
    }
    public String instrument() {
	if ( !this.instrumentIsSet ) {
	    logger.warn("Instrument is not defined: Returning empty string");
	}
	return new String(this.instrument);	    
    }
    public boolean instrumentIsSet() {
	return this.instrumentIsSet;
    }

    ////  Mjdref
    public void setMJDref(double mjdref) {
        this.mjdref = mjdref;
	this.mjdrefIsSet = true;
    }
    public double mjdref() {
	if ( !this.mjdrefIsSet ) {
	    logger.warn("MJD ref is not defined: Returning Double.NaN");
	}
	return this.mjdref;
    }
    public boolean mjdrefIsSet() {
	return this.mjdrefIsSet;
    }

    ////  Dates and times of observations
    public void setDateObsEnd(String dateObs, String dateEnd) {
	this.dateObs = new String(dateObs);
	this.dateEnd = new String(dateEnd);
	this.dateObsEndAreSet = true;
    }
    public void setTimeObsEnd(String timeObs, String timeEnd) {
	this.timeObs = new String(timeObs);
	this.timeEnd = new String(timeEnd);
	this.timeObsEndAreSet = true;
    }
    ////  DATE-OBS and DATE-END
    public String dateObs() {
	if ( !this.dateObsEndAreSet ) {
	    logger.warn("DATE-OBS and DATE-END are not set: Returning empty string");
	}
	return this.dateObs;
    }
    public String dateEnd() {
	if ( !this.dateObsEndAreSet ) {
	    logger.warn("DATE-OBS and DATE-END are not set: Returning empty string");
	}
	return this.dateEnd;
    }
    public String[] dateObsEnd() {
	if ( !this.dateObsEndAreSet ) {
	    logger.warn("DATE-OBS and DATE-END are not set: Returning empty strings");
	}
	return new String[] {new String(this.dateObs), new String(this.dateEnd)};
    }
    ////  TIME-OBS and TIME-END
    public String timeObs() {
	if ( !this.timeObsEndAreSet ) {
	    logger.warn("TIME-OBS and TIME-END are not set: Returning empty string");
	}
	return this.timeObs;
    }
    public String timeEnd() {
	if ( !this.timeObsEndAreSet ) {
	    logger.warn("TIME-OBS and TIME-END are not set: Returning empty string");
	}
	return this.timeEnd;
    }
    public String[] timeObsEnd() {
	if ( !this.timeObsEndAreSet ) {
	    logger.warn("TIME-OBS and TIME-END are not set: Returning empty strings");
	}
	return new String[] {new String(this.timeObs), new String(this.timeEnd)};
    }

    ////  Time error
    // setters
    public void setRelTimeError(double relTimeError) {
	this.relTimeError = relTimeError;
	this.relTimeErrorIsSet = true;
    }
    public void setAbsTimeError(double absTimeError) {
	this.absTimeError = absTimeError;
	this.absTimeErrorIsSet = true;
    }
    public void setTimeErrors(double relTimeError, double absTimeError) {
	this.relTimeError = relTimeError;
	this.relTimeErrorIsSet = true;
	this.absTimeError = absTimeError;
	this.absTimeErrorIsSet = true;
    }

    // getters
    public double relTimeError() {
	if ( !this.relTimeErrorIsSet ) {
	    logger.warn("Relative time error is not set: Returning Double.NaN");
	}
	return this.relTimeError;
    }
    public double absTimeError() {
	if ( !this.absTimeErrorIsSet ) {
	    logger.warn("Absolute time error is not set: Returning Double.NaN");
	}
	return this.absTimeError;
    }
    public double[] getTimeErrors() {
	double relTimeError = relTimeError();
	double absTimeError = absTimeError();
	return new double[] {relTimeError, absTimeError};
    }
    
	
    //  About Bins
    public int nBins() { return this.nBins; }
    public double tStart() { return this.tStart; }
    public double tStop() { return this.tStop; }
    public double tMid() { return this.tMid; }
    public double duration() { return this.duration; }
    public double[] getBinCentres() { return Arrays.copyOf(this.binCentres, this.binCentres.length); }
    public double[] getBinWidths() { return Arrays.copyOf(this.binWidths, this.binWidths.length); }
    public double[] getHalfBinWidths() { return Arrays.copyOf(this.halfBinWidths, this.halfBinWidths.length); }
    public double[] getBinEdges() { return Arrays.copyOf(this.binEdges, this.binEdges.length); }
    public double[] getLeftBinEdges() { return Arrays.copyOf(this.leftBinEdges, this.leftBinEdges.length); }
    public double[] getRightBinEdges() { return Arrays.copyOf(this.rightBinEdges, this.rightBinEdges.length); }
    public double binCentreAtMinBinHeight() { return this.binCentreAtMinBinHeight; }
    public double binCentreAtMaxBinHeight() { return this.binCentreAtMaxBinHeight; }
    public double minBinWidth() { return this.minBinWidth; }
    public double maxBinWidth() { return this.maxBinWidth; }
    public double avgBinWidth() { return this.avgBinWidth; }
    public double binWidth()  throws TimeSeriesException {
        if ( !this.binWidthIsConstant ) {
	    throw new TimeSeriesException("BinWidth is not constant. Use getBinWidths()");
	}
	return binWidths[0];
    }
    public double sumOfBinWidths() { return this.sumOfBinWidths; }
    public double ontime() { return this.sumOfBinWidths; }
    //  abstract methods relating to time on target
    public abstract double livetime();
    public abstract double exposureOnTarget();
    
    //  About Gaps
    public int nGaps() { return this.nGaps; }
    public double[] getGapEdges() { return Arrays.copyOf(this.gapEdges, this.gapEdges.length); }
    public double[] getGapLengths() { return Arrays.copyOf(this.gapLengths, this.gapLengths.length); }
    public double meanGap() { return this.meanGap; }
    public double minGap() { return this.minGap; }
    public double maxGap() { return this.maxGap; }
    public double sumOfGaps() { return this.sumOfGaps; }
    public int nSamplingFunctionBins() { return this.nSamplingFunctionBins; }
    public double[] getSamplingFunctionValues() { return Arrays.copyOf(this.samplingFunctionValues, this.samplingFunctionValues.length); }
    public double[] getSamplingFunctionBinEdges() { return Arrays.copyOf(this.samplingFunctionBinEdges, this.samplingFunctionBinEdges.length); }
    
    //  About Intensities
    public double[] getBinHeights() { return Arrays.copyOf(this.binHeights, this.binHeights.length); }
    public double sumOfBinHeights() { return this.sumOfBinHeights; }
    public double meanBinHeight() { return this.meanBinHeight; }
    public double minBinHeight() { return this.minBinHeight; }
    public double maxBinHeight() { return this.maxBinHeight; }
    public double varianceInBinHeights() { return this.varianceInBinHeights; }
    public double meanDeviationInBinHeights() { return this.meanDeviationInBinHeights; }
    public double kurtosisInBinHeights() { return this.kurtosisInBinHeights; }
    public double kurtosisStandardError() { return this.kurtosisStandardError; }
    public double skewnessInBinHeights() { return this.skewnessInBinHeights; }
    public double skewnessStandardError() { return this.skewnessStandardError; }
    public double[] getRates() { return Arrays.copyOf(this.rates, this.rates.length); }
    public double meanRate() { return this.meanRate; }
    public double minRate() { return this.minRate; }
    public double maxRate() { return this.maxRate; }
    public double errorOnMeanRate() { return this.errorOnMeanRate; }
    public double weightedMeanRate() { return this.weightedMeanRate; }
    public double errorOnWeightedMeanRate() { return this.errorOnWeightedMeanRate; }
    public double varianceInRates() { return this.varianceInRates; }
    public double meanDeviationInRates() { return this.meanDeviationInRates; }
    public double kurtosisInRates() { return this.kurtosisInRates; }
    public double skewnessInRates() { return this.skewnessInRates; }
    public double[] getErrorsOnRates() {
        if ( errorsAreSet ) 
            return Arrays.copyOf(this.errorsOnRates, this.errorsOnRates.length);
        else {
            double[] errorsOnRates = new double[this.nBins];
            double uncertainty = Math.sqrt(this.meanBinHeight);
            for ( int i=0; i < this.nBins; i++ ) {
                //double uncertainty = Math.sqrt(this.binHeights[i]);
                errorsOnRates[i] = uncertainty/this.binWidths[i];
            }
            return errorsOnRates;
        }
    }
    public double[] getMeanSubtractedRates() { 
        double[] meanSubRates = new double[this.nBins];
        for ( int i=0; i < this.nBins; i++ ) {
            meanSubRates[i] = this.rates[i] - this.meanRate;
        }
        return meanSubRates;
    }
    public double[] getMeanSubtractedBinHeights() { 
        double[] meanSubBinHeights = new double[this.nBins];
        for ( int i=0; i < this.nBins; i++ ) {
            meanSubBinHeights[i] = this.binHeights[i] - this.meanBinHeight;
        }
        return meanSubBinHeights;
    }
    
    //  Boolean checkers
    public boolean binWidthIsConstant() { return this.binWidthIsConstant; }
    public boolean thereAreGaps() { return this.thereAreGaps; }
    public boolean thereAreNaNs() { return this.thereAreNaNs; }
    public boolean errorsAreSet() { return this.errorsAreSet; }
    
    //  Write as QDP
    public void writeCountsAsQDP(String filename) throws IOException {
	QDPTimeSeriesFileWriter.writeCounts(this, filename);
    }
    public void writeCountsAsQDP(double[] function, String filename) throws IOException, TimeSeriesFileFormatException {
	QDPTimeSeriesFileWriter.writeCounts(this, function, filename);
    }
    public void writeCountsAndSamplingAsQDP(String filename) throws IOException, TimeSeriesFileFormatException {
    	QDPTimeSeriesFileWriter.writeCountsAndSampling(this, filename);
    }
    public void writeRatesAsQDP(String filename) throws IOException {
    	QDPTimeSeriesFileWriter.writeRates(this, filename);
    }
    public void writeRatesAsQDP(double[] function, String filename) throws IOException, TimeSeriesFileFormatException {
    	QDPTimeSeriesFileWriter.writeRates(this, function, filename);
    }
    public void writeRatesAndSamplingAsQDP(String filename) throws IOException, TimeSeriesFileFormatException {
    	QDPTimeSeriesFileWriter.writeRatesAndSampling(this, filename);
    }

    // //  Write as Fits
    public void writeCountsAsFits(String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeCounts(this, filename);
    }
    public void writeCountsAsFits(double[] function, String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeCounts(this, function, filename);
    }
    public void writeCountsAndSamplingAsFits(String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeCountsAndSampling(this, filename);
    }
    public void writeRatesAsFits(String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeRates(this, filename);
    }
    public void writeRatesAsFits(double[] function, String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeRates(this, function, filename);
    }
    public void writeRatesAndSamplingAsFits(String filename) throws IOException, FitsException {
    	FitsTimeSeriesFileWriter.writeRatesAndSampling(this, filename);
    }

    
    // //  Write as JS
    public void writeCountsAsJS(String filename) throws IOException {
	JSTimeSeriesFileWriter.writeCounts(this, filename);
    }
    // public void writeCountsAsJS(double[] function, String filename) throws IOException, TimeSeriesFileFormatException {
    //  	JSTimeSeriesFileWriter.writeCounts(this, function, filename);
    // }
    // public void writeCountsAndSamplingAsJS(String filename) throws IOException, TimeSeriesFileFormatException {
    //     JSTimeSeriesFileWriter.writeCountsAndSampling(this, filename);
    // }
    // public void writeRatesAsJS(String filename) throws IOException {
    // 	JSTimeSeriesFileWriter.writeRates(this, filename);
    // }
    // public void writeRatesAsJS(double[] function, String filename) throws IOException {
    // 	JSTimeSeriesFileWriter.writeRates(this, function, filename);
    // }
    // public void writeRatesAndSamplingAsJS(String filename) throws IOException, TimeSeriesFileFormatException {
    //  	JSTimeSeriesFileWriter.writeRatesAndSampling(this, filename);
    // }
    

    // public void writeRatesAsPLT(String filename) throws IOException {
    // 	TimeSeriesWriter.writeRatesAsQDP(this, filename);
    // }
    
    // public void writeRatesAsXML(String filename) throws IOException {
    // 	TimeSeriesWriter.writeRatesAsQDP(this, filename);
    // }


}
























