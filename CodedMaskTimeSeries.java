package gb.esac.timeseries;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import java.awt.geom.Point2D;
import jsky.coords.WorldCoords;
import nom.tam.fits.FitsException;
import gb.esac.tools.CoordUtils;

/**
 * 
 *  The class <code>CodedMaskTimeSeries</code> extends <code>AbstractTimeSeries</code> and implements <code>ITimeSeries</code>
 *  to store the information that is relevant and important for a time series made from a coded mask instrument. This includes
 *  coordinates of the target, the energy range used to make the  time series, the angular distance from the pointing axis to 
 *  the target, and the maximum distance for full coding.
 *
 *  @author <a href="mailto:guilaume.belanger@esa.int">Guillaume Belanger</a>
 *  @version December 2016 (last modfied)
 *  @see gb.esac.timeseries#AbstractTimeSeries for details
 */


public class CodedMaskTimeSeries extends TimeSeries implements ITimeSeries  {

    //  Class variables
    private static Logger logger  = Logger.getLogger(CodedMaskTimeSeries.class);
    private double maxDistForFullCoding;
    private int nFullyCoded;
    private double fullyCodedFraction;
    
    // about Ra, Dec of pointing
    private boolean raDecsOfPointingsAreSet = false;
    private Point2D.Double[] raDecsOfPointings;
    private double[] rasOfPointings;
    private double[] decsOfPointings;
    private double minRaOfPointings;
    private double maxRaOfPointings;
    private double minDecOfPointings;
    private double maxDecOfPointings;
    private Point2D.Double avgRaDecOfPointings;
    private double[] distToAvgRaDec;
    private double meanDistToAvgRaDec;
    private double varianceInPointingDirections;
    private double meanDeviationInPointingDirections;
    private double sumOfDistToAvgRaDec;
    private double sumOfSquaredDistToAvgRaDec;
    private int nNonNaN_raDecs;

    
    // about pointing durations
    private double[] effectivePointingDurations;
    private double[] deadTimeDurations;
    private double[] liveTimeFractions;
    private double[] deadTimeFractions;
    private double minEffectivePointingDuration;
    private double maxEffectivePointingDuration;
    private double meanEffectivePointingDuration;
    private double varianceInEffectivePointingDuration;
    private double meanDeviationInEffectivePointingDuration;    
    private double sumOfEffectivePointingDurations;
    private double sumOfSquaredEffectivePointingDurations;
    private double minDeadTimeDuration;
    private double maxDeadTimeDuration;
    private double meanDeadTimeDuration;
    private double sumOfDeadTimeDurations;
    private double minLiveTimeFraction;
    private double maxLiveTimeFraction;
    private double meanLiveTimeFraction;
    private double sumOfLiveTimeFractions;
    private double minDeadTimeFraction;
    private double maxDeadTimeFraction;
    private double meanDeadTimeFraction;
    private double sumOfDeadTimeFractions;
    private int nNonNaN_pointingDurations;
    
    
    // about distances to pointing axis
    private double[] distToPointingAxis;
    private double minDistToPointingAxis;
    private double maxDistToPointingAxis;
    private double sumOfDistToPointingAxis;
    private double sumOfSquaredDistToPointingAxis;
    private double meanDistToPointingAxis;
    private double varianceInDistToPointingAxis;
    private double meanDeviationInDistToPointingAxis;
    private int nNonNaN_distToPointingAxis;
    
    // about exposure on target
    private boolean exposuresOnTargetAreSet = false;    
    private double[] exposuresOnTarget = null;
    private double minExposureOnTarget = Double.NaN;
    private double maxExposureOnTarget = Double.NaN;
    private double sumOfExposuresOnTarget = Double.NaN;
    private double sumOfSquaredExposuresOnTarget = Double.NaN;
    private double meanExposureOnTarget = Double.NaN;
    private double varianceInExposureOnTarget = Double.NaN;
    private double meanDeviationInExposureOnTarget = Double.NaN;
    private int nNonNaN_exposuresOnTarget = (int)Double.NaN;
    

    //  Constructors

