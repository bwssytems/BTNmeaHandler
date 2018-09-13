package com.bwssystems.marine.nmea.test;

import org.junit.Assert;
import org.junit.Test;

import com.bwssystems.marine.nmea.NmeaValueNotification;

public class SentenceValueHandleTestCase {


	@Test
    public void testPositive() {
        String testChunk = "262,W,155702.00,A,D*7A\r\n$HCHDM,182.00,M\r\n$APGLL,4153";
        String targetSentence = "$HCHDM,182.00,M\r\n";
        
        NmeaValueNotification aNotify = new NmeaValueNotification();
        
        aNotify.run(testChunk.getBytes());
        Assert.assertEquals(aNotify.getLastSentence(), targetSentence); 
    }
    
	@Test
    public void testPositive2() {
        String testChunk1 = "3.08592,N,08736.70135,W,1";
        String testChunk2 = "71652.00,A,D*77\r\n!AIVDM,1,1,,B,15NS7U0P00q";
        String testChunk3 = "fu@:Gv5sP0?wV2@NA,0*52\r\n$GPGLL,4153.08462,N,";
        String targetSentence = "!AIVDM,1,1,,B,15NS7U0P00qfu@:Gv5sP0?wV2@NA,0*52\r\n";

        NmeaValueNotification aNotify = new NmeaValueNotification();
        aNotify.run(testChunk1.getBytes());
        aNotify.run(testChunk2.getBytes());
        aNotify.run(testChunk3.getBytes());
        Assert.assertEquals(aNotify.getLastSentence(), targetSentence); 
    }
    
    @Test
    public void testNoCrLf() {
        String testChunk1 = "$GPVTG,,T,,M,0.040,N";
        String testChunk2 = ",0.075,K,D*20$HCHDM,182.";
        String targetSentence = "$GPVTG,,T,,M,0.040,N,0.075,K,D*20\r\n";
        NmeaValueNotification aNotify = new NmeaValueNotification();
        aNotify.run(testChunk1.getBytes());
        aNotify.run(testChunk2.getBytes());
        Assert.assertEquals(aNotify.getLastSentence(), targetSentence); 
    }
    
    @Test
    public void testNoLf() {
        String testChunk1 = "$GPVTG,,T,,M,0.040,N";
        String testChunk2 = ",0.075,K,D*20\r$HCHDM,182.";
        String targetSentence = "$GPVTG,,T,,M,0.040,N,0.075,K,D*20\r\n";
        NmeaValueNotification aNotify = new NmeaValueNotification();
        aNotify.run(testChunk1.getBytes());
        aNotify.run(testChunk2.getBytes());
        Assert.assertEquals(aNotify.getLastSentence(), targetSentence); 
    }
    @Test
    public void testNoCr() {
        String testChunk1 = "$GPVTG,,T,,M,0.040,N";
        String testChunk2 = ",0.075,K,D*20\n$HCHDM,182.";
        String targetSentence = "$GPVTG,,T,,M,0.040,N,0.075,K,D*20\r\n";
        NmeaValueNotification aNotify = new NmeaValueNotification();
        aNotify.run(testChunk1.getBytes());
        aNotify.run(testChunk2.getBytes());
        Assert.assertEquals(aNotify.getLastSentence(), targetSentence); 
    }
}
