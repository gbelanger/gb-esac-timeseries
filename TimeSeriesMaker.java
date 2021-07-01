package gb.esac.timeseries;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;

import cern.colt.list.DoubleArrayList;
import hep.aida.IAnalysisFactory;
import hep.aida.IAxis;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import org.apache.log4j.Logger;

import gb.esac.binner.Binner;
import gb.esac.binner.BinningException;
import gb.esac.binner.BinningUtils;
import gb.esac.eventlist.EventList;
import gb.esac.eventlist.EventListException;
import gb.esac.eventlist.EventListSelector;


public final class TimeSeriesMaker {

    private static Logger logger  = Logger.getLogger(TimeSeriesMaker.class);

    //  Constructors    
    public static TimeSeries makeTimeSeries(TimeSeries ts) {
		return new TimeSeries(ts);
    }

    /**
     * Construct a <code>TimeSeries</code> from a file containing time series data.
     *
     * @param filename a <code>String</code> value
     * @return a <code>TimeSeries</code> value
     * @exception TimeSeriesException if an error occurs
     * @exception IOException if an error occurs
     * @exception Exception if an error occurs
     */
    public static ITimeSeries makeTimeSeries(String filename) throws Exception, IOException, TimeSeriesFileException {
		return TimeSeriesFileReader.readTimeSeriesFile(filename);
    }

    /**
     * Construct a <code>TimeSeries</code> from an <code>EventList</code> with the Nyquist (minimum) binwidth = 1/(2*minEventSpacing).
     *
     * @param evlist an <code>EventList</code> value
     * @return a <code>TimeSeries</code> value
     */
    public static TimeSeries makeTimeSeries(EventList evlist) throws TimeSeriesException {
		logger.info("Using effective Nyquist (minimum) binWidth defined as 1/(2*meanCountRate)");
		double effectiveNyquistBinWidth = 0.5/evlist.meanRate();
		int nBins = (int) Math.floor(evlist.duration()/effectiveNyquistBinWidth);
		return makeTimeSeries(evlist, nBins);
    }

    /**
     * Construct a <code>TimeSeries</code> from an <code>EventList</code> with the specified number of bins.
     *
     * @param evlist an <code>EventList</code> value
     * @param nBins an <code>int</code> value
     * @return a <code>TimeSeries</code> value
     */
    public static TimeSeries makeTimeSeries(EventList evlist, int nBins) throws TimeSeriesException {
		logger.info("Making TimeSeries from EventList using nBins = "+nBins);
		double binWidth = evlist.duration()/nBins;
		return makeTimeSeries(evlist, binWidth);
    }

    /**
     * Construct a <code>TimeSeries</code> from an <code>EventList</code> with the specified bin width.
     *
     * @param evlist an <code>EventList</code> value
     * @param binWidth a <code>double</code> value
     * @return a <code>TimeSeries</code> value
     */
    public static TimeSeries makeTimeSeries(EventList evlist, double binWidth) throws TimeSeriesException {
		logger.info("Making TimeSeries from EventList using binWidth = "+binWidth);

		//  Define number of bins
		double n = evlist.duration()/binWidth;

		//  Use this to drop the last partial bin
		int nBins = (int) Math.floor(n);
		double diff = n - nBins;
		double lastPartialBinWidth = diff*binWidth;
		int nIgnoredEvents = (int) Math.round(lastPartialBinWidth*evlist.meanRate());
		if ( nIgnoredEvents >= 1 ) {
		    logger.warn("  Ignoring last partial bin: "+lastPartialBinWidth+" s");
		    logger.warn("  This will result in ignoring approx "+nIgnoredEvents+" events");
		    logger.warn("  To use all events, specify a number of bins instead of a binWidth");
		}
	 	double[] t = evlist.getArrivalTimes();
		double tStart = t[0];
		double tStop = t[0] + evlist.duration() + 0.5*Math.ulp(0);
	 	double[] binEdges = null;
		try {
		    binEdges = BinningUtils.getBinEdges(tStart, tStop, binWidth);
		}
		catch ( BinningException e ) {
		    throw new TimeSeriesException("Cannot construct bin edges");
		}

		//  EITHER: Bin the data with Binner
		double[] counts = Binner.binData(t, binEdges);

		//  OR: Bin using Histogram1D
		
		// //  Create Histogram1D 
		// IAnalysisFactory af = IAnalysisFactory.create();
		// ITree tree = af.createTreeFactory().create();
		// IHistogramFactory hf = af.createHistogramFactory(tree);
		// double lowerEdge = evlist.tStart();
		// double upperEdge = lowerEdge + nBins*binWidth + 1e-6;
		// IHistogram1D histo = hf.createHistogram1D("Histo", nBins, lowerEdge, upperEdge);
		// int nOverflowEvents=0;
		// //  Fill with arrival times
		// double[] arrivalTimes = evlist.getArrivalTimes();
		// for ( int i=0; i < evlist.nEvents(); i++ ) {
		//     histo.fill(arrivalTimes[i]);
		//     if ( arrivalTimes[i] > upperEdge ) {
		// 	nOverflowEvents++;
		//     }
		// }
		// if ( nIgnoredEvents >= 1 ) {
		//     logger.info("Actual number of events that were dropped is: "+nOverflowEvents);
		// }
		// //  Get the counts in each bin and bin edges
		// IAxis histoAxis = histo.axis();
		// double[] counts = new double[nBins];
		// for ( int i=0; i < nBins; i++ ) {
		//     counts[i] = histo.binHeight(i);
		//     binEdges[2*i] = histoAxis.binLowerEdge(i);
		//     binEdges[2*i+1] = histoAxis.binUpperEdge(i);
		// }

		//  Return the TimeSeries
		double[] zeroedBinEdges = resetToZero(binEdges);
		return new TimeSeries(evlist.tStart(), zeroedBinEdges, counts);
    }