    // Minimal
    CodedMaskTimeSeries(double maxDistForFullCoding, double[] binEdges, double[] effectivePointingDurations, double[] rates, double[] errors, double[] distToPointingAxis) {
	if ( Double.isNaN(maxDistForFullCoding) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attribute maxDistForFullCoding");
	}
	if ( effectivePointingDurations == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires effective durations of pointings");
	}
	if ( distToPointingAxis == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires angle information for each pointing");
	}
	double tStart = binEdges[0];
	setBinEdges(tStart, binEdges);
	setRates(rates);
	setErrorsOnRates(errors);
	printRateInfo();
	setMaxDistForFullCoding(maxDistForFullCoding);
	setPointingDurations(effectivePointingDurations);
	setDistToPointingAxis(distToPointingAxis);
	printCodedMaskInfo();
    }

    
    CodedMaskTimeSeries(Point2D.Double targetRaDec, Point2D.Double energyRangeMinMax, String telescope, String instrument, double maxDistForFullCoding, double tStart, double[] binEdges,
			double[] effectivePointingDurations, double[] rates, double[] errors, Point2D.Double[] raDecsOfPointings, double[] exposuresOnTarget) throws IllegalArgumentException {
	if ( Double.isNaN(targetRaDec.getX()) || Double.isNaN(targetRaDec.getY()) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attributes: targetRA, targetDec");
	}
	if ( Double.isNaN(energyRangeMinMax.getX()) || Double.isNaN(energyRangeMinMax.getY()) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attributes: energyRangeMin, energyRangeMax");
	}
	if ( Double.isNaN(maxDistForFullCoding) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attribute maxDistForFullCoding");
	}
	if ( effectivePointingDurations == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires effective durations of pointings");
	}
	if ( raDecsOfPointings == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires angle information for each pointings");
	}
	if ( exposuresOnTarget == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires effective exposures on target");
	}
        setBinEdges(tStart, binEdges);
        setRates(rates);
        setErrorsOnRates(errors);
        printRateInfo();
	setTelescope(telescope);
	setInstrument(instrument);
	setTargetRaDec(targetRaDec);
	setEnergyRange(energyRangeMinMax);
	setMaxDistForFullCoding(maxDistForFullCoding);
	setPointingDurations(effectivePointingDurations);
	// optional
	setRaDecsOfPointings(raDecsOfPointings);
	setExposures(exposuresOnTarget);
	printCodedMaskInfo();
    }
    
    //  Constructor that defines distances to pointing axis, WITHOUT: RA, Dec of pointings, and exposures on target
    CodedMaskTimeSeries(Point2D.Double targetRaDec, Point2D.Double energyRangeMinMax,  String telescope, String instrument, double maxDistForFullCoding, double tStart, double[] binEdges,
			double[] effectivePointingDurations, double[] rates, double[] errors, double[] distToPointingAxis) throws IllegalArgumentException {
	if ( Double.isNaN(targetRaDec.getX()) || Double.isNaN(targetRaDec.getY()) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attributes: targetRA, targetDec");
	}
	if ( Double.isNaN(energyRangeMinMax.getX()) || Double.isNaN(energyRangeMinMax.getY()) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attributes: energyRangeMin, energyRangeMax");
	}
	if ( Double.isNaN(maxDistForFullCoding) ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires attribute maxDistForFullCoding");
	}
	if ( effectivePointingDurations == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires effective durations of pointings");
	}
	if ( distToPointingAxis == null ) {
	    throw new IllegalArgumentException("CodedMaskTimeSeries requires angle information for each pointing");
	}
        setBinEdges(tStart, binEdges);
        setRates(rates);
        setErrorsOnRates(errors);
        printRateInfo();
	setTelescope(telescope);	
	setInstrument(instrument);
	setTargetRaDec(targetRaDec);
	setEnergyRange(energyRangeMinMax);
	setMaxDistForFullCoding(maxDistForFullCoding);
	setPointingDurations(effectivePointingDurations);
	setDistToPointingAxis(distToPointingAxis);
	printCodedMaskInfo();
    }
    
