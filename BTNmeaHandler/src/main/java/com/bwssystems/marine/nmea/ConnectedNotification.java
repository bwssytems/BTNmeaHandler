package com.bwssystems.marine.nmea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Author: bwssystems.com
 * Read NMEA Sentence from bluetooth for adapter and then output to
 * a tcp port for kplex to read.
 */

import tinyb.*;

public class ConnectedNotification implements BluetoothNotification<Boolean> {
	private BluetoothManager manager;
	private final static Logger log = LoggerFactory.getLogger(ConnectedNotification.class);
	
    public ConnectedNotification(BluetoothManager aManager) {
		super();
		manager = aManager;
	}


	public void run(Boolean connected) {
            log.info("Connected");
            manager.stopDiscovery();
    }

}