    /**
     * Construct a <code>TimeSeries</code> from an <code>EventList</code> with the specified number of bins, minimum and maximum energies.
     */
    public static TimeSeries makeTimeSeries(EventList evlist, int nBins, double emin, double emax) throws EventListException, TimeSeriesException {
		double[] selectedArrivalTimes = EventListSelector.getArrivalTimesInEnergyRange(evlist, emin, emax);
		logger.info("Making TimeSeries from EventList using nBins = "+nBins+" and [emin, emax] = ["+emin+", "+emax+"]");
		EventList selectedEvlist = new EventList(selectedArrivalTimes);
		return makeTimeSeries(selectedEvlist, nBins);
    }

    /**
     * Construct a <code>TimeSeries</code> from an <code>EventList</code> with the specified number of bins, minimum and maximum energies, as well as bounding detector coordinates.
     */
    public static TimeSeries makeTimeSeries(EventList evlist, int nBins, double emin, double emax, double xmin, double xmax, double ymin, double ymax) throws EventListException, TimeSeriesException {
		//  Select according to X and Y coordinate range
		double[] coordSelectedArrivalTimes = EventListSelector.getArrivalTimesInCoordinateRange(evlist, xmin, xmax, ymin, ymax);
		EventList coordSelectedEvlist = new EventList(coordSelectedArrivalTimes);
		//  Select according to energy range
		double[] coordAndEnergySelectedArrivalTimes = EventListSelector.getArrivalTimesInEnergyRange(coordSelectedEvlist, emin, emax);
		EventList selectedEvlist = new EventList(coordAndEnergySelectedArrivalTimes);
		logger.info("Making TimeSeries from EventList using nBins = "+nBins+
			    " and [emin, emax] = ["+emin+", "+emax+"]"+
			    " and [xmin, xmax] = ["+xmin+", "+xmax+"]"+
			    " and [ymin, ymax] = ["+ymin+", "+ymax+"]"
			    );
		return makeTimeSeries(selectedEvlist, nBins);
    }

    /**
     * Construct a <code>TimeSeries</code> from arrival times, specifying the number of bins.
     */
    public static TimeSeries makeTimeSeries(double[] arrivalTimes, int nBins) throws EventListException, TimeSeriesException {
		EventList evlist = new EventList(arrivalTimes);
		return makeTimeSeries(evlist, nBins);
    }

    /**
     * Construct a <code>TimeSeries</code> from arrival times, specifying the bin width.
     *
     * @param arrivalTimes a <code>double[]</code> value
     * @param binWidth a <code>double</code> value
     * @return a <code>TimeSeries</code> value
     * @exception EventListException if an error occurs
     */
    public static TimeSeries makeTimeSeries(double[] arrivalTimes, double binWidth) throws EventListException, TimeSeriesException {
		EventList evlist = new EventList(arrivalTimes);
		return makeTimeSeries(evlist, binWidth);
    }