    // private info-printing 
    public void printCodedMaskInfo() {
	logger.info("CodedMaskTimeSeries extends TimeSeries");
	logger.info("RA of target = "+this.targetRA());
	logger.info("Dec of target = "+this.targetDec());
	logger.info("Energy range = "+this.energyRangeMin()+" - "+this.energyRangeMax());
	logger.info("Max distance for full coding = "+this.maxDistForFullCoding);
	logger.info("Telescope = "+this.telescope());
	logger.info("Instrument = "+this.instrument());
	if ( this.raDecsOfPointingsAreSet ) {
	    logger.info("Number of non-NaN pointing directions (RA, Dec) = "+this.nNonNaN_raDecs);
	}
	logger.info("Number of non-NaN pointing durations = "+this.nNonNaN_pointingDurations);
	logger.info("  Total = "+this.sumOfEffectivePointingDurations);
	logger.info("  Mean effective duration = "+this.meanEffectivePointingDuration);
	logger.info("  Min = "+this.minEffectivePointingDuration);
	logger.info("  Max = "+this.maxEffectivePointingDuration);
	logger.info("  Mean deviation = "+this.meanDeviationInEffectivePointingDuration);
	logger.info("Dead time duration:");
	logger.info("  Total = "+this.sumOfDeadTimeDurations);
	logger.info("  Mean deadtime duration = "+this.meanDeadTimeDuration);
	logger.info("  Min = "+this.minDeadTimeDuration);
	logger.info("  Max = "+this.maxDeadTimeDuration);
	logger.info("Live time fraction:");
	logger.info("  Mean livetime fraction = "+this.meanLiveTimeFraction);
	logger.info("  Min = "+this.minLiveTimeFraction);
	logger.info("  Max = "+this.maxLiveTimeFraction);
	logger.info("Dead time fraction:");
	logger.info("  Mean deadtime fraction = "+this.meanDeadTimeFraction);
	logger.info("  Min = "+this.minDeadTimeFraction);
	logger.info("  Max = "+this.maxDeadTimeFraction);
	logger.info("Number of non-NaN distances to pointing axis = "+this.nNonNaN_distToPointingAxis);	
	logger.info("  Fully coded points = "+this.nFullyCoded);
	logger.info("  Fully coded fraction = "+this.fullyCodedFraction);
	logger.info("  Mean distance = "+this.meanDistToPointingAxis);
	logger.info("  Min = "+this.minDistToPointingAxis);
	logger.info("  Max = "+this.maxDistToPointingAxis);
	logger.info("  Mean deviation = "+this.meanDeviationInDistToPointingAxis);
	if ( this.exposuresOnTargetAreSet ) {
	    logger.info("Number of non-NaN exposures on target = "+this.nNonNaN_exposuresOnTarget);
	    logger.info("  Total = "+this.sumOfExposuresOnTarget);
	    logger.info("  Mean exposure = "+this.meanExposureOnTarget);
	    logger.info("  Min = "+this.minExposureOnTarget);
	    logger.info("  Max = "+this.maxExposureOnTarget);
	    logger.info("  Mean deviation = "+this.meanDeviationInExposureOnTarget);
	}
    }

    private void setMaxDistForFullCoding(double maxDistForFullCoding) {
	this.maxDistForFullCoding = maxDistForFullCoding;
    }
    
