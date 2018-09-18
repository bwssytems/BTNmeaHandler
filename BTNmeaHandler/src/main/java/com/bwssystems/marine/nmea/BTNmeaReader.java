package com.bwssystems.marine.nmea;

/*
 * Author: bwssystems.com
 * Read NMEA Sentence from bluetooth for adapter and then output to
 * a tcp port for kplex to read.
 */

import tinyb.*;
import java.time.*;
import java.util.concurrent.locks.*;
import java.io.*;
import java.net.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BTNmeaReader {
    static boolean running = true;
	private final static Logger log = LoggerFactory.getLogger(BTNmeaReader.class);

    static void printDevice(BluetoothDevice device) {
        log.info("Found device: Address = " + device.getAddress() + ", Name = " + device.getName() + ", Connected = " + device.getConnected());
    }

    static float convertCelsius(int raw) {
        return raw / 128f;
    }

    /*
     * This program connects to a SH-HC-08 and reads the data characteristic exposed by the device over
     * Bluetooth Low Energy. The parameter provided to the program should be the MAC address of the device.
     *
     * The API used in this example is based on TinyB v0.3, which only supports polling, but v0.4 will introduce a
     * simplied API for discovering devices and services.
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        String clientSentence;

        if (args.length < 2) {
            log.error("Run with <bt_device_address> <port> arguments");
            System.exit(-1);
        }

        ServerSocket welcomeSocket = new ServerSocket(Integer.parseInt(args[1]));

        /*
         * To start looking of the device, we first must initialize the TinyB library. The way of interacting with the
         * library is through the BluetoothManager. There can be only one BluetoothManager at one time, and the
         * reference to it is obtained through the getBluetoothManager method.
         */
        BluetoothManager manager = BluetoothManager.getBluetoothManager();

        /*
         * The manager will try to initialize a BluetoothAdapter if any adapter is present in the system. To initialize
         * discovery we can call startDiscovery, which will put the default adapter in discovery mode.
         */
        boolean discoveryStarted = manager.startDiscovery();

        log.info("The discovery started: " + (discoveryStarted ? "true" : "false"));

        /*
         * After discovery is started, new devices will be detected. We can find the device we are interested in
         * through the manager's find method.
         */
        BluetoothDevice marineDevice = manager.find(null, args[0], null, Duration.ofSeconds(10));

        if (marineDevice == null) {
            log.error("No marineDevice found with the provided address.");
            System.exit(-1);
        }

        marineDevice.enableConnectedNotifications(new ConnectedNotification(manager));

        if (marineDevice.connect())
            log.info("marineDevice with the provided address connected");
        else {
            log.error("Could not connect device.");
            System.exit(-1);
        }

        log.info("Waiting for TCP connection...");
        Socket connectionSocket = welcomeSocket.accept();
        log.info("Socket connected...");
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        Lock lock = new ReentrantLock();
        Condition cv = lock.newCondition();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                running = false;
                lock.lock();
                try {
                    cv.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        });

        /*
         * Our device should expose a data service, which has a UUID we can find out from the data sheet.
         */
        BluetoothGattService dataService = marineDevice.find( "0000ffe0-0000-1000-8000-00805f9b34fb");

        if (dataService == null) {
            log.error("This device does not have the service we are looking for.");
            marineDevice.disconnect();
            System.exit(-1);
        }
        log.info("Found service " + dataService.getUUID());

        BluetoothGattCharacteristic dataValue = dataService.find("0000ffe1-0000-1000-8000-00805f9b34fb");

        if (dataValue == null) {
            log.error("Could not find the correct characteristics.");
            marineDevice.disconnect();
            System.exit(-1);
        }

        log.info("Found the data characteristics");

        dataValue.enableValueNotifications(new NmeaValueNotification(outToClient));

        lock.lock();
        try {
            while(running) {
                clientSentence = inFromClient.readLine();
                dataValue.writeValue(clientSentence.getBytes());
                cv.await();
            }
        } finally {
            lock.unlock();
        }
        marineDevice.disconnect();
        welcomeSocket.close();

    }
}