package gb.esac.timeseries;

import java.util.Random;

import gb.esac.periodogram.WindowFunction;
import gb.esac.tools.DataUtils;
import org.apache.log4j.Logger;

import java.lang.Math.*;


public class TestTimeSeries {

    private static Logger logger  = Logger.getLogger(TestTimeSeries.class);

    public static void main(String[] args) throws Exception  {

	// String dir = "/Users/gbelanger/Documents/astroData/xmm/mkn421/results/";
        // String[] excellent_obsID = new String[]{"0153950601", "0158971201", "0502030101"};
        // String[] excellent_filename = new String[excellent_obsID.length];
        // TimeSeries[] ts_ex = new TimeSeries[excellent_obsID.length];
        // for ( int i=0; i < excellent_obsID.length; i++ ) {
        //     excellent_filename[i] = dir+"Mkn421."+excellent_obsID[i]+".lc.qdp";
        //     ts_ex[i] = (TimeSeries) TimeSeriesMaker.makeTimeSeries(excellent_filename[i]);
        // }
	// System.exit(-1);
	
 	String filename = "flare.fits";
	//filename = "/Users/harryholt/esa/java_version2/gb/esac/timeseries/flare.fits";
	//filename = "/Users/harryholt/esa/java_version2/gb/esac/timeseries/saxj1808_lightcurve_16msec.lc";

	// filename = "/Users/gbelanger/Documents/astroData/rxte/saxj1808_lc1.fits";
	// TimeSeries lc1 = (TimeSeries) TimeSeriesMaker.makeTimeSeries(filename);
	// filename = "/Users/gbelanger/Documents/astroData/rxte/saxj1808_lc3.fits";
	// TimeSeries lc2 = (TimeSeries) TimeSeriesMaker.makeTimeSeries(filename);
	// filename = "/Users/gbelanger/Documents/astroData/rxte/saxj1808_lc2.fits";
	// TimeSeries lc3 = (TimeSeries) TimeSeriesMaker.makeTimeSeries(filename);
	//TimeSeries lc = TimeSeriesOperations.combine(new TimeSeries[] {lc1, lc2, lc3});
	
 	//filename = "saxj1808_lightcurve_16msec.lc";
	filename = "saxj1808_lc1.fits";	

	TimeSeries lc = (TimeSeries) TimeSeriesMaker.makeTimeSeries(filename);

	double tStart = 20;
	double tStop = 40;
	TimeSeries ts = (TimeSeries) TimeSeriesOperations.getSegment(lc, tStart, tStop);

	//lc = (TimeSeries) TimeSeriesResampler.resample(lc, ts.nBins()/32);
	
	WindowFunction window = new WindowFunction("Hann");
	double[] function = DataUtils.scale(window.getFunction(ts.nBins()), ts.meanBinHeight());

	// Test QDPWriter 
	
	// ts.writeCountsAsQDP("saxj_counts.qdp");
	// ts.writeCountsAsQDP(function, "saxj_counts_func.qdp");
	// ts.writeCountsAndSamplingAsQDP("saxj_counts_sampling.qdp");
	// ts.writeRatesAsQDP("saxj_rates.qdp");
	// function = DataUtils.scale(window.getFunction(ts.nBins()), ts.meanRate());
	// ts.writeRatesAsQDP(function, "saxj_rates_func.qdp");	
	// ts.writeRatesAndSamplingAsQDP("saxj_rates_sampling.qdp");

	// Test FitsWriter

	double targetRA = 272.11475;
	double targetDec = -36.97897;
	ts.setTelescope("RXTE");
	ts.setInstrument("PCA");
	ts.setTargetName("SAX J1808");
	ts.setTargetRaDec(targetRA, targetDec);
	ts.setEnergyRange(2, 10);
	ts.setDateObsEnd("1998-04-18","1998-04-18");
	ts.setTimeObsEnd("03:08:32","09:21:52");
	
	// function = DataUtils.scale(window.getFunction(ts.nBins()), ts.meanRate());
	// ts.writeCountsAsFits("saxj_counts.fits");
	// ts.writeCountsAsFits(function, "saxj_counts_func.fits");
	// ts.writeCountsAndSamplingAsFits("saxj_counts_sampling.fits");
	// ts.writeRatesAsFits("saxj_rates.fits");
	// ts.writeRatesAsFits(function, "saxj_rates_func.fits");	
	// ts.writeRatesAndSamplingAsFits("saxj_rates_sampling.fits");

	//  Generate angles around target coordinates
	Random random = new Random();
	double[] ras = new double[ts.nBins()];
	double[] decs = new double[ts.nBins()];	
	double[] onTarget = new double[ts.nBins()];	
	double[] dt = ts.getBinWidths();
	for ( int i=0; i < ts.nBins(); i++ ) {
	    double ra = 5*(random.nextGaussian()-0.5) + targetRA;
	    double dec = 5*(random.nextGaussian()-0.5) + targetDec;
	    ras[i] = ra;
	    decs[i] = dec;
	    double d = Math.sqrt( Math.pow(ra-targetRA,2) + Math.pow(dec-targetDec,2) );
	    onTarget[i] = dt[i]*(1 - d/15);
	}
	
        // Write normal timeseries
        //ts.writeCountsAsJS("lc_counts.tsv");
        //ts.writeCountsAndSamplingAsJS("lc_counts_samp.tsv");
        //ts.writeRatesAsJS("lc_rates.tsv");
        //ts.writeRatesAndSamplingAsJS("lc_rates_samp.tsv");
        
	CodedMaskTimeSeries ts2 = TimeSeriesMaker.makeCodedMaskTimeSeries(targetRA, targetDec, 20, 35, "INTEGRAL", "ISGRI", 4.5, ts.getBinEdges(), dt, ts.getRates(), ts.getErrorsOnRates(), ras, decs, onTarget);
	ts2.setTargetName("SAX J1808");
	ts2.setDateObsEnd("1998-04-18","1998-04-18");
	ts2.setTimeObsEnd("03:08:32","09:21:52");
	ts2.setTargetRaDec(targetRA, targetDec);	

	// Test QDPWriter
	
	// ts2.writeCountsAsQDP("coded_counts2.qdp");
	// ts2.writeCountsAndSamplingAsQDP("coded_counts_samp.qdp");
	// ts2.writeRatesAsQDP("coded_rates.qdp");
	// ts2.writeRatesAndSamplingAsQDP("coded_rates_samp.qdp");
	// ts2.writeAllDataAsQDP("coded_all.qdp");

	// Test FitsWriter

	 ts2.writeCountsAsFits("coded_counts2.fits");
	 ts2.writeCountsAndSamplingAsFits("coded_counts_samp.fits");
	 ts2.writeRatesAsFits("coded_rates.fits");
	 ts2.writeRatesAndSamplingAsFits("coded_rates_samp.fits");
	 ts2.writeAllDataAsFits("coded_all.fits");

	// Test JSWriter

	// ts2.writeCountsAsJS("coded_counts.tsv");
	// ts2.writeRatesAsJS("coded_rates.tsv");
        // ts2.writeAllDataAsJS("coded_all.tsv");
    }
    
}
