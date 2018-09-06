package com.gmail.amaarquardi.rccarcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Amaar on 2017-06-10.
 */
public class ArduinoSerialWriter {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public interface OnFinishConnectionAttemptListener {
        void onFinishConnectionAttempt(boolean connected);
    }

    private static BluetoothSocket socket;

    /**
     * The OutputStream to write the data to.
     */
    private static OutputStream hc06OutputStream;

    private static boolean debugMode;

    /**
     * Opens a Bluetooth connection with the HC-06 Bluetooth chip connected to the Arduino.
     * Also, gets a reference to the corresponding OutputStream to write the data to.
     */
    public static void init(final Activity activity, final OnFinishConnectionAttemptListener onFinishConnectionAttemptListener) {
        debugMode = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("debugMode", false);
        if (debugMode) {
            onFinishConnectionAttemptListener.onFinishConnectionAttempt(true);
            return;
        }

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(activity)
                    .setTitle("Error!")
                    .setMessage("This device does not have bluetooth capabilities.")
                    .show();
            return;
        }
        else if (!bluetoothAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ArduinoConnectActivity.REQUEST_ENABLE_BLUETOOTH);
            return;
        }
        BluetoothDevice hc06 = null;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals("HC-06")) {
                hc06 = device;
                break;
            }
        }
        if (hc06 == null) {
            new AlertDialog.Builder(activity)
                    .setTitle("Error!")
                    .setMessage("No paired device named \"HC-06\" found.")
                    .show();
            return;
        }
        final BluetoothDevice hc06_ = hc06;
        new Thread(() -> {
            try {
                socket = hc06_.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                hc06OutputStream = socket.getOutputStream();
                if (hc06OutputStream == null) throw new IOException("No OutputStream found");
            }
            catch (IOException e) {
                e.printStackTrace();
                close();
                onFinishConnectionAttemptListener.onFinishConnectionAttempt(false);
                return;
            }
            onFinishConnectionAttemptListener.onFinishConnectionAttempt(true);
        }).start();
    }

    public static boolean attemptReconnection(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return false;
        BluetoothDevice hc06 = null;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals("HC-06")) {
                hc06 = device;
                break;
            }
        }
        if (hc06 == null) return false;
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                socket = hc06.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                hc06OutputStream = socket.getOutputStream();
                if (hc06OutputStream == null) throw new IOException("No OutputStream found");
            }
            catch (IOException e) {
                e.printStackTrace();
                close();
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Writes the 3 bytes to the HC-06 Bluetooth chip which write them to the Arduino's serial port.
     * The first byte is the header with all 1's apart from the last bit.
     * The last bit is 1 if the RC car is driving forwards and false otherwise.
     * The second byte is the angle to turn the servo to.
     * The third byte is the speed to set the motor to.
     * @return Whether or not the data was written successfully.
     *
     * @param bytes The byte[3] to send to the Arduino via the HC-06 Bluetooth chip.
     */
    public static boolean writeToArduino(byte[] bytes) {
        if (bytes.length != 3) throw new IllegalArgumentException("Must send 3 bytes");
        if (debugMode) return true;
        try {
            hc06OutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            close();
            return false;
        }
        return true;
    }

    public static void close() {
        if (debugMode) return;
        if (hc06OutputStream != null) {
            try {
                hc06OutputStream.close();
            } catch (IOException ignore) {}
            hc06OutputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignore) {}
            socket = null;
        }
    }
}
