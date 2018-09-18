package com.bwssystems.marine.nmea;

/*
 * Author: bwssystems.com
 * Read NMEA Sentence from bluetooth for adapter and then output to
 * a tcp port for kplex to read.
 */

import tinyb.*;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NmeaValueNotification implements BluetoothNotification<byte[]> {
	private final static Logger log = LoggerFactory.getLogger(NmeaValueNotification.class);
	private String nmeaSentence;
	private String lastSentence;
	private boolean sentenceInprogress;
	private boolean cReturn;
	private DataOutputStream outToClient;

	public NmeaValueNotification(DataOutputStream anOutToClient) {
		super();
		nmeaSentence = null;
		lastSentence = null;
		sentenceInprogress = false;
		cReturn = false;
		outToClient = anOutToClient;
	}

	public void run(byte[] tempRaw) {
		String sentenceChunk = null;
		
		if(sentenceInprogress) {
			sentenceChunk = new String(nmeaSentence);
			nmeaSentence = null;
		}

		for (byte b : tempRaw) {
			if (b == '$' || b == '!') { // check for nmea sentence start
				if (sentenceInprogress) {
					if (cReturn) {
						try {
							sentenceChunk = sentenceChunk + new String(new byte[] { 0x0A }, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						try {
							sentenceChunk = sentenceChunk + new String(new byte[] { 0x0d, 0x0A }, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(nmeaSentence == null)
						nmeaSentence = new String();
					nmeaSentence = nmeaSentence + sentenceChunk;
					lastSentence = new String(nmeaSentence);
	                try {
	                    outToClient.writeBytes(nmeaSentence);
	                }
	                catch(Exception e) {
	                    log.error("TCP Write exception occurred for sentence: " + nmeaSentence);
	                }
					log.debug("NMEA Sentence from BT to IP: " + nmeaSentence);
					nmeaSentence = null;
					sentenceChunk = null;
					cReturn = false;
				}
				sentenceInprogress = true;
				if(sentenceChunk == null)
					sentenceChunk = new String();
				try {
					sentenceChunk = sentenceChunk + new String(new byte[] { b }, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Encoding Error on byte: " + b);
				}
			} else if (b == 13 && sentenceInprogress) { // check for carriage return 
				cReturn = true;
				try {
					sentenceChunk = sentenceChunk + new String(new byte[] { b }, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Encoding Error on byte: " + b);
				}

			} else if (b == 10 && sentenceInprogress) { // check for line feed
				if(!cReturn) {
					try {
						sentenceChunk = sentenceChunk + new String(new byte[] { 0x0D }, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						log.error("Encoding Error on byte: " + b);
					}
				}
				try {
					sentenceChunk = sentenceChunk + new String(new byte[] { b }, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Encoding Error on byte: " + b);
				}
				if(nmeaSentence == null)
					nmeaSentence = new String();
				nmeaSentence = nmeaSentence + sentenceChunk;
				lastSentence = new String(nmeaSentence);
                try {
                    outToClient.writeBytes(nmeaSentence);
                }
                catch(Exception e) {
                    log.error("TCP Write exception occurred for sentence: " + nmeaSentence);
                }
				log.debug("NMEA Sentence from BT to IP: " + nmeaSentence);
				nmeaSentence = null;
				sentenceChunk = null;
				cReturn = false;
				sentenceInprogress = false;
			} else if(sentenceInprogress)
				try {
					sentenceChunk = sentenceChunk + new String(new byte[] { b }, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Encoding Error on byte: " + b);
				}
		}
		
		if(nmeaSentence == null && sentenceChunk != null)
			nmeaSentence = new String(sentenceChunk);
	}

	/*
	 * Used for test methods
	 */
	public String getLastSentence() {
		return lastSentence;
	}

}