    private void setRaDecsOfPointings(Point2D.Double[] raDecsOfPointings) {
	this.raDecsOfPointings = new Point2D.Double[raDecsOfPointings.length];
	this.rasOfPointings = new double[raDecsOfPointings.length];
	this.decsOfPointings = new double[raDecsOfPointings.length];
	this.distToPointingAxis = new double[raDecsOfPointings.length];
	WorldCoords targetCoords = new WorldCoords(this.targetRA(), this.targetDec());
	int nNonNaN_raDecs = 0;
	int nNonNaN_distToPointingAxis = 0;
	int nFullyCoded = 0;
	double minRaOfPointings = Double.MAX_VALUE;
	double maxRaOfPointings = -Double.MAX_VALUE;
	double minDecOfPointings = Double.MAX_VALUE;
	double maxDecOfPointings = -Double.MAX_VALUE;
	double minDistToPointingAxis = Double.MAX_VALUE;
	double maxDistToPointingAxis = -Double.MAX_VALUE;
	double[] rates = this.getRates();
	double sumOfDist = 0;
	double sum2OfDist = 0;
	for ( int i=0; i < raDecsOfPointings.length; i++ ) {
	    this.raDecsOfPointings[i] = raDecsOfPointings[i];
	    this.rasOfPointings[i] = raDecsOfPointings[i].getX();
	    this.decsOfPointings[i] = raDecsOfPointings[i].getY();
	    if ( Double.isNaN(this.rasOfPointings[i]) || Double.isNaN(this.decsOfPointings[i]) ) {
		if ( ! Double.isNaN(rates[i]) ) {
		    logger.warn("There is a NaN value in RA or Dec whose corresponding rate is not NaN.");
		}
	    }
	    else {
		nNonNaN_raDecs++;
		minRaOfPointings = Math.min(minRaOfPointings, this.rasOfPointings[i]);
		maxRaOfPointings = Math.max(maxRaOfPointings, this.rasOfPointings[i]);
		minDecOfPointings = Math.min(minDecOfPointings, this.decsOfPointings[i]);
		maxDecOfPointings = Math.max(maxDecOfPointings, this.decsOfPointings[i]);
	    }
	    WorldCoords pointingCoords = new WorldCoords(this.rasOfPointings[i], this.decsOfPointings[i]);
	    double dist = targetCoords.dist(pointingCoords); // returns arc minutes
	    double distInDeg = dist/60.;
	    this.distToPointingAxis[i] = distInDeg;
	    if ( Double.isNaN(this.distToPointingAxis[i]) ) {
		if ( ! Double.isNaN(rates[i]) ) {
		    logger.warn("There is a NaN value in distance from target to pointing axis whose corresponding rate is not NaN.");
		}
	    }
	    else {
		nNonNaN_distToPointingAxis++;
		minDistToPointingAxis = Math.min(minDistToPointingAxis, distInDeg);
		maxDistToPointingAxis = Math.max(maxDistToPointingAxis, distInDeg);
		sumOfDist += distInDeg;
		sum2OfDist += distInDeg*distInDeg;
		if ( distInDeg <= this.maxDistForFullCoding ) {
		    nFullyCoded++;
		}
	    }
	}
	this.nNonNaN_raDecs = nNonNaN_raDecs;
	this.nNonNaN_distToPointingAxis = nNonNaN_distToPointingAxis;
	this.nFullyCoded = nFullyCoded;
	this.fullyCodedFraction = (double)this.nFullyCoded/(double)this.nNonNaN_distToPointingAxis;
	this.minDistToPointingAxis = minDistToPointingAxis;
	this.maxDistToPointingAxis = maxDistToPointingAxis;
	this.sumOfDistToPointingAxis = sumOfDist;
	this.sumOfSquaredDistToPointingAxis = sum2OfDist;
	this.minRaOfPointings = minRaOfPointings;
	this.maxRaOfPointings = maxRaOfPointings;
	this.minDecOfPointings = minDecOfPointings;
	this.maxDecOfPointings = maxDecOfPointings;
	this.avgRaDecOfPointings = CoordUtils.getAverageRaDec(this.raDecsOfPointings);
	WorldCoords avgPointingCoords = new WorldCoords(this.avgRaDecOfPointings.getX(), this.avgRaDecOfPointings.getY());
	this.distToAvgRaDec = new double[raDecsOfPointings.length];
	for ( int i=0; i < raDecsOfPointings.length; i++ ) {
	    WorldCoords pointingCoords = new WorldCoords(this.rasOfPointings[i], this.decsOfPointings[i]);
	    double dist = avgPointingCoords.dist(pointingCoords); // returns arc minutes
	    double distInDeg = dist/60.;
	    this.distToAvgRaDec[i] = distInDeg;
	    this.sumOfDistToAvgRaDec += distInDeg;
	    this.sumOfSquaredDistToAvgRaDec += distInDeg*distInDeg;
	}
	setStatsOnRaDecsOfPointings();
	setStatsOnDistToPointingAxis();
    }

    private void setStatsOnRaDecsOfPointings() {
	this.meanDistToAvgRaDec = this.sumOfDistToAvgRaDec/this.nNonNaN_raDecs;
	this.varianceInPointingDirections = Descriptive.sampleVariance(this.nNonNaN_raDecs, this.sumOfDistToAvgRaDec, this.sumOfSquaredDistToAvgRaDec);
	this.meanDeviationInPointingDirections = Descriptive.meanDeviation(new DoubleArrayList(this.distToAvgRaDec), this.meanDistToAvgRaDec);
	this.raDecsOfPointingsAreSet = true;
    }

