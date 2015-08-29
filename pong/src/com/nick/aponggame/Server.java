/*****************************************************************************
 * Ankoor Shah
 * p2p pong, Server Class
 * Functions as a game host. Needed when inviting peers to connect and share
 * data.
 *
 * ORIGINAL STYLE CONVERTED FROM NICK'S. COMMENTS ARE LEFT UNTOUCHED.
 ****************************************************************************/
package com.nick.aponggame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Server
{
	// Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public Server(Context context, Handler handler)
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state)
    {
        if (D)
        {
            Log.d(TAG, "setState() " + mState + " -> " + state);
        }
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(GameModeActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        return;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState()
    {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start()
    {
        if (D)
        {
            Log.d(TAG, "start");
        }

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel(); mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null)
        {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }


        setState(STATE_LISTEN);
        return;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device)
    {
        if (D)
        {
            Log.d(TAG, "connect to: " + device);
        }

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null)
            {
                mConnectThread.cancel(); mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);


        return;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D)
        {
            Log.d(TAG, "connected");
        }

        // Cancel the thread that completed the connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel(); mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null)
        {
            mAcceptThread.cancel(); mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(GameModeActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(GameModeActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);


        setState(STATE_CONNECTED);
        return;
    }

    /**
     * Stop all threads
     */
    public synchronized void stop()
    {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null)
        {
            mConnectThread.cancel(); mConnectThread = null;
        }
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }
        if (mAcceptThread != null)
        {
            mAcceptThread.cancel(); mAcceptThread = null;
        }


        setState(STATE_NONE);
        return;
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out)
    {
    	System.out.println("writing to output!");
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }


        // Perform the write unsynchronized
        r.write(out);
        return;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed()
    {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(GameModeActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GameModeActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);


        return;
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost()
    {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(GameModeActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GameModeActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);


        return;
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread
    {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try
            {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run()
        {
            if (D)
            {
                Log.d(TAG, "BEGIN mAcceptThread" + this);
            }
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED)
            {
                try
                {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null)
                {
                    synchronized (Server.this)
                    {
                        switch (mState)
                        {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;

                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try
                                {
                                    socket.close();
                                }
                                catch (IOException e)
                                {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                            default: break;
                        }
                    }
                }
            }
            if (D)
            {
                Log.i(TAG, "END mAcceptThread");
            }


            return;
        }

        public void cancel()
        {
            if (D)
            {
                Log.d(TAG, "cancel " + this);
            }
            try
            {
                mmServerSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of server failed", e);
            }


            return;
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try
            {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run()
        {
            boolean connectFailed=false;
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException e)
            {
                connectionFailed();
                // Close the socket
                try
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                Server.this.start();
                connectFailed=true;
            }

            if(!connectFailed)
            {
                // Reset the ConnectThread because we're done
                synchronized (Server.this)
                {
                    mConnectThread = null;
                }

                // Start the connected thread
                connected(mmSocket, mmDevice);
            }


            return;
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e)
            {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            boolean dataRead=true;

            // Keep listening to the InputStream while connected
            while (dataRead)
            {
                try
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(GameModeActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    dataRead=false;
                }
            }


            return;
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(GameModeActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Exception during write", e);
            }


            return;
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }


            return;
        }
    }
}
