package gb.esac.timeseries;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import gb.esac.tools.BasicStats;
import gb.esac.tools.DataUtils;
import org.apache.log4j.Logger;


/**
 *

 This interface defines the public methods that any type of time series object needs to implement.
 Implementation of these methods is done in AbstractTimeSeries

 @author <a href="mailto: guilaume.belanger@esa.int">Guillaume Belanger</a>, ESA/ESAC, SRE-O, Villanueva de la Canada (Madrid), Spain
 @version August 2016

**/

public interface ITimeSeries {

    // About attributes
    public String timeUnit();
    public String telescope();
    public String instrument();
    public double mjdref();
    public String targetName();
    public double targetRA();
    public double targetDec();
    public Point2D.Double targetRaDec();
    public double energyRangeMin();
    public double energyRangeMax();
    public Point2D.Double energyRange();
    public String dateObs();
    public String dateEnd();
    public String[] dateObsEnd();
    public String timeObs();
    public String timeEnd();
    public String[] timeObsEnd();
    public double relTimeError();
    public double absTimeError();
    
    // For bins
    public int nBins();
    public double tStart();
    public double tStop();
    public double tMid();
    public double duration();
    public double[] getBinCentres();
    public double[] getBinWidths();
    public double[] getHalfBinWidths();
    public double[] getBinEdges();
    public double[] getLeftBinEdges();
    public double[] getRightBinEdges();
    public double binCentreAtMinBinHeight();
    public double binCentreAtMaxBinHeight();
    public double minBinWidth();
    public double maxBinWidth();
    public double avgBinWidth();
    public double binWidth() throws TimeSeriesException;
    public double sumOfBinWidths();
    public double ontime();
    public double livetime();
    public double exposureOnTarget();
    
    //  About Gaps
    public int nGaps();
    public double[] getGapEdges();
    public double[] getGapLengths();
    public double meanGap();
    public double minGap();
    public double maxGap();
    public double sumOfGaps();
    public int nSamplingFunctionBins();
    public double[] getSamplingFunctionValues();
    public double[] getSamplingFunctionBinEdges();
    
    //  About Intensities
    public double[] getBinHeights();
    public double sumOfBinHeights();
    public double meanBinHeight();
    public double minBinHeight();
    public double maxBinHeight();
    public double varianceInBinHeights();
    public double meanDeviationInBinHeights();
    public double kurtosisInBinHeights();
    public double kurtosisStandardError();
    public double skewnessInBinHeights();
    public double skewnessStandardError();
    public double[] getRates();
    public double meanRate();
    public double minRate();
    public double maxRate();
    public double errorOnMeanRate();
    public double weightedMeanRate();
    public double errorOnWeightedMeanRate();
    public double varianceInRates();
    public double meanDeviationInRates();
    public double kurtosisInRates();
    public double skewnessInRates();
    public double[] getErrorsOnRates();
    public double[] getMeanSubtractedRates();
    public double[] getMeanSubtractedBinHeights();
    
    //  Boolean checkers
    public boolean binWidthIsConstant();
    public boolean thereAreGaps();
    public boolean errorsAreSet();
    public boolean instrumentIsSet();
    public boolean targetNameIsSet();
    public boolean targetRaDecAreSet();
    
}