    private void setDistToPointingAxis(double[] distToPointingAxis) {
	this.distToPointingAxis = new double[distToPointingAxis.length];
	int nNonNaN_distToPointingAxis = 0;
	int nFullyCoded = 0;
	double minDistToPointingAxis = Double.MAX_VALUE;
	double maxDistToPointingAxis = -Double.MAX_VALUE;
	double[] rates = this.getRates();
	double sumOfDist = 0;
	double sum2OfDist = 0;
	for ( int i=0; i < distToPointingAxis.length; i++ ) {
	    double distInDeg = distToPointingAxis[i];
	    this.distToPointingAxis[i] = distInDeg;
	    if ( Double.isNaN(this.distToPointingAxis[i]) ) {
		if ( ! Double.isNaN(rates[i]) ) {
		    logger.warn("There is a NaN value in distance to pointing axis whose corresponding rate is not NaN.");
		}
	    }
	    else {
		nNonNaN_distToPointingAxis++;
		minDistToPointingAxis = Math.min(minDistToPointingAxis, distInDeg);
		maxDistToPointingAxis = Math.max(maxDistToPointingAxis, distInDeg);
		sumOfDist += distInDeg;
		sum2OfDist += distInDeg*distInDeg;
		if ( distInDeg <= this.maxDistForFullCoding ) {
		    nFullyCoded++;
		}
	    }
	}
	this.nNonNaN_distToPointingAxis = nNonNaN_distToPointingAxis;
	this.nFullyCoded = nFullyCoded;
	this.fullyCodedFraction = (double)this.nFullyCoded/(double)this.nNonNaN_distToPointingAxis;	
	this.minDistToPointingAxis = minDistToPointingAxis;
	this.maxDistToPointingAxis = maxDistToPointingAxis;
	this.sumOfDistToPointingAxis = sumOfDist;
	this.sumOfSquaredDistToPointingAxis = sum2OfDist;
	setStatsOnDistToPointingAxis();
    }
    
    private void setStatsOnDistToPointingAxis() {
	this.meanDistToPointingAxis = this.sumOfDistToPointingAxis/this.nNonNaN_distToPointingAxis;
 	this.varianceInDistToPointingAxis = Descriptive.sampleVariance(this.nNonNaN_distToPointingAxis, this.sumOfDistToPointingAxis, this.sumOfSquaredDistToPointingAxis);
	this.meanDeviationInDistToPointingAxis = Descriptive.meanDeviation(new DoubleArrayList(this.distToPointingAxis), this.meanDistToPointingAxis);
    }