    /**
     * Construct a <code>TimeSeries</code> from arrival times, specifying the bin width and the start time.
     *
     */
    public static TimeSeries makeTimeSeries(double[] arrivalTimes, double binWidth, double startTime) throws TimeSeriesException, EventListException {
		EventList evlist = new EventList(arrivalTimes);
		double[] selectedArrivalTimes = EventListSelector.getArrivalTimesFromTo(evlist, startTime, evlist.tStop());
		EventList selectedEvlist = new EventList(selectedArrivalTimes);
		return makeTimeSeries(selectedEvlist, binWidth);
    }

    /**
     * Construct a <code>TimeSeries</code> from arrival times, specifying the bin width and the start time.
     *
     */
    public static TimeSeries makeTimeSeries(double[] arrivalTimes, double binWidth, double startTime, double endTime) throws TimeSeriesException, EventListException {
		EventList evlist = new EventList(arrivalTimes);
		double[] selectedArrivalTimes = EventListSelector.getArrivalTimesFromTo(evlist, startTime, endTime);
		EventList selectedEvlist = new EventList(selectedArrivalTimes);
		return makeTimeSeries(selectedEvlist, binWidth);
    }

    /**
     * Construct a <code>TimeSeries</code> from bin edges and counts
     *
     */
    public static TimeSeries makeTimeSeries(double[] binEdges, double[] counts) {
		double tStart = binEdges[0];
		double[] zeroedBinEdges = resetToZero(binEdges);
		TimeSeries ts = new TimeSeries(tStart, zeroedBinEdges, counts);
		if ( ts.thereAreNaNs() ) {
		    ts = TimeSeriesUtils.dropLeadingAndTrailingNaNs(ts);
		}
		return ts;
    }
    
    /**
     * Construct a  <code>TimeSeries</code> from bin centres, rates and errors on rates. 
     * We assume that the bins are adjacent and that the first two bins are of equal width.
     */
    public static TimeSeries makeTimeSeries(double[] binCentres, double[] rates, double[] errorsOnRates) throws TimeSeriesException, BinningException {
		logger.info("Making TimeSeries from binCentres, rates and errors");
		logger.warn("Assuming adjacent bins");
		double[] halfBinWidths = BinningUtils.getHalfBinWidthsFromBinCentres(binCentres);
		return makeTimeSeries(binCentres, halfBinWidths, rates, errorsOnRates);
    }

    /**
     * Construct a  <code>TimeSeries</code> from bin centres, halfBinWidths, rates and errors on rates. 
     * We assume that the bins are adjacent and that the first two bins are of equal width.
     */
    public static TimeSeries makeTimeSeries(double[] binCentres, double[] halfBinWidths, double[] rates, double[] errorsOnRates) throws TimeSeriesException {
		logger.info("Making TimeSeries from binCentres, halfBinWidths, rates and errors");
		double firstHalfBinWidth = halfBinWidths[0];
		double tStart = binCentres[0] - firstHalfBinWidth;
		double[] centres = Arrays.copyOf(binCentres, binCentres.length);
		if ( tStart < 0 ) {
		    for ( int i=0; i < centres.length; i++ ) {
			centres[i] += tStart;
		    }
		    tStart = 0.0;
		}
		double positiveOffset = firstHalfBinWidth;
		double[] zeroedBinCentres = resetToZero(centres, positiveOffset);
		double[] binEdges = null;
		try {
		    binEdges = BinningUtils.getBinEdgesFromBinCentresAndHalfWidths(zeroedBinCentres, halfBinWidths);
		}
		catch ( BinningException e ) {
		    throw new TimeSeriesException("Cannot construct bin edges", e);
		}
		// Remove all bins with rate=0.0 and error=0.0
		DoubleArrayList goodBinEdges = new DoubleArrayList();
		DoubleArrayList goodRates = new DoubleArrayList();
		DoubleArrayList goodErrors = new DoubleArrayList();
		for ( int i=0; i < rates.length; i++ ) {
		    if ( rates[i] != 0.0 && errorsOnRates[i] != 0.0 ) {
			goodBinEdges.add(binEdges[2*i]);
			goodBinEdges.add(binEdges[2*i+1]);
			goodRates.add(rates[i]);
			goodErrors.add(errorsOnRates[i]);
		    }
		}
		goodBinEdges.trimToSize();
		goodRates.trimToSize();
		goodErrors.trimToSize();
		if ( goodBinEdges.size() == 0 || goodRates.size() == 0 || goodErrors.size() == 0 ) {
		    throw new TimeSeriesException("All bins are zeros: No TimeSeries can be made");
		}
		TimeSeries ts = new TimeSeries(tStart, goodBinEdges.elements(), goodRates.elements(), goodErrors.elements());
		if ( ts.thereAreNaNs() ) {
		    ts = TimeSeriesUtils.dropLeadingAndTrailingNaNs(ts);
		}
		return ts;
    }

