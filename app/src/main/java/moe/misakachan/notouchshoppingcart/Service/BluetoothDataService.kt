package moe.misakachan.notouchshoppingcart.Service

import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import moe.misakachan.notouchshoppingcart.MainActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BluetoothDataService : Service() {
    val handlerState = 0 //used to identify handler message
    var bluetoothIn: Handler? = null
    private var btAdapter: BluetoothAdapter? = null
    private var mConnectingThread: ConnectingThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var stopThread = false
    private val recDataString = StringBuilder()
    override fun onCreate() {
        super.onCreate()
        Log.d("BT SERVICE", "SERVICE CREATED")
        stopThread = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BT SERVICE", "SERVICE STARTED")
        bluetoothIn = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                Log.d("DEBUG", "handleMessage")
                if (msg.what == handlerState) { //if message is what we want
                    val readMessage =
                        msg.obj as String // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage)
                    Log.d("RECORDED", recDataString.toString())
                    // Do stuff here with your data, like adding it to the database
                }
                recDataString.delete(0, recDataString.length) //clear all string data
            }
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter() // get Bluetooth adapter
        checkBTState()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothIn!!.removeCallbacksAndMessages(null)
        stopThread = true
        if (mConnectedThread != null) {
            mConnectedThread!!.closeStreams()
        }
        if (mConnectingThread != null) {
            mConnectingThread!!.closeSocket()
        }
        Log.d("SERVICE", "onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private fun checkBTState() {
        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE")
            stopSelf()
        } else {
            if (btAdapter!!.isEnabled) {
                Log.d(
                    "DEBUG BT",
                    "BT ENABLED! BT ADDRESS : " + btAdapter!!.address + " , BT NAME : " + btAdapter!!.name
                )
                try {
                    val device =
                        btAdapter!!.getRemoteDevice(macAddress)
                    Log.d(
                        "DEBUG BT",
                        "ATTEMPTING TO CONNECT TO REMOTE DEVICE : $macAddress"
                    )
                    mConnectingThread = ConnectingThread(device)
                    mConnectingThread!!.start()
                } catch (e: IllegalArgumentException) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : $e")
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE")
                    stopSelf()
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE")
                stopSelf()
            }
        }
    }

    // New Class for Connecting Thread
    private inner class ConnectingThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice
        override fun run() {
            super.run()
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN")
            // Establish the Bluetooth socket connection.
// Cancelling discovery as it may slow down connection
            btAdapter!!.cancelDiscovery()
            try {
                mmSocket!!.connect()
                Log.d("DEBUG BT", "BT SOCKET CONNECTED")
                mConnectedThread = ConnectedThread(mmSocket)
                mConnectedThread!!.start()
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED")
                //I send a character when resuming.beginning transmission to check device is connected
//If it is not an exception will be thrown in the write method and finish() will be called
                mConnectedThread!!.write("x")
            } catch (e: IOException) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : $e")
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE")
                    mmSocket!!.close()
                    stopSelf()
                } catch (e2: IOException) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :$e2")
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE")
                    stopSelf()
                    //insert code to deal with this
                }
            } catch (e: IllegalStateException) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : $e")
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        fun closeSocket() {
            try { //Don't leave Bluetooth sockets open when leaving activity
                mmSocket!!.close()
            } catch (e2: IOException) { //insert code to deal with this
                Log.d("DEBUG BT", e2.toString())
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        init {
            Log.d("DEBUG BT", "IN CONNECTING THREAD")
            mmDevice = device
            var temp: BluetoothSocket? = null
            Log.d("DEBUG BT", "MAC ADDRESS : $macAddress")
            Log.d("DEBUG BT", "BT UUID : $BTMODULEUUID")
            try {
                temp =
                    mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID)
                Log.d("DEBUG BT", "SOCKET CREATED : $temp")
            } catch (e: IOException) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :$e")
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE")
                stopSelf()
            }
            mmSocket = temp
        }
    }

    // New Class for Connected Thread
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN")
            val buffer = ByteArray(256)
            var bytes: Int
            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = mmInStream!!.read(buffer) //read bytes from input buffer
                    val readMessage = String(buffer, 0, bytes)
                    Log.d("DEBUG BT PART", "CONNECTED THREAD $readMessage")
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn!!.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget()
                } catch (e: IOException) {
                    Log.d("DEBUG BT", e.toString())
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                    stopSelf()
                    break
                }
            }
        }

        //write method
        fun write(input: String) {
            val msgBuffer =
                input.toByteArray() //converts entered String into bytes
            try {
                mmOutStream!!.write(msgBuffer) //write bytes over BT connection via outstream
            } catch (e: IOException) { //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE $e")
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                stopSelf()
            }
        }

        fun closeStreams() {
            try { //Don't leave Bluetooth sockets open when leaving activity
                mmInStream!!.close()
                mmOutStream!!.close()
            } catch (e2: IOException) { //insert code to deal with this
                Log.d("DEBUG BT", e2.toString())
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        //creation of the connect thread
        init {
            Log.d("DEBUG BT", "IN CONNECTED THREAD")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try { //Create I/O streams for connection
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.d("DEBUG BT", e.toString())
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                stopSelf()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val remoteViews = RemoteViews(packageName, R.layout.notification_service)
        val builder: NotificationCompat.Builder
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "snwodeer_service_channel"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SnowDeer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            builder = NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            builder = NotificationCompat.Builder(this)
        }
        builder.setSmallIcon(R.mipmap.sym_def_app_icon)
            .setContent(remoteViews)
            .setContentIntent(pendingIntent)
        startForeground(1, builder.build())
    }

    companion object {
        // SPP UUID service - this should work for most devices
        private val BTMODULEUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // String for MAC address
    private var macAddress = "YOUR:MAC:ADDRESS:HERE"
        get() = field
        set(value) {
            field = value
        }
}