    private void setPointingDurations(double[] effectivePointingDurations) {
	this.effectivePointingDurations = new double[effectivePointingDurations.length];
	this.deadTimeDurations = new double[effectivePointingDurations.length];
	this.liveTimeFractions = new double[effectivePointingDurations.length];
	this.deadTimeFractions = new double[effectivePointingDurations.length];
	double[] rates = this.getRates();
	double[] binWidths = this.getBinWidths();
	double min = Double.MAX_VALUE;
	double max = -Double.MAX_VALUE;
	double sum = 0;
	double sum2 = 0;
	double minDeadTime = Double.MAX_VALUE;
	double maxDeadTime = -Double.MAX_VALUE;
	double sumDeadTime = 0;
	double minLiveFraction = Double.MAX_VALUE;
	double maxLiveFraction = -Double.MAX_VALUE;
	double sumLiveFraction = 0;
	double minDeadFraction = Double.MAX_VALUE;
	double maxDeadFraction = -Double.MAX_VALUE;
	double sumDeadFraction = 0;
	int n = 0;
	for ( int i=0; i < this.nBins(); i++ ) {
	    this.effectivePointingDurations[i] = effectivePointingDurations[i];
	    if ( Double.isNaN(effectivePointingDurations[i]) ) {
		if ( ! Double.isNaN(rates[i]) ) {
		    logger.warn("There is a NaN value in RA or Dec whose corresponding rate is not NaN.");
		}
		this.deadTimeDurations[i] = Double.NaN;
		this.liveTimeFractions[i] = Double.NaN;
		this.deadTimeFractions[i] = Double.NaN;
	    }
	    else {
		n++;
		// effective duration
		sum += effectivePointingDurations[i];
		sum2 += effectivePointingDurations[i]*effectivePointingDurations[i];		
		min = Math.min(min, effectivePointingDurations[i]);
		max = Math.max(max, effectivePointingDurations[i]);
		// dead time
		double deadTime = binWidths[i] - effectivePointingDurations[i];
		this.deadTimeDurations[i] = deadTime;
		minDeadTime = Math.min(minDeadTime, deadTime);
		maxDeadTime = Math.max(maxDeadTime, deadTime);
		sumDeadTime += deadTime;
		// live time fraction
		double liveTimeFraction = effectivePointingDurations[i]/binWidths[i];
		this.liveTimeFractions[i] = liveTimeFraction;
		minLiveFraction = Math.min(minLiveFraction, liveTimeFraction);
		maxLiveFraction = Math.max(maxLiveFraction, liveTimeFraction);
		sumLiveFraction += liveTimeFraction;
		// dead time fraction
		double deadTimeFraction = 1. - liveTimeFraction; 
		this.deadTimeFractions[i] = deadTimeFraction;
		minDeadFraction = Math.min(minDeadFraction, deadTimeFraction);
		maxDeadFraction = Math.max(maxDeadFraction, deadTimeFraction);
		sumDeadFraction += deadTimeFraction;
	    }
	}
	this.nNonNaN_pointingDurations = n;
	this.sumOfEffectivePointingDurations = sum;
	this.sumOfSquaredEffectivePointingDurations = sum2;
	this.minEffectivePointingDuration = min;
	this.maxEffectivePointingDuration = max;

	this.sumOfDeadTimeDurations = sumDeadTime;
	this.minDeadTimeDuration = minDeadTime;
	this.maxDeadTimeDuration = maxDeadTime;

	this.sumOfLiveTimeFractions = sumLiveFraction;
	this.minLiveTimeFraction = minLiveFraction;
	this.maxLiveTimeFraction = maxLiveFraction;
	
	this.sumOfDeadTimeFractions = sumDeadFraction;
	this.minDeadTimeFraction = minDeadFraction;
	this.maxDeadTimeFraction = maxDeadFraction;
	setStatsOnPointingDurations();
    }

    private void setStatsOnPointingDurations() {
	this.meanEffectivePointingDuration = this.sumOfEffectivePointingDurations/this.nNonNaN_pointingDurations;
	this.varianceInEffectivePointingDuration = Descriptive.sampleVariance(this.nNonNaN_pointingDurations, this.sumOfEffectivePointingDurations, this.sumOfSquaredEffectivePointingDurations);
	this.meanDeviationInEffectivePointingDuration = Descriptive.meanDeviation(new DoubleArrayList(this.effectivePointingDurations), this.meanEffectivePointingDuration);	
	this.meanDeadTimeDuration = this.sumOfDeadTimeDurations/this.nNonNaN_pointingDurations;
	this.meanLiveTimeFraction = this.sumOfLiveTimeFractions/this.nNonNaN_pointingDurations;
	this.meanDeadTimeFraction = this.sumOfDeadTimeFractions/this.nNonNaN_pointingDurations;
    }
    
    private void setExposures(double[] exposuresOnTarget) {
	this.exposuresOnTarget = new double[exposuresOnTarget.length];
	double min = Double.MAX_VALUE;
	double max = -Double.MAX_VALUE;
	double sum = 0;
	double sum2 = 0;
	int n = 0;
	double[] rates = this.getRates();
	for ( int i=0; i < exposuresOnTarget.length; i++ ) {
	    this.exposuresOnTarget[i] = exposuresOnTarget[i];
	    if ( Double.isNaN(exposuresOnTarget[i]) ) {
		if ( ! Double.isNaN(rates[i]) ) {
		    logger.warn("There is a NaN value in effective exposures whose corresponding rate is not NaN.");
		}
	    }
	    else {
		min = Math.min(min, exposuresOnTarget[i]);
		max = Math.max(max, exposuresOnTarget[i]);
		sum += exposuresOnTarget[i];
		sum2 += exposuresOnTarget[i]*exposuresOnTarget[i];
		n++;
	    }
	}
	this.nNonNaN_exposuresOnTarget = n;
	this.minExposureOnTarget = min;
	this.maxExposureOnTarget = max;
	this.sumOfExposuresOnTarget = sum;
	this.sumOfSquaredExposuresOnTarget = sum2;
	setStatsOnExposures();
    }
    