    public static TimeSeries makeTimeSeries(double[] binCentres, double halfBinWidth, double[] rates, double[] errorsOnRates) throws TimeSeriesException {
		double[] halfBinWidths = new double[binCentres.length];
		for ( int i=0; i < binCentres.length; i++ ) {
		    halfBinWidths[i] = halfBinWidth;
		}
		return makeTimeSeries(binCentres, halfBinWidths, rates, errorsOnRates);
    }

    public static TimeSeries makeTimeSeries(double[] binCentres, double halfBinWidth, double[] rates, double errorOnRates) throws TimeSeriesException {
		double[] halfBinWidths = new double[binCentres.length];
		double[] errorsOnRates = new double[binCentres.length];
		for ( int i=0; i < binCentres.length; i++ ) {
		    halfBinWidths[i] = halfBinWidth;
		    errorsOnRates[i] = errorOnRates;
		}
		return makeTimeSeries(binCentres, halfBinWidths, rates, errorsOnRates);
    }

    //  CodedMaskTimeSeries

    // Define all CodedMaskTimeSeries fields and attributes
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] binEdges,
							      double[] effectivePointingDurations, double[] rates, double[] errors, double[] rasOfPointings, double[] decsOfPointings, double[] exposuresOnTarget) throws BinningException {
        logger.info("Making CodedMaskTimeSeries");
        int[] arrayLengths = new int[] {effectivePointingDurations.length, rates.length, errors.length, rasOfPointings.length, decsOfPointings.length, exposuresOnTarget.length};
        for ( int i=0; i < arrayLengths.length; i++ ) {
            if ( arrayLengths[i] != arrayLengths[0] ) {
                throw new IllegalArgumentException("Incompatible input array lengths");		
            }
        }
		if ( binEdges.length != 2*rates.length ) {
		    throw new IllegalArgumentException("Incompatible bin edges with input data");	    
		}
        Point2D.Double targetRaDec = new Point2D.Double(targetRA, targetDec);
        Point2D.Double energyMinMax = new Point2D.Double(emin, emax);
        Point2D.Double[] raDecsOfPointings = new Point2D.Double[rasOfPointings.length];
        for ( int i=0; i < rasOfPointings.length; i++ ) {
            raDecsOfPointings[i] = new Point2D.Double(rasOfPointings[i], decsOfPointings[i]);
        }
        double tStart = binEdges[0];
        if ( tStart < 0 ) { tStart = 0; }
        double[] zeroedBinEdges = resetToZero(binEdges);
        return new CodedMaskTimeSeries(targetRaDec, energyMinMax, telescope, instrument, maxDistForFullCoding, tStart, zeroedBinEdges, effectivePointingDurations, rates, errors, raDecsOfPointings, exposuresOnTarget);
    }

    // With targetName
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(String targetName,
      double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] binEdges,
      double[] effectivePointingDurations, double[] rates, double[] errors, double[] rasOfPointings, double[] decsOfPointings, double[] exposuresOnTarget) throws BinningException {
	CodedMaskTimeSeries ts = makeCodedMaskTimeSeries(targetRA, targetDec, emin, emax, telescope, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, rasOfPointings, decsOfPointings, exposuresOnTarget);
	ts.setTargetName(targetName);
	return ts;
    }
    
    // Using leftBinEdges and rightBinEdges instead of binEdges
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] leftBinEdges, double[] rightBinEdges,
	    double[] effectivePointingDurations, double[] rates, double[] errors, double[] rasOfPointings, double[] decsOfPointings, double[] exposuresOnTarget) throws BinningException {

		double[] binEdges = BinningUtils.getBinEdges(leftBinEdges, rightBinEdges);
		return makeCodedMaskTimeSeries(targetRA, targetDec, emin, emax, telescope, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, rasOfPointings, decsOfPointings, exposuresOnTarget);
    }
    // With targetName
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(String targetName,
		double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] leftBinEdges, double[] rightBinEdges,
		double[] effectivePointingDurations, double[] rates, double[] errors, double[] rasOfPointings, double[] decsOfPointings, double[] exposuresOnTarget) throws BinningException {

		double[] binEdges = BinningUtils.getBinEdges(leftBinEdges, rightBinEdges);
		CodedMaskTimeSeries ts = makeCodedMaskTimeSeries(targetRA, targetDec, emin, emax, telescope, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, rasOfPointings, decsOfPointings, exposuresOnTarget);
		ts.setTargetName(targetName);
		return ts;
    }

    
    // Define distToPointingAxis only, instead of raDecsOfPointings and exposuresOnTarget
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] binEdges,
	    double[] effectivePointingDurations, double[] rates, double[] errors, double[] distToPointingAxis) throws BinningException {        

        logger.info("Making minimal CodedMaskTimeSeries");
        int[] arrayLengths = new int[] {effectivePointingDurations.length, rates.length, errors.length, distToPointingAxis.length};
        for ( int i=0; i < arrayLengths.length; i++ ) {
            if ( arrayLengths[i] != arrayLengths[0] ) {
                throw new IllegalArgumentException("Incompatible input array lengths");
            }
        }
		if ( binEdges.length != 2*rates.length ) {
		    throw new IllegalArgumentException("Incompatible bin edges with input data");	    
		}
        Point2D.Double targetRaDec = new Point2D.Double(targetRA, targetDec);
        Point2D.Double energyMinMax = new Point2D.Double(emin, emax);
        double tStart = binEdges[0];
        if ( tStart < 0 ) { tStart = 0; }
        double[] zeroedBinEdges = resetToZero(binEdges);
        return new CodedMaskTimeSeries(targetRaDec, energyMinMax, telescope, instrument, maxDistForFullCoding, tStart, zeroedBinEdges, effectivePointingDurations, rates, errors, distToPointingAxis);
    }
    
    // With targetName
    public static CodedMaskTimeSeries makeCodedMaskTimeSeries(String targetName,
							      double targetRA, double targetDec, double emin, double emax, String telescope, String instrument, double maxDistForFullCoding, double[] binEdges,
							      double[] effectivePointingDurations, double[] rates, double[] errors, double[] distToPointingAxis) throws BinningException {	
		CodedMaskTimeSeries ts = makeCodedMaskTimeSeries(targetRA, targetDec, emin, emax, telescope, instrument, maxDistForFullCoding, binEdges, effectivePointingDurations, rates, errors, distToPointingAxis);
		ts.setTargetName(targetName);
		return ts;
    }
   
	private static double[] resetToZero(double[] data) {
		return resetToZero(data, 0);
    }

    private static double[] resetToZero(double[] data, double addedPositiveOffset) {
		int n = data.length;
		double[] zeroedData = new double[n];
		double zero = data[0];
		for ( int i=0; i < n; i++ ) {
		    zeroedData[i] = data[i] - zero + addedPositiveOffset;
		}
		return zeroedData;
    }
 
    
}


//    Error interval calculations
// 	    // Classical
// 	    //double[] classicalOneSigmaBounds = Stats.getClassicalPoissonOneSigmaBounds((int) binnedEvents[i]);
// 	    //errors[i] = 0.5*(classicalOneSigmaBounds[1] - classicalOneSigmaBounds[0])/lc.binWidths[i];
// 	    //logger.debug("Classical = "+classicalOneSigmaBounds[0] +"\t"+ binnedEvents[i] +"\t"+ classicalOneSigmaBounds[1]);

// 	    // ML
// 	    //double[] mlOneSigmaBounds = Stats.getOneSigmaBoundsFromPoissonLogLikelihood(binnedEvents[i]);
// 	    //errors[i] = 0.5*(mlOneSigmaBounds[1] - mlOneSigmaBounds[0])/lc.binWidths[i];
// 	    //logger.debug("ML Bounds = "+ mlOneSigmaBounds[0] +"\t"+ binnedEvents[i] + "\t"+ mlOneSigmaBounds[1]);
	    
// 	    //  Standard sqrt
// 	    //errors[i] = Math.sqrt(binnedEvents[i])/lc.binWidths[i];
