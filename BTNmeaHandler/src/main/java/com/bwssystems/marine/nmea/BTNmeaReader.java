package com.bwssystems.marine.nmea;

/*
 * Author: bwssystems.com
 * Read NMEA Sentence from bluetooth for adapter and then output to
 * a tcp port for kplex to read.
 */

import tinyb.*;
import java.time.*;
import java.util.concurrent.locks.*;

public class BTNmeaReader {
    static boolean running = true;

    static void printDevice(BluetoothDevice device) {
        System.out.print("Address = " + device.getAddress());
        System.out.print(" Name = " + device.getName());
        System.out.print(" Connected = " + device.getConnected());
        System.out.println();
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
    public static void main(String[] args) throws InterruptedException {

        if (args.length < 1) {
            System.err.println("Run with <device_address> argument");
            System.exit(-1);
        }

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

        System.out.println("The discovery started: " + (discoveryStarted ? "true" : "false"));

        /*
         * After discovery is started, new devices will be detected. We can find the device we are interested in
         * through the manager's find method.
         */
        BluetoothDevice marineDevice = manager.find(null, args[0], null, Duration.ofSeconds(10));

        if (marineDevice == null) {
            System.err.println("No marineDevice found with the provided address.");
            System.exit(-1);
        }

        marineDevice.enableConnectedNotifications(new ConnectedNotification());

        System.out.print("Found device: ");
        printDevice(marineDevice);

        if (marineDevice.connect())
            System.out.println("marineDevice with the provided address connected");
        else {
            System.out.println("Could not connect device.");
            System.exit(-1);
        }

        /*
         * After we find the device we can stop looking for other devices.
         */
        //manager.stopDiscovery();

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
            System.err.println("This device does not have the service we are looking for.");
            marineDevice.disconnect();
            System.exit(-1);
        }
        System.out.println("Found service " + dataService.getUUID());

        BluetoothGattCharacteristic dataValue = dataService.find("0000ffe1-0000-1000-8000-00805f9b34fb");

        if (dataValue == null) {
            System.err.println("Could not find the correct characteristics.");
            marineDevice.disconnect();
            System.exit(-1);
        }

        System.out.println("Found the data characteristics");

        dataValue.enableValueNotifications(new NmeaValueNotification());

        lock.lock();
        try {
            while(running)
                cv.await();
        } finally {
            lock.unlock();
        }
        marineDevice.disconnect();

    }
}