    private void setStatsOnExposures() {
	this.meanExposureOnTarget = this.sumOfExposuresOnTarget/this.nNonNaN_exposuresOnTarget;
	this.varianceInExposureOnTarget = Descriptive.sampleVariance(this.nNonNaN_exposuresOnTarget, this.sumOfExposuresOnTarget, this.sumOfSquaredExposuresOnTarget);
	this.meanDeviationInExposureOnTarget = Descriptive.meanDeviation(new DoubleArrayList(this.exposuresOnTarget), this.meanExposureOnTarget);	
	this.exposuresOnTargetAreSet = true;
    }

    //  About coding 
    public double maxDistForFullCoding() { return this.maxDistForFullCoding; }
    public int nFullyCoded() { return this.nFullyCoded; }
    public double fullyCodedFraction() { return this.fullyCodedFraction; }

    // About RA and Dec of pointings
    public double[] getRasOfPointings() {
	if ( raDecsOfPointingsAreSet ) return Arrays.copyOf(this.rasOfPointings, this.rasOfPointings.length);
	else { logger.warn("Ra, Dec of pointings are not defined: Returning the null object."); return null; }
    }	
    public double[] getDecsOfPointings() {
	if ( raDecsOfPointingsAreSet ) return Arrays.copyOf(this.decsOfPointings, this.decsOfPointings.length);
	else { logger.warn("Ra, Dec of pointings are not defined: Returning the null object."); return null; }
    }		
    public Point2D.Double[] getRaDecsOfPointings() {
	if ( raDecsOfPointingsAreSet ) return Arrays.copyOf(this.raDecsOfPointings, this.raDecsOfPointings.length);
	else { logger.warn("Ra, Dec of pointings are not defined: Returning the null object."); return null; }
    }
    public int nNonNaN_raDecs() {
	if ( raDecsOfPointingsAreSet ) return this.nNonNaN_raDecs;
	else { logger.warn("Ra, Dec of pointings are not defined: Returning zero."); return 0; }
    }
    public double minRaOfPointings() {
	if ( raDecsOfPointingsAreSet ) return this.minRaOfPointings;
	else { logger.warn("Ra, Dec of pointings are not defined: Returning zero."); return 0; }
    }
    public double maxRaOfPointings() {
	if ( raDecsOfPointingsAreSet ) return this.maxRaOfPointings;
	else { logger.warn("Ra, Dec of pointings are not defined: Returning zero."); return 0; }
    }
    public double minDecOfPointings() {
	if ( raDecsOfPointingsAreSet ) return this.minDecOfPointings;
	else { logger.warn("Ra, Dec of pointings are not defined: Returning zero."); return 0; }	
    }
    public double maxDecOfPointings() {
	if ( raDecsOfPointingsAreSet ) return this.maxDecOfPointings;
	else { logger.warn("Ra, Dec of pointings are not defined: Returning zero."); return 0; }	
    }
    // public double meanRaDecOfPointings() { return this.meanRaDecOfPointings; }
    // public double meanDeviationInRasOfPointings(); { return this.meanDeviationInRasOfPointings(); }
    // public double meanDeviationInDecsOfPointings(); { return this.meanDeviationInDecsOfPointings(); }
    // public double varianceInRasOfPointings() { return this.varianceInRasOfPointings(); }
    // public double varianceInDecsOfPointings() { retutn this.varianceInDecsOfPointings();}

