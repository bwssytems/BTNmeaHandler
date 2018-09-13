package com.bwssystems.marine.nmea;

/*
 * Author: bwssystems.com
 * Read NMEA Sentence from bluetooth for adapter and then output to
 * a tcp port for kplex to read.
 */

import tinyb.*;

public class ConnectedNotification implements BluetoothNotification<Boolean> {

    public void run(Boolean connected) {
            System.out.println("Connected");
    }

}
