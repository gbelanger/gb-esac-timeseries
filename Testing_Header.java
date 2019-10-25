package gb.esac.timeseries;

import gb.esac.binner.BinningException;
import gb.esac.binner.BinningUtils;
import gb.esac.eventlist.AsciiEventFileException;
import gb.esac.eventlist.AsciiEventFileReader;
import gb.esac.eventlist.EventList;
import gb.esac.eventlist.EventListException;
import gb.esac.io.AsciiDataFileFormatException;
import gb.esac.io.AsciiDataFileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

import java.io.*;

// Just wrote to test the extraction possess for the headers of QDP Coded Mask Time Series Files. 

class Testing_Header {
    
    private static Logger logger = Logger.getLogger(Testing_Header.class);
    
    public static void main(String[] args) throws Exception {
        
        String filename = "ts_isgri_83.633_22.01_20-35keV.qdp";
        
        AsciiDataFileReader dataFile = null;
        
        dataFile = new AsciiDataFileReader(filename);
/**
        String[] header = dataFile.getHeader();
        String[] stringsToFind = new String[] {"RA=", "Dec=", "LAB OT ISGRI Time Series (", "LAB OT ISGRI Time Series (20-", "LAB OT "};
        int[] indexes = new int[stringsToFind.length];
        for ( int i=0; i < stringsToFind.length; i++ ) {
            int j=0;
            boolean found = header[j].contains(stringsToFind[i]);
            while ( ! found ) {
                j++;
                found = header[j].contains(stringsToFind[i]);
            }
            indexes[i] = j;
        }
        
        int RARow = indexes[0];
        int DECRow = indexes [1];
        int LABOTRow = indexes[2];
 
        String targetRAstr = header[RARow].substring((header[RARow].indexOf(stringsToFind[0]))+stringsToFind[0].length(), header[RARow].indexOf(","));
        
        String targetDECstr = header[DECRow].substring((header[DECRow].indexOf(stringsToFind[1]))+stringsToFind[1].length(), header[DECRow].indexOf(")"));
        
        String eminStr = header[LABOTRow].substring((header[LABOTRow].indexOf(stringsToFind[2]))+stringsToFind[2].length(), header[LABOTRow].indexOf("-"));

        String emaxStr = header[LABOTRow].substring((header[LABOTRow].indexOf(stringsToFind[3]))+stringsToFind[3].length(), header[LABOTRow].indexOf("keV"));
        String instrumentStr = header[LABOTRow].substring((header[LABOTRow].indexOf(stringsToFind[4]))+stringsToFind[4].length(), header[LABOTRow].indexOf(" T"));

        
        double targetRA = (new Double(targetRAstr)).doubleValue();
        double targetDEC = (new Double(targetDECstr)).doubleValue();
        double emin = (new Double(eminStr)).doubleValue();
        double emax = (new Double(emaxStr)).doubleValue();
        String instrument = instrumentStr;
        //double maxDistForFullCoding = (new Double(maxDistForFullCodingStr)).doubleValue();
        //double[] effectivePointingDuration = (new Double(effectivePointingDurationStr)).doubleValue();
        //double[] rasOfPointings =
        //double[] decsOfPointings =
        //double[] effectiveExposures =
        
        System.out.println(targetRA);
        System.out.println(targetDEC);
        System.out.println(emin);
        System.out.println(emax);
        System.out.println(instrument);
*/
         
         // Just For reference
         // public static CodedMaskTimeSeries makeCodedMaskTimeSeries(double targetRA, double targetDec, double emin, double emax, String instrument, double maxDistForFullCoding, double[] binEdges, double[] effectivePointingDurations, double[] rates, double[] errors, double[] rasOfPointings, double[] decsOfPointings, double[] effectiveExposures) throws BinningException
        
        String[] header = dataFile.getHeader();
        String stringsToFind = "LAB X Time (s) since MJD ";
            int j=0;
            boolean found = header[j].contains(stringsToFind);
            while ( ! found ) {
                j++;
                found = header[j].contains(stringsToFind);
            }
            int tStartRow = j;
        String tStartStr = header[tStartRow].substring((header[tStartRow].indexOf(stringsToFind))+stringsToFind.length());;
        double tStart = (new Double(tStartStr)).doubleValue();
        
        System.out.println(tStart);
        
    }
}