    //  About pointing durations
    public double[] getEffectivePointingDurations() { return Arrays.copyOf(this.effectivePointingDurations, this.effectivePointingDurations.length); }
    public double minEffectivePointingDuration() { return this.minEffectivePointingDuration; }
    public double maxEffectivePointingDuration() { return this.maxEffectivePointingDuration; }
    public double meanEffectivePointingDuration() { return this.meanEffectivePointingDuration; }
    public double sumOfEffectivePointingDurations() { return this.sumOfEffectivePointingDurations; }
    public double varianceInEffectivePointingDuration() { return this.varianceInEffectivePointingDuration; }
    public double meanDeviationInEffectivePointingDuration() { return this.meanDeviationInEffectivePointingDuration; }    
    public double[] getDeadTimeDurations() { return Arrays.copyOf(this.deadTimeDurations, this.deadTimeDurations.length); }
    public double minDeadTimeDuration() { return this.minDeadTimeDuration; }
    public double maxDeadTimeDuration() { return this.maxDeadTimeDuration; }
    public double meanDeadTimeDuration() { return this.meanDeadTimeDuration; }
    public double sumOfDeadTimeDurations() { return this.sumOfDeadTimeDurations; }
    public double[] getLiveTimeFractions() { return Arrays.copyOf(this.liveTimeFractions, this.liveTimeFractions.length); }
    public double minLiveTimeFraction() { return this.minLiveTimeFraction; }
    public double maxLiveTimeFraction() { return this.maxLiveTimeFraction; }
    public double meanLiveTimeFraction() { return this.meanLiveTimeFraction; }
    public double sumOfLiveTimeFractions() { return this.sumOfLiveTimeFractions; }
    public double[] getDeadTimeFractions() { return Arrays.copyOf(this.deadTimeFractions, this.deadTimeFractions.length); }    
    public double minDeadTimeFraction() { return this.minDeadTimeFraction; }
    public double maxDeadTimeFraction() { return this.maxDeadTimeFraction; }
    public double meanDeadTimeFraction() { return this.meanDeadTimeFraction; }
    public double sumOfDeadTimeFractions() { return this.sumOfDeadTimeFractions; }
    
    // About distance from pointing axis
    public double[] getDistToPointingAxis() { return Arrays.copyOf(this.distToPointingAxis, this.distToPointingAxis.length); }
    public int nNonNaN_distToPointingAxis() { return this.nNonNaN_distToPointingAxis; }
    public double minDistToPointingAxis() { return this.minDistToPointingAxis; }
    public double maxDistToPointingAxis() { return this.maxDistToPointingAxis; }
    public double meanDistToPointingAxis() { return this.meanDistToPointingAxis; }
    public double varianceInDistToPointingAxis() { return this.varianceInDistToPointingAxis; }
    public double meanDeviationInDistToPointingAxis() { return this.meanDeviationInDistToPointingAxis; }
    public double sumOfDistToPointingAxis() { return this.sumOfDistToPointingAxis; }
    public double sumOfSquaredDistToPointingAxis() { return this.sumOfSquaredDistToPointingAxis; }

    // About exposures on target
    public void setExposureOnTarget(double exposureOnTarget) {
	if ( !this.exposuresOnTargetAreSet ) {
	    this.sumOfExposuresOnTarget = exposureOnTarget;
	}
    }
    public double[] getExposuresOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning null object.");
	}
	return Arrays.copyOf(this.exposuresOnTarget, this.exposuresOnTarget.length);
    }
    public int nNonNaN_exposuresOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.nNonNaN_exposuresOnTarget;
    }
    public double minExposureOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.minExposureOnTarget;
    }
    public double maxExposureOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.maxExposureOnTarget;
    }
    public double meanExposureOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.meanExposureOnTarget;
    }
    public double varianceInExposureOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.varianceInExposureOnTarget;
    }
    public double meanDeviationInExposureOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning NaN.");
	}
	return this.meanDeviationInExposureOnTarget;
    }
    public double sumOfSquaredExposuresOnTarget() {
	if ( !this.exposuresOnTargetAreSet ) {
	    logger.warn("Exposures on target are not defined: Returning the NaN.");
	}
	return this.sumOfSquaredExposuresOnTarget;
    }
    
    //  Abstract methods in AbstractTimeSeries that require implementation in each sub-class
    public double livetime() {
	return this.sumOfEffectivePointingDurations();
    }
    public double exposureOnTarget() {
	return this.sumOfExposuresOnTarget;
    }

    // Write methods that are specific to CodedMaskTimeSeries
    public void writeAllDataAsQDP(String filename) throws IOException {
	QDPTimeSeriesFileWriter.writeAllData(this, filename);
    }
    public void writeAllDataAsFits(String filename) throws IOException, FitsException {
	FitsTimeSeriesFileWriter.writeAllData(this, filename);
    }
    // public void writeAllDataAsJS(String filename) throws IOException {
    //     JSTimeSeriesFileWriter.writeAllData(this, filename);
    // }
     